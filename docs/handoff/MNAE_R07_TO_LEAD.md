# Lead status — Blender 17-joint to actual Fabric renderer

Source verified R06 annotated tag, Mid Guard chosen for max practical head clearance, derived not overwritten. New R07 Blender pipeline samples constraints/IK on all 43 frames and exports 17 true joint poses and one hand-bound sword socket (rather than six C3 cubes). New experimental ArticulatedAvatarLayer draws 17 independently rotated/translated segments in Minecraft third person on eligible first R sword combo; F8 old C3/new MNAE, F9 adds articulated R07. Old renderer remains for armor, first-person, offhand or unsupported weapons. No gameplay/saves changed.

Main risks: not a full in-world accepted animation; source Blender nine-frame review not game capture. Original avatar skin 64×64 texture hints are rudimentary and must be tested Steve/Alex. Held item uses Blender hand socket but vanilla ItemInHandLayer still adds its own transform — MUST capture actual sword before claiming grip safe. 17-bone mesh currently synthetic rigid cuboids; knees and elbows physically separate, but polygon edges/armor seams and contact constraints need real F5 QA. Same 14 ticks and 5/14 impact.

No user-world upgrade from historical MNAE v2 branch to integrated v3/v4; integration PR #30 must receive only carefully reviewed animation patches. Maintain R06 tag immutable and do not add many clips before R07 passes in-game.
