# For «главный» — Issue #43 P0 animation status

The old recording did not provide controlled same-weapon C3/MNAE A/B: the sword moved from mainhand to offhand while mainhand became empty. Code confirmed axe/pickaxe routed to legacy sword clip; this patch explicitly fixes profile selection, arms and selected active MAIN_HAND semantics. New renderer HUD reports actual C3/MNAE/VANILLA path and reason, not only F8 toggle. Offhand-only weapon + empty selected mainhand deliberately suppresses unverified special unarmed combo rather than allowing held sword to flick behind head.

MNAE bridge now evaluates actual hierarchy (body/head/shoulder runtime pivots) instead of six independent raw rotations. Pure `WeaponGripSocket` provides testable model-space nominal handle/blade tip locations but NOT confirmed Minecraft held-item layer or in-world tip clearance. This means **P0 visual acceptance is still pending**; no honest claim that sword behind head is eliminated until real video is reviewed. Seven item family fixtures and 15 legacy C3 exact sample checks from PR38 are required.

Rolls still rigid six-cube 360° world spins. No physical/visual roll change until filmstrip and ground contact QA. No new clips, server damage/timing, stamina or physics changes. This branch's v2 saves must not replace integrated #30's v3 world. Do not merge main.
