#!/usr/bin/env python3
"""Generate an optional, ORIGINAL 64x LabPBR 1.3 Minecraft 26.3 resource pack.

No third-party textures, AI screenshots, forced shaderpacks or vanilla model
overrides. The shipped JAR continues to use original low-cost 16x assets.
"""
from __future__ import annotations

import argparse
import io
import json
import math
from pathlib import Path
import struct
import zipfile
import zlib

ROOT=Path(__file__).resolve().parents[2]
SOURCE=ROOT/"src/main/resources/assets/mkm/textures"
BLOCKS=("aged_ashlar","mossy_ashlar","cracked_ashlar","charred_timber",
        "rusted_iron","rune_slate","raider_war_mask")
ITEMS=("raider_insignia","stalker_fang")
SIZE=64


def read_rgba16(path: Path) -> list[tuple[int,int,int,int]]:
    data=path.read_bytes()
    assert data.startswith(bytes((137,80,78,71,13,10,26,10))),path
    at=8
    chunks=[]
    while at<len(data):
        n=struct.unpack(">I",data[at:at+4])[0]
        kind=data[at+4:at+8]
        payload=data[at+8:at+8+n]
        assert zlib.crc32(kind+payload)==struct.unpack(">I",data[at+8+n:at+12+n])[0]
        at+=12+n
        if kind==b"IHDR":
            assert struct.unpack(">IIBBBBB",payload)==(16,16,8,6,0,0,0)
        if kind==b"IDAT": chunks.append(payload)
        if kind==b"IEND": break
    raw=zlib.decompress(b"".join(chunks))
    assert len(raw)==16*(1+16*4)
    out=[]
    for y in range(16):
        row=raw[y*65:(y+1)*65]
        assert row[0]==0,"Input PNG uses unsupported filter; make renderer explicit"
        out += [tuple(row[1+i*4:1+i*4+4]) for i in range(16)]
    return out


def png64(pixels: list[tuple[int,int,int,int]]) -> bytes:
    assert len(pixels)==SIZE*SIZE
    raw=bytearray()
    for y in range(SIZE):
        raw.append(0)
        for rgba in pixels[y*SIZE:(y+1)*SIZE]:
            raw.extend(rgba)
    def chunk(name,body):
        return struct.pack(">I",len(body))+name+body+struct.pack(">I",zlib.crc32(name+body))
    return bytes((137,80,78,71,13,10,26,10))+chunk(b"IHDR",struct.pack(
        ">IIBBBBB",SIZE,SIZE,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(raw,9))+chunk(b"IEND",b"")


def hash_noise(x:int,y:int,salt:int) -> int:
    n=(x*374761393+y*668265263+salt*2246822519)&0xffffffff
    n=((n^(n>>13))*1274126177)&0xffffffff
    return ((n^(n>>16))%21)-10


def albedo(original,salt):
    result=[]
    for y in range(SIZE):
        for x in range(SIZE):
            pixel=original[(y//4)*16+x//4]
            # Fine 64px surface granularity, still respectful of 16px massing.
            variation=hash_noise(x,y,salt)*0.65
            if (x%4)==0 or (y%4)==0:
                variation-=1.4
            rgb=tuple(max(0,min(255,int(c+variation))) for c in pixel[:3])
            result.append((*rgb,pixel[3]))
    return result


def pbr(albedo_pixels,name):
    light=[]
    for r,g,b,a in albedo_pixels:
        light.append((r*0.2126+g*0.7152+b*0.0722)/255.0)
    normal=[]
    spec=[]
    metal=name=="rusted_iron"
    rune=name=="rune_slate"
    wood=name=="charred_timber"
    mask=name=="raider_war_mask"
    for y in range(SIZE):
        for x in range(SIZE):
            idx=y*SIZE+x
            r,g,b,a=albedo_pixels[idx]
            lx=light[y*SIZE+((x-1)%SIZE)]-light[y*SIZE+((x+1)%SIZE)]
            ly=light[((y+1)%SIZE)*SIZE+x]-light[((y-1)%SIZE)*SIZE+x]
            nx=max(-0.4,min(0.4,lx*0.55))
            ny=max(-0.4,min(0.4,ly*0.55))
            normal.append((int(128+nx*127),int(128+ny*127),226, # AO
                           max(10,min(245,int(126+light[idx]*70)))))
            if metal:
                spec.append((120,230,0,255))   # labPBR material id 230: iron
            elif rune and g>120 and g>r*1.15:
                spec.append((38,14,14,174))  # subtle emissive rune (optional shader)
            else:
                smooth=20 if wood else 30 if mask else 35
                spec.append((smooth,12,24 if not wood else 10,255))
    return normal,spec


def create_pack(destination:Path):
    destination.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(destination,"w",compression=zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
        meta={"pack":{"description":"MKM Cinematic 64: optional original LabPBR 1.3 materials (26.3)",
                      "min_format":[97,1],"max_format":[97,1]}}
        archive.writestr("pack.mcmeta",json.dumps(meta,ensure_ascii=False,indent=2)+"\n")
        # Iris supports metadata to identify LabPBR materials, but a compatible
        # shaderpack has to be installed separately by the player.
        archive.writestr("assets/minecraft/optifine/texture.properties",
                         "format=lab-pbr/1.3\n")
        for i,name in enumerate(BLOCKS):
            rgba=albedo(read_rgba16(SOURCE/"block"/(name+".png")),i+13)
            n,s=pbr(rgba,name)
            path="assets/mkm/textures/block/"
            archive.writestr(path+name+".png",png64(rgba))
            archive.writestr(path+name+"_n.png",png64(n))
            archive.writestr(path+name+"_s.png",png64(s))
        for i,name in enumerate(ITEMS):
            pixels=albedo(read_rgba16(SOURCE/"item"/(name+".png")),i+47)
            archive.writestr("assets/mkm/textures/item/"+name+".png",png64(pixels))
    with zipfile.ZipFile(destination) as archive:
        names=archive.namelist()
        assert len(names)==1+1+len(BLOCKS)*3+len(ITEMS)
        assert len(names)==len(set(names))
        meta=json.loads(archive.read("pack.mcmeta"))
        assert meta["pack"]["min_format"]==[97,1]
        for name in BLOCKS:
            for suffix in ("","_n","_s"):
                path=f"assets/mkm/textures/block/{name}{suffix}.png"
                data=archive.read(path)
                assert data[:8]==bytes((137,80,78,71,13,10,26,10))
                assert struct.unpack(">II",data[16:24])==(64,64)
    print(f"PASS: {destination} ({destination.stat().st_size} bytes), {len(BLOCKS)} LabPBR materials with original 64x color/normal/spec maps + {len(ITEMS)} item textures; no core shader overrides")
    print("UNVERIFIED: look/format interpretation with installed Iris + exact shaderpack, FPS/VRAM on target AMD iGPU")


if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--out",type=Path,default=ROOT/"build/environment_packs/MKM_Cinematic_64_LabPBR_26.3.zip")
    arguments=parser.parse_args()
    create_pack(arguments.out)
