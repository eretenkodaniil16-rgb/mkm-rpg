## MKM seven-stage change record

See docs/workflow/MKM_UNIFIED_WORKFLOW.md and docs/workflow/MKM_WORKSTREAM_PROTOCOLS.md. For animation see docs/workflow/MKM_ANIMATION_WORKFLOW.md. Complete relevant fields only; for a trivial/docs-only PR describe why other gates are not applicable.

### Identity and scope

- Domain / owner / independent reviewer:
- Base branch + SHA / dependencies / source PRs:
- Minecraft / loader / Java / persisted schema:
- Intended behavior, failure case and **non-goals** (S1):

### References and design (S2-S3)

- Current owning code/tests and external references or motion sources (with versions/licensing where relevant):
- Alternatives, decision, compatibility/ownership constraints:
- Assumptions, risks and rollback:

### Prototype and implementation (S4-S5)

- Reproducible minimal demo/negative test:
- Exact changed source files or assets and key contracts:
- Cross-domain consumers/producers, including gameplay vs presentation authority:

### Verification and correction (S6-S7)

| Gate | Evidence (exact SHA, CI URL, file/video/test) | PASS/FAIL/BLOCKED/UNVERIFIED |
| --- | --- | --- |
| Static tests / build | | |
| Blender or target runtime | | |
| Minecraft real client / F5 / user-visible behavior | | |
| Dedicated server / 2-player / packet replay | | |
| Save migration / relog / crash recovery | | |
| Neighboring-system regression | | |

- Observed defects and corrections (before/after):
- Outstanding P0/P1/P2 blockers:
- Current status (PROPOSED / PROTOTYPE / DRAFT / VERIFIED / ACCEPTED / BLOCKED):
- Required final approval (owner, integration QA, user for play-feel or animation):

**Do not merge on green CI alone.** Historical NeoForge main, newer-save downgrade, client-authoritative gameplay and unfinished visual QA are stop conditions. Clearly distinguish headless startup, Blender previews and actual gameplay acceptance.
