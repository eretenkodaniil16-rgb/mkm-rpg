package com.eretenkodaniil.mkm.character.origin;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** One authoritative server write. Legacy CharacterData v4 is never reset or reallocated. */
public final class CharacterOriginService {
    private CharacterOriginService() {}

    public static CharacterOriginData get(Player player) {
        return player.getAttachedOrCreate(CharacterOriginAttachments.PROFILE);
    }

    public static void onLogin(ServerPlayer player) {
        if (player.getAttached(CharacterOriginAttachments.PROFILE) == null) {
            player.setAttached(CharacterOriginAttachments.PROFILE, new CharacterOriginData());
        }
    }

    public static CharacterOriginRules.Result choose(
            ServerPlayer player, String raceId, String classId, int expectedRevision) {
        CharacterOriginData previous = get(player);
        if (player.isSpectator() || !player.isAlive()
                || CharacterService.get(player).schemaVersion() != CharacterData.CURRENT_SCHEMA_VERSION)
            return new CharacterOriginRules.Result(CharacterOriginRules.Outcome.UNSUPPORTED, previous);
        CharacterOriginRules.Result result = CharacterOriginRules.select(
                previous, raceId, classId, expectedRevision);
        if (result.outcome() == CharacterOriginRules.Outcome.SELECTED) {
            player.setAttached(CharacterOriginAttachments.PROFILE, result.data());
        }
        return result;
    }
}
