# Report for «главный» — MNAE M1.1

Two MNAE demos remain unchanged in count and server timing. Correctness hardening added: timeline-preserving speed changes, masked crossfades without popping outgoing bones, bounded v2 input, native sword tag classification, proper Blender constrained proxy geometry and first automated Blender frame artifact.

Still **not** approved as cinematic: client smoke doesn't capture in-world strikes. Run 60fps F5 C3 vs MNAE on the same camera with sword and empty hand, then test left handed, shield, armor and locomotion. Watch for stiffness and vanilla six-joint limits. Keep normal LMB and first-person implementation gated until film review to avoid damaging the vanilla attack/mining path.

No gameplay files touched, no main merge. Draft PR stacked on #40.
