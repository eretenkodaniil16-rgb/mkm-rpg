package com.eretenkodaniil.mkm.quests.presentation;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.function.Consumer;

/**
 * Gameplay-to-MNAE semantic channel. It NEVER chooses clips, evaluates poses or authorizes combat.
 * World emits only after SERVER-validated narrative state transitions. MNAE owns transport/rendering.
 */
public final class NpcVisualSignals {
    private static final Logger LOGGER = LoggerFactory.getLogger(NpcVisualSignals.class);
    public enum Cue {
        IDLE("mkm:npc/idle", 0),
        WAIT("mkm:npc/wait", 0),
        GREET("mkm:npc/greet", 1),
        TALK("mkm:npc/talk", 1),
        REACT("mkm:npc/react", 2),
        REWARD("mkm:npc/reward", 2),
        THREAT("mkm:npc/threat", 3),
        DEATH("mkm:npc/death", 4);

        private final String id;
        private final int priority;
        Cue(String id, int priority) { this.id=id; this.priority=priority; }
        public String id() { return id; }
        public int priority() { return priority; }
    }

    public static final String HUMANOID = "mkm:rig/humanoid_v1";
    public static final String CREATURE_PROFILE_PREFIX = "mkm:rig/creature/";

    /** Source identity is a world entity UUID; start tick is server-owned, never client-claimed. */
    public record Intent(String actorUuid, String rigProfile, Cue cue, long startTick, int sequence) {
        public Intent {
            Objects.requireNonNull(actorUuid);
            Objects.requireNonNull(rigProfile);
            Objects.requireNonNull(cue);
            if (actorUuid.isBlank() || rigProfile.isBlank() || startTick < 0L || sequence < 0)
                throw new IllegalArgumentException("Invalid NPC visual intent");
            if (!rigProfile.equals(HUMANOID) && !rigProfile.startsWith(CREATURE_PROFILE_PREFIX))
                throw new IllegalArgumentException("Unregistered rig profile namespace");
        }
        public String actionId() { return cue.id(); }
        public int priority() { return cue.priority(); }
    }

    /** No-op until MNAE explicitly installs an observer. World NEVER plays animation directly. */
    private static volatile Consumer<Intent> observer = intent -> {};
    private static final AtomicInteger COSMETIC_SEQUENCE = new AtomicInteger();

    private NpcVisualSignals() {}

    public static void installObserver(Consumer<Intent> sink) {
        observer = Objects.requireNonNull(sink);
    }

    public static void publish(Intent intent) {
        Objects.requireNonNull(intent);
        try {
            observer.accept(intent);
        } catch (RuntimeException renderingFailure) {
            // Observer transport/renderer is an optional cosmetic consumer.
            // Its failure must never interrupt an authoritative quest or death callback.
            LOGGER.warn("MKM NPC cosmetic observer failed for {} cue {}", intent.actorUuid(),
                    intent.actionId(), renderingFailure);
        }
    }

    /** Logical-server only, with cosmetic ordering scoped to the current server process. */
    public static void emit(Entity actor, Cue cue) {
        emit(actor, HUMANOID, cue);
    }

    /** Future nonhuman actors must supply their own creature rig, never reuse humanoid bones. */
    public static void emit(Entity actor, String rigProfile, Cue cue) {
        if (actor == null || !(actor.level() instanceof ServerLevel level)) return;
        publish(new Intent(actor.getUUID().toString(), rigProfile, cue,
                Math.max(0L, level.getGameTime()),
                COSMETIC_SEQUENCE.getAndIncrement() & 0x7FFFFFFF));
    }
}
