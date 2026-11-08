# MKM RPG — user vision handoff (115 of 115 answers, 2026-11-08)

**Status: DESIGN INTENT, NOT SHIPPED GAMEPLAY OR APPROVED TECHNICAL IMPLEMENTATION.** Source: player-completed 23-section, 115-question MKM questionnaire (2026-11-08 02:45:37). **115 answers recorded; all questionnaire items answered.** The additional choice S20-04 is **A+B+C simultaneously** (accessibility across readability, motion/flash control, and controls). 29 user free-text clarifications, including 6 custom D responses. **All section priority ratings are unset.** This is an operational handoff summary; the original completed survey and line-by-line answer register are retained in the coordinating conversation, not reproduced in full here. Do not invent missing answers or treat assistant technical recommendations as explicit approvals.

## User goals, by domain

1. **RPG tone, scope (S01):** exploration, quests and combat in balance, Dark Fantasy, extensive RPG transformation of vanilla survival, discoveries via books/NPCs, per-world rules of death/difficulty.
2. **Exploration (S02):** content across all vanilla biomes/dimensions with selective replacement and extension; dynamic region threat, discovered-world map, patrols and bounded unpredictable encounters. Several composite A+C answers.
3. **Structures (S03):** rare large handmade sites, hybrid authored/procedural dungeons, persistent consequences, multiple interactive mechanisms (custom 'all' response).
4. **Races (S04):** **strongly meaningful** mechanically, narratively and visually; unique MKM cultures, race-specific rigs/armor, rare conditional evolutions, culture quest/trait trees. Current PR #55 Human/Dwarf stat modifiers are ONLY a first prototype.
5. **Classes (S05):** hybrid restricted classes with faction-linked specializations, class resources, limited second-class / cross-class traits after quests, mentor-based progression and possible respec.
6. **Progression (S06):** fast early levels/slower endgame to lvl100, actions and quests combined for advancement, several level-up reward types, limited respec, diversified XP.
7. **Combat (S07):** requested **server-authoritative delayed-impact ordinary LMB** in combat mode, quick/charged hold-release, 3–5 hotbar techniques, stamina on dodge/guard/heavy, constrained blade-arc collision. **Vanilla mining and normal gameplay must remain correct.**
8. **Fencing (S08):** substantive historical-school-inspired gameplay, manual selection of three frozen R06 poses, parries/counters/ripostes, meaningful stagger/knockdown and visually fast DMC-like combos.
9. **Equipment (S09):** 6–8 weapon families, distinct physical combat effects, some bounded procedural/modular equipment without upkeep overload; vanilla durability remains, advanced armor/cloth cosmetics later.
10. **Magic (S10):** circles I–X unlocked via level **plus** rare knowledge/research, distinct or original schools (exact catalog unconfirmed: S10-02='unique'), WIL/INT + level softcap mana, limited access outside magic specialist.
11. **Abilities (S11):** casting motion varies by school, interruptibility and mana costs, server-owned projectiles/AOE/telegraphs, shared actor effect for enemy AI, spellbook + quick slots.
12. **Enemies (S12):** 30–60 **gameplay-distinct** kinds, bespoke monsters/hunters/constructs, squad roles and advanced local sensors for lighting, stealth, trace and allies, bounded phased tactical AI. Unique variety does NOT require 60 independent core frameworks.
13. **Bosses (S13):** multi-phase cinematic arena bosses with controlled block destruction, faction pursuers, clear pose+ground+sound telegraph, 2-player adaptation, distinct one-time/repeatable/seasonal boss policies.
14. **Quests (S14):** open narrative sandbox with optional local plotlines, contextual branches by race/class/stats/reputation/items, world consequences and investigative journal.
15. **Factions (S15):** multi-faction diplomacy, settlement economy/social life, small recruitable squads, occasional territory conflicts; NO expensive unloaded-chunk simulated entity ticking.
16. **Loot/crafting (S16):** bounded procedural and rare authored rewards, regional materials, upgrading station, trade routes/dynamic prices, legendary uniqueness through boss/quest logic.
17. **MNAE (S17):** own 17–26-joint articulated rigs, intense acrobatic choreographies, forward roll + lateral evasive step + backward retreat, near-vanilla first-person arms, real observer/network animation state.
18. **Graphics (S18):** optional high-end 64x+ PBR and complex 3D geometry with Iris, localized volumetric/distortion/weather effects; THREE presets: economy, standard, cinematic. Base mod must not require shaders.
19. **Audio (S19):** regional and combat dynamic music, mostly text/dialogue/no voice acting, impact sounds react to swing direction/speed, vanilla creature sounds kept, separate MKM audio settings.
20. **UI (S20):** RPG character model/skills/styling, HUD customization and hybrid hotbar/controller, intentionally few handholding hints. `S20-04` is **A+B+C combined, explicitly selected**: scalable UI/text/high-contrast/colour aids; configurable camera shake, flashes and motion; remappable keys, assisted input and controller support. Implement incrementally but do not drop any branch. Section-wide prioritization remains unset.
21. **Coop (S21):** solo + validated two-player cooperative as first target, common encounters but personal rewards, PvE priority over PvP, physical friendly-fire for abilities, synchronized observer MNAE clip state.
22. **Performance (S22):** base MKM should run **without mandatory extra optimizer mods**; target 40 diverse active enemies, explore more aggressive parallel pure processing with serious testing, hardware baseline Ryzen7 5800U/16GB RAM, suggest but never auto-install optimizer profiles.
23. **Release (S23):** integrated feature-complete QA builds rather than scattered public releases, **new disposable worlds** for incompatible branches, automated Java25/client/server/video/2-player/save/load/performance matrix, Windows official Minecraft Launcher, weekly integration governance.

