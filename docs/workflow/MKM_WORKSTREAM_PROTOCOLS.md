# MKM RPG — workstream-specific seven-stage protocols v1.0

Common stages S1 Intent, S2 References, S3 Synthesis/Design, S4 Prototype, S5 Implementation, S6 QA, S7 Correction are defined in MKM_UNIFIED_WORKFLOW.md. The stages below give **concrete work products and acceptance gates**, not permission to bypass source ownership. Each stream must also follow root AGENTS.md.

## 1. Game Design, balancing and rules

1. Intent: formulate an observable player decision or mechanic, required emotion/pacing, target power budget and explicit non-goals.
2. References: compare vanilla Minecraft, prior MKM gameplay clips, similar action RPG rules and any measured play-test outcomes; cite limitations rather than assume a popular design is balanced.
3. Synthesis: weigh player skill, fairness, counterplay, learning curve, accessibility and exploit surface. Make constants/coefficients provisional with explicit alternatives.
4. Prototype: simulate one canonical interaction or tune one weapon/ability archetype on isolated fixtures; establish minimum/maximum and expected edge behavior.
5. Implementation: update exactly one authoritative rules/definition path with configuration where warranted; preserve stable IDs and backwards compatibility.
6. QA: verify PvE/PvP, extreme stats, difficulty modes, mixed builds, numerical boundaries, regression interactions and actual play-feel. Distinguish calculated DPS from observed combat.
7. Correction: measure differences and adjust bounded parameters only after ruling out animation/render/input errors. Update balance rationale and expected tradeoffs.

Done when one meaningful player decision is demonstrably improved without hidden damage/accuracy changes. Large class trees and hundreds of coefficients are not a prototype.

## 2. Character Core / progression / persistence (PRs #15, #29, #35)

1. Intent: define how XP, attributes, points or rewards change and what a returning player must retain.
2. References: inspect current CharacterData schema and active branch, v1/v2/v3/v4 migration contracts, existing PlayerModifierSnapshot and reward ledger tests.
3. Synthesis: choose one source of truth, stable reward IDs, versioned codec and exact idempotent transaction/recovery rule; reject parallel XP or derived-stat engines.
4. Prototype: test one XP award, allocation and replay, plus a simulated interrupted reward across old/new save representations.
5. Implementation: server-only CharacterService boundary; migrate fields monotonically; do not persist visual animation states or client-authoritative allocations.
6. QA: v1/v2/v3 -> v4 where applicable, disk roundtrip, death/respawn, relog, reconnect, world restart, duplicate receipt, corrupt/future schema, owner-only sync and 2-player isolation. Verify *real* old-world backup restore and rollback policy separately from pure codec tests.
7. Correction: repair smallest migration or receipt failure and add a crash-cut test. Block release if XP, points, attributes or receipts are lost or duplicated.

Done when existing world progress survives and grantExperienceOnce semantics hold. Never install isolated v2/v3 builds over v4 saves.

## 3. Combat / stamina / damage / guard (PR #36 and existing Combat C3/D3)

1. Intent: specify player input, attack state, active hand, weapon family, expected cost/damage and normal vanilla interaction.
2. References: inspect vanilla hit/damage/cooldown/mining, CombatService/ActionTimelineRules, EquipmentRules profiles, client AttackPresentationEvent and user video failures in Issue #43.
3. Synthesis: choose smallest change preserving vanilla LMB authority; separate read-only cosmetic signals from accepted damage. Define hand/main-offhand resolver and priorities.
4. Prototype: one fixed attack sequence with sword, offhand sword+empty mainhand, axe, shield, unarmed and left-handed avatar; include blocked and canceled hits.
5. Implementation: server validates action and resources; keep damage single-path; presentational LMB observation must not re-invoke player.attack or break mining.
6. QA: multiplayer, PvP/PvE, guard/stamina costs, cancel/double events, mixed hand items, cooldown, projectile attribution, eight dodges, held-item swaps, latency and real F5/first-person display.
7. Correction: isolate rules vs presentation faults; reject any cosmetic change that causes extra damage, erroneous active hand or shield cost duplication.

Done when combat outcomes and input semantics are stable *and* cosmetic effects accurately depict accepted actions.

## 4. Equipment / items / loot (PRs #16, #22, #23, #27)

1. Intent: choose one equipment modifier or player-visible item behavior, slots, rarity/identity and equip/unequip expectations.
2. References: inspect EquipmentRules, existing typed weapon family mapping, snapshots and vanilla tag/slot/shield mechanics.
3. Synthesis: maintain one modifier aggregation source with typed attribution, stable item IDs and exact stacking semantics; define offhand and handedness.
4. Prototype: equip/unequip one item with simultaneous combat and magic modifier inspection; stress shield or weapon switch paths.
5. Implementation: one authoritative registered item/affix path, read-only snapshot exposure to Combat/Magic/Animation; no independent client stat formulas.
6. QA: slot swap, offhand, shield/stamina, melee vs ranged provenance, projectile after weapon swap, inventory full, stack duplication, dropped/destroyed equipment, relog and two-player visibility.
7. Correction: narrow offending profile or snapshot consumer and lock down stale/canceled hits, duplicate stat contributions and unclaimable rewards.

