package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import net.minecraft.world.entity.player.Player;
import java.util.Map;

/**
 * Combined v4 Equipment + saved Origin + level/INT mana delta applied
 * before ONE final Character Core clamp, not an old v3 snapshot shortcut.
 */
public final class AbilityModifierAccess {
    private AbilityModifierAccess() {}
    public static PlayerModifierSnapshot snapshot(Player player) {
        CharacterData character = CharacterService.get(player);
        if (character.schemaVersion() != CharacterData.CURRENT_SCHEMA_VERSION)
            throw new IllegalStateException("Unsupported CharacterData schema");
        return EquipmentSnapshots.snapshot(player, character,
                Map.of(PlayerModifierId.MAX_MANA, ManaCapacityPolicy.extra(character)));
    }
}
