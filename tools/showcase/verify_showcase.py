#!/usr/bin/env python3
"""MKM RC1 fail-closed static gate. Real structure runtime is tested separately.
Generates a source-SHA-anchored diagnostics manifest; NOT proof of F5 acceptance.
"""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[2]
JAVA = ROOT / "src/main/java/com/eretenkodaniil/mkm"
ASSETS = ROOT / "src/main/resources/assets/mkm"
DATA = ROOT / "src/main/resources/data/mkm"
TOWER_BLOCKS = ("aged_ashlar", "mossy_ashlar", "cracked_ashlar",
                "charred_timber", "rusted_iron", "rune_slate")


def read(path):
    p = ROOT / path
    assert p.is_file(), "Missing file: " + str(path)
    return p.read_text(encoding="utf-8")


def exact_once(text, token):
    n = text.count(token)
    assert n == 1, f"Expected exactly one {token}, got {n}"


def verify():
    mod = read("src/main/java/com/eretenkodaniil/mkm/MkmMod.java")
    client = read("src/main/java/com/eretenkodaniil/mkm/client/MkmClient.java")
    for token in (
        "CharacterAttachments.initialize();",
        "CharacterOriginAttachments.initialize();",
        "EncounterAttachments.initialize();",
        "WatchtowerAttachments.initialize();",
        "MkmBlocks.initialize();",
        "MkmItems.initialize();",
        "EnemyService.initialize();",
        "RuneSlateInteraction.initialize();",
        "WatchtowerPlacementCommands.register(dispatcher)",
        "EncounterCommands.register(dispatcher)",
        "EnemyCommands.register(dispatcher)",
        "MkmPerformanceRuntime.initialize();",
        "CharacterEvents::onEntityDeath",
        "EncounterDirector::onDeath",
    ):
        exact_once(mod, token)
    assert mod.index("MkmItems.initialize();") < mod.index("MkmBlocks.initialize();")
    assert mod.index("MkmBlocks.initialize();") < mod.index("MkmCreativeTabs.initialize();")
    assert "CURRENT_SCHEMA_VERSION = 4" in read(
        "src/main/java/com/eretenkodaniil/mkm/character/CharacterData.java")
    assert "grantExperienceOnce(" in read(
        "src/main/java/com/eretenkodaniil/mkm/environment/RuneSlateInteraction.java")
    assert "WatchtowerAttachments.SITES" in read(
        "src/main/java/com/eretenkodaniil/mkm/environment/WatchtowerPlacementCommands.java")
    assert "siteRegistry.reserve" in read(
        "src/main/java/com/eretenkodaniil/mkm/environment/WatchtowerPlacementCommands.java")
    assert "verifiedRune" in read(
        "src/main/java/com/eretenkodaniil/mkm/environment/RuneSlateInteraction.java")
    assert "getAttached" in read(
        "src/main/java/com/eretenkodaniil/mkm/character/origin/CharacterOriginService.java")
    assert "HudElementRegistry.attachElementAfter" in client
    assert "MkmCombatHud::extract" in client and "MkmAbilityHud::extract" in client
    assert "MkmShowcaseHud::extract" in client
    assert "ShowcaseAccessibility.load();" in client
    for text in ("ShowcaseCommands.register(dispatcher)", "RuneSlateInteraction.initialize();"):
        exact_once(mod, text)

    # Global server payload receivers may be registered only once by TYPE.
    receivers = {}
    dangerous_imports = []
    for file in JAVA.rglob("*.java"):
        source = file.read_text(encoding="utf-8")
        if (not file.is_relative_to(JAVA / "client")
                and not file.is_relative_to(JAVA / "mixin/client")) and (
                "import net.minecraft.client." in source
                or "import com.eretenkodaniil.mkm.client." in source):
            dangerous_imports.append(file.relative_to(ROOT).as_posix())
        for payload in re.findall(
                r"ServerPlayNetworking\s*\.\s*registerGlobalReceiver\s*\(\s*([A-Za-z0-9_.]+)\.TYPE",
                source):
            receivers.setdefault(payload, []).append(file.relative_to(ROOT).as_posix())
    assert not dangerous_imports, "Client-only import leaked to server: " + str(dangerous_imports)
    assert receivers, "No C2S receivers found"
    dupes = {name: paths for name, paths in receivers.items() if len(paths) > 1}
    assert not dupes, "Duplicate C2S global receiver type: " + str(dupes)

    for key in ("en_us", "ru_ru"):
        translations = json.loads((ASSETS / "lang" / (key + ".json")).read_text("utf-8"))
        for name in TOWER_BLOCKS:
            assert "block.mkm." + name in translations
        assert "message.mkm.showcase.reward" in translations
        for text in ("key.mkm.showcase_hud", "key.mkm.showcase_contrast",
                     "key.mkm.showcase_vfx", "message.mkm.showcase.site"):
            assert text in translations
        assert "message.mkm.showcase.unverified" in translations
        assert "key.mkm.open_character_sheet" in translations
        assert "key.mkm.cast_renewal" in translations
    for name in TOWER_BLOCKS:
        for section in ("blockstates", "items", "models/block", "models/item",
                        "textures/block"):
            suffix = ".png" if section == "textures/block" else ".json"
            assert (ASSETS / section / (name + suffix)).is_file(), section + "/" + name
    templates = []
    for n in (1, 2, 3):
        path = DATA / "structure" / ("ruined_watchtower_0" + str(n) + ".nbt")
        assert path.stat().st_size > 15000
        templates.append({"id": "mkm:ruined_watchtower_0" + str(n),
                          "bytes": path.stat().st_size,
                          "sha256": hashlib.sha256(path.read_bytes()).hexdigest()})
    assert (DATA / "encounter/ford_ambush.json").is_file()
    definition = json.loads((DATA / "encounter/ford_ambush.json").read_text("utf-8"))
    assert definition["reward_xp"] == 100 and len(definition["roster"]) == 3
    loot = json.loads(read("src/main/resources/data/mkm/loot_table/chests/watchtower.json"))
    assert loot["type"] == "minecraft:chest" and loot["pools"]

    version = "0.0.7-fabric.0-rc.1"
    assert "mod_version=" + version in read("gradle.properties")
    for name in ("distribution/windows/package.py", "distribution/windows/Install_MKM.ps1",
                 ".github/workflows/build.yml"):
        assert version in read(name), "Version mismatch in " + name
    pack = read("distribution/windows/package.py")
    assert "SOURCE_SHA.txt" in pack and "SHA256SUMS.txt" in pack
    sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
    assert re.fullmatch(r"[0-9a-f]{40}", sha)

    return {"schema": 1, "source_commit": sha, "version": version,
            "base": "QA5 PR69 / 440dfe07f67d4cce1a8fc10f2bd62a5a2574e137",
            "native_watchtower_templates": templates,
            "original_blocks": list(TOWER_BLOCKS),
            "existing_systems": ["character_v4_receipts", "origins_human_dwarf_warrior_mage",
                                 "world_e2_1", "encounter_m1", "enemy_raider_stalker",
                                 "performance_m0", "combat_c3_7", "renewal_m1"],
            "new_systems": ["protected_tower_placement", "persistent_site_provenance",
                            "rune_interact_once_and_origin_class_effect"],
            "server_c2s_receivers": receivers,
            "confirmed_here": ["static content", "paths", "C2S symbol uniqueness",
                               "server import boundary", "version and registered entrypoints"],
            "not_confirmed_here": ["interactive GUI", "2-client reward",
                                   "reloading real player NBT", "Minecraft F5 screenshot",
                                   "TPS FPS or power-cut consistency"]}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path)
    args = parser.parse_args()
    info = verify()
    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps(info, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print("PASS: " + info["version"] + " @ " + info["source_commit"]
          + ", 3 native NBT + 6 materials + preserved v4/core registrars; C2S unique")
    print("NOT GAME QA: F5, 2 players, world migration and MSPT are unverified")


if __name__ == "__main__":
    main()
