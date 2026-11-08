#!/usr/bin/env python3
"""Original visual resources and client/server contract static verifier, stdlib only.

Never replaces Gradle, actual Minecraft loading or a real screenshot.
"""
from __future__ import annotations

import json
import pathlib
import struct
import zlib

ROOT = pathlib.Path(__file__).resolve().parents[2]
RES = ROOT / "src/main/resources/assets/mkm"
JAVA = ROOT / "src/main/java/com/eretenkodaniil/mkm"


def inspect_png(path: pathlib.Path, transparent: bool) -> None:
    data = path.read_bytes()
    assert data[:8] == bytes((137,80,78,71,13,10,26,10)), path
    offset = 8
    image = bytearray()
    ihdr = False
    while offset < len(data):
        length = struct.unpack(">I", data[offset:offset+4])[0]
        name = data[offset+4:offset+8]
        payload = data[offset+8:offset+8+length]
        crc = struct.unpack(">I", data[offset+8+length:offset+12+length])[0]
        assert zlib.crc32(name + payload) == crc, path
        offset += length + 12
        if name == b"IHDR":
            assert struct.unpack(">IIBBBBB", payload) == (16,16,8,6,0,0,0), path
            ihdr = True
        if name == b"IDAT":
            image.extend(payload)
        if name == b"IEND":
            break
    assert offset == len(data) and ihdr, path
    raw = zlib.decompress(image)
    assert len(raw) == 16 * (1 + 16 * 4)
    alpha = []
    for y in range(16):
        assert raw[y*65] == 0
        for x in range(16):
            alpha.append(raw[y*65 + 4 + 4*x])
    assert any(a == 255 for a in alpha)
    if transparent: assert any(a == 0 for a in alpha), path
    else: assert all(a == 255 for a in alpha), path


def main():
    for block in ("aged_ashlar","mossy_ashlar","cracked_ashlar",
                  "charred_timber","rusted_iron","rune_slate","raider_war_mask"):
        inspect_png(RES / "textures/block" / (block+".png"), False)
        state = json.loads((RES/"blockstates"/(block+".json")).read_text())
        model = json.loads((RES/"models/block"/(block+".json")).read_text())
        item_model = json.loads((RES/"models/item"/(block+".json")).read_text())
        disp = json.loads((RES/"items"/(block+".json")).read_text())
        assert state["variants"][""]["model"] == "mkm:block/"+block
        assert model["parent"] == "minecraft:block/cube_all"
        assert model["textures"]["all"] == "mkm:block/"+block
        assert item_model["parent"] == "mkm:block/"+block
        assert disp["model"]["model"] == "mkm:item/"+block
    for item in ("raider_insignia","stalker_fang"):
        inspect_png(RES/"textures/item"/(item+".png"), True)
        disp = json.loads((RES/"items"/(item+".json")).read_text())
        model = json.loads((RES/"models/item"/(item+".json")).read_text())
        assert disp["model"]["model"] == "mkm:item/"+item
        assert model["parent"] == "minecraft:item/generated"
        assert model["textures"]["layer0"] == "mkm:item/"+item
    for lang in ("en_us","ru_ru"):
        data=json.loads((RES/"lang"/(lang+".json")).read_text(encoding="utf-8"))
        assert len(data)>110,"QA5 locale keys were overwritten"
        for key in ("block.mkm.raider_war_mask",
                    "key.mkm.environment.profile",
                    "key.mkm.environment.reduced_effects",
                    "item.mkm.raider_insignia","item.mkm.stalker_fang"):
            assert key in data, (lang,key)
    mod=(JAVA/"MkmMod.java").read_text()
    assert all(x in mod for x in (
        "CharacterOriginAttachments.initialize();",
        "EncounterDefinitions.initialize();",
        "EnemyService.initialize();",
        "MkmPerformanceRuntime.initialize();",
        "MkmBlocks.initialize();",
        "EnvironmentVisualNetworking.initialize();",
        "EnvironmentRuneInteraction.initialize();",
        "WatchtowerPlacementCommands.register(dispatcher)"))
    client=(JAVA/"client/MkmClient.java").read_text()
    assert "EnvironmentVisualSettings.load(" in client
    assert "EnvironmentRuneEvent.TYPE" in client
    assert "EnvironmentVisualEvent.TYPE" in client
    visual=(JAVA/"environment/net/EnvironmentVisualNetworking.java").read_text()
    assert "ServerPlayNetworking.canSend" in visual
    assert "ServerPlayNetworking.send" in visual
    assert "clientboundPlay()" in visual
    assert "player.distanceToSqr(mob) <= 32.0D * 32.0D" in visual
    rule=(JAVA/"environment/EnvironmentVfxBudget.java").read_text()
    assert "Math.floorMod(tick + entityId, 40)" in rule
    assert "reduced" in rule
    assert not list((ROOT/"src/main/resources/data/mkm/worldgen").rglob("structure_set/*.json"))
    print("PASS: original 7 block PNGs + 2 transparent item icons, 26.3 models, QA5 registries, bounded S2C visuals, locale, no auto worldgen")
    print("NOT TESTED BY STATIC CHECK: actual helmet skin render, in-client screenshot, shaderpack FPS, right-click/sound interaction")


if __name__ == "__main__":
    main()