## Critical engineering changes relative to current QA5

- **Current integrated QA5:** PR #69 = CharacterData v4, human/dwarf x warrior/mage starter mechanics, World E2.1 + Encounter M1, 2 enemy prototypes, Performance M0 diagnostics. It **does NOT ship** Magic M4 #58, Watchtower #60, Fencing #65, Articulated MNAE R07 #63.
- **P0 correctness:** v3->v4 backup + receipts, single server damage/XP authority, stable actor/weapon/ability contract, two-player lifecycle checks, exact-build CI.
- **P1 gameplay vertical slices:** opt-in delayed LMB and one R07 in-game articulated sword swing on modern v4, meaningful race/quest feature and class-specific action, one extra AI/sensing role, one encounter + discoverable structure, Magic M4 v4 adapter.
- **P2 scale:** major enemy roster, specialized schools/classes, dungeons/boss/factions/economy/audio/PBR, based on the successful prototypes and profiling.

## Architectural tension / acceptance gates

- `S07-01` delayed LMB vs vanilla breaking: require separate mode with mining fallback; no client-side hit/damage or duplicate server attack.
- `S04` meaningful races vs present PR #55 only stat deltas: race-specific story/mechanics needed; altered hitboxes and armor remain separate security/visual problems.
- `S12-01` 30–60 distinct creatures vs tick budget: shared low-level identity/AI framework with behavior modules; reject 60 naive per-tick global scans.
- `S18-01` high-res PBR vs `S22-04` Ryzen iGPU: high-end packs optional, default low-overhead 16×/vanilla+.
- `S15-05` no expensive unloaded simulation vs living war/economy: persist abstract events, instantiate full AI only near player.
- `S21-04` friendly-fire vs 2-player: server-authoritative target and team rules, no self-inflicted duplicate events.
- `S10-02` ‘unique’ schools not enumerated, do not invent a definitive school catalog without design approval.
- `S20-04` is resolved as **all A+B+C**. Accessibility is an acceptance criterion: UI scale/contrast, independent camera-shake/flash reduction, full MKM key rebinding and controller input mapping; test settings persistence and client-only behaviour. Controls must not bypass server attack/ability authority.

## Workstream ownership

- **Integrator/QA:** PR #69 baseline, server authority/backup/CI and future safe combined JAR.
- **Character:** deep race traits, cultural quests, class resources & safe versioned progression.
- **Combat:** active-mode LMB, hit timeline, verified fencing and single damage event.
- **Magic:** #58 protocol adapted to v4, rare learn grants and school-specific cast, AI-shared executor.
- **Enemy AI:** 30–60 gameplay-distinct archetypes on reusable bounded safety framework, 20/40/80 benchmarks.
- **World/Quests:** World E2.1, Encounter Director, dungeons, POI, reputation/faction/persistent events.
- **Environment/Visual:** PR #60 structures/resources, optional PBR and lower-cost fallback, VFX/audio.
- **MNAE:** PR #63 art/renderer port on v4 without v2 data schema, weapon/socket/armor, spectator state.
- **Performance:** PR #64 telemetry + immutable-plan workers only if measured.
- **Independent balance reviewer:** evaluate 2x2 race-class, 1–100 curve, 40 mob fight, phase boss, loot rarities.

**Release policy:** no automatic merge to NeoForge `main`; no v4 downgrade into older standalone v2/v3; no claims of user F5/client/two-player acceptance from green CI alone.
