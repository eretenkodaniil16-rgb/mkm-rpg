# MKM RPG · Environment V1 — Vanilla+ Dark Fantasy

**Development status:** source-art prototype on Fabric 26.3 (Java 25), stacked on World E2.1 PR #45. NOT a release, NOT a gameplay-verified worldgen system. Do not merge into historical NeoForge `main`.

## Design decision
| Approach | Compatibility | Runtime cost | Maintenance | Result |
|---|---|---|---|---|
| A vanilla only | highest | very low | low | limited visual identity |
| **B custom blocks + vanilla models/structures** | **high** | **low** | **moderate** | **recognizable, cohesive fantasy** |
| C mandatory custom global shaders | variable (Iris/Sodium and pipelines) | potentially high | high | dramatic, but fragile |

**Choose B**. No core shader modifications. Iris is optional; shaderpacks are not installed by the mod. Vanilla models, lighting, random-display vanilla particles and stock materials provide visual identity independent of GPU features.

## Palette & art direction (all colors are original MKM decisions)
- Soot shadow `#24282B`, cold mortar `#54575B`, aged stone `#747474`, moss `#596B4D`.
- Timbers `#42332C`, iron `#4D4743`, oxide `#805848`.
- Rune accent `#48B9AD`, sparse spectral highlight `#8CDFCF`, fires `#E1A36C`.
- Keep silhouettes readable at 24–64 blocks; big masses first, then a few 1-block details. Always preserve player-size 2-high doorways (2-wide preferred), navigable 2-wide paths and 2x2 spawn footprints.
- Materials: large weathered masonry, limited moss (not every stone), burnt timber near failed roof, deeply oxidized iron on hinges, sparse rune slate as a focal point. Readable metal frames, no photographic grain/no borrowed texture.
- **Base texture 16×16** per block. Integer nearest-neighbor pixel silhouettes, 3–5 luminance groupings, restrained pixel noise. A 32×32 option is a future OPTIONAL art pack, not a default.
- Light: warm campfire against cold stone; high-contrast torch/lantern accents; **rune texture is NOT dynamically emissive** without separate lighting implementation. Vanilla lighting must suffice.
- Vanilla coherence: full-cube block models, vanilla-sized edges/shadows and existing daylight/biome tint behavior; no global color grading, forced fog, player shader or skin override.

## Original material IDs
`mkm:aged_ashlar`, `mkm:mossy_ashlar`, `mkm:cracked_ashlar`, `mkm:charred_timber`, `mkm:rusted_iron`, `mkm:rune_slate`.

Each has registered block + item, standard blockstate/cube model/item-dispatch JSON and original RGBA 16px PNG. Transparent pixels are not used. Existing player skins, armor renderers and HUD remain untouched.

## Watchtower prototype
- Templates `mkm:ruined_watchtower_01`, `_02`, `_03` under `data/mkm/structure/` contain distinct physical damage; one watchtower plus attached campsite. Size 23×14×19 blocks.
- Coordinates relative to template: tower outer walls X4–13/Z3–12; walkable ground Y4; two-wide door X8–9/Z12; ladder at X5/Z7; elevated elite platform Y11; barrel at (11,5,5) with `mkm:chests/watchtower` loot table; rune at (7,5,6); camp east at X16–20, Z10–16. No enemy entities are serialized in the template.
- Foundation pillars are 4-block-deep at selected corners. **Controlled GM-only placement now implemented** via `/mkmenv watchtower <1..3> <x> <y> <z>`: the entire 23×14×21 target must be AIR; all 16 support anchors must contact sturdy terrain within 4 blocks below, and chunks must already be loaded. Four-step two-wide approach stairs are added and pillars extended only through air. Rejection is side-effect-free. This is a manual placement gate, not a claims/provenance or automatic worldgen system.
- **Use `/mkmenv watchtower 1 x y z` in a disposable creative test world.** It refuses ANY non-air block within the entire target volume and never digs into supporting ground. Vanilla `/place template` remains destructive and bypasses this guard, so never run it near player buildings. This guard cannot prevent indirect neighbor updates or give ACID rollback on engine failure and is not yet a multiplayer anti-grief guarantee.
- No structure sets, jigsaw pools, biome modifications or new spawn rules in V1. Prevents unintended placement of thousands of buildings. For World team: implement server-side placement authority with claim/provenance-aware guard BEFORE any procedural generation; natural-looking blocks can also be placed by players, so a natural-block allowlist alone cannot guarantee safety.
- Encounter ownership, NPC logic, PER requirements and quest persistence remain in World E2.1. Proposed anchors are **metadata only**, not active spawns: regular enemy (7,5,9) and (18,5,13), elite (10,12,8), chest (11,5,5). World team must validate collision/pathfinding and persistent encounter reservations.

## Effects
`RuneSlateBlock.animateTick`: client-only vanilla `END_ROD`, max one particle each successful random-display tick, ~1/12 chance; no traffic to dedicated servers, no block entity, no dynamic light, no screen post effect. Users may start their client with `-Dmkm.runeParticles=false`; vanilla reduced-particles settings offer further control. Not a replacement for ability visuals. Check in-game before calling it accepted.

## 26.3 compatibility notes
Resource format **97.1**, data format **121.0**, DataVersion **5023**. Fabric 26.3 changes dynamic registries and worldgen feature layouts. Templates use native compressed NBT and the ordinary `data/mkm/structure` singular folder; there is no cross-version pack override. Resource model `minecraft:block/cube_all` requires only an ordinary vanilla atlas sprite. Post effect command `/posteffect` exists in 26.3 but a full-screen pass is unjustified for a single altar; overriding core shaders is unsupported. [Minecraft 26.3 release notes](https://www.minecraft.net/en-us/article/minecraft-java-edition-26-3), [Fabric 26.3 notes](https://fabricmc.net/2026/09/15/263.html).

## Release gates
1. Run Python structure/assets verifier, Java unit tests of fail-closed site survey, Java 25 `./gradlew test build`, dedicated `./gradlew runServer`, client `./gradlew runClient`.
2. Manual creative test: `/mkmenv watchtower 1 <x> <y> <z>` in empty loaded area (Y = first air block above ground); repeat 2/3 at other test sites; reload resources (F3+T). Test rejection against artificial block and vegetation. Do not use naked `/place template`. Check pink/black missing textures, loot barrel, ladder, door, platform, leaf decay, mob AI footprints and local particles.
3. Benchmark before/after using same seed, view distance, fixed coordinates and hardware. Record client FPS/1% low with shaders OFF and an OPTIONAL Iris+Sodium shaderpack, server MSPT (F3+L or Spark with version-compatible license), new world chunks/second, chunk file sizes, BE counts, particle counts. Test 0, 1, 16 and 64 site copies in a disposable world. No measurements are available in this research run.
4. **Do not turn on worldgen until** World chat approves biomes, spacing, player-build protection, slope policy and elite/loot ownership.

All 16px textures are procedural original patterns, made specifically for MKM; no texture/structure downloads or external art dependencies.
