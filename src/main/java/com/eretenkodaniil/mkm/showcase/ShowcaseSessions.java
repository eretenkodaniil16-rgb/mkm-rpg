package com.eretenkodaniil.mkm.showcase;

import java.security.SecureRandom;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/** One-use, player-scoped server capability for a specific rune/block and choice mode. */
public final class ShowcaseSessions {
    private static final long TTL = 20L * 45L;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<ServerPlayer, Session> OPEN = new WeakHashMap<>();

    public record Session(long nonce, BlockPos rune, byte mode, long issued, long expires) {
        public boolean matches(long submitted, BlockPos block, byte actionMode, long now) {
            return nonce != 0 && nonce == submitted && rune.equals(block)
                    && mode == actionMode && now >= issued && now <= expires;
        }
    }
    private ShowcaseSessions() {}
    public static Session open(ServerPlayer player, BlockPos rune, byte mode, long tick) {
        long nonce;
        do { nonce = RANDOM.nextLong(); } while (nonce == 0L);
        Session session = new Session(nonce,rune.immutable(),mode,tick,
                tick > Long.MAX_VALUE - TTL ? Long.MAX_VALUE : tick + TTL);
        OPEN.put(player,session);
        return session;
    }
    public static boolean consume(ServerPlayer player, long nonce, BlockPos rune,
                                  byte mode, long tick) {
        Session s=OPEN.get(player);
        if (s==null || !s.matches(nonce,rune,mode,tick)) return false;
        OPEN.remove(player);
        return true;
    }
    public static void invalidate(ServerPlayer player) { OPEN.remove(player); }
}
