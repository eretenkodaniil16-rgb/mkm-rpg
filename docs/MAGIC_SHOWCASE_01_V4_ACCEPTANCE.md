# SHOWCASE-01 Magic — QA5 v4 playable two-spell slice

Issue [#76](https://github.com/eretenkodaniil16-rgb/mkm-rpg/issues/76) ·
design [#70](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/70) ·
base [#69](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/69) ·
prior Magic [#37](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/37),
[#42](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/42),
[#58](https://github.com/eretenkodaniil16-rgb/mkm-rpg/pull/58).

**Release constraint:** this draft is based on QA5 **CharacterData v4**, which includes
persistent XP/allocation AND `claimedRewardIds`. It retains
`CharacterOriginData v1` and original World/Enemy/XP services;
never replace it with the old Magic v2/v3 JAR. Installer name
`0.0.6-fabric.0-qa.5-magic.1` is intentionally distinct from QA5.
A GitHub Actions headless server/client boot is NOT game visual acceptance.

## Method-level dependency audit

| Old M4 source | QA5 destination / decision |
| --- | --- |
| `CharacterData` v3 | **NOT COPIED**. Existing v4 persists every claim receipt and point. Magic checks schema v4. |
| `AbilityGrantPolicy` synthetic class IDs | Replaced: read real owner-synced/saved `CharacterOriginData`, `OriginCatalog` Mage or Warrior; rare tome proven by actual held registered item, not client intent. |
| `PlayerModifierSnapshot` from equipment alone | Extended existing `EquipmentSnapshots.snapshot` with *bounded extra MAX_MANA delta*; existing equipment + origin + level/INT summed before **one** `PlayerModifierCalculator` clamp. Other Combat consumers unchanged. |
| `ManaLedger` v1 | Existing QA5 codec/attachment ID and owner-only sync unchanged. Cast uses same ledger, costs/deadlines. No extra gameplay save ledger. |
| `AbilityCatalog`, `AbilityRules` | Preserved; shared new `AbilityCastPlan` calls existing regeneration/spend for both real effects. Added the Character M5 canonical `mkm:mage_ward` executable instant descriptor (castTicks=0); `mkm:arcane_barrier` remains a separate **disabled** 12-tick future channel prototype. |
| M4 C2S `cast_ability` v2 | Ported monotone positive request ID and strict length/version checks. Client + server must both use Magic-v4 JAR, not mix with old QA5 v1. |
| M3 S2C `ability_visual_event` v2 | Ported dimension-scoped owner ACK and MNAE callback; now supports two semantic action IDs. **No client-side duplicate particle burst**. |
| M4 class/AI schema | Player class v4 bridged; no host AI casting enabled in this slice. No duplicate damage executor or AI resource ledger. |
| HUD v3 M3 | Adapted to v4 origin, equipment and title, left mana+stamina column with actual font pixel truncation and bilingual H/V statuses; C3.7 probe preserved. |

## 2 real spells and the actor's right to use them

| Spell | Class / rare source | Effect | Cost at 1.0 cost multiplier | Cooldown | Visible server-initiated VFX |
| --- | --- | --- | ---: | ---: | --- |
| **Renewal**, Restoration circle I, **H** | Existing baseline ability, even for v4 legacy characters with no chosen origin | Native `LivingEntity.heal` +6 HP adjusted by the existing healing modifiers; only if injured | 20 MP | 60 ticks | Green `HAPPY_VILLAGER` + warm `END_ROD`; 14-tick semantic `mkm:renewal_cast` |
| **Arcane Ward**, Abjuration circle I, **V** | Saved `mkm:mage`, or a selected non-mage physically holding `mkm:arcane_tome` | Vanilla `MobEffects.ABSORPTION` amplifier 0 (4 extra absorption HP) for **100 ticks** (future Character M5 mentor: **180**); refuses refresh while any absorption is active | 25 MP Mage; **37.5 MP** off-class with tome | 180 ticks | Violet `ENCHANT` + cold `END_ROD`; 20-tick semantic `mkm:arcane_ward_cast` |

The tome is a registered non-stackable MKM item in the creative tab.
**No survival crafting, drop table or shop access** is added by Magic. Only
operators can deliberately place it in a QA survival world, until World
design approves a rare loot/research reward source. It is checked against
the actual held main/offhand `ItemStack` on the server; the client cannot
claim an arbitrary license through C2S. `claimedRewardIds` NEVER grant spells.

**Executable grant invariant**: valid Character v4 + trusted origin v1 /
licensed held item + appropriate tier + explicit server effect allowlist.
A catalog icon, arbitrary learned ID, tier I–X or Mage naming alone cannot
activate `arcane_bolt`, `repulsion_wave` or other unimplemented damage
effects. Warrior without rare tome cannot cast Ward. Existing Renewal is
intentionally kept available to avoid breaking early user worlds.

## Resource balance and single-clamp v4 contract

```text
rawManaDelta = 6 * sqrt(CharacterData.level() - 1)
             + 2 * AttributeScaling.effectiveDelta(INT)

finalMaxMana = PlayerModifierCalculator.clamp(
    Character BASE MAX_MANA (100)
    + already-defined WIL contribution (+3 per soft effective point)
    + existing equipped item deltas
    + real saved Origin (Mage +6 MP)
    + rawManaDelta,
    MAX_MANA safety cap = 300 MP
)
```

Other mana regen, cost, delay, spell/healing modifiers are still derived
**once** from `PlayerModifierSnapshot`. INT contributes to mana
capacity in a bounded secondary way while still owning offensive magic;
WIL has primary mana/regen/healing/cost weight. Both character level and
INT have diminishing effective returns; repeated item/class bonuses
cannot escape the shared 300 MP cap. At INT=10/WIL=10 with no equipment,
Mage gets about **106 MP at level 1**, **132.15 at 20**,
**148 at 50**, **165.70 at 100**. High INT/WIL adds but still caps at
300. No mana refill, reset of cooldown, reallocation or new reward
is performed on level up. Numeric prototype balance needs gameplay QA.

The ten future circles I–X (1, 10, 20, ... 90) and nine conventional
schools remain metadata. The player's GDD says **unique schools** but
does not specify a final taxonomy; treat these names as **provisional**
rather than asserting that nine lore schools were approved. Real:
**Restoration and Abjuration** only. All others are design-only.


**Conflict with Character M5 PR #87:** Character already provides
`mkm:mage_ward` and maps `V` through `OriginClassActionRequest`
to its own short `AbilityService.tryMageWard`. **Integration must
retain exactly one server executor:** migrate Mage V to Magic's
versioned cast intent `mkm:mage_ward`, remove Character's duplicate
`tryMageWard` while keeping its `OriginProgressService.mastered`
longer 180-tick variant; Warrior V must continue going to Combat's
brace, not Magic. The class action and tome access must be reconciled
with UX PR #88 action palette before publishing. This draft on QA5
does NOT include Character M5 mentoring or Warrior brace. Since
`V` is also used by Character's branch, do not merge both
`MkmClient` files wholesale; choose one rebindable owner for input.

## Observable server, network and animation contract

1. Request `mkm:cast_ability` v2: serverbound bounded ASCII ID and
   positive strictly increasing session sequence; no target, damage,
   health, cost, level, cast duration or claimed class field.
2. Player's server `AbilityGrantPolicy` reads real v4 character +
   persisted origin + actual held tome; Combat `ActionTimeline.idle()`,
   heavy recovery, health/effect eligibility, cooldown, mana, spectator
   and death checks. **One accepted attempt per server tick** plus
   monotone replay filter. Replayed old ID fails even after cooldown.
3. Exactly once resource and cooldown commit precedes a single native
   effect. **APPLIED only after real positive heal or successful
   native absorption application**. On failure no particles/ACK.
4. Server's `sendParticles` broadcasts one finite colored burst per
   **successful cast to all tracking players**; owner gets a small
   v2 S2C `AbilityVisualEvent` with semantic action ID,
   actor entity ID, dimension ID and monotonically increasing sequence.
   The client's `AbilityVisualInbox` only notifies MNAE, never
   initiates a second burst or applies HP/defense.
5. MNAE owns any future first/third-person skeleton gesture and its
   `onStop` callback. Ward uses **20 cosmetic ticks**; Renewal uses
   **14**. No independent magic pose/animation player added.

### Honest shortfall against issue #76

Ward is intentionally **instant** (Gameplay castTicks 0). A real
interruptible/aimed 12-tick channel is NOT implemented because the
current Combat ActionTimeline only owns combos/dodges and Magic must
not introduce a parallel action timeline. Combat-owner review is
required for a new `CASTING` stage, server-authoritative impact
interruption and reservation/rollback protocol. Merely animating the
hand for 20 ticks would NOT satisfy that gameplay requirement.
Arcane Bolt remains disabled until Combat owns the magic
DamageSource, targeting, PvP/friendly fire (GDD S21-04 allows friendly
fire), block occlusion and hit-time permission. A ward avoids
inventing a second damage source but is still a **real defensive spell**.

## Reproducible in-Minecraft QA on copied v4 world

1. Back up world and profile before changing the mod. Use
   `0.0.6-fabric.0-qa.5-magic.1` on both client and server, Fabric 26.3 /
   Java 25; don't use the old Magic #37/#42/#58 artifacts.
2. Take a clean v4 character. Open character sheet **K**, select Mage
   once through the existing Origin UI. Existing v4 XP/points/quests
   must remain unchanged. Invite second player to watch at <16 blocks.
3. Damage Mage below full HP. Press **H** once. Verify HP increases,
   MP decreases **once**, a green burst appears for BOTH players and
   cooldown applies. Press H while full HP/busy/cooldown/low mana:
   no new APPLIED, visual burst or debit.
4. Press **V** (Ward). Confirm 4 native absorption HP (two golden hearts),
   protection against actual incoming damage for 5 s, **25 MP**
   cost (at multiplier 1), 9 s cooldown and violet effect visible
   to both players. Repeated V while active/cooldown does not grant
   more hearts or spend again.
5. Repeat as Warrior: Ward denied without book; with an actual
   **held** `mkm:arcane_tome` (QA operator:
   `/give <player> mkm:arcane_tome`) it is allowed at +50% cost,
   without granting a permanent mage class.
6. Spam H/V and send duplicate/reordered requests from a
   deliberately modified test client. Verify no second effect,
   old cooldown cannot be replayed. Check unselected, spectator,
   dead, Combat busy, different dimension, late packets.
7. Relog, respawn, dimension change, client disconnect/reconnect,
   chunk unload and server restart. Verify no stale MNAE visual,
   no duplicate mana grant, no quest receipt deletion. Character
   must retain allocated points, origin and `claimedRewardIds`.
8. Compare GUI scaled width 120, 200, 320 with F5. Mana and stamina
   bars, rebindable H/V keys and localized statuses remain within
   the left column; no central camera/model occlusion. No shake,
   shader dependency or strong flash in these VFX.
9. Collect **real recorded F5 first-person/third-person footage** and
   two-player screenshots/logs. Run the server and client on the
   tested Ryzen 7 5800U / 16 GiB hardware if available; measure
   experience, not just Gradle pass.
10. Only a subsequent integrator can approve commit, CI, screenshots,
    save integrity and gameplay checks for a Fabric showcase release;
    **never merge into historical NeoForge `main`**.

## CI and boundaries

GitHub Actions Java25/Gradle/JUnit and server+Xvfb client smoke
verify compilation and startup, **not** in-world spell execution,
pixel quality or shared-particle observation. The exact head SHA
must be checked after the final code change. Draft PR remains
unmerged pending real gameplay evidence.
