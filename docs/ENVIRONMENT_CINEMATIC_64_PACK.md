# MKM Cinematic 64 (optional, experimental) — original materials

**This pack is OPTIONAL and is not loaded automatically, bundled into the Fabric JAR, or required by the server.**

Generate by running:
```bash
python3 tools/environment/generate_cinematic_pack.py
```

Then install `build/environment_packs/MKM_Cinematic_64_LabPBR_26.3.zip` into the client's Minecraft `resourcepacks/` folder and activate it manually. The pack uses the Minecraft Java **26.3** resource format **97.1** (`min_format: [97,1]`, `max_format: [97,1]`) and original 64× albedo and LabPBR 1.3 normal/specular images for seven original MKM material blocks; two original loot icons also receive 64× color images. It affects no vanilla textures.

**Iris is not provided or required.** LabPBR effects work only with a compatible third-party shaderpack that reads `_n` and `_s` textures; check Iris + shaderpack support for 26.3 and test manually. Without shaders, the optional pack is just sharper diffuse materials. The client-side `CINEMATIC` particle profile does not silently enable this resource pack or a shaderpack.

Materials are original procedural pixels derived from MKM's original 16× base, with controlled 64× detail; no textures from other mods or commercial packs are copied.

### Acceptance
- Screenshot same seed/camera, before/after 16× and 64×.
- Verify `F3+T` reload and no missing texture/path/warnings; test on 26.3 client.
- Compare GPU memory and median/1% low FPS at idle and 40 enemies, vanilla lighting vs verified compatible Iris/shaderpack.
- Ensure no GI/core renderer overwrites or required client dependencies.
- The LabPBR encoding is a **testable art prototype**, not visually certified with Iris; pending manual verification.
