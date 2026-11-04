# MKM RPG Pull Request — evidence and integration checklist

## Identity / baseline
- Goal and player-visible (or engineering) outcome:
- Workstream owner(s) / reviewer(s):
- Base branch + exact base SHA:
- Feature/PR dependencies actually present (do not infer from open drafts):
- Minecraft / loader / Java / mod versions:
- Persistent schema and network-contract versions affected:
- Non-goals and exact ownership boundaries:

## S1–S4: intent, research, synthesis and prototype
- Current problem / reproduction / acceptance scenario:
- Existing implementation and references inspected (links/files, source/rights if media):
- Selected approach vs smallest alternative, with justification:
- Prototype artifact/test/mock/source render and **what it does not prove**:

## S5: implementation
- Changed code/assets/contracts:
- New/updated regression tests:
- Any Character, Combat, Equipment, Magic, World, MNAE or UI handoff needed:
- Compatibility, world backups, migration and rollback plan:

## S6: evidence checklist (write PASS / FAIL / BLOCKED / NOT APPLICABLE with reason)
- [ ] Exact commit SHA, build/CI URL and tests attached
- [ ] Java 25 build and pure tests (when Java/asset logic changed)
- [ ] Fabric dedicated-server startup (when common/server code changed)
- [ ] Real client interaction/GUI or F5 evidence (when user-visible behavior changed)
- [ ] Two-player/owner isolation, stale/replayed packet tests (when relevant)
- [ ] Real save/relog/death/restart and migration from supported schemas (when relevant)
- [ ] Animation source frames + real Minecraft renderer/hand/grip comparison (when relevant)
- [ ] Adjacent subsystem regression tests on the **integrated** commit (when relevant)

Evidence links and exact claims:

| Test | Exact run/build/video/file | Verdict and what remains unverified |
| --- | --- | --- |
| Static | | |
| Runtime | | |
| Gameplay | | |
| Migration/multiplayer | | |

## S7 + G8: correction, review, release decision
- Defect/correction log, with before/after evidence:
- Open P0/P1/P2, unresolved risk and mitigation:
- Independent QA verdict:
- Integrator verdict:
- User approval for aesthetic/play-feel changes (if needed):
- **Decision:** PROPOSED / PROTOTYPE / DRAFT / VERIFIED / ACCEPTED / BLOCKED
- **Release status:** NOT RELEASED / RELEASE CANDIDATE (requires explicit signoff)

Do not equate a successful CI build, generated .blend or a HUD status with accepted in-game animation or gameplay. Never merge experimental Fabric PRs into historical NeoForge main. The authoritative process is [MKM Unified Workflow](../docs/workflow/MKM_UNIFIED_WORKFLOW.md), [Workstream Protocols](../docs/workflow/MKM_WORKSTREAM_PROTOCOLS.md) and [Integration QA](../docs/workflow/MKM_INTEGRATION_QA.md). For animation also read [Animation Workflow](../docs/workflow/MKM_ANIMATION_WORKFLOW.md).
