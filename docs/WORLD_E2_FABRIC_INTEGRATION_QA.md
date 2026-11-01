# World E2.1 — Fabric C3.7 integration QA with CharacterData v4

**Status:** Draft/QA ONLY. **Branch** `integration/world-e2-1-fabric-character-v4-qa`; PR #45 stacked on experimental Fabric PR #30. Sources: World E2.1 PR #39, Character M2 #29/M3 #35. Issue #43 MNAE visual-audit changes are deliberately NOT included.

## Minimal integration boundary

Imported World E2.1 `quests/**` logic, owner-only `FordClientNetworking`, Ilra dialogue/J journal screens and their deterministic tests. Only `MkmMod` and `MkmClient` gained corresponding registration/hotkey hooks; C3.7 Combat, Equipment D3, Magic/Renewal, PlayerModifierCalculator, equipment-fed Character Sheet and vanilla hits stay on the existing integration code. No clips, rig, model mixins or second animation runtime.

Updated Character service and `CharacterData` to the **official v4 schema** from Character M3: the legacy v1/v2/v3 player attachment migrates to schema 4 while retaining XP, seven attributes, earned credits and allocation counts. Stable server-only `claimed_reward_ids` is persisted in the same Character attachment as XP. It is NOT synced to clients. The item/Equipment reward is deferred; no double attribute calculation.

At quest claim, `QuestRewardRecovery.decide` runs first; `GRANT_ONCE` credits via `CharacterService.grantExperienceOnce`, `FINALIZE_QUEST` completes a saved pending quest WITHOUT more XP, and unknown/ambiguous states fail closed. The ID is `mkm:ford_warden/complete` per player. The old `reward_xp_floor` continues to decode but NEVER causes top-up.

## Build and verification

```bash
./gradlew clean test build --no-daemon
./gradlew runServer --no-daemon
# Separate Xvfb/display-capable agent:
xvfb-run -a ./gradlew runClient --no-daemon
```

GitHub Actions also checks Fabric API bundling and Windows installer syntax/artifact. Headless CI DOES NOT prove actual interaction and 2-player gameplay.

## Manual acceptance matrix (two actual Minecraft clients)

1. Back up `world/playerdata` and world-level Fabric attachments before install. Verify a saved Character v3 player with XP and allocated points migrates to v4 with identical values and no invented receipts. Verify v4 persists across relog/death/restart. A legacy `rewarded` quest with no receipt remains completed and flagged for manual audit, never auto-credited.
2. Place one tagged Ilra with privileged `/mkmworld` command on solid ground; right-click from main hand. Verify dialog screen, no vanilla trading, and no right-click interference with other villagers. At PER 10, investigation is disabled and server rejects forged INSPECT. At PER 12, inspect succeeds before or after acceptance and increases claim reward 75→100 XP.
3. Open/close dialogue repeatedly; send stale stage/expired/duplicate/foreign entity ID/foreign owner nonce. Only a valid once-only token can trigger action. After an accepted valid action, a stale replay must not respawn enemies or re-grant rewards. Closing stale screens must not reopen them.
4. Press J with and without an active quest; stage and PER clue are owner-only. During journal display, alter quest on server: screen refreshes. Close it, then receive delayed push ID=0: journal stays closed. Open inventory and deliver delayed correlated response: inventory must not be replaced.
5. Player A and B independently accept the quest at the same Ilra. Verify *two distinct* owned Husk UUIDs. Kill B's Husk by A: only B's quest advances to return stage; ordinary M1 player-kill XP remains attributed to actual killer. Do not treat combat XP 25 as quest bonus. Claim A and B separately and verify two independent Character v4 receipt ledgers for identical stable reward ID.
6. On a saved live instance, leave the chunk containing A's enemy; use `/mkmworld recover`. No duplicate spawn; shown coordinates point to the original anchor. Return to original chunk, target unchanged. Restart server, reload both clients, then verify encounter UUID and owner isolation persist.
7. Reproduce a partial-save cut with world `mkm:ford_encounter_registry` persisted but player stage reset to `not_started`; reconnect/reaccept: if original alive/loaded, bind existing UUID; if unloaded, refuse new spawn and show anchor. Do not automatically respawn after modded removal, Peaceful deletion, `/kill` or unknown status.
8. Kill A's registered raider while A is offline, reconnect A and ensure world-persisted tombstone advances A's quest. Also test A online when death occurs, abruptly restart after death but before player save; persisted death evidence should be replayable if world checkpoint captured it. Death proof is retained (bounded ledger) rather than deleted in a separate save cut.
9. At return stage, claim once: Character M3 receipt, XP and level credits saved together; subsequent claim NO new XP. At level cap, save the receipt even for zero awarded XP. Simulate character receipt persisted but quest stage preclaim: retry must only finalize stage. Admin XP reduction must not trigger cumulative XP floor repair.
10. Simulate optional MNAE observer throwing a RuntimeException; server quest and death logic must continue. Without MNAE, vanilla NPC models/AI render and fight normally. No generated skeleton or new gameplay damage signal is expected.
11. Verify GUI at narrow 320×240 and regular 1920×1080, input bound J/K/H/R/G, 1st-person and 3rd-person inventory interactions. Check client/dedicated server with matching builds; packet protocol changed from E1.

## Known limits — fail closed

- World-level encounter registry, death tombstones and player file **are not one ACID transaction**. Crash before writing a complete checkpoint may still lose a death record or leave an orphan. We do not claim absolute exactly-once persistence across arbitrary power loss. Character XP and receipt *within the same player record* are idempotent.
- A deleted/unloaded entity cannot be positively distinguished without a dedicated persistent entity lifecycle authority. A lost enemy requires an audited privileged repair process; blind respawn is prohibited.
- Death tombstones retained to survive save cuts are bounded at 4096 and currently not automatically pruned. Review a bounded compaction/archival policy before scaling story quests.
- MNAE's weapon-family, grip, skeleton application and roll problems in Issue #43 remain under Animation/Combat. This World PR uses semantic `NpcVisualSignals` only, with no animation player or pose ownership.
- No E2 equipment item grant, settlements, factions, automatic worldgen, or live scripted two-client end-to-end acceptance yet.

**Merge gate:** Headless CI all green + manual two-client and restart/save-cut validation + independent reviewer acceptance. Keep draft, do not merge PR #45 directly to NeoForge main.
