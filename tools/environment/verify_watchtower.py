#!/usr/bin/env python3
"""Dependency-free, deterministic static QA for the MKM 26.3 Environment V1 pack.

Not a substitute for starting Minecraft; verifies gzip/NBT, sprite CRC, models,
block registry source, loot pointers, three damage variants and protected rollout.
"""
from __future__ import annotations

import gzip
import json
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/mkm"
DATA = RES / "data/mkm"
BLOCKS = (
    "aged_ashlar", "mossy_ashlar", "cracked_ashlar",
    "charred_timber", "rusted_iron", "rune_slate",
)


class Nbt:
    def __init__(self, buf: bytes):
        self.buf = memoryview(buf)
        self.i = 0

    def read(self, n: int) -> bytes:
        result = self.buf[self.i:self.i + n].tobytes()
        if len(result) != n:
            raise AssertionError("Truncated NBT")
        self.i += n
        return result

    def number(self, fmt: str):
        return struct.unpack(">" + fmt, self.read(struct.calcsize(">" + fmt)))[0]

    def name(self):
        return self.read(self.number("H")).decode("utf-8")

    def value(self, tag: int):
        if tag == 1:
            return self.number("b")
        if tag == 2:
            return self.number("h")
        if tag == 3:
            return self.number("i")
        if tag == 4:
            return self.number("q")
        if tag == 8:
            return self.name()
        if tag == 9:
            element, count = self.number("B"), self.number("i")
            assert 0 <= count <= 100000
            return [self.value(element) for _ in range(count)]
        if tag == 10:
            entries = {}
            while True:
                child = self.number("B")
                if child == 0:
                    return entries
                key = self.name()
                assert key not in entries
                entries[key] = self.value(child)
        raise AssertionError("Unsupported tag " + str(tag))

    def root(self):
        assert self.number("B") == 10
        assert self.name() == ""
        value = self.value(10)
        assert self.i == len(self.buf), "Trailing NBT"
        return value


def read_png(path: pathlib.Path):
    data = path.read_bytes()
    assert data.startswith(bytes((137, 80, 78, 71, 13, 10, 26, 10))), path
    cursor = 8
    idat = bytearray()
    while cursor < len(data):
        n = struct.unpack(">I", data[cursor:cursor + 4])[0]
        kind = data[cursor + 4:cursor + 8]
        payload = data[cursor + 8:cursor + 8 + n]
        crc_found = struct.unpack(">I", data[cursor + 8 + n:cursor + 12 + n])[0]
        assert zlib.crc32(kind + payload) == crc_found, (path, kind)
        cursor += 12 + n
        if kind == b"IHDR":
            assert struct.unpack(">IIBBBBB", payload) == (16, 16, 8, 6, 0, 0, 0)
        if kind == b"IDAT":
            idat.extend(payload)
        if kind == b"IEND":
            break
    assert cursor == len(data)
    rgba = zlib.decompress(bytes(idat))
    assert len(rgba) == 16 * (16 * 4 + 1), path
    for row in range(16):
        assert rgba[row * 65] == 0, path
        assert all(rgba[row * 65 + 4 + x * 4] == 255 for x in range(16)), path


