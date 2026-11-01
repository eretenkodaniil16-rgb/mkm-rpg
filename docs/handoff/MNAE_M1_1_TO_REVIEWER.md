# Handoff for «проверяющий» — MNAE M1.1

Review `feature/mnae-m1-1-transitions-blender-review` against `feature/mnae-m1-native-core-two-attacks` (PR #40). Validate:

- `AnimationPlayer.setSpeedAt(rate,now)`: same pose immediately before/after rate change, monotonic playback afterwards; old `speed(rate)` is invalid mid-clip.
- `PoseMixer.crossfade`: blend union keys including outgoing-only channels to identity, exact final map when alpha >= 1, smooth `stop(now,fadeTicks)`, interrupt-safe replay.
- `NativeClipLoader` <= 256 KiB, <= 48 tracks, <= 256 keys per track, <= 64 markers before allocating poses; malformed/NaN fail closed; embedded C3 loader untouched.
- `VanillaAvatarAdapter` only uses two demo assets on empty hand and `ItemTags.SWORDS`, other items fall back to C3.8. F8 switching cannot double-apply animations, camera, damage or held-item transforms.
- CI Gradle/JUnit, user-visible version 0.0.5-fabric.7, server/client smoke, and true Blender background renders (.blend + 10 PNG) from six-piece *proxy* mesh.
- Check impact 5/14 and that no CombatService/DamageSource/ActionTimelineRules/stamina changes are introduced. In Equipment D3 integration preserve exact heavy-hit provenance and positive unblocked HitConfirm.

Do **not** equate generated Blender proxy frames with real Minecraft 60 FPS gameplay. All eight dodge directions and actual armor/held-item visual output still need independent in-world QA. No main merges.
