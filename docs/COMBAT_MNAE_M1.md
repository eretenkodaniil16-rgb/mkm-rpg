# Combat MNAE M1 — vanilla LMB event, input priority, guard A/B and weapon identity

**Base:** experimental Fabric integration PR #30 (`integration/fabric-26.3-c3.7-all-systems-v1`); animation architecture #33 supersedes PAL spike; gameplay constraints #32. **Draft / not release-approved.** Minecraft 26.3 / Fabric / Java 25.

## 1. Ordinary vanilla LMB integration — what is actually wired

`client.VanillaAttackPresentationMixin` injects at **RETURN** of client `Minecraft.startAttack` and observes only `true` return (vanilla accepted processing), runs a read-only observer and **never cancels** vanilla attack processing, breaks a block, changes the attack cooldown, performs target selection, invokes `player.attack` or transmits a server attack request. This is the native method responsible for vanilla attack attempts, including air swings. The actual Minecraft client/server action remains unchanged.

`VanillaAttackPresentationFeed` creates a bounded (16 event) local queue of immutable `AttackPresentationEvent` values: action kind, `EquipmentRules.Family`, hand, leading arm, stage, `mkm:*` clip ID, client start game tick, local sequence ID and explicit **LOCAL_PREDICTED** status. It excludes Minecraft block hit-results, GUI, spectator and using item/shield, and suppresses extra cosmetic signals while C2 action timeline is non-idle. For nonempty/unknown items use a generic profile, not a fake sword. Right/left dominant main arm uses Minecraft's actual player setting. `drain()` is the future first-party MNAE read boundary; the existing C3 renderer is intentionally untouched.

**Do not mistake this for server hit confirmation.** LMB misses have no damage, and the observed native method can still reject attacks later. This signal is **cosmetic input prediction**, not proof of server acceptance or damage. The `true` native return prevents predictions from a locally rejected attack attempt but is not a network acknowledgement. Existing server `CombatEvents`, `LivingEntityCombatMixin`, `PendingHitProvenance`, confirmed combo `HitConfirm` and vanilla damage remain the sole authorities. MNAE must distinguish `LOCAL_PREDICTED` from any future server-confirmed status and must not replay a second attack from this queue.

**No visual changes are claimed in this PR**: MNAE bridge/renderer integration belongs to Animation team, and first-person and observer replication remain separate tasks.

## 2. Input precedence matrix — gameplay remains vanilla

| Context / input | Server/gameplay authority | Local MNAE event in M1 | Design decision |
| --- | --- | --- | --- |
| LMB on entity | Vanilla `startAttack` / server damage | `VANILLA_SWING/LOCAL_PREDICTED` | No extra `player.attack` |
| LMB into empty space | Vanilla swing/miss | `VANILLA_SWING/LOCAL_PREDICTED` | Miss remains zero damage |
| LMB on block | Vanilla mining | **none** | Mining, resource drops, hardness, creative block removal untouched |
| Shift+LMB toward mob | Existing C2 `tryHeavy`, server stamina + vanilla swing | `HEAVY/LOCAL_PREDICTED` | May predict visually even if server rejects the heavy; no second damage |
| Shift+LMB on block | Vanilla mining | **none** | Shift does not reinterpret block mining |
| R | Existing `CombatService.tryCombo` | no extra LMB event | Special technique remains independent |
| G | Existing dodge server impulse/timeline | no extra LMB event | Direction/ticks unchanged |
| Shield, food, bow or active item use | Vanilla item use | **none** | Cannot interrupt active item-use via cosmetic event |
| GUI / spectator | Vanilla GUI/spectator rules | **none** | No cosmetic attack |
| Existing C2 R/G timeline busy | Existing timeline | **none** | Prevent overlap with special/dodge clip |
| LMB release | **no action** in M1 | **none** | Critical safeguard against double damage |
| PvP, enchanted armor, different attack speed | Native server damage/eligibility | same cosmetic event | No change to damage, attack cooldown or reach |
| High latency/reconnect | Server handles attack legality | local prediction may be stale | Reset local queue on relog; no client damage authority |

