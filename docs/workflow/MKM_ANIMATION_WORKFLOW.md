# MKM Animation Workflow v1.0 — обязательный цикл от движения до игрового кадра

Applies to attack, parry, shield, dodge, roll, locomotion, spellcasting, NPC gesture, death, camera-assisted visual cues and authored item motion.
General gates S1-S7 and evidence statuses: MKM_UNIFIED_WORKFLOW.md.
Current central blockers: Issue #43, PR #36 (typed presentation), MNAE PRs #40/#41/#46, MNAE QA #38/#44, experimental Blender toolchain PR #47 and pilot PR #48. These are separate drafts; none is assumed merged.

## A. Seven ordered stages

### 1. Intent / Motion Plan

Before Blender, create a motion brief:
- Name, user action, weapon family, attacking hand/arm, left-handed behavior, first-/third-person and observer view.
- Purpose/character: sharp/light cut, heavy axe weight, evasive step, defensive recovery, etc.
- Sequence of readable poses and movement phases; foot support, pelvis-to-shoulder transfer, weapon handle/socket and weapon-tip arc.
- Required event alignment and units: animation FPS, action ticks at 20 TPS, visual contact frame, server-authoritative impact event if any.
- Invariants: game hit detection, player movement, stamina, damage and hitbox unchanged unless Combat explicitly authorizes separately.
- Visible success: player perceives load, acceleration, impact, inertia and balanced recovery, and the item does not detach/clip.

### 2. Reference Search / Evidence

Gather multiple **distinct** real references where they improve decisions: real martial movement (safe stage-combat demonstration), high-quality game cinematic, licensed mocap, films, skeletal studies, accessible animation clips, Minecraft mods, drawings, and observed in-game baseline.

At minimum identify the best source for:
- overall move/choreography;
- key pose silhouettes and support;
- timing/weight/inertia;
- weapon grip/tool orientation.

For each reference store: source URL or approved private local path, description, timestamp/range and view, evidence contributed, license/permission to copy vs only to observe, limitations and confidence. Avoid public uploading of private videos and unlicensed copying of complete animation data. If the internet is unavailable, mark S2 partial and use lawful existing local footage, do not fabricate references.

### 3. Synthesis / Original Choreography

Extract useful properties without tracing an external animation: anticipation from A, grounded transfer from B, weapon weight from C, readable Minecraft silhouette from D. Define an **original** motion, not a repackaged animation.

Write a compact pose timing table: idle -> load -> windup -> acceleration -> visual contact -> follow-through -> recovery. Specify body segments, stance/support, gaze/head, weapon socket and trajectory in each phase. Determine whether this should be a cut, thrust, dodge step or roll. Preserve item-family identity: axe must not use sword fallback silently.

Reject choreography based on rigid whole-body spinning, implausible sword travel through head, simultaneous unmotivated joint movement or feet hovering. If no body/weapon hierarchy is available in the runtime, choose an honest limited fallback or specify a new renderer bridge, rather than claiming AAA articulation.

### 4. Visual Prototype / Storyboard

Create at least one original 2D storyboard or rough Blender preview for a new complex motion. Use 5-9 key poses (at least nine timeline samples in the final visual audit), two viewing angles where helpful, silhouettes and sword-tip path. Use reference for the **idea**, not copyrighted frame-for-frame reproduction.

Before any in-game export, inspect a 60fps-equivalent timed animatic or render sequence if accessible. Static pose sheets alone do not establish velocity, collisions or temporal quality; compare intermediate frames and play a continuous source animation whenever possible.

Artifacts: motion-brief, reference register, pose table, image contact sheet, source Action or executable Blender generator, and the exact camera/FPS.

### 5. Implementation / Blender to MNAE

