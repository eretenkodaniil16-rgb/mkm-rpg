# MNAE M1 — summary for «главный»

Implemented new first-party pure animation core with quaternion, hierarchical skeleton transform, immutable clip, sampling, additive/override layers, time-driven player transitions. Two native authored v2 clips. A/B mode F8 default legacy, limits new engine to first R combo attack, preserves all other old C3 animations. Java 25/Fabric CI pending at drafting; acceptance depends on PASS and actual Windows Minecraft 60fps F5.

Why not yet cinematic: native six-part ModelPart still lacks knee, elbow, wrist, true foot support and actual local skeleton hierarchy in vanilla renderer. Translation is a constrained visual illusion. Need explicit artist-verified Blender .blend and real F5 visual comparison; CI thumbnails are schematic, not game frames.

Defer normal LMB to next step to avoid duplicate vanilla damage/mining regression. First-person only after successful third-person test, hand/item/armor conflict audit; server physics/combat remain unaffected. Coordinate staged merge with integration PR #30 and D3 heavy provenance PR #23. Do not merge into main.
