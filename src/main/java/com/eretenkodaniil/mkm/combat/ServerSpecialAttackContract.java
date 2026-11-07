package com.eretenkodaniil.mkm.combat;

import com.eretenkodaniil.mkm.combat.presentation.AttackPresentationEvent;
import com.eretenkodaniil.mkm.equipment.EquipmentRules;
import com.eretenkodaniil.mkm.equipment.PhysicalDamageType;
import java.util.Objects;

/**
 * Pure contract for FUTURE enemy techniques and player class skills.
 * This code does NOT schedule damage, select targets, call attack(), hurt(),
 * spawn projectiles, debit stamina, or grant invulnerability frames.
 *
 * All validation evidence must come from the game server, NEVER MNAE or a
 * client-supplied animation marker. The only implementation authorized by a
 * returned READY_FOR_VANILLA is an existing, once-only server-owned vanilla
 * damage path with normal armor, shield, reach and hit validation.
 */
public final class ServerSpecialAttackContract {
    public enum Owner { CLIENT_PREVIEW, SERVER_AI, SERVER_CLASS }
    public enum Delivery { NATIVE_MELEE, NATIVE_PROJECTILE }
    public enum Decision { PRESENTATION_ONLY, REJECTED, READY_FOR_VANILLA }

    public record Intent(String actionId, Delivery delivery,
                         AttackPresentationEvent.Hand activeHand,
                         EquipmentRules.Family weaponFamily,
                         PhysicalDamageType physicalDamageType,
                         int sequenceStep, long serverAttemptId) {
        public Intent {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(delivery, "delivery");
            Objects.requireNonNull(activeHand, "activeHand");
            Objects.requireNonNull(weaponFamily, "weaponFamily");
            Objects.requireNonNull(physicalDamageType, "physicalDamageType");
            if (actionId.length() > 96
                    || !actionId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")
                    || sequenceStep < 0 || sequenceStep > 4
                    || serverAttemptId < 0) {
                throw new IllegalArgumentException("Malformed server attack description");
            }
        }
    }

    public record ServerEvidence(boolean onServerThread, boolean actorValid,
                                 boolean targetServerSelected, boolean sameDimension,
                                 boolean reachAndLineOfSightValid, boolean cooldownReady,
                                 boolean resourcesCommitted, boolean oneShotReceiptClaimed) {}

    private ServerSpecialAttackContract() {}

    public static Decision assess(Owner origin, Intent intent, ServerEvidence evidence) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(intent, "intent");
        Objects.requireNonNull(evidence, "evidence");
        if (origin == Owner.CLIENT_PREVIEW) return Decision.PRESENTATION_ONLY;
        if (!evidence.onServerThread() || !evidence.actorValid()
                || !evidence.targetServerSelected() || !evidence.sameDimension()
                || !evidence.reachAndLineOfSightValid() || !evidence.cooldownReady()
                || !evidence.resourcesCommitted() || !evidence.oneShotReceiptClaimed()) {
            return Decision.REJECTED;
        }
        return Decision.READY_FOR_VANILLA;
    }
}
