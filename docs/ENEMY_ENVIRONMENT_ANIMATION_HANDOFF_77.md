# SHOWCASE-01 #77 — Environment + Animation handoff

**Owner:** Enemy AI provides semantics and combat-safe windows, Environment authors visuals, MNAE owns render/rig and clip playback. Only original/licensed-in-project source assets are acceptable; do NOT borrow textures/clips/models from third-party mods or videos.

## Actual V2 temporary visual signal (already in server code)

- **Road Raider guard:** vanilla Husk silhouette, equipped iron helmet, leather chestplate, sword and visible offhand shield; shield raise occurs in ~24-tick guarded window when a visible target is within distance² 22 (mob uses vanilla shield item). CRIT particle accompanies deliberate guard.
- **Road Raider flank:** alternate same-size Husk host with axe/leather chestplate, fewer metallic accents. Moving around guard-target is tactical but cannot guarantee complete collision-aware circling until actual test.
- **Cave Stalker:** vanilla spider silhouette, low dark crawler. Ambient spider sound and smoke preceding ~15-tick velocity-only pounce in dark.
- **Elite Shadow Alpha:** same Spider entity dimensions, differentiated by visible soul-flame aura, 20-tick warning, brief lateral feint and sharper leap impulse. No independent damage formula or boosted boss HP.
- Two new **original author-drawn 16×16 PNG loot icons**: `assets/mkm/textures/item/raider_insignia.png` (bronze/crimson badge) and `stalker_fang.png` (mint-green claw/drop). Own work created for MKM; standard vanilla item renderer and no external copyright source.

## Target distinctive silhouette (Environment backlog)

| Actor | Original asset requirement | Rig profile | Silhouette acceptance |
|---|---|---|---|
| Road Raider guard | 32×64 authored biped skin/overcoat + pauldrons + distinct strapped shield + metal mask | `mkm:rig/humanoid_v1` | Reads upright, broad shield triangle and right-shoulder iron from 20 m |
| Road Raider flanker | Original torn cloak, red sash, axe sheath, narrow profile | `mkm:rig/humanoid_v1` | Reads light-footed axe, no shield |
| Cave Stalker | Original chitin surface, curved forelegs, teal venom sac without copying vanilla Spider look | `mkm:rig/creature/spider_v1` | Reads low segmented crawler with frontal fang pair at 12–20 m |
| Elite Stalker | Posed forelegs, brighter venom sac/sigil, original three-part feint silhouette | same creature rig, distinct status | Detectable as elite without overhead nameplate |

Suggested pipeline: author original Blender/MNAE rig texture source + license note, preview front/side/back at default 16px/32px detail, shader-off vanilla+ and optional PBR variants, profile over 40 mobs. Standardized origin textures should not globally replace vanilla Husk/Spider; implement **tag-sensitive custom renderer/model binding** only after validated MNAE ownership and remote multiplayer sync. Do not overwrite other mobs' texture atlas.

## Semantic clip contract / sequencing

AI is **logical-server authoritative**, MNAE must not create hits from animation markers. Existing `NpcVisualSignals.Intent(actorUuid, rigProfile, cue, startTick, sequence)` supports THREAT, REACT, DEATH with species rig; visual adapter may ignore safely. For final VFX, coordinate versioned event family:

- `mkm:enemy/notice`: 12 ticks, eyes/head and weight shift; no hit.
- `mkm:enemy/guard_raise`: 24 ticks, shield and shoulder movement; actual vanilla `startUsingItem` is authoritative.
- `mkm:enemy/flank`: locomotion blended, no hit.
- `mkm:enemy/stalker_windup`: 15 ticks normal / 20 elite, leg retraction + particle ring + sound; only during valid shadow LOS.
- `mkm:enemy/stalker_feint`: elite only, final 10 ticks in windup, side-step via navigation; no hit.
- `mkm:enemy/stalker_pounce`: velocity impulse on server, actual hit remains vanilla melee.
- `mkm:enemy/stagger`, `hurt`, `die`: interoperable with Combat Poise and native health/death.

**Not yet implemented:** the new cue IDs, custom animated creature rig, custom renderer/model, or network clip replay. The current in-game telegraph uses first-party vanilla packet-visible particles/sound. Owner streams must coordinate before interpreting the candidate cue names as active protocol.

## Art/gameplay review footage

In third-person from the same 20-block camera: guard vs flanker silhouette, normal vs elite Stalker silhouette, torch on/off, lunge 15/20 ticks and shield raise. Capture F5 side/front/back + low-height obstacle; second human client must perceive cues at playable FPS. Do not pass visual QA solely by a PNG preview, clean compile, or fake animation marker.
