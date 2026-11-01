# For «проверяющий» — Issue #43 P0 independent integration gate

Review draft stacked branch `feature/mnae-p0-weapon-grip-render-contract` against PR #41 and math PR #38. Test:
1. `WeaponPresentationResolver`: no `nonempty == sword` in runtime; correct MAIN_HAND vs physical left/right; offhand-only sword never triggers jab or sword; axes/tools/bow/shield get neutral, not sword. All combo stages and dodge.
2. `LocalCombatAnimation.frame` uses resolver once; `PlayerModelAnimationMixin` applies one path, protects secondary hand, avoids C2.1 sword fallback for unsupported weapons. HUD uses actual render probe, active family, hand, step and fallback. Probe means pose at setupAnim, not actual held-item framebuffer.
3. Runtime `Skeleton.world` turns torso and moves head/shoulder pivots. Validate per-instance pivots, left-handed mirroring, vanilla yaw/head look, sleeves/jacket/armor, actual held-item layer. Pure nominal `WeaponGripSocket` is NOT proof sword correctly attaches or avoids head. Verify actual item transform before acceptance.
4. PR #38 port: all 15 C3 clips at 201 timestamps quaternion parity; uniform scale/shear rejection. M1.1 preserved speed/interruption and 256KiB JSON limit.
5. Preserve native loader, no PAL dependency. No changes to CombatService, damage, stamina, physics, server S2C. Do NOT merge into integrated PR #30 v3 schema without picking animation-only changes and validating Character v3/v4 migration.
6. Complete 60fps actual Minecraft F5 A/B controlled mainhand sword, unarmed two-empty-hands and two roll directions plus offhand sword, left handed, shield and armor. User's private video is not to be published without permission.

Report P0 complete ONLY after real weapon grip rendering and contact QA prove no tip/head or armor intersections. Current CI is mathematical/boot proof, not gameplay.
