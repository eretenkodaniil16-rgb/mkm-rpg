package com.eretenkodaniil.mkm.character.origin;

import com.eretenkodaniil.mkm.abilities.AbilityService;
import com.eretenkodaniil.mkm.combat.CombatService;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Small dispatch only; combat and magic retain independent resource/effect authority. */
public final class OriginClassActionService {
    private OriginClassActionService() {}

    public static boolean execute(ServerPlayer player, byte intent) {
        if (intent != 1 || player == null || !player.isAlive() || player.isSpectator()) return false;
        var profile = CharacterOriginService.get(player);
        if (profile.schemaVersion() != CharacterOriginData.SCHEMA_VERSION
                || OriginCatalog.race(profile.raceId()) == null) return false;
        boolean performed = switch (profile.classId()) {
            case "mkm:warrior" -> CombatService.tryWarriorBrace(player);
            case "mkm:mage" -> AbilityService.tryMageWard(player);
            default -> false;
        };
        if (!performed) {
            player.sendOverlayMessage(Component.literal(
                    "[MKM] Классовое действие недоступно: проверь ресурсы, экипировку и откат."));
        }
        return performed;
    }
}
