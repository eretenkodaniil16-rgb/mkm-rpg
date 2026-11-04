# MKM Sword Slash R02 — reference register and motion plan

Status: ORIGINAL BLENDER AUTHORING EXPERIMENT (NOT IN-GAME ACCEPTED). Parent PR #48, branch stacked on R01, Fabric 26.3/Java25. Process reference PR #49 is separate and unmerged.

## G0/S1: objective and constraints

One-handed right-mainhand diagonal cut, attacker-right high to attacker-left low, visually legible from front and 3/4; not a theatrical full-body spin. Start guarded, twist hips/shoulders, lift sword **outside** right temple, short left foot forward step, sword pass through **front** hemisphere without face strike, controlled follow-through, return to guard. Keep right foot planted; server gameplay, damage, hitbox, stamina, action timeline and persisted records unchanged. MNAE Issue #43 still blocks game acceptance and articulated model rendering.

14 game ticks at 20 TPS = 0.70 seconds = 42 intervals at Blender 60 FPS, frame 1 through 43. Visual contact is at server reference tick 5 = frame 16. This is a reference marker only: Blender never produces gameplay damage.

## S2: external sources and permissions

| ID | Source | Evidence used | Caveat |
| --- | --- | --- | --- |
| H1 | Richard F. Burton, A New System of Sword Exercise for Infantry (1876), sword cutting diagrams, public-domain text: https://www.gutenberg.org/cache/epub/59336/pg59336-images.html | Clear high-to-low lines and recovery to guard | Static historical illustration, not our character animation |
| H2 | Charles Roworth, The Art of Defence on Foot (1804), cut I described as downward right-to-left: https://www.scribd.com/document/505216212/Roworth-the-Art-of-Defense-on-Foot-With-the-Broad-Sword-and-Sabre-1804 | Independent support for intended diagonal direction and guard recovery | Historical text transcription; not a licensable modern motion clip |
| M1 | Better Combat original animation authoring guide https://github.com/ZsoltMolnarrr/BetterCombat/blob/26.2/animations/README.md | Separate upswing, impact tipping point, hit-end and recovery; deliberate speed/impact alignment | Their example duration 20 ticks differs from our 14; no source animations imported |
| M2 | Epic Fight authors documentation https://github.com/Antikythera-Studios/epic-fight.github.io/blob/main/docs/API/Starting.en.md and https://github.com/Antikythera-Studios/epicfight | Demonstrates articulated Blender rig and game animation export as comparative architecture | Epic Fight assets have restrictive redistribution terms; no code, models, or animations copied |
| I1 | MKM Issue #43 https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/43 and R01 PR #48 | Actual MKM weapon/hand mismatch, poor R01 grip and animated performance shortcomings | Private player recordings must never be published to public GitHub |

Limitations: H1/H2 do not validate target timing or full-body biomechanics; M1 is an animation architecture analogue, not proof of R02 efficacy. The original sequence below is our design hypothesis awaiting recorded tests.

## S3: original synthesized choreography

- **Guard:** low-right blade, stance shoulder width, knees unlocked, head facing opponent.
- **Load:** pelvis starts the turn; shoulders counter-rotate; sword moves right and OUTSIDE the face line.
- **High guard:** forearm bends, blade extends high-right, tip well away from head and back.
- **Accelerate:** hips unwind before chest; left foot performs a small grounded step while the right foot remains supported.
- **Visual contact:** hand cuts forward in front of face at 5/14; blade direction moves toward lower left across target, not character head.
- **Follow:** sword continues, decelerates below the center line; non-weapon arm counterbalances.
- **Recover:** arms and pelvis settle; guard is stable for the next action. No 360 spin, no game root translation.

| Frame @60fps | Game tick (approx) | Phase | Visible action |
| ---:| ---:| --- | --- |
| 1 | 0 | READY | Sword low-right, both feet settled |
| 4 | 1 | LOAD | Rear support, pelvis winds |
| 8 | 2.33 | WINDUP | Wrist raised outside right silhouette |
| 10 | 3 | HIGH GUARD | Tip clear above/right shoulder, left foot starts step |
| 13 | 4 | ACCEL | Hip then shoulder acceleration and left foot plant |
| **16** | **5** | **CONTACT REFERENCE** | Blade crosses in front of torso safely |
| 21 | 6.67 | FOLLOW | Low-left sword tip and braked body turn |
| 27 | 8.67 | BRAKE | Hand tracks sword finish |
| 35 | 11.33 | RECOVER | Return toward guarded posture |
| 43 | 14 | READY | Same repeat-ready stance |

## S4–S7: implementation and QA gates

First create original pose storyboard (concept only) and exact per-frame animation source, then editable Blender scene, 43 rendered frames/normal-speed and slowed MP4, nine critical stills, motion and clearance manifest. Image-generated infographics may have invented timing labels; **JSON spec is timing authority**. Source includes articulated elbows/knees, two wrist/hand targets and sword grip socket. Do NOT losslessly export into current six-part vanilla renderer; this needs separate MNAE bridge.

Acceptable Blender-source evidence: finite transforms, <= 0.015 m grip-to-wrist drift in sampled frames, approximate head-surface clearance >=0.04 m, right planted foot XY drift <=0.08 m and toe floor penetration <=0.05 m, no one-frame tip teleports, clear transitions. Numeric measures alone cannot prove visual quality; inspect entire moving clip. Game acceptance separately requires user-reviewed Minecraft F5 60fps, real active-hand/weapon family, armor, spectator, networking and issue #43 resolution.

Do not add third-party animations, copyright footage or personal videos to public repository. Keep all new assets authoring-only. If a source QA gate fails, report it and iterate rather than labeling PASS.