**Other mods:** the mixin only reads/captures at `RETURN` for native-accepted invocations, never cancels, but coexistence with other mods that overwrite `startAttack` still requires client QA. Method name/injection must pass 26.3 boot smoke; alternate hook needed if another mod owns the method.

## 3. Two architectures for LMB combination

**A. Full server action timeline for every normal LMB:** strict ordering, server-confirmed phases, easiest cross-player deterministic pose timings, but would hijack vanilla combat cadence/held mining and potentially break attack speed and input compatibility. It would force all weapon categories into C2 action scheduler and require joint gameplay/animation timing changes. **Rejected for M1.**

**B. Vanilla attacks with separate deterministic cosmetic cadence (chosen):** each genuine vanilla `startAttack` remains vanilla; `AttackSequenceRules` only chooses 1/2/3/4, reset/alternate windows per weapon family (e.g. sword fast ≤14 ticks and alternate 15–20; axe fast ≤21 and alternate 22–30). It NEVER authorizes damage, attack speed, reach, stamina, input repetition or recovery. A finisher visual at maximum stage is **not yet a stronger actual hit**. R remains server special. To add a stronger finisher later, Combat Core would need a separately authenticated hit provenance/authorization and test of vanilla cooldown. This preserves Minecraft behavior at the cost of local prediction and future observer S2C reconciliation.

## 4. Charged hold-to-release LMB — NOT ACTIVE

A safe future *combat-only intercept* would own the press state: `IDLE → PRESSED → (SHORT_RELEASE → ONE vanilla attack) / (HELD≥8–12 server-confirmed ticks → CHARGED_RELEASE → ONE attack)`, then `RESET`. **Never execute the initial vanilla attack on keydown and then the charged hit on keyup.** A transition is either one ordinary legal attack OR one charged legal attack, never both. Input buffering should use server-authorized nonce/sequence, time bounds and target revalidation, with no client-supplied damage or cooldown.

Before any change, resolve mining vs combat context, shift priority, hand use/shield, GUI and high-ping reconciliation with animation/QA/control chats. **Current M1 retains immediate vanilla LMB plus existing server Shift+LMB, and ignores LMB release**. A separate opt-in charge hotkey is safer for the first gameplay test.

## 5. Weapon archetypes — metadata, not new stats

`WeaponCombatProfiles.forItem` reuses `EquipmentRules.profile`, which is already used by damage/snapshot and avoids a duplicate item registry. `presentationSpeed`, `proposedStaminaCostFactor` and `proposedPoiseEmphasis` are **experimental design fields, not live gameplay multipliers**; vanilla attack speed, base weapon damage, Minecraft reach, protection and D3 poise all remain unchanged.

| Archetype | Presentation speed (relative) | Proposed stamina factor | Cosmetic stages | Proposed poise emphasis | Stable MNAE IDs |
| --- | ---: | ---: | ---: | ---: | --- |
| Sword | 1.00 | 1.00 | 3 | 0.05 | `mkm:attack/sword/*`, `mkm:special/sword` |
| Axe | 0.72 | 1.35 | 2 | 0.25 | `mkm:attack/axe/*`, `mkm:special/axe` |
| Mace | 0.65 | 1.45 | 2 | 0.40 | `mkm:attack/mace/*`, `mkm:special/mace` |
| Spear/trident | 0.90 | 1.10 | 3 | 0.12 | `mkm:attack/spear/*`, `mkm:special/spear` |
| Unarmed | 1.25 | 0.70 | 4 | 0.00 | `mkm:attack/unarmed/*`, `mkm:special/unarmed` |
| Other / bows, tools | 1.00 | 1.00 | 1 | 0.00 | `mkm:attack/generic/*`, `mkm:special/generic` |

