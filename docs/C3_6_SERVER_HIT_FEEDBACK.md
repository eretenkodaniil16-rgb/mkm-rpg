# C3.6 — Server-confirmed impact feedback (first pass)

## Principle
An animation's `impact` JSON marker indicates the *scheduled attack phase*, not proof that a target was hit. Spawning a visual "hit" merely on crossing that marker would mislead players on misses, shields and invulnerability. The authoritative trigger therefore comes from Fabric `ServerLivingEntityEvents.AFTER_DAMAGE` after **positive, unblocked** direct-player melee damage during the current MKM `LIGHT_COMBO/ACTIVE` window.

## Changes
- Small clientbound `HitConfirm` packet: server-selected target entity id, actual post-resolution damage and stagger flag; it cannot be sent by a client as an action.
- `CombatEvents.afterDamage` sends to the attacking owner only after a real qualifying hit, and only when that client can receive it.
- `HitFeedbackParticles` locates the target locally and produces a short, deterministic 5–12-particle CRIT burst near the target's body center, with slightly more particles for true poise stagger. It does not alter damage, stamina, collision, knockback, camera, or other client mechanics.
- `HitFeedbackRules` rejects nonfinite, negative and zero damage and caps particle count. Unit tests enforce bounded output.
- Existing vanilla swing/impact sounds remain; we do not layer duplicate sounds yet. Camera shake is intentionally deferred pending accessibility preferences and an opt-in toggle.

## Acceptance / limitations
1. Single-player F5 hit: a confirmed server-damage combo shows the brief effect around the actual mob.
2. Miss, shield block, out-of-range, invulnerability: no MKM hit-confirm particles.
3. Normal vanilla attacks outside MKM active combo: no additional MKM particles.
4. Staggered hit: subtle larger burst; no false effects while merely winding up.
5. Client reconnect and multiplayer: no crashes or duplicate subscriptions; an unmodded/incompatible receiver should not be sent the packet.

Because a remote render world can be delayed relative to the packet, a despawned entity may simply produce no extra particles. That is preferable to predicting a hit that never occurred. This first pass only enriches impact legibility and does not claim AAA-quality VFX, camera shake, enemy procedural reactions, or complete multiplayer spectator animation sync.
