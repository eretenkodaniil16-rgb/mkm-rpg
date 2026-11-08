# SHOWCASE-01 RC1 — Integration QA handoff to «главный»

**Issue #72; master GDD PR #70; source baseline QA5 PR #69 @ 440dfe07f67d4cce1a8fc10f2bd62a5a2574e137; Environment PR #60 @ c2efa9ccb929cd257c067fe069588117fe6781a9.**  
**Branch:** integration/fabric-26.3-showcase-rc1, **Fabric 26.3 / Java 25 / CharacterData v4**. Experimental only. Historic NeoForge main remains untouched.

## Source audit and staged-vs-omnibus decision

- QA5 #69 already included #64 Performance→#54 Encounter→#55 Origins→#56 Enemy, World E2.1/Character v4. Its latest SHA passed GitHub Actions #42425548114 and #42425525053. Baseline already had selectable Human/Dwarf×Warrior/Mage, two vanilla-host enemy types and GM-triggered night ambush.
- #60 (Environment): native gzip NBT structures and six 16x16 original materials, protected placement; verified separate Java25, RCON placement and static QA #42422984954/#42422984955; **not present** in QA5.
- #59/#65 Combat: built atop #36/#30, Character v3; would replace shared CombatEvents/EquipmentSnapshots and introduce experimental fencing. #58 Magic M4 inherits #42 v3 with incompatible payload and owner HUD; #63 R07 inherits #46 Character v2 and replaces MkmClient/item transforms. **Do not bulk import.** #43 visual problems remain; non-GPU CI is not F5.
- #70 source decision GDD: 115 answers, S20-04 A+B+C, meaningful race/class, 17-bone later, 2-player coop/40 active mobs aspirational. PR70 docs are not proof of runtime.
- Conflict registry from code: MkmMod initialization/effect hooks and CharacterEvents (only one XP writer on entity death); MkmClient receiver registration/HUD; EquipmentSnapshots single derived calculation, v4 receipt; UseBlockCallback original RuneSlateInteraction only; Combat-owned WeaponPresentationResolver from #62; ItemInHandLayer not modified.

Three candidate sequences: (1) ten-way merge ⇒ immediate schema downgrade and incompatible registrars. (2) all added behind feature flags ⇒ significantly more untested production-code surface and misleading "single JAR". (3) **staged gameplay vertical (chosen)**: QA5 source + only #60 environment + explicit playable, server-authoritative discovery flow, followed later by independent Magic and R07 after actual game proof.

## RC1 new concrete runtime relative to QA5

1. 3 genuinely distinct NBT native 23×14×19 watchtower variants, six proprietary 16px blocks, distinct item/creative-tab definitions, rune particles and chest loot. Safe GM \`/mkmenv watchtower 1..3 X Y Z\`, environmental source copied via **Git blob SHA**, preserving actual PNG and .nbt bytes.
2. New world-persisted \`mkm:watchtower_sites_v1\` attachment with PENDING before placement and PLACED after rune/site completion. 32-site cap; invalid/future records fail closed. A loose player-created rune cannot issue XP. Post-crash PENDING never auto-credits and requires site manual QA.
3. Actual \`UseBlockCallback\` on rune: mainhand right click on registered PLACED rune; server-only origin check and **existing CharacterService.grantExperienceOnce** with stable \`mkm:showcase/watchtower_rune\` reward ID. One-time 25 XP, no second writer. Warrior receives 15s Resistance, Mage 50s Night Vision, Human/Dwarf different clues; follow-up clicks don't renew either status or XP.
4. New top-right \`MkmShowcaseHud\` guide, player-readable \`/mkmshowcase guide/status\` showing nearest confirmed site and own receipt state, plus persistent client-side O/U/B keybinds: display, high contrast and reduced ambient rune VFX. Input changes never grant server powers or send new gameplay network packets.

Untouched: client combat attacking/item mining, hitbox, vanilla damage, shield/offhand logic, preexisting Enemy/Encounter magic callbacks, permanent CharacterData v4 codec, equipment snapshot and MNAE. No custom R07 animation, delayed LMB, or Magic M4.

## Test and release evidence requirements

Automated, independent:
- original 4+ new JUnit WatchtowerSiteRulesTest and WatchtowerShowcaseIntegrationTest; one-time v4 receipt roundtrip, distinct Human/Dwarf × Warrior/Mage, 32-site bounds, immutable persistence.
- preexisting 200+ QA5 tests and full Fabric Java25 Gradle \`build\`.
- static \`tools/environment/verify_watchtower.py\`: authentic 3 gzip NBT/PNG CRC/loot/geometry; \`tools/showcase/verify_showcase.py\`: explicit mod registrars, class boundary, UI, C2S symbol uniqueness, version and source SHA.
- **actual ephemeral dedicated server** with RCON and protected placement of all 3 tower variants, no overwrite, camp/bridge/rune/ladder and real chest loot. Passing this is in-engine server evidence, but no F5 screenshot or right-click in graphical game.
- dedicated server + Xvfb client startup, Windows Installer parser and unique \`0.0.7-fabric.0-rc.1\` jar/package, embedded API, SHA256 + \`SOURCE_SHA.txt\` in installer ZIP.
- content diagnostics GitHub artifact with final source SHA (not game FPS).

Manual gates still open: actual rune right click under 2 clients and independent XP after restart, server chunk unload, real v3→v4 NBT world copy, admin GM interactions, 10–15-minute full run and two-player recording, A/B/C UI at 320×240/1920×1080, 20/40 enemy FPS/1%-low/p95/p99 MSPT, shaderless fallback, attack/offhand/shield mining and final >4 distinct differences **visually** captured. Because Encounter M1 is world-once and owner-only, **two-player Encounter BONUS payout for both is NOT shipped**; independent 2-player quest claims belong to FordQuestService; no fake proof.

### Identified limitations
- World Site PENDING and actual structure modification are non-ACID; power cut can leave pending tower requiring admin repair. No automatic rescan or duplicate-worldgen.
- Character receipt+XP atomically co-reside inside the character file but World encounter, quest attachment, entity chunks and item drop files are not a single durable transaction. Exactly-once across arbitrary power cuts cannot be guaranteed.
- A+B+C rollout is **partial**: A high contrast (GUI scale via vanilla), B suppress ambient rune particles (not every server VFX), C remappable keys (no real gamepad yet). Config local and persistent.
- AI prototypes use vanilla Husk and Spider models rather than new enemy rigs; R07, Magic M4 and Fencing not integrated.
- Independent Environment RCON smoke must run successfully on current SHA, not historical PR #60 alone; green source branches don't certify combined JAR.

**Release decision:** DRAFT. No merge into NeoForge main, no public build. After exact HEAD CI, test real two-client showcase on disposable v3→v4 copied world. If acceptance succeeds, next staged PR should be a v4-aware Magic M4 slice; R07 must remain its own renderer QA branch.
