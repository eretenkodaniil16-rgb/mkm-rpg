# Handoff · Environment V1 → World and Main

**Base**: World E2.1 Fabric integration PR #45, NOT NeoForge main.
**Implementation**: six registrable decorative blocks (all original 16px pixels), native NBT three-damage variants for one tower/campsite, one rune client particle, one loot table, static resource checks.
**Excluded**: auto worldgen, structure_set registrations, mobs, quest changes, post effects, atlas overrides, Iris dependency, player build replacement rules.

## Request to World chat
Please define (1) approved biomes and exclusions; (2) deterministic placement spacing/rarity; (3) protected-build and claimed-region integration with provenance; (4) slope/soil/hydrology constraints, chunk reservation and unload/restart behavior; (5) encounter marker ownership/reward guards; (6) no-chunk-ticket safety and 2-client regression. Do not add global worldgen yet.

## Request to Main / Integrator
Keep as draft until CI and in-game visual + dedicated server checks. Branch is stacked on experimental PR #45; do NOT merge it to `main`. Validate eventual integration order and coordinate with World and AI on elite spawn. The current prototype has **no live spawn placement or zero-damage guarantee** for admin `/place template`: this must be addressed before natural generation.

## QA / perf status
Static verifier defined; client / dedicated server / shaderpack / performance measurements are pending until CI or an actual run. Do not treat this as an approved playable world segment yet.
