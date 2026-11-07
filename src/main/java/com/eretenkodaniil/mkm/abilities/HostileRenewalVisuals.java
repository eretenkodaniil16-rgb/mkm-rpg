package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualEvent;
import com.eretenkodaniil.mkm.abilities.visual.AbilityVisualPolicy;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import net.minecraft.world.entity.Mob;

/**
 * Server-only cosmetic source for future MNAE observer replication.
 * Does not broadcast owner-only player packets to arbitrary clients.
 * Observer transport will be added by MNAE, with tracking/permission checks.
 */
public final class HostileRenewalVisuals {
    private static final Map<Mob, Integer> SEQUENCES = new WeakHashMap<>();
    private static final Consumer<AbilityVisualEvent> NO_OBSERVERS = event -> {};
    private static Consumer<AbilityVisualEvent> observerBridge = NO_OBSERVERS;

    private HostileRenewalVisuals() {}

    public static void installObserverBridge(Consumer<AbilityVisualEvent> listener) {
        observerBridge = listener == null ? NO_OBSERVERS : listener;
    }

    public static void onApplied(Mob actor) {
        int prior = SEQUENCES.getOrDefault(actor, 0);
        if (prior == Integer.MAX_VALUE) return;
        int next = prior + 1;
        SEQUENCES.put(actor, next);
        AbilityVisualEvent event = new AbilityVisualEvent(
                AbilityCatalog.RENEWAL.id(), AbilityVisualPolicy.RENEWAL_ACTION_ID,
                actor.getId(), next, AbilityVisualEvent.Hand.MAIN_HAND,
                AbilityVisualEvent.Phase.APPLIED, actor.level().getGameTime(),
                AbilityVisualPolicy.RENEWAL_VISUAL_TICKS,
                actor.level().dimension().identifier().toString());
        try {
            observerBridge.accept(event);
        } catch (RuntimeException ex) {
            // A cosmetic-only subscriber can never roll back a committed heal.
            MkmMod.LOGGER.warn("MKM hostile Renewal cosmetic subscriber failed", ex);
        }
    }
}
