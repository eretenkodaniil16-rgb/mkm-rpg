package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.character.CharacterService;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import net.minecraft.world.entity.player.Player;

/**
 * One read-only snapshot boundary for all ability effects, resources and HUD.
 * The Equipment D1/D3 branch owns equipped aggregation. On reviewed branch
 * integration, this method must delegate to EquipmentSnapshots.snapshot(player,
 * CharacterService.get(player)); do not copy equipment rules into abilities.
 *
 * The C2-based draft cannot import a class only present in an unmerged PR.
 */
public final class AbilityModifierAccess {
    private AbilityModifierAccess() {}

    public static PlayerModifierSnapshot snapshot(Player player) {
        return PlayerModifierCalculator.calculate(CharacterService.get(player));
    }
}