Done when item behavior is deterministic across save, network and related systems; no phantom damage or double item grants.

## 5. Magic / active and passive abilities (PRs #19, #28, #37, #42)

1. Intent: define spell outcome, target type, server-accepted conditions, mana/cooldown and intended visuals.
2. References: inspect Renewal server rules, Combat action interlocks, resource ledger and MNAE AbilityVisualEvent semantic contract.
3. Synthesis: use validated definitions and allowlisted effects; reserve visuals for server-confirmed success. No second ActionTimeline or client-driven effects.
4. Prototype: one successful Renewal plus full-health, insufficient-mana, busy and spam/invalid-input negatives.
5. Implementation: server authorizes and pays once; healed amount checked before APPLIED visual event, client reads owner-only synchronized resource state and short cosmetic duration.
6. QA: malformed/replayed packets, exact mana spend once, cooldown and clock rebasing, relog/dimension/respawn, 2-player owner isolation, spell-key rebind, HUD scaling, equipment modifiers and failed-cast silence.
7. Correction: fix eligibility/ledger/event boundary instead of increasing effects or inventing cast animations inside Magic.

Done when the spell's gameplay outcome is authoritative and the visual matches it without duplicates; R/H/G controls coexist.

## 6. World / NPC / dialogue / quests / narrative (PRs #18, #26, #39, #45)

1. Intent: define player-facing quest choice, moral/attribute condition, authored scene and consequences, including alternate and failed paths.
2. References: inspect existing Ford Warden/Ilra scenario as represented in the target branch, actual dialogue/quest state machine, PER thresholds, game narrative references, server persistence and Character receipt contract.
3. Synthesis: choose explicit state graph, per-player vs world scope, validated dialog choice/session, keyed rewards and encounter identity/respawn policy.
4. Prototype: NPC -> dialogue -> accept/refuse -> encounter -> kill/resolve -> once-only XP -> journal -> relog; include PER below/above threshold.
5. Implementation: server owns stage choice and rewards; call Character grantExperienceOnce rather than independently changing XP; use semantic NPC cues for MNAE, not separate skeleton animation state.
6. QA: two independent players, forged/stale dialogue and journal packets, duplicate claim, hard restart between receipt and quest state, missing NPC, killed/unloaded enemy, dimension/Peaceful/chunk unload, UI focus and accessibility.
7. Correction: trace a bad result to owning quest state/reward/encounter component; prohibit reward-floor double payouts and blind NPC/entity respawns.

Done when one complete quest survives reload and cannot be exploited. Expansion into worldgen/factions waits for first vertical slice.

## 7. AI / encounters / world population

1. Intent: state the behavior purpose: guard patrol, threat discovery, target selection, ranged position, safe casting, reaction to a corpse, quest encounter persistence.
2. References: inspect Minecraft server AI/pathfinding/navigation rules, MKM combat/ability contracts and actual observed gameplay; separate cinematic references from pathfinding constraints.
3. Synthesis: define bounded finite-state/behavior logic, senses, authority, tick budget, memory lifetime, group interactions and fail-safe target rules.
4. Prototype: one NPC role on controlled terrain with enemy/ally, line of sight loss, re-acquisition and chunk unload.
5. Implementation: server-controlled bounded decisions; existing damage/resource systems reused; no global scans, client-only AI or unbounded recursive retry.
6. QA: 1 vs multiple players, obstruction, friendly fire, path failures, water/height, unloading and reload, despawn, lag/tick budget, targeting fairness, peaceful worlds and no leaked quest rewards.
7. Correction: audit one wrong decision and deterministic reproduction seed/coordinates; update thresholds and record before/after CPU and behavioral evidence.

Done when measured AI behavior and performance agree with the scenario; animation expressions are separate MNAE presentation cues.

## 8. Animation / 3D models / MNAE (PRs #40, #41, #43, #46, #47, #48)

Follow the detailed MKM_ANIMATION_WORKFLOW.md. Summary:
1. Intent: motion phases, family, hand, item and game marker.
2. References: real footage + games/cinema/mods/art and license/reproducible sample.
3. Synthesis: original grounded choreography and weapon trajectory.
4. Prototype: key poses, storyboard, Blender .blend and timed animatic.
5. Implementation: rig/IK/socket -> validated conversion -> MNAE render bridge (never silent 17->6 bone downgrade).
6. QA: >=9 source frames, actual animated preview, fixed-camera F5 60fps A/B, gear/hand matrix and server behavior.
7. Correction: separate pose, importer, hierarchy, render, gameplay-timing and camera/armor issues.

Done only after actual Minecraft gameplay visual review; a Blender source may be valid but marked AUTHORING ONLY.

## 9. UI/UX / input / HUD / accessibility

