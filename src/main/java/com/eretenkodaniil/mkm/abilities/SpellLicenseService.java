package com.eretenkodaniil.mkm.abilities;

import com.eretenkodaniil.mkm.registry.MkmItems;
import java.util.Set;
import net.minecraft.world.entity.player.Player;

/**
 * Server-verified rare equipment license: an ACTUAL registered MKM item in
 * a held slot, not a client-supplied spellbook ID or Character reward receipt.
 * The book is not survival-loot-enabled in this QA prototype.
 */
public final class SpellLicenseService {
    private SpellLicenseService() {}

    public static Set<String> heldResearchLicenses(Player player) {
        if (player == null) return Set.of();
        boolean holdsTome = player.getMainHandItem().is(MkmItems.ARCANE_TOME)
                || player.getOffhandItem().is(MkmItems.ARCANE_TOME);
        return holdsTome ? Set.of("mkm:arcane_barrier") : Set.of();
    }

    public static double offClassManaMultiplier(String classId, boolean rareLicense) {
        return rareLicense && !"mkm:mage".equals(classId) ? 1.50D : 1.0D;
    }
}
