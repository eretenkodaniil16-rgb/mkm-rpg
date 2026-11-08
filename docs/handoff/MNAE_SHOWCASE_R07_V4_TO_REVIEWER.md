# Независимому проверяющему — Issue #73 / PR #84

Base strictly QA5 PR #69 CharacterData v4 SHA 440dfe07; no CharacterData, CombatService, server packet, kill XP, mana ledger, stamina, hitboxes, saves modified. Verify via git comparison.

Read exact PR #63 old 17-joint architecture, PR #46 weapon routing, PR #49 review stages, Issue #43 known bug and PR #70 115 decisions. Frozen R06 tag mkm-guards-r06-approved commit 5cae0c0 and Blender SHA 024d2f85.... Compare 43 Blender sample goldens and per-frame socket.

Check client-only F8/F9 gates, model Part visibility saved/restored, 17 independent geometry, actual RenderLayer submission and skin texture path, held ItemInHandLayer transform and real sword/head/armor clearance. For mainhand axe and offhand sword selected with empty mainhand, no false sword profile. Back/side/front dodge prototypes contain no rigid 360 and no changed server impulse.

Explicitly **do not** pass E4 without true 60fps F5 3-mode evidence, do not pass spectator gate without S2C confirmed clip observer behavior, do not sign off armor/Alex/left without frames. Use tool capture_f5_comparison.py. Fail on missing body/weapon, overlaid six-part body, wrong impact, duplicate damage or world migration.

CI compile/boot is E3 only. Never merge historical NeoForge main.
