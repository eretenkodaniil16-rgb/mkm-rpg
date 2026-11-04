# MKM Unified Development Workflow v1.0

Status: proposed mandatory process standard, documentation-only draft.
Scope: MKM RPG on Minecraft 26.3 / Fabric / Java 25; Character, Combat, Equipment, Magic, Animation/MNAE, World/NPC/quests, AI/encounters, UI/UX, infrastructure, QA and release.
Normative entry points: this file, MKM_ANIMATION_WORKFLOW.md, MKM_WORKSTREAM_PROTOCOLS.md, MKM_WORK_ITEM_TEMPLATE.md and root AGENTS.md.

## 0. Architecture and baseline first

- The active experimental cross-system basis is PR #30 / integration/fabric-26.3-c3.7-all-systems-v1. It is still a draft; historical main and old NeoForge 26.2 documents are NOT release targets for Fabric 26.3 work.
- Other PR branches are **not implicitly integrated**. A reviewed, successful source PR does not make its code available in another branch. State the exact base branch, prerequisite PRs, source SHA, target Minecraft/Fabric/Java version and save schema in every non-trivial work item.
- Only the server owns damage, stamina/mana debit, progression, persistence, quest rewards and world effects. The client owns presentation, inputs and local animation only. Preserve one source of truth for each state or calculation.
- CharacterData v2, v3 and v4 exist in different draft branches. Never install a standalone older-schema artifact over a newer world without tested migration and backup. Any non-lossless downgrade is blocked.
- Existing source code and tests outrank obsolete design prose for describing actual implemented behavior. Resolve contradictory documentation before proposing broad rewrites.
- No experimental animation, AI asset, build output, cosmetic cue or reference material may become gameplay authority.

## 1. Seven-stage universal loop

### S1 — INTENT: define desired outcome and non-goals

Record one concrete user-visible or engineering outcome, current failure, player scenario, impacted systems, acceptance criteria, assumptions and prohibited changes. Define the active owner and system of record for gameplay and persisted data.

Exit gate G1: clear bounded objective; expected behavior stated for success, failure and boundary cases; all blocking constraints named.

### S2 — REFERENCES: investigate before proposing

Inspect current branch code, tests, relevant internal ADRs/issues and the actual dependency graph. Where useful, gather external primary references: official engine/loader/API docs, reputable game motion footage, licensed mocap, accessibility guidance or published technical research. Treat discovered examples as evidence, not code to copy.

For every consequential reference capture URL/path, what it establishes, version/date when relevant, license/usage rights where applicable, and relevant limitation. User-provided private videos/assets remain private; never upload to public issues without permission.

Exit gate G2: existing implementation and at least one genuinely relevant baseline/reference are understood; missing information and source limitations are explicit. For tiny bug fixes, a reproducing test and owning code may suffice; external research is not ceremonial.

### S3 — SYNTHESIS: compare and design

Compare at least the smallest safe fix with larger alternatives, based on correctness, user quality, compatibility, cost, maintenance and regression risk. Choose one design; identify data ownership, API/contracts, migration, inter-stream dependencies, test strategy and a rollback plan. Separate hard facts from opinions and hypotheses.

Exit gate G3: one implementable plan, reasons for rejection of risky alternatives, expected tradeoffs, and no duplicate engine/ledger/resolver without documented approval.

### S4 — PROTOTYPE: validate hardest uncertainty first

Use the least costly vertical slice that can disprove the approach: a unit reproduction, an isolated Blender storyboard/scene, UI mock, simulated migration, single NPC encounter, measured combat interaction or one spell. Set observable falsification criteria, not just a success demo.

Exit gate G4: evidence from an actual prototype plus its limitations; if unavailable, label as PROPOSED or BLOCKED and do not assert a working implementation.

### S5 — IMPLEMENTATION: integrate with owned contracts

Implement the minimal scoped change on a correct base. Preserve public IDs, server-client boundaries, safe persistence and player-visible semantics unless explicitly approved. Add test fixtures, documentation and instrumentation. Keep feature PR isolated from unrelated animation, balance, worldgen or content expansion.

Exit gate G5: code reviewed for ownership, feature-level unit tests, compilable assets where relevant, and no unreviewed changes outside the owned subsystem.

### S6 — SELF-QA + INDEPENDENT QA: prove relevant behavior

Run automated checks appropriate to scope, including Java 25 build/JUnit where Java changes, serialization/replay/migration as needed, Fabric dedicated server, real client for client-facing code, and 2-player tests where state scope matters. Verify integration SHA, not only source-branch green CI.

Establish a claim/evidence ladder:
- SPECIFIED: concept and acceptance tests described.
- GENERATED: code/images/assets produced, not yet executed or inspected.
- STATIC PASS: lint/unit/format/contract tests pass.
- RUNTIME PASS: application or Blender actually executes and output is observed.
- GAMEPLAY PASS: target behavior is observed in a real Minecraft client/server with exact build and scenario.
- ACCEPTED: designated integration/QA owner and, for subjective visuals/play-feel, user have approved.

Do not label static CI an animation, UX, two-client, save migration or gameplay PASS. An independent reviewer should seek a negative/counterexample, not merely restate success. Attach failures, logs and test matrices.