1. Intent: task-first screen or interaction, player information needs, keyboard navigation, visual hierarchy and failure feedback.
2. References: inspect current Character Sheet, dialogue/journal, ability/stamina HUD, keybindings, translations and supported Minecraft GUI scaling and input patterns.
3. Synthesis: design minimal wireframe and information architecture, reduce occlusion of F5 avatar, choose accessible feedback with non-color-only status.
4. Prototype: static layout or clickable mock plus scripted representative state: active action, cooldown, error, empty list, long localized label, small/large scale.
5. Implementation: owner-synced read-only state, bounded validated intent packets, deterministic focus and rebindable keys; no HUD-derived gameplay facts.
6. QA: actual Minecraft client, F1/F5 if relevant, 2+ GUI scales, RU/EN and long text, low resolution, key conflicts R/G/H/J/K, mouse/keyboard focus, disconnected/invalid/spectator states and 2-player owner privacy.
7. Correction: inspect screenshots/interaction recordings; fix collision, contrast, focus or stale synchronization before adding visual ornament.

Done when the user can complete the task in game, not merely when a screen component renders in a test.

## 10. Data-driven content / registries / localization / assets

1. Intent: define stable content IDs and how players/modders will encounter the content.
2. References: existing assets/mkm and data/mkm registries, packaging conventions, Minecraft 26.3 loader constraints, actual translations and licensing.
3. Synthesis: choose minimal schema and validation of IDs, budgets, ranges, resource sizes and versioning; document overrides and missing-content fallbacks.
4. Prototype: one asset or data record consumed by current loader; include malformed/oversized/unavailable fixtures.
5. Implementation: add only needed definitions, locale keys, manifests or models; keep consistent naming and resource ownership.
6. QA: clean startup + missing/invalid resource path, locale switch, reload, CI assets package, multiclient compatibility and save IDs persistence.
7. Correction: fix validator/load error or missing translation; no broad resource rename without migration.

Done when the actual built JAR loads and can use the content without breaking old IDs.

## 11. Infrastructure / packaging / deployment

1. Intent: state target Windows + Minecraft/Fabric/Java combination, installer behavior, upgrade/rollback and offline expectations.
2. References: examine exact Gradle properties, GitHub Actions, installer sources, bundled Fabric API contract, latest integrated build artifact hashes and actual logs.
3. Synthesis: compare minimal compatible fix with new tooling; prefer reproducible CI without admin privileges or unnecessary secrets.
4. Prototype: package one isolated build from a pinned SHA, restore a backed-up test world and verify no duplicate JAR.
5. Implementation: narrowly update scripts/workflows and versioned artifact manifests; do not mutate user files silently.
6. QA: Java 25 compilation, JUnit, dedicated server boot, GUI client start, fresh and upgrade installs, mods folder conflicts, checksum, Windows installer paths and rollback; headless boot != playable F5 validation.
7. Correction: reproduce failure with exact environment, fix dependency order or path assumptions, rerun release candidate checks.

Done when installation and downgrade safety are demonstrated on the exact target platform, with an explicit migration plan.

## 12. Integration / independent QA / release authority

1. Intent: define integration scope, accepted feature versions, target worlds and release acceptance matrix.
2. References: inspect source/base HEADs and stacked PR ancestry, isolated CI results, current issue blockers, save schemas, gameplay videos and cross-team owners.
3. Synthesis: write a conflict matrix and merge/cherry-pick order; reject mismatched v2/v3/v4, duplicate combat formulas, multiple equipment resolvers and clashing input/UI handlers.
4. Prototype: one combined Fabric build with representative Character -> Combat -> Equipment -> Renewal -> Quest -> UI loop; create backup snapshots and crash-cut fixtures.
5. Implementation: merge only reviewed compatible changes into a dedicated integration branch; retain exact source SHAs and stable IDs. Never auto-merge to historical NeoForge main.
6. QA: build and test the **integrated SHA**, dedicated server, Windows client, real 2-player end-to-end scenario, F5 animation, world upgrade/death/relog/restart and recovery after interruptions; report all missing evidence.
7. Correction: bisect regressions, revert smallest unsafe integration, require both owners to sign off on changes to shared contracts and repeat critical scenario QA.

Release must remain BLOCKED if any P0 is unresolved: duplicated/vanished XP, data loss, unauthorized packets, extra damage, wrong hand/weapon visuals in accepted animations, broken inventory/quests or missing required in-game validation.

## 13. Reference scenario across all streams

A future integration test should cover:
- spawn in a backed-up world with v3->v4 migration if selected;
- equip sword/shield -> verify typed family and modifiers;
- attack once via vanilla LMB and via special combo -> single damage path and matching cosmetics;
- dodge and cast Renewal -> independent server costs, no duplicate visuals;
- open NPC dialogue, make PER-gated choice, complete encounter, claim keyed XP once;
- open journal and character sheet, allocate points;
- disconnect, reload, change dimension, respawn and repeat all relevant requests;
- two players see correct owner-scoped state, and third-person animation remains synchronized where claimed;
- render real F5 footage and cross-check same equipment/hand/clip/renderer against source.

This is an **acceptance plan**, not a claim the current draft integration already passes it. Each owning stream supplies its fixtures and evidence.
