# MNAE M1 — independent QA / integration handoff

Branch `feature/mnae-m1-native-core-two-attacks`, base C3.8 `feature/c3-8-pose-evidence-motion-qa`; architecture issue #33/PR #34; integration base PR #30. Do not merge to main.

Verify first-party Java source (no PAL or dependency), immutable core math and complete channel validation; bounded keys, finite transforms, quaternion slerp shortest arc, skeleton DAG cycles/missing parents, priority/mask compositing, deterministic normalized server time.

Inspect F8 A/B: C3 default, MNAE only combo step 1 sword/unarmed; no double ModelPart rotation, valid fallback on malformed v2 resources, pose debug HUD accurate, perspective limited to third person. No new packets. Keep all 15 old C3 assets and 8 dodges intact. Check left-handed, armor sleeve/pants pivot copy; check that torso visual offset does not dislocate body parts.

Cross-domain: PR #23 D3 exact heavy source provenance must survive combination with C3.6 server-confirmed HitConfirm. MNAE **does not** touch either. Do not allow animation events to execute damage.

Run Gradle/JUnit, Python --verify and 2-clip side-by-side schema QA, server and headless client smoke. Interactive F5 60fps same camera/video for C3 and MNAE required before visual acceptance. Real Blender authoring .blend + rendered comparison requires installed Blender: not asserted by CI.
