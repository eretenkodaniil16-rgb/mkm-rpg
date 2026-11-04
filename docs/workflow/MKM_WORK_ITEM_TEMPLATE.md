# MKM work item, review and handoff templates v1.0

Applies to new features, defects, experiments and integration tasks. This template may be shortened for trivial changes; do not fabricate stages or results to complete checkboxes.
Canonical stages and evidence levels: MKM_UNIFIED_WORKFLOW.md. Domain-specific gates: MKM_WORKSTREAM_PROTOCOLS.md; animation: MKM_ANIMATION_WORKFLOW.md.

## A. New task / delegation prompt

### Identity

- Task ID / issue / stream:
- Owner and reviewer:
- Branch and base SHA (exact; not just main):
- Minecraft/Fabric/Java versions:
- Data schema(s) on the source branch and intended integration branch:
- Dependencies and consumer/producer contracts:
- Existing world compatibility / sensitive user files:

### S1 — Intent

- Intended user-visible behavior or concrete defect (1–3 sentences):
- Reproduction / acceptance scenario (include failure and boundary):
- Scope and non-goals:
- Server ownership, client presentation, domain of data:
- Acceptance criteria that can actually be observed:

### S2 — Evidence

| Source or reference | Date/version and exact link/file | What it proves | Limitations / license |
| --- | --- | --- | --- |
| Existing owning source |  |  |  |
| Known failing test / user observation |  |  |  |
| Outside reference, only if useful |  |  |  |

Unknowns and explicit assumptions:

### S3 — Design decision

- Smallest viable fix:
- Alternative considered:
- Why the selected approach best preserves reliability and project architecture:
- Affected APIs, schema, resource IDs, event ownership and compatibility:
- Rollback / migration requirements:
- Independent falsification test:

### S4 — Prototype

- Artifact / commit / local reproducible command:
- Expected and observed output:
- What it has NOT tested:
- Evidence status: SPECIFIED / GENERATED / STATIC PASS / RUNTIME PASS / BLOCKED:

### S5 — Implementation

- Exact changed files and why:
- Tests and fixture files added:
- Shared interface agreements and reviewers required:
- No unrelated gameplay/save/renderer changes (or explicit approved exception):

### S6 — Verification evidence

| Gate | Evidence: SHA / CI run / screenshot / save fixture | Status PASS/FAIL/BLOCKED/UNVERIFIED |
| --- | --- | --- |
| Build + unit/contract tests |  |  |
| Server or Blender runtime |  |  |
| Target Minecraft client/gameplay |  |  |
| Multiplayer / network / anti-replay |  |  |
| Save migration / relog / crash replay |  |  |
| Visual UX and reference comparison |  |  |
| Regression of affected neighbors |  |  |

State precisely if a gate is not applicable, not just N/A. For gameplay changes, real in-game execution cannot be replaced by a startup log; for art, a Blender still is not a game animation clip.

### S7 — Correction and decision

- Defects ranked P0/P1/P2, affected phase and causal hypothesis:
- What changed after the last review:
- Regression evidence of correction and unchanged invariants:
- Revert/rollback steps and remaining risks:
- Current status: PROPOSED / PROTOTYPE / DRAFT / VERIFIED / ACCEPTED / BLOCKED.
- Approval: owner / independent QA / integration lead / user (when subjective play feel or art quality matters).

## B. Fast bug-fix variant

Use for a narrow defect without repeating unneeded meetings or mockups:
1. Reproduce with a single source/test/video and state the expected behavior.
2. Identify owning code and baseline branch/schema.
3. Add a failing test or measurable proxy.
4. Implement the smallest causal fix.
5. Run regression + applicable runtime/integration gates and label unrun checks.
6. Repeat if failed; keep draft if no real game evidence.

## C. Cross-stream handoff

A consumer should never be asked to infer a producer contract from UI or temporary code. Handoff must include:
- Producer and consumer; stable event/resource/packet identifiers; server vs client truth and event timing.
- Versioned payload/schema, limits, replay/idempotency rules and explicit failure semantics.
- Exact branch/PR/SHA on which the producer is present; confirm *not merged* vs *actually integrated*.
- Two positive and at least two negative test fixtures.
- Downstream build/migration scenario and rollback boundary.
- What remains BLOCKED and whose approval is required.

Examples:
- Character -> World: keyed grantExperienceOnce receipt; no floor-based XP auto-top-up.
- Combat/Equipment -> MNAE: stable active hand, equipment family and accepted action event; no damage from cosmetics.
- Magic -> MNAE: APPLIED only after real successful healing and server debit; scene/connection reset.
- World -> UI/MNAE: validated dialog state and semantic NPC cues; no client-written quest state.
- MNAE -> QA: Blender scene/manifest plus actual in-game clips/renderer with same camera and hand.

## D. Pull request completion rubric

A docs-only workflow PR may stop at static docs/link validation. A production feature PR may not claim complete until:
- Branch ancestry and integrated SHA match advertised versions and schema.
- Compilers/tests and valid asset limits pass.
- Required dedicated-server, real client, multiplayer and migration gates pass.
- Negative tests (replay, invalid packet, stale event, missing target) match declared contract.
- No new unauthorized client authority; no duplicate damage, XP or modifier logic.
- Every outstanding blocker is explicitly reported and assigned.
- User-facing animation/UX/play-feel is accepted by the user when needed.
- Revert and backward-compatibility risk are understood.

No skipped gate may be relabeled PASS by opinion, successful build, or an image that does not demonstrate the behavior.
