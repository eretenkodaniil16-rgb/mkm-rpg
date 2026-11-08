package com.eretenkodaniil.mkm.character.origin;

import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.quests.FordQuestService;
import net.minecraft.server.level.ServerPlayer;

/** Server-only trainer. World owns stage, Character owns immutable unlock list. */
public final class OriginProgressService {
    private OriginProgressService() {}

    public static void onLogin(ServerPlayer player) {
        if (player.getAttached(OriginProgressAttachments.PROGRESS) == null) {
            player.setAttached(OriginProgressAttachments.PROGRESS, new OriginProgressData());
        }
    }
    public static OriginProgressData get(ServerPlayer player) {
        return player.getAttachedOrCreate(OriginProgressAttachments.PROGRESS);
    }
    public static boolean mastered(ServerPlayer player) {
        return get(player).has(OriginProgressData.FORD_MENTORSHIP);
    }
    public static OriginShowcaseRules.Unlock learnAtWarden(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return OriginShowcaseRules.Unlock.UNSUPPORTED;
        OriginProgressData old = get(player);
        OriginShowcaseRules.Unlock decision = OriginShowcaseRules.canLearn(
                CharacterService.get(player), CharacterOriginService.get(player), old,
                FordQuestService.get(player));
        if (decision == OriginShowcaseRules.Unlock.GRANTED)
            player.setAttached(OriginProgressAttachments.PROGRESS,
                    old.unlock(OriginProgressData.FORD_MENTORSHIP));
        return decision;
    }
}