- Author with hierarchical rig, elbows/knees/wrists if present, body center of mass and weapon grip socket, two-hand state and IK support. Keep left/right naming and coordinate conventions explicit.
- Keep the original editable .blend and script/asset source. Bake only documented channels and marker semantics, with deterministic serialization and resource limits. Use original Blender->MKM six-part exporter only for the compatible *six-part* path.
- Evaluate current MNAE world transforms, pose composition, actual held-item mapping and active hand. A 17-bone Blender authoring source cannot be rendered as 17 bones by vanilla six ModelParts. Opt-in articulated mesh requires separate approval, skin/armor compatibility, and game-frame tests.
- The animation layer consumes Combat's visual events; it never invokes attack/damage, manipulates collision, allocates XP or decides magic success. Avoid duplicate C3/vanilla overlays.

### 6. Quality Control / Independent Visual QA

Source tests:
- keyframes finite/bounded, interpolation continuous, neutral/transition behavior;
- intended shoulder/pelvis counterrotation, hand-to-handle drift, blade-tip path, head/armor intersection, knee/elbow articulation and planted-foot drift;
- timing matched to declared gameplay reference; no misleading client-authored impact;
- screenshot/contact sheet at >=9 frames, including original, anticipation, actual visual contact, follow-through, recovery.

Minecraft tests (separate mandatory gate for released animations):
- Real 60fps F5 frames, fixed camera, actual installed Fabric JAR SHA and identical action/gear, F8 A/B when applicable;
- right/left handed, sword/axe/pickaxe/bow/shield/unarmed/offhand-only, stages, locomotion, eight dodge directions, first person and remote observer if in-scope;
- actual renderer/clip and reason for fallback, item hand grip, local/world conversion, armor, GUI and ground contact;
- dedicated server and two-player safety for networked presentations.

Classify each result Blender PASS / runtime PASS / GAMEPLAY PASS / BLOCKED / FAIL. A unit test, Blender proxy and HUD POSE APPLIED are not framebuffer evidence.

### 7. Correction / Iterative Motion Refinement

Find actual visual mismatch (prefer frame/time and before/after evidence); classify its origin:
1. bad reference or motion design -> return S2/S3;
2. bad skeletal authoring, foot IK, curves, socket -> return S4/S5;
3. importer/model-space/hand routing/render adapter -> fix appropriate engine bridge with a targeted regression;
4. server action and cosmetic timing mismatch -> explicit cross-owner contract review;
5. camera/armor/item overlap or observer missing -> targeted visual integration QA.

Do not hide a mapping defect by exaggerating angles. Re-render the same contact frames, compare identical conditions, and repeat until relevant visual and game gates are accepted. A second iteration being prettier in Blender is not sufficient for release.

## B. Animation passport (one per clip)

Use the following fields:
- Clip ID / source .blend / owner / version / baseline commit / action family / hands;
- intent and phases; references and reuse rights;
- exact FPS, frames, action and impact ticks, root-motion contract, supported renderer path;
- rig hierarchy/coordinate conventions, weapon grip socket and important contacts;
- image/video evidence paths and camera, numeric proxy checks, outstanding uncertainties;
- status and reviewer verdict: SPECIFIED / GENERATED / STATIC PASS / BLENDER PASS / GAMEPLAY PASS / ACCEPTED / BLOCKED;
- corrections log with frame numbers and exact changed conditions.

## C. First application: Sword Slash R01

The R01 scene in draft PR #48 is an experimental articulated source, not a shippable MNAE asset. It was generated and reviewed in Blender; no Minecraft 17-bone runtime acceptance exists.

Next steps under this process: rework the body/weapon choreography using researched references, render a 9-frame fixed-camera sequence plus a played animation, inspect clearance and foot contact, then separately solve Issue #43 grip/render bridge before any integrated game delivery. Do not retroactively mark the existing source as accepted.

## D. Acceptance stop conditions

Do not accept if the sword clips the head, equipment family is wrong, weapon detaches, feet float/slide beyond reviewed limits, the screen image differs from declared source transforms, the server authority changes, the same hit fires twice, or the test only proves a Blender six-part proxy. Aesthetic approval requires the user's review.
