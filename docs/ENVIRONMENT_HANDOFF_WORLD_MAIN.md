# Handoff · Environment V1 → World and Main

**Base**: World E2.1 Fabric integration PR #45, NOT NeoForge main.
**Implementation**: six registrable decorative blocks (all original 16px pixels), native NBT three-damage variants for one tower/campsite, one rune client particle, one loot table, static resource checks.
**Included after initial prototype**: GM-only `/mkmenv watchtower <variant> <x> <y> <z>` no-overwrite preflight and 4-block foundation adaptation (source/test in `environment/WatchtowerSiteRules`).
**Excluded**: auto worldgen, structure_set registrations, mobs, quest changes, post effects, atlas overrides, Iris dependency, player build replacement rules.

## Request to World chat
Please define (1) approved biomes and exclusions; (2) deterministic placement spacing/rarity; (3) protected-build and claimed-region integration with provenance; (4) slope/soil/hydrology constraints, chunk reservation and unload/restart behavior; (5) encounter marker ownership/reward guards; (6) no-chunk-ticket safety and 2-client regression. Do not add global worldgen yet.

## Request to Main / Integrator
Keep as draft until CI and in-game visual + dedicated server checks. Branch is stacked on experimental PR #45; do NOT merge it to `main`. Validate eventual integration order and coordinate with World and AI on elite spawn. Vanilla admin `/place template` **bypasses** the new guard and remains potentially destructive. `/mkmenv watchtower` performs fail-closed AIR volume inspection, loaded-chunk checks and non-destructive foundation extension, but this is not a full claim/provenance authority or atomic rollback. No live spawns yet.

## QA / perf status
Static verifier defined; client / dedicated server / shaderpack / performance measurements are pending until CI or an actual run. Do not treat this as an approved playable world segment yet.
