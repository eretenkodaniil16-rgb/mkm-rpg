# Equipment D2 — shield stamina, physical defenses and stagger resistance

**Status:** stacked draft on D1 / C2.0; subject to client PvP acceptance. No new save fields, network packets or action animation timings.

## Implemented changes

1. Vanilla shields remain responsible for deciding whether an attack was actually blocked. When Fabric's `AFTER_DAMAGE` reports `blocked=true` for a server player actively using a *vanilla shield*, MKM debits stamina exactly once per callback. A block costing more than remaining stamina exhausts it, releases shield use and applies a 60-tick vanilla shield cooldown. The already-resolved block is not retroactively reversed. Shield durability and Minecraft's cooldown rules remain vanilla.
2. Guard cost per legitimate successful block: `min(18, 4 + 0.5 * blocked_damage) * STAMINA_COST / max(0.25, BLOCK_STABILITY)`. Server regeneration occurs before the debit and is delayed afterwards. `BLOCK_EFFICIENCY` and parries remain reserved; not silently introduced.
3. Physical resistance applies against **direct player melee** and additionally against vanilla `MOB_ATTACK` (weapon-defined type: sword/axe = slashing, otherwise blunt) and `ARROW`/`TRIDENT` damage (piercing) **when the target is a player**. Scaling is at the same existing `hurtServer` injection point before vanilla armor/shield; no new mixin is added. Explosions, magic, environmental damage and unrecognized attack types retain their vanilla outcomes.
4. Only direct player melee receives a player-origin strength/melee offensive multiplier and can consume Shift-heavy stamina. Projectile offense remains vanilla pending launch-time weapon snapshot capture (to prevent exploit by switching held weapon before impact). For physical ranged damage against players, RPG resistance applies with zero offensive penetration.
5. Incoming direct mob melee can build temporary player poise. Direct player melee can build poise on mobs and players. `STAGGER_RESISTANCE` from the **defender's** composed snapshot now reduces poise buildup. Stagger is a 25-tick Slowness effect, not a true input lock or animation stun. Existing mob/vanilla hit validation and actual damage requirements are preserved.
6. CombatService's internal server-side snapshot retrieval is routed through the same equipped snapshot factory without changing action, combo, heavy/dodge calculations or animation pipelines. This prepares stamina cost modifiers from future equipment without a second calculator.

## Validation and future gates

Pure JUnit tests cover shield debits, regen delay, guard break, zero/invalid resources and stagger resistance. GitHub CI build and dedicated server smoke run on this branch; manual tests remain mandatory, especially shield block direction/cooldown, rapid attacks, two-player combat, zombie arrows, trident projectiles, creative/spectator and repeated reconnects.

**Known limitations:** ranged attack bonuses are not yet wired; player guard may receive multiple callbacks for distinct real hits (correct per-hit charging); `AFTER_DAMAGE` must be verified to report completely blocked shield hits under Minecraft 26.3 in live gameplay. Shield use cancellation and cooldown are server-only. This prototype does not introduce perfect parries, shield stamina before block acceptance, invincibility, alternate hitboxes, custom enchantments, quality affixes or full stun interruption.

## Integration message for control chat

- A (animation): `CombatEvents` is edited; semantic hit/miss and stance/clip timing unchanged. Animation branch `feature/c3-6-authoritative-hit-feedback` ALSO edits `CombatEvents` and `CombatService`: conflict likely at integration; preserve animation hit-confirm calls when reconciling.
- B (character): source attributes/schema untouched. Character screen D1 already overlaps progressions branch; resolve there.
- C (magic): no magic damage type or spell cast hook; physical mitigation explicitly excludes spells and magical sources.
- D (equipment): new guard helper `ShieldGuardService` consumes CombatAttachments.PLAYER, no extra attachment. Any later shield weapon quality effects should compose `BLOCK_STABILITY` once.
- World/quests: vanilla items already supported; any MKM quest reward item should define an equipment profile before claiming custom gameplay effects.