The IDs are a **contract**, not a claim that MNAE already provides authored clips. Animation team may map new IDs to C3 fallbacks until clips exist. `minecraft:trident` counts as a spear in melee, but ranged damage still requires launch-time snapshot per D3–C3.7 integration (#27). Do not increase reach without server verification.

## 6. Shield A/B experiment and once-only debit

Default unchanged: `min(18, 4 + 0.5 × blockedDamage) × STAMINA_COST / max(0.25, BLOCK_STABILITY)`.

Opt in at **server startup** with JVM `-Dmkm.guard.costFactor=1.5` (control is absent or `1.0`). Unknown values fall back safely to `1.0`. Experiment replaces **only the cost** with `originalCost × factor`, without touching vanilla shield block eligibility, damage, durability, enchantments or guard-break timing. This is read on the server; no client request can set it.

`ConfirmedBlockLedger` now issues one **receipt on each Fabric `ALLOW_DAMAGE` callback** and consumes one receipt for the matching `AFTER_DAMAGE` blocked callback, instead of suppressing all repeated object-identical `DamageSource` hits. Legitimate repeated hits using the same source can each spend stamina. Duplicate callbacks without a fresh damage attempt cannot. It keeps up to 64 unconsumed receipts per player, expiring after one tick, and clears state on disconnect. This depends on the vanilla/Fabric synchronous ordering `ALLOW_DAMAGE → AFTER_DAMAGE`; canceled attempts simply expire. Concurrent nested events and mods which short-circuit or independently fire `AFTER_DAMAGE` still need real server QA; the receipt is a per-attempt marker, not a global event UUID. No retroactive blocked-hit damage is invented on guard break. Shield repair, initial facing, enchantments and PvP/PvE validation stay Minecraft's.

### Formula-only A/B at neutral stamina cost and stability

| Incoming blocked damage | Control ×1.0 | Experiment ×1.5 |
| ---: | ---: | ---: |
| 2 | 5.0 | 7.5 |
| 6 | 7.0 | 10.5 |
| 10 | 9.0 | 13.5 |
| 20 | 14.0 | 21.0 |

These are costs in MKM stamina units, **not** observed live PvP results.

## 7. QA for integration branch

- JUnit: stable metadata, action and handedness, cadence pause/reset/finisher, priority matrix, weapon archetypes, guard cost 1.0/1.5 across damage 2/6/10/20, stability and guard break, once-only ledger on repeated callbacks.
- CI: Java 25 `./gradlew build`, dedicated Fabric server, client startup under Xvfb, MNAE mixin application, C3 animation timing regression, release packaging.
- Interactive QA required: survival LMB hit and miss, held mining wood/stone/ores, creative instant mining, attack speed/enchant/shields, left-handed and remapped controls, offhand item use, Shift+LMB existing heavy, R/G timeline, PvP under lag, two-player spectator/observer presentation, shield block from front/back and multiple sources same tick, relog/disconnect.
- Do not claim true 2-player or client visual QA based on server boot. Current native mixin may require retargeting if 26.3 mappings disagree.
- Animation: consume `VanillaAttackPresentationFeed.drain()` in the new native MNAE adapter *only after visual A/B test*, avoid simultaneously driving old and new renderer. Never trigger damage from animation.
- Main/control: coordinate semantic changes to input/release and real server-modified finishers only after stage A's review. Keep PR draft and never merge into legacy NeoForge `main`.

### Cadence vs vanilla attack cooldown

Cosmetic cadence windows intentionally differ by weapon: sword up to 14 ticks and alternate pause 15–20, axe 21/22–30, mace 35/36–46, spear 14/15–23, unarmed 7/8–12. This allows players who wait for normal vanilla weapon cooldowns to still receive stage-2/3 animation metadata. It **does not change Minecraft attack speed**, nor does it force an attack during any window.

### Fabric event semantics and source identity (D4 correctness note)

The Fabric API publishes `ALLOW_DAMAGE` before mitigation and `AFTER_DAMAGE` after applied **or shield-blocked** damage (but not after fatal damage). The M1.1 correction explicitly leverages that attempt ordering. Returning `true` from our observer never cancels a hit; no second damage path is introduced. See Fabric `ServerLivingEntityEvents` API docs for the exact callbacks. Old D4 source-only dedup was **unsafe** for repeated damage from one source within a tick; it is superseded by attempt receipts. Both variants require in-world PvE/PvP validation with real repeated projectiles, mob melee and enchanted gear.
