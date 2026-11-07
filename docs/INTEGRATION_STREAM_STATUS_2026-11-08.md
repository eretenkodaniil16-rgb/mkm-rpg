# MKM RPG cross-stream integration status — 2026-11-08

**Source of truth:** GitHub PR heads and Java 25 CI. This document reports a review and proposed merge sequence, **not** an already built omnibus JAR. Historic NeoForge `main` is out of scope.

## Available but isolated work

| Area | Source | Exists in source PR | Compatibility/acceptance boundary |
|---|---|---|---|
| v4 integration | #45 → #62 → #64 (current base) | v3→v4 migration, World E2.1, canonical weapons, passive perf diagnostics | Needs real saved-world and two-player QA |
| races/classes | #55 | `mkm:character_origin` data, Human/Dwarf × Warrior/Mage, K UI | Independent origin attachment; needs client/MkmMod integration |
| enemy AI | #56 | Raider/Husk and Stalker/Spider, phase AI, loot and GM test commands | Needs coherent death XP with #54/#45 |
| encounters | #54 | Night ambush 2 Husks + 1 Skeleton with persisted roster/receipt | Independent of #56 AI hosts; unify identity/lifecycle |
| structures | #60 | 6 vanilla+ textures/blocks + NBT tower variants + controlled placement | No natural generation; real-world placement untested |
| physical combat | #59→#65 | LivingEntity combat contract, 3 fencing-school states | Avoid duplicate `EquipmentSnapshots` contributions and animation claims |
| magic | #58 | Unified actor spells, tier I–X, schools and +1 mana/level cap 300 | Based on older CharacterData **v3**; C2S protocol migration needed |
| player animation | #63 | 17-bone Blender R07 experimental F9 avatar cut | Based on CharacterData **v2**; no unconditional armor/offhand/first-person |
| performance | #64 | /mkmperf diagnostics, safe pure-worker utility (inactive) | No measured FPS/MSPT speedup yet |

Individual CI for PR #54/#55/#56/#58/#59/#60/#62/#63/#64/#65 succeeded on their reviewed heads. **This does NOT establish combined compile or game safety.**

## Known same-file overlapping changes

- `MkmMod.java`: PR #54, #55, #56, #58, #59, #60, #64.
- `EquipmentSnapshots.java`: #55, #59, #65 (note #65 is stacked on #59).
- `MkmClient.java`: #55, #58, #63.
- `lang/en_us.json`, `ru_ru.json`: #55, #56, #60.
- `.github/workflows/build.yml`: #54, #55, #58, #59, #64, #65.
- Windows packaging & mod version: #55, #63, #64.

Do not hide conflicts with `git merge -X ours` or `-X theirs`. In particular, do not downgrade `CharacterData` from v4 (base) to v2/v3 (R07/Magic). Require one server-owned XP receipt and one canonical item family resolver.

## Rehearsal and gate

`tools/integration/rehearse_crossstream.py` attempts a merge of selected branches **in GitHub Actions temporary checkout only**, records conflicts and uploads report. No git push and no user-facing merged JAR is produced by this workflow. The script is diagnostic; per-file and behavioral reconciliation still requires active engineering.

Once there is a conflict-free integrated source: run full Java25 Gradle/JUnit, server and client smoke, two-player quest/reward, migration backup, 17-bone F5 art review, 20/40/80 mobs MSPT, and actual structure placement. Only then create a uniquely versioned integrated QA JAR, strictly separated from previous 0.0.6-fabric.0-qa.2.

## Desired user answers

The coordinating chat has produced an offline interactive product survey with **115 questions and exactly three implementation examples each**, grouped in 23 areas. Answers should be captured separately from GitHub code until the player approves the new requirements. Avoid re-asking existing constraints: Fabric/Java25, 7 stats, 100 levels, native MNAE and server authority.