def main():
    entries = []
    signatures = []
    for idx in (1, 2, 3):
        path = DATA / "structure" / f"ruined_watchtower_0{idx}.nbt"
        doc = Nbt(gzip.decompress(path.read_bytes())).root()
        assert doc["DataVersion"] == 5023
        assert doc["size"] == [23, 14, 19], doc["size"]
        assert doc["entities"] == [], "No auto-spawn NPC/enemies from template"
        palette = doc["palette"]
        assert all("id" in entry and "Name" not in entry for entry in palette), "Legacy block-state palette is invalid for 26.3"
        assert all("Properties" not in entry for entry in palette), "Block properties must be lowercase in 26.3"
        blocks = doc["blocks"]
        assert len(blocks) >= 450, len(blocks)
        pos = {}
        for b in blocks:
            x, y, z = b["pos"]
            assert 0 <= x < 23 and 0 <= y < 14 and 0 <= z < 19
            key = (x, y, z)
            assert key not in pos, key
            assert 0 <= b["state"] < len(palette)
            pos[key] = (palette[b["state"]]["id"], b.get("nbt"))
        assert all(n != "minecraft:air" for n, _ in pos.values())
        assert all((x, y, 12) not in pos for x in (8, 9) for y in (5, 6, 7)), "Door obstructed"
        assert all((10, y, 8) not in pos for y in (12, 13)), "Elite headspace"
        assert pos[(10, 11, 8)][0] in ("mkm:charred_timber", "mkm:aged_ashlar")
        assert pos[(11, 5, 5)][0] == "minecraft:barrel"
        assert pos[(11, 5, 5)][1]["LootTable"] == "mkm:chests/watchtower"
        assert pos[(7, 5, 6)][0] == "mkm:rune_slate"
        assert pos[(18, 5, 14)][0] == "minecraft:campfire"
        assert all(pos[(5, y, 7)][0] == "minecraft:ladder" for y in range(5, 12))
        assert all(pos[(4, y, 7)][0] == "mkm:aged_ashlar" for y in range(5, 12)), "Missing west ladder anchor"
        assert all((x, 5, 11) not in pos for x in (8, 9)), "Tower approach corridor blocked"
        for x in range(12, 18):
            for z in (10, 11):
                assert (x, 4, z) in pos, "Tower-camp bridge has a hole"
                assert all((x, y, z) not in pos for y in range(5, 8)), "Bridge headroom obstructed"
        assert all(pos[(14, y, 10)][0] == "mkm:charred_timber" for y in range(0, 4))
        assert all(pos[(15, y, 11)][0] == "mkm:charred_timber" for y in range(0, 4))
        names = {name for name, _ in pos.values()}
        assert set("mkm:" + b for b in BLOCKS).issubset(names)
        entries.append(len(blocks))
        signatures.append(frozenset(pos.keys()))
    assert len(set(signatures)) == 3, "Damage variants accidentally identical"
    for b in BLOCKS:
        read_png(ASSETS / "textures/block" / (b + ".png"))
        blockstate = json.loads((ASSETS / "blockstates" / (b + ".json")).read_text("utf-8"))
        model = json.loads((ASSETS / "models/block" / (b + ".json")).read_text("utf-8"))
        item = json.loads((ASSETS / "items" / (b + ".json")).read_text("utf-8"))
        item_model = json.loads((ASSETS / "models/item" / (b + ".json")).read_text("utf-8"))
        assert blockstate["variants"][""]["model"] == "mkm:block/" + b
        assert model["parent"] == "minecraft:block/cube_all"
        assert model["textures"]["all"] == "mkm:block/" + b
        assert item["model"]["model"] == "mkm:item/" + b
        assert item_model["parent"] == "mkm:block/" + b
        assert (ASSETS / "textures/block" / (b + ".png")).is_file()
        for lang in ("en_us", "ru_ru"):
            assert "block.mkm." + b in json.loads((ASSETS / "lang" / (lang + ".json")).read_text("utf-8"))
    loot = json.loads((DATA / "loot_table/chests/watchtower.json").read_text("utf-8"))
    assert loot["type"] == "minecraft:chest" and len(loot["pools"]) >= 1
    assert json.loads((DATA / "worldgen/processor_list/watchtower_none.json").read_text()) == {"processors": []}
    assert not list(DATA.rglob("worldgen/structure_set/*.json"))
    assert not list(DATA.rglob("worldgen/template_pool/*.json"))
    java = (ROOT / "src/main/java/com/eretenkodaniil/mkm/registry/MkmBlocks.java").read_text()
    assert all('"' + b + '"' in java for b in BLOCKS)
    effect = (ROOT / "src/main/java/com/eretenkodaniil/mkm/environment/RuneSlateBlock.java").read_text()
    assert "animateTick" in effect and "ParticleTypes.END_ROD" in effect
    print(f"PASS: 6 original 16x16 RGBA PNG + referenced models + lang; native 26.3 NBT blocks={entries}; loot + rune cue; auto-worldgen absent")
    print("UNVERIFIED: Java compile/runtime, world placement, shader compatibility, FPS/MSPT, build-protection guarantees")


if __name__ == "__main__":
    main()
