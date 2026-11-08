#!/usr/bin/env python3
"""Cheap static regression guard, *not* a proof of client visuals or PvP."""
from pathlib import Path
import re

root = Path(__file__).resolve().parents[3]
source = root / "src/main/java/com/eretenkodaniil/mkm"
read = lambda p: (source / p).read_text(encoding="utf-8")
client = read("client/CombatModeClient.java")
mixin = read("mixin/client/CombatModeAttackMixin.java")
service = read("combat/mode/CombatModeService.java")
events = read("combat/CombatEvents.java")
server_mixin = read("mixin/LivingEntityCombatMixin.java")
equipment = read("equipment/EquipmentSnapshots.java")
character = read("character/CharacterData.java")

assert "CombatModeInputRules.intercept" in client
assert "EntityHitResult" in client
assert "CombatModeStatus.TYPE" in client
assert "client.player.attack(" not in client
assert ".hurt(" not in client
assert "client.player.attack(" not in mixin
assert "method = \"startAttack\"" in mixin
assert service.count("player.attack(victim);") == 1
assert "player.attack(victim);" not in events
assert service.count("s.hitConfirmed = true;") == 1
assert "CombatModeService.onConfirmedDamage" in events
assert server_mixin.count('@ModifyVariable(method = "hurtServer"') == 1
assert "OriginModifierRules.snapshot" in equipment
assert "FencingSchoolModifierRules.combine" in equipment
assert "public static final int CURRENT_SCHEMA_VERSION = 4;" in character
assert "mkm.combat.experimental" in service
print("PASS: v4 preserved, only server calls native sword attack, MNAE/client cannot deal damage, gated entity-only LMB")
