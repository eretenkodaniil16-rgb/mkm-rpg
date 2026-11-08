# Showcase-01 UX: A+B+C accessible gameplay UI (Issue #81)

**Baseline:** Fabric 26.3, Java 25, integrated QA5 PR #69 / CharacterData v4.
**Scope:** executable UI/client cosmetic changes only; no change to existing gameplay packets,
CharacterData, origins, combat server rules, XP writer, abilities, save migration or equipment.

## Usable gameplay slice

- Replace the two isolated HUD registrations with **one** top-left MKM RPG panel (K character sheet stays).
- Display owner-synced selected race/class, distinct Warrior/Mage role labels (real underlying class modifiers from QA5),
  Stamina/Mana bars and numeric labels, action timeline phase, *basic* combat stance, Renewal eligibility reason.
  WARRIOR and MAGE differ in available modifier benefits; do **not** claim class-locked abilities:
  QA5 Renewal is executable for every class, and Magic M4 spell-grant policy is **not** integrated.
- New key **M** opens a clickable single-key action palette (Combo/Dodge/Renewal + accessibility).
  Existing G/R/H/J/K are registered **once** through vanilla Fabric KeyMapping and remain rebindable in Options > Controls.
  In simplified input mode R opens the palette instead of issuing Combo. K -> Origin/Class remains the creation flow.
- Client action routing is centralized. All original C2S packet schemas and server validators remain intact;
  no client-computed damage, target, stamina, mana or XP. Palette presses and keybinds go through the
  same bounded per-action input gate. Normal gameplay input is blocked in all GUI screens; the palette
  has one explicit, narrow exception. Held vanilla Attack blocks action hotkeys during mining.
- Single player vs multiplayer: all settings are stored under client `config/mkm-accessibility.properties`;
  never synced or saved on a world/server. Reading an old config with unknown keys is safe.

## A — readability

- HUD panel scale 75/100/125/150%, separately scalable label text 75/100/125/150%.
- High-contrast monochrome panel text and two distinct bright bar colors, plus numerical resource labels.
- Optional color-distinct mode: contrasting gold vs blue bars, *different tick patterns*,
  and explicit text labels for **all** action eligibility states (no red/green-only messaging).
- Responsive geometry pure tests for 1920×1080, 854×480, 320×240, 200×160 GUI coordinates.
  Text-scale is bounded by available width; Minecraft global GUI scale is left unchanged.

## B — motion and effects

- Independent persistent toggles `reducedShake`, `reducedFlash` and VFX `NORMAL / REDUCED / OFF`.
- Active runtime hook: server-confirmed MKM hit particles are reduced to at most 2 or fully suppressed;
  the default keeps previous limited effects.
- **BLOCKED/NOT PRESENT in QA5:** articulated MNAE R07 (#63) and Environment (#60) are not integrated;
  no current MKM camera shake or full-screen flash source exists to modify. Shake/flash preferences are
  reserved for their subsequent port, not represented as suppression of vanilla Minecraft effects.
  Explicitly wire the flags in *the relevant new renderer* when those PRs are safely integrated.

## C — input

- Fully functional existing vanilla keyboard rebinding; M action palette additionally supports mouse
  and standard keyboard focus on button controls. One key can open an action palette.
- Keyboard KeyMapping ownership remains in MkmClient; no duplicate mappings.
- **BLOCKED:** Fabric 26.3 QA5 has no supported native controller input adapter. The C tab explicitly
  says this, rather than claiming gamepad support. Future implementation must detect genuine device
  events, expose focus/axes/deadzones, test connect/disconnect and ensure exactly one C2S per press.

## Manual acceptance, not yet performed

On Java 25 Fabric 26.3 **matching client/server**, copied v4 world:
1. New player: K -> Race/Class -> select Warrior and Mage on two separate players. Compare resource
   values/modifiers and the HUD labels; test rejoin, death respawn, world restart and saved v4 receipts.
2. At physical 1920×1080, GUI 320×240 and 200×160, test each A choice. Capture F1 screenshots,
   check legibility and overlaps with tooltip/chat/health/vanilla hotbar.
3. Normal and reduced/off VFX: verified hit must emit original particles, at most 2, or none;
   no visual effects for misses. Confirm switching does not alter damage numbers.
4. Rebind G/R/H/K/J/M in Minecraft Controls. Test keyboard repeats, mining (held LMB),
   GUI open, death, disconnect/rejoin, R in simple mode, and palette action buttons. Ensure
   server rejects invalid resources/cooldown and never duplicates cast or hit.
5. Test ru_ru and en_us UI and switching, two-player world (local settings may be different);
   test invalid/malformed/older config and persistence after relaunch.
6. When R07 and Environment are integrated, exercise their camera shake/flash hooks and
   actual HUD stance switch. These are **blocked today**.

## Automation

JUnit: config roundtrip/bounds/cycles, per-session per-intent debounce and multiple GUI sizes.
GitHub Actions: Java25 `./gradlew build` and existing server/client headless smoke from QA5.
These cannot replace an actual in-world screenshot or two-human gameplay test.

## Merge safety

This PR is stacked on #69's QA5 branch, **not** historic NeoForge main.
Do not merge #58 or #63 by blindly applying their old CharacterData v3/v2 snapshots.
Do not call this a complete controller adapter, full player-facing skill-tree/spellbook
or final class-specific balancing implementation.