Exit gate G6: required evidence exists for the claimed status; otherwise label missing gates BLOCKED / UNVERIFIED.

### S7 — CORRECTION: amend, regress, re-evaluate

Prioritize P0 safety/data-loss/wrong-authority > P1 broken user behavior > P2 polish. Reproduce each defect; fix the smallest causal component; add a regression test; re-run the relevant stages. A failed prototype can return directly to S2/S3, not just endless value tweaking. Record decisions, changed assumptions and the before/after comparison.

Exit gate G7: no unacknowledged P0, required user-visible acceptance met, migration/rollback understood, integration approval given. Otherwise remain draft; no release claims.

## 2. Lightweight vs full execution

- Trivial typo or narrow docs-only correction: record S1, inspect owning file (S2), implement (S5), check links/tests (S6), do S7 only if needed. No forced mockups or research.
- Bug fix: S1 reproduction -> S2 code/test evidence -> S3 smallest repair -> S4 failing regression test -> S5 fix -> S6 regression/runtime -> S7 if failure.
- New visible feature, asset, schema or cross-subsystem change: complete S1-S7 and gate records, with visual/interactive acceptance where relevant.
- Research/proposal-only issue: finish at S3 with status PROPOSED; do not claim S5/S6.
- Agent cannot access laptop or live client: deliver source, tests and runnable CI artifacts, label unverified real-world gates; never invent live render evidence.

## 3. Ownership boundaries and review vetoes

| Domain owner | Authority | Must not own |
| --- | --- | --- |
| Character | Player attributes, XP, progression, receipts, modifier snapshot contract | Animation state, quest/world decisions or custom combat damage |
| Combat | Hit eligibility, action timeline, damage/stamina/guard, accepted actions | Cosmetic animation sampling or duplicated spell/XP engines |
| Equipment | Typed item family, equipment effects and stable item contracts | A second modifier calculator or client-decided damage |
| Magic/Abilities | Ability eligibility, resource spend, effect and cooldown; cosmetic event semantics | MNAE skeleton, damage bypass, independent action timeline |
| Animation/MNAE | Rig, pose, blending, weapon grip and presentation | Gameplay damage, collision, stamina or persistent CharacterData |
| World/NPC/Quests | Authoritative choices, quest states, encounter identities and once-only reward requests | Character XP ledger, animation player or client-authored success |
| AI/Encounters | Server-side threat, navigation and bounded encounter behavior | Unbounded per-tick global scans or double rewards |
| UI/UX | Read-only synced presentation, validated player intents and accessibility | Mutable client copies of gameplay truth |
| Integrator/QA | Conflict resolution, cross-system verification, release gates and artifact provenance | Silent schema downgrade or bypassing failed gates |

Cross-domain changes need an explicit written consumer/producer contract and both owners' review. Every visual cue names who produces and who consumes it. A draft PR creates no implicit dependency resolution.

## 4. Deliverable and evidence contract

Every work item should leave:
1. Issue/PR identifier, objective, base SHA, branch, dependencies and domain owner.
2. Evidence from S2 and selected S3 approach; assumptions, licenses and known uncertainties.
3. Source artifact or code delta and acceptance test fixtures.
4. S6 results labelled by evidence level, with CI URL/SHA and what is *not* tested.
5. P0/P1/P2 defects, next corrective action, migration/backup/rollback when relevant.
6. Explicit state: PROPOSED / PROTOTYPE / DRAFT / VERIFIED / ACCEPTED / BLOCKED; do not equate CI green with MERGEABLE or RELEASED.

Use MKM_WORK_ITEM_TEMPLATE.md and the repository PR template. For unverified outcomes, do not generate an acceptance badge or claim success. Distinguish authored renderer preview from final Minecraft frame; similarly distinguish a simulated save roundtrip from real save/reload and crash-recovery.

## 5. Change control

- No merging into historical NeoForge main, force pushes to shared integration branches, unapproved changes to gameplay mechanics, or downgrade of user saves.
- P0 failure, missing authoritative ownership, breaking schema migration, invalid packet acceptance, replayed reward, doubled damage, client-only dependency in server code, or unverified game render must block the affected release.
- Research into other games, mods, films and motion libraries is permitted for reference/synthesis; respect copyright, usage licenses, and privacy. Prefer original authored assets; do not automatically vendor third-party code or animations.
- Integration order follows verified dependencies, not PR number or enthusiasm. Independent QA must review the exact integrated commit and provide reproducible evidence for user-critical behavior.
- Amend this process through a focused docs PR with rationale, examples and version increment; different workstreams may specialize but not weaken ownership and release gates.

## 6. Practical starting points

- Motion/Blender: MKM_ANIMATION_WORKFLOW.md and Issue #43.
- Character / Combat / Equipment / Magic / World / AI / UI / QA: MKM_WORKSTREAM_PROTOCOLS.md.
- A new work item, delegation prompt or review request: MKM_WORK_ITEM_TEMPLATE.md.
- Code agent: root AGENTS.md, then relevant domain sources and these normative process documents.

All other historical MKM design documents remain domain references; this workflow does not automatically mark any existing draft PR complete.
