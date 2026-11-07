# Reviewer handoff — R07 vs PR #46, R06 frozen source

- Do not alter tag `mkm-guards-r06-approved` or its approved .blend, nine PNGs, manifest; verify peeled commit `5cae0c0dae1daf1bc7cfe07945af1fb67ea741b8`.
- CI Blender export source from this exact tag and 43 frames/17 bones, original Mid Guard frame 20, strict v3 JSON, 14 tick marker 5/14 and readable Blender nine-frame storyboard. Check v3 world quaternion and local-space parent identity with independent matrix goldens, hand socket relative to actual grip/tip source mesh and no frame anomalies.
- Check 17 independent model joints `ArticulatedAvatarLayer`, LayerDefinition cached once, rendering skin, no extra per-frame mesh allocation; ModelPart skipDraw reset on every frame, safe fallback if layer missing or slot invalid.
- Test F8 C3/MNAE and F9 opt-in only mainhand sword combo1 no offhand, no armor, third person; no changes to vanilla LMB or server CombatService, ActionTimelineRules, damage, stamina, hitbox, root movement, saves.
- Carefully inspect `PlayerModel.translateToHand` transform in 26.3: vanilla item layer has additional transform after this call. Test whether actual sword root/grip/tip calibration matches the hand. If not, fail P0 visual gate, do not silently move clip angles.
- Need real Minecraft F5 60fps recording same input/gear/camera with C3.8, six-part MNAE, and R07; record head, handle/tip, left physical main arm, Steve/Alex, armor fallback, offhand shield fallback, camera. Blender proxy frames are **not** sufficient.
- Branch inherits CharacterData v2 from PR #46, while integrated PR #30 has v3: DO NOT merge/shipping as upgrade. Cherry-pick visual-only files after Character v4 migration/regression and Equipment D3/Combat #36 review.
- PR49 evidence stages: Source/E1, independent math tests/E2, client boot/E3, video in-world/E4, merge/safe-world/E5. No main merge until E4+.
