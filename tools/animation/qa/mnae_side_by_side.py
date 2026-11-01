#!/usr/bin/env python3
"""Pure schematic old/new MNAE comparison, NOT actual Minecraft frames."""
import argparse,json,math
from pathlib import Path
from xml.sax.saxutils import escape
from biomechanics_audit import read_clip,pose as c3pose
ROOT=Path(__file__).resolve().parents[3]
T=[0,.18,5/14,.54,1]
def native(name):
    return json.loads((ROOT/"src/main/resources/assets/mkm/mnae"/(name+".json")).read_text())
def interpolate(keys,t):
    if t<=0:return keys[0]["r"]
    if t>=1:return keys[-1]["r"]
    for i in range(1,len(keys)):
        if t<=keys[i]["t"]:
            a,b=keys[i-1],keys[i]
            u=(t-a["t"])/(b["t"]-a["t"]);u=u*u*(3-2*u)
            return [a["r"][j]*(1-u)+b["r"][j]*u for j in range(3)]
    return keys[-1]["r"]
def figure(clip,t,is_native,x,y):
    d={k:interpolate(v,t) for k,v in clip["tracks"].items()} if is_native else c3pose(clip,t)
    def part(k):return d.get(k,[0,0,0])[0]
    bx=x+21*math.sin(part("body"));by=y
    sx=bx+31*math.sin(part("body"));sy=by-32*math.cos(part("body"))
    line=lambda x1,y1,x2,y2,col,w:f'<line x1="{x1:.1f}" y1="{y1:.1f}" x2="{x2:.1f}" y2="{y2:.1f}" stroke="{col}" stroke-width="{w}" stroke-linecap="round"/>'
    s=line(x-38,y+39,x+38,y+39,"#6d7788",1)+line(bx,by,sx,sy,"#e3e8ef",10)
    for bone,dx,color in [("right_leg",8,"#eea25f"),("left_leg",-8,"#64c2c5")]:
        a=part(bone);s+=line(bx+dx,by,bx+dx+29*math.sin(a),by+29*math.cos(a),color,7)
    for bone,dx,color in [("right_arm",10,"#eea25f"),("left_arm",-10,"#64c2c5")]:
        a=part(bone);s+=line(sx+dx,sy,sx+dx+27*math.sin(a),sy+27*math.cos(a),color,6)
    s+=f'<circle cx="{sx:.1f}" cy="{sy-12:.1f}" r="10" fill="#e3e8ef"/>'
    return s
def main():
    ap=argparse.ArgumentParser();ap.add_argument("--out",default="build/reports/animation-qa")
    a=ap.parse_args();out=Path(a.out);out.mkdir(parents=True,exist_ok=True)
    pairs=[("Sword strike","sword/light_1","sword_cut"),
           ("Unarmed punch","unarmed/jab","straight_punch")]
    w,h=1210,690;parts=[f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}">',
        '<rect width="100%" height="100%" fill="#15202b"/>',
        '<text x="18" y="30" fill="#f1f5f8" font-size="20">MKM C3.8 vs MNAE M1 / Schematic side-plane pose proxy</text>',
        '<text x="18" y="50" fill="#b8c5d4" font-size="12">NOT game render | 20 TPS | 14 ticks both | 5/14 server impact unchanged</text>']
    report=[]
    for group,(label,legacy,nat) in enumerate(pairs):
        old=read_clip(legacy);new=native(nat)
        assert old["duration_ticks"]==new["duration_ticks"]==14
        assert any(abs(e["t"]-5/14)<1e-6 for e in new["markers"] if e["name"]=="impact")
        report.append({"action":label,"legacy":"mkm:"+legacy,"native":new["id"],"duration_ticks":14,
                       "impact_normalized":5/14,"qualifier":"side-plane proxy only"})
        for row,(kind,clip,native_mode) in enumerate([("C3.8",old,False),("MNAE",new,True)]):
            y=78+group*298+row*143
            parts.append(f'<text x="18" y="{y+80}" fill="#e4e9ef" font-family="monospace">{escape(label)} {kind}</text>')
            for i,t in enumerate(T):
                x=190+i*200
                parts.append(f'<rect x="{x}" y="{y}" width="188" height="138" rx="6" fill="#253444"/>')
                parts.append(figure(clip,t,native_mode,x+94,y+92))
                parts.append(f'<text x="{x+10}" y="{y+15}" font-size="12" fill="#d5e0ed">t={t:.3f}</text>')
    parts.append('</svg>')
    (out/"mnae-m1-vs-c38-proxy.svg").write_text("\n".join(parts)+"\n")
    (out/"mnae-m1-comparison.json").write_text(json.dumps(report,indent=2)+"\n")
    print("MNAE M1 side-by-side proxy PASS, 2 clips")
if __name__=="__main__":main()
