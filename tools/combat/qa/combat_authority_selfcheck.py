#!/usr/bin/env python3
"""Fail-closed static architecture gate for MKM combat authority.

Not a replacement for multiplayer/in-world QA. This catches accidental
cosmetic-side damage calls and repeated LivingEntity.hurtServer mixin hooks.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[3]
JAVA = ROOT / "src/main/java/com/eretenkodaniil/mkm"
COSMETIC_FILES = (
    "client/animation/VanillaAttackPresentationFeed.java",
    "combat/presentation/AttackPresentationEvent.java",
    "combat/presentation/WeaponPresentationResolver.java",
    "combat/presentation/MnaeDemoClipRouting.java",
    "combat/ServerSpecialAttackContract.java",
)
FORBIDDEN_DAMAGE = re.compile(
    r"\.(?:attack|hurt|hurtServer|setHealth|setDeltaMovement)\s*\("
    r"|ServerPlayNetworking\.send\s*\("
    r"|\bnew\s+DamageSource\s*\("
)
failures = []
for relative in COSMETIC_FILES:
    file = JAVA / relative
    if not file.is_file():
        failures.append(f"Missing cosmetic contract: {relative}")
        continue
    content = file.read_text(encoding="utf-8")
    for hit in FORBIDDEN_DAMAGE.finditer(content):
        line = content.count("\n", 0, hit.start()) + 1
        failures.append(f"Cosmetic damage or network mutation: {relative}:{line} ({hit.group()})")
    if "import com.eretenkodaniil.mkm.combat.CombatEvents;" in content:
        failures.append(f"Cosmetic layer imported server damage hook: {relative}")

mixin_file = JAVA / "mixin/LivingEntityCombatMixin.java"
if not mixin_file.is_file():
    failures.append("Missing authoritative damage mixin")
else:
    text = mixin_file.read_text(encoding="utf-8")
    if text.count('@ModifyVariable(method = "hurtServer"') != 1:
        failures.append("Expected exactly one authoritative hurtServer modify hook")

mixins = list((JAVA / "mixin").rglob("*.java"))
other_hooks = [str(p.relative_to(ROOT)) for p in mixins if p != mixin_file
               and re.search(r'@ModifyVariable\s*\(\s*method\s*=\s*"hurtServer"', 
                             p.read_text(encoding="utf-8"))]
if other_hooks:
    failures.append(f"Additional hurtServer modify hooks: {other_hooks}")

if failures:
    for failure in failures:
        print("FAIL:", failure, file=sys.stderr)
    sys.exit(1)
print("PASS: MNAE presentation and future ability contracts contain no hit/damage calls; single damage hook")
