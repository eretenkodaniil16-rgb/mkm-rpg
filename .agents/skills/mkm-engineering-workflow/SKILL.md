---
name: mkm-engineering-workflow
description: Apply the MKM RPG seven-stage intent/reference/synthesis/prototype/implementation/QA/correction workflow to any gameplay, animation, Blender, magic, character, equipment, world, AI, UI or integration task.
---

# MKM engineering workflow — required for non-trivial project changes

Read AGENTS.md and docs/workflow/MKM_UNIFIED_WORKFLOW.md. Select the relevant domain section from docs/workflow/MKM_WORKSTREAM_PROTOCOLS.md. Read docs/workflow/MKM_ANIMATION_WORKFLOW.md when editing motion, rig, animation, import, Blender preview or MNAE rendering. Use docs/workflow/MKM_WORK_ITEM_TEMPLATE.md to structure tasks, handoffs and PRs.

Before substantial implementation:
1. Determine exact current repository and base branch, existing code owner, current schema and its integration status. Main may still be historical NeoForge while the active tested integration branch uses Fabric 26.3 / Java 25.
2. Define S1 observable success, failure and stop conditions.
3. Complete S2 with current code/tests and genuinely relevant primary sources. For animation, consult multiple complementary motion references with licensing and permissions.
4. Complete S3 by comparing at least smallest safe fix vs alternatives, plus architecture, save, packet, visual and rollback risks.
5. Make a falsifiable S4 prototype using existing infrastructure and without duplicating an engine.
6. Implement S5 strictly in the owning subsystem; server owns gameplay, client owns presentation. Existing public IDs, combat timelines, CharacterData and MNAE boundaries are not modified silently.
7. Perform S6 tests as appropriate, separating generated output, static CI, actual Blender runtime, actual Minecraft frames, multiplayer, persistence and user approval. Never promote one layer of evidence to another.
8. If the test fails, S7 must identify root cause and rerun the relevant stages. Do not treat incremental angle tweaks as fixes for a broken coordinate/weapon bridge.

Domain release blockers include any XP duplication or loss, wrong schema upgrade/downgrade, extra damage from animation, untrusted client effects, duplicate item grants, malformed input accepted or unverified target gameplay/visual QA when required.

Deliver task status in precise categories: PROPOSED, PROTOTYPE, DRAFT, VERIFIED, ACCEPTED or BLOCKED. A successful Blender render does not mean an animation is integrated; a green CI build does not mean game-playable.

For trivial docs-only changes use the lightweight procedure in MKM_UNIFIED_WORKFLOW.md; no performative research. If laptop tools are unavailable, use GitHub/CI and state which real device checks remain blocked.
