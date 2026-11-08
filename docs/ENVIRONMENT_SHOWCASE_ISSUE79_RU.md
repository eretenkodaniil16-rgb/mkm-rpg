# MKM RPG — Environment SHOWCASE-01 / Issue #79

**Source base:** PR #69 QA5 (Fabric 26.3, Java 25, CharacterData v4). Previous Environment PR #60 is preserved, not force-merged. **Draft PR #83** is the new QA5-compatible preview. Never merge to historical NeoForge main.

## Implemented gameplay slice
- Six original 16px Vanilla+ block textures and three damage variants of the 23×14×19 tower with camp, supported two-wide bridge, stairs, ladder, loot barrel and rune slate. 26.3 native NBT palette uses lowercase `id/properties`.
- Server-only GM placement: `/mkmenv watchtower <1|2|3> <x> <y> <z>`. Required site: target 23×14×21 all AIR, 19 supports reaching sturdy soil within four blocks, only loaded chunks, Overworld and border checks. Fail-closed. Non-transactional, no protection from separately invoked vanilla `/place template`.
- A new physical red/bone `mkm:raider_war_mask` model and original 16px Raider Insignia + Stalker Fang item sprites. QA5 Raider spawned through `/mkmenemy spawn raider` equips the mask; Cave Stalker retains its vanilla spider model but receives violet portal-signature particles; Raider has a pale amber signature. **This is a marked vanilla-host prototype, not a fully custom EntityRenderer or mesh.** Coordinate full enemy body art with Enemy stream before modifying renderer/mixins.
- Two server-triggered enemy VFX families: (1) ambient phase cue, only every 40 ticks per mob, distance 32 blocks per subscribed player; (2) impact cue only after positive, unblocked `EnemyService.afterDamage`. Both use client-only vanilla particles and cannot trigger damage/XP. The original rune slate can be right-clicked to trigger a chime and brief local particle pulse, server gated to 1 per 20 ticks per player. Client packets carry entity ID or rune coordinates and no gameplay state.
- Optional sound art direction: warm low resonant amethyst note on rune activation (implemented using native sound); future Raider metallic rustle before melee and Stalker dry scrape for proximity/telegraphy (NOT yet authored audio assets; never copy unlicensed samples).

## Three in-client visual profiles and S20-04 B
- **ECONOMY:** original 16px blocks; 0 ambient particles, 2 particles for positive impact, reduced rune random-display rate. No shader dependency.
- **STANDARD:** same native materials, 1 ambient particle per cue and 4 per impact, existing rune ticks.
- **CINEMATIC:** 3 ambient particles and 7 per impact, higher rune tick density. It is a **presentation preset, not a 64× PBR resource pack**. PBR 64× textures, material normal/specular maps and Iris shaderpack remain optional future delivery; do not assert the profile currently enables real PBR.
- `V` cycles profiles; `F10` toggles persistent reduced effects independent of profile. Both are rebinding-friendly Minecraft key mappings, saved to `config/mkm-environment.properties`. Reduced mode suppresses ambient VFX and caps impact/rune pulses to 1 particle. No screen flash, full-screen post process, camera shake, or custom core shader.
- Native Minecraft particle setting can reduce effects further. UI scaling/contrast/controls are other S20-04 A/C workstreams, not implemented here.

## Expected particle budget, 40 marked enemies / 1 near client (not measured FPS)
Assume 40 alive and fully indexed, stable identity. Ambient rate capped by modulo schedule: exactly 40/40 = **1 ambient event/server tick = 20 packets/sec per client**, no more than 32-block radius. Economy/reduced = 0 client ambient particles; standard = 20/s; cinematic = 60/s, plus positive-hit events and rune random-display ticks. Maximum particles on *one* confirmed hit: economy 2, standard 4, cinematic 7, reduced 1. No persistent block entity for rune stone; tower contains a vanilla loot barrel and lit campfire (vanilla BE/renderer behavior). This is a **mathematical budget, not a measured benchmark or guarantee** under varied entity IDs, number of clients or hostile traffic; count actual events and render calls on target hardware. All event packets drop if no subscribed/nearby client.

Estimated raw source-art resource overhead: nine 16× PNG files × 1,108 bytes = 9,972 bytes plus three NBT structures ~58–59 KB; models and language files add several KB. This excludes Minecraft/Fabric runtime and JAR nesting, and does not measure save-file growth or draw calls.

## QA / integration gate
1. `python3 tools/environment/verify_watchtower.py` and `python3 tools/environment/verify_showcase_assets.py`.
2. `./gradlew test build --no-daemon`, existing dedicated-server/client smoke. New dedicated `smoke_watchtower_server.py` places all three variants in disposable superflat, checks no-overwrite, actual 26.3 rune+camp+ladder+bridge, real loot data.
3. In actual Minecraft with QA5 jar (on TEST WORLD): enable cheats and Survival or Creative; at a fully **air-filled preflight volume** with solid ground Y=64, use `/mkmenv watchtower 1 0 65 0`. In daytime/night, enter from southern stairs, climb interior ladder, cross east bridge to camp, right-click rune; open barrel. Compare second and third variants at other cleared sites. `/mkmenemy spawn raider 3` and `/mkmenemy spawn stalker 3` from a player; check mask and purple cues, injury VFX, no second XP event. Confirm `V` and `F10` effects.
4. Capture real **F1+F2 screenshots in Minecraft** from south door and east camp at day and night. Log F3+T (atlas texture errors) and repeat with optional Sodium + Iris only after checking exact MC 26.3 compatible builds. No AI-generated image counts as a screenshot.
5. For measured benchmark: same seed, 0/1/16 tower sites (disposable worlds only), 0/20/40 active tagged mobs, two players, 120-second warmup + five 60-second samples, client FPS median/1% low, server MSPT (Spark), GC, chunk sizes, native BE counters and packet/particle counts. Repeat with profile ECONOMY/STANDARD/CINEMATIC, no shaders, then a compatible shaderpack; record exact CPU/GPU and configs. Source code alone cannot establish FPS or draw calls.
6. Keep PR draft until manual screenshot, no missing-texture render, actual rune click, server restart, terrain edge rejection and 2-player gameplay acceptance.

## Risks / remaining tasks
- Mandatory in-game visual inspection and screenshot not yet performed in this automated run. Static PNG and server boot prove pack integrity, NOT optical quality or camera/UI state.
- The 16× mask is equipped as a block item. Actual head-slot rendering of this 26.3 host should be visually confirmed, including original HUD/armour and model overlaps; if invisible or rigid, implement proper renderer in coordination with Enemy team instead of claiming success.
- This does NOT auto-place structures, guarantee ACID rollback, or protect against vanilla template placements. World chat owns provenance claims and biome/camp ecology; Enemy chat owns full custom creature bodies.
- Cine PBR 64×/Iris A/B, 40-mob interactive FPS and multiplayer are still outstanding.
