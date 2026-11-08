# MKM SHOWCASE-01 — Workstream Assignment Index (2026-11-08)

**User request:** substantial 90–180-minute engineering packets focused on visible Minecraft outcomes, not architecture-only PRs. **Current base:** QA5 PR #69, not legacy NeoForge main. **Approved S20-04:** ALL A+B+C accessibility. [Master Epic #71](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/71).

Authoritative details:
- [ALL 115 user requirements](MKM_GDD_USER_DECISIONS_115.md)
- [Full execution prompts for all 11 teams](MKM_SHOWCASE_01_EXECUTION_ORDERS.md)

## Ticket for each workstream

| # | Send to chat / owner | Ticket | Playable result |
|---:|---|---|---|
| 1 | «проверяющий» | [#72](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/72) | Combined v4 RC with visible gameplay, 2-player/relog and checksums |
| 2 | «анимки» | [#73](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/73) | Real 17-bone R07 in F5 with sword grip and video proof |
| 3 | «бой» | [#74](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/74) | Opt-in delayed combat LMB, quick/charged and real fencing |
| 4 | «перс и прогрессия» | [#75](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/75) | Human/Dwarf × Warrior/Mage changes actions and NPC response |
| 5 | «магия» | [#76](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/76) | Renewal + one new executable, class-aware v4 visual spell |
| 6 | «враги и AI» | [#77](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/77) | Raider/Stalker visually/tactically distinct; 40-mob measurements |
| 7 | «мир» | [#78](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/78) | Ilra → discover tower → encounter → saved branching outcome |
| 8 | «визуал и структуры» | [#79](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/79) | Rendered/placed original watchtower, materials, VFX, enemy silhouettes |
| 9 | «производительность» / QA | [#80](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/80) | Spark p95/p99/1%-low and measured safe optimization |
| 10 | «интерфейс и доступность» / Character+Visual | [#81](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/81) | Usable RPG HUD and A+B+C UI/motion/keys/controller |
| 11 | Gemini reviewer | [#82](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/82) | Independent numerical design falsification |

## Contract
QA5 does NOT include Magic #58, Environment #60, Combat Fencing #65 or MNAE R07 #63. No merge to historical NeoForge main. Every PR must show SHA, base/branch, schema safety, client/server tests, real in-game difference and limitations. Integrator owns conflicts in MkmMod/MkmClient/EquipmentSnapshots. Cosmetic visual markers never cause damage/XP.

**Chat routing:** Give each working chat its ticket + the complete execution document; issue creation does not automatically initiate other conversations.