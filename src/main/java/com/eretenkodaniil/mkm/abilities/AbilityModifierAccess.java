package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import net.minecraft.world.entity.player.Player;

/**
 * One read-only snapshot boundary for all ability effects, resources and HUD.
 * This combined C3.7 preview integrates reviewed D1/D3 code from PR #30,
 * so Magic uses the SAME equipment-aware snapshot as Combat Core.
 * Never apply equipment deltas again inside AbilityService or the HUD.
 */
public final class AbilityModifierAccess {
    private AbilityModifierAccess() {}

    public static PlayerModifierSnapshot snapshot(Player player) {
        return EquipmentSnapshots.snapshot(player, CharacterService.get(player));
    }
}
