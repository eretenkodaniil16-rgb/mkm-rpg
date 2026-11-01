# MNAE M1.1 QA: PR #38 compatibility assessment

Target: PR #41 `3335e788`. Ported **only** mathematical/legacy C3 compatibility classes and QA test from the independently tested PR #38 at `5508554d`:
`Transform`, `NativeClip`, `LegacyC3Importer`, `PoseSampler`, `NativeAnimationCoreContractTest`, plus one assertion in the pre-existing native math test.

Two requirements from PR #38 are compatible with M1.1's present authored assets (all native scales 1): reject anisotropic TRS scales rather than lose shear, and preserve original C3 v1 PCHIP interpolation on import. Native format-v2 retains its authored Hermite/SQUAD curves.

**Intentional exception:** MNAE M1.1's `NativeClipLoader` 256 KiB preparse cap is STRICTER than PR #38's 1 MiB cap and has been preserved without rewriting loader. PR #41's `AnimationPlayer` transition changes are also preserved untouched.

These fixes address mathematical contracts only, not real weapon grip, native hierarchy rendering, actual use of AnimationPlayer in the model adapter, Blender-to-Minecraft transform conversion, or ground contact in the legacy 360° roll. The pure `GripSpaceMath` golden is not a rendered pose proof.

Before accepting any integrated JAR: in-world F5 A/B at same camera and with correctly selected mainhand sword, offhand equipment and actual first-person/multiplayer bounds, plus dedicated-server/client CI. Keep CharacterData v3 from PR #30 until an approved v4 migration.
