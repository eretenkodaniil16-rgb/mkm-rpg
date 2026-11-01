package com.eretenkodaniil.mkm.quests;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Transient server-only dialogue capability, scoped to the actual ServerPlayer instance.
 * One use per opening, bound to an exact NPC UUID, entity ID, stage and server tick.
 * Does not authenticate a player by itself: service still checks live range/actor/state.
 */
public final class FordDialogueSessions {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final long TTL_TICKS = 20L * 60L;
    private static final Map<ServerPlayer, Session> ACTIVE = new WeakHashMap<>();

    public record Session(long id, UUID npcUuid, int entityId, byte stage,
                          long openedAt, long expiresAt) {
        public boolean matches(long token, UUID actor, int entity, byte currentStage, long now) {
            return id != 0L && token == id && npcUuid.equals(actor)
                    && entityId == entity && stage == currentStage
                    && now >= openedAt && now <= expiresAt;
        }
    }

    private FordDialogueSessions() {}

    public static Session open(ServerPlayer player, UUID npcUuid, int entityId,
                               byte stage, long gameTime) {
        // The nonce is a capability for one response; no state or reward is encoded in it.
        long token;
        do { token = RANDOM.nextLong(); } while (token == 0L);
        long expiration = gameTime > Long.MAX_VALUE - TTL_TICKS
                ? Long.MAX_VALUE : gameTime + TTL_TICKS;
        Session session = new Session(token, npcUuid, entityId, stage, gameTime, expiration);
        ACTIVE.put(player, session);
        return session;
    }

    public static boolean consume(ServerPlayer player, UUID npcUuid, int entityId,
                                  byte stage, long sessionId, long gameTime) {
        Session session = ACTIVE.get(player);
        if (session == null || !session.matches(sessionId, npcUuid, entityId, stage, gameTime))
            return false;
        ACTIVE.remove(player);
        return true;
    }

    public static void invalidate(ServerPlayer player) { ACTIVE.remove(player); }
}
