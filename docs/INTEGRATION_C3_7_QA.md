# MKM RPG — experimental integrated Fabric QA (C3.7)

This branch is an experimental integration of #13, #15, #16/#22/#23, #19, #18 and animation #9–#12, #14, #17, #20, #21, #25. **Never merge to historical NeoForge main.** Keep draft until combined CI, save and real-client QA pass.

## Pinned source revisions
- C3.7 baseline: `2be0fdca080cb09a4ba05065224a8c1b58b9cb0b`
- Character M1: `fc297566bb671d344201f290d094b45a27f7100f`
- Equipment D1–D3: `2b7b2701656a4b6d1f7cb70efdb11b19234f6c45`
- Magic Renewal: `bd5b4347d3ff3bea5427e94afd7edb24c1ac77f6`
- World E1: `e1f0612d13cc6baadc79e922266dd1efeccd08bf`
- Architecture: `a3e06317f81abe7182257c3246f4168821d78846`

## Integration contracts
- CombatEvents: D3 exact target + source identity + tick heavy attribution, physical mitigation/guard; retained C3 server-confirmed light-combo hit VFX packet only after positive unblocked Mob damage.
- CombatService: C3 directional dodge argument and authored action times unchanged; D3 equipped snapshot supplies resource modifiers.
- CharacterSheetScreen: M1 staged allocation and owner ack; equipped preview applied to staged CharacterData, not a stale base snapshot.
- MkmClient: allocation result listener, H Renewal key, mana HUD, C3 hit particles and renderer diagnostic all coexist.
- MkmMod: register M1, ability and quest attachments, networking and death hooks without overwriting Combat Core.
- /mkm modifiers: equipped snapshot with physical protection, M1 points retained.
- One authoritative PlayerModifierCalculator/PlayerModifierSnapshot. No parallel stat or damage subsystem.
- Source C3.7 mod version preserved; server and client must update together for schema-v3 packet shape.

## Automated CI gate
- `./gradlew build --no-daemon` (Java 25), existing JUnit and new cross-workstream allocation + equipment test.
- Bundled Fabric API, dedicated-server boot, Xvfb client startup, Windows installer packaging according to build workflow.
- Verify actions run against the **integration commit SHA** rather than accepting only green source-branch builds.

## Real gameplay checklist (NOT yet performed)
1. 26.3 single-player and 2 clients + dedicated server, key collisions R/G/H/K; HUD positions at small and large GUI scale.
2. F5: 3 sword and 3 unarmed attacks, 8 rolls, C3.7 `RENDERED` diagnostic; ensure foot contact and correct hit timing. Xvfb boot is NOT F5 visual QA.
3. Heavy/guard: shield blocked damage consumes stamina, exact unrelated hit cannot inherit heavy marker, stagger threshold, resistance versus PvP/mob/arrow, weapon swap projectiles.
4. Progression: 4 hostile deaths for 100 MKM XP, 2 build credits, K preview+apply, replay/stale packet rejects, death, relog, save/reload, backup migration from v2 to v3.
5. Magic: Renewal H once injured, low mana, cooldown, casts during combo/dodge, death/relog and multiplayer owner-only sync.
6. Quest: GM place Warden, PER 10/12, accept/spawn/kill/claim, repeated claim, 2-player owner isolation, Peaceful/blocked spawn, recover budget, chunk unload.
7. Fault injection: crash between quest mark and XP award, downgrade recovery from v3 data, duplicate events, possible vanilla attack bypass during active combo.

## Release blockers
- E1 quest state and character XP use separate non-atomic attachments. Crash-window can lose quest XP; `grantExperienceOnce` transaction/recovery contract required.
- No proven v3-to-v2 rollback preservation; backup before conversion, block unsupported downgrade release.
- Inherited vanilla attack during active combo/dodge may evade timeline; require game design/Combat owner decision before changing damage semantics.
- End-to-end save/multiplayer and F5 visual QA pending. Green CI is not a final approval.
