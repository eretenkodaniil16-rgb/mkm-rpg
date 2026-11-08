package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.abilities.AbilityAttachments;
import com.eretenkodaniil.mkm.abilities.AbilityCatalog;
import com.eretenkodaniil.mkm.abilities.AbilityRules;
import com.eretenkodaniil.mkm.abilities.RenewalEligibility;
import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginAttachments;
import com.eretenkodaniil.mkm.character.origin.OriginCatalog;
import com.eretenkodaniil.mkm.client.accessibility.MkmAccessibility;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.equipment.EquipmentSnapshots;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Single HUD element. All data is owner-synced and read-only; zero client-side
 * mana/stamina prediction. The QA5 baseline has a basic combat stance only.
 */
public final class MkmRpgHud {
    private static final int PANEL_W = 224;
    private static final int PANEL_H = 118;
    private MkmRpgHud() {}

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null
                || mc.gui.screen() != null || mc.player.isSpectator()) return;
        var options = MkmAccessibility.get();
        RpgHudGeometry geom = RpgHudGeometry.forViewport(
                graphics.guiWidth(), graphics.guiHeight(), options.hudScale());
        int bg = options.highContrast() ? 0xF4000000 : 0xCB101727;
        int border = options.highContrast() ? 0xFFFFFFFF : 0xFF9A7742;
        int primary = options.highContrast() ? 0xFFFFFFFF : 0xFFE8E9EE;
        int dim = options.highContrast() ? 0xFFFFFFFF : 0xFFB9C1D2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(geom.left(), geom.top());
        graphics.pose().scale(geom.scale(), geom.scale());
        graphics.fill(0, 0, PANEL_W, PANEL_H, bg);
        graphics.fill(0, 0, PANEL_W, 2, border);

        var origin = mc.player.getAttached(CharacterOriginAttachments.PROFILE);
        Component ancestry = Component.translatable("screen.mkm.origin.unselected");
        Component clazz = ancestry;
        boolean mage = false;
        if (origin != null && origin.selected()) {
            var race = OriginCatalog.race(origin.raceId());
            var profession = OriginCatalog.clazz(origin.classId());
            if (race != null) ancestry = Component.translatable(race.titleKey());
            if (profession != null) {
                clazz = Component.translatable(profession.titleKey());
                mage = OriginCatalog.MAGE.id().equals(profession.id());
            }
        }
        drawText(graphics, mc, Component.translatable("hud.mkm.identity", ancestry, clazz), 7, 7, primary, 210);
        drawText(graphics, mc, Component.translatable(mage ? "hud.mkm.mage_role" : "hud.mkm.warrior_role"),
                7, 20, dim, 210);

        var combat = mc.player.getAttached(CombatAttachments.PLAYER);
        var timeline = mc.player.getAttached(CombatAttachments.TIMELINE);
        var character = mc.player.getAttached(CharacterAttachments.CHARACTER);
        var mana = mc.player.getAttached(AbilityAttachments.PLAYER);
        if (combat == null || timeline == null || character == null || mana == null) {
            drawText(graphics, mc, Component.translatable("hud.mkm.sync_pending"), 7, 46, primary, 210);
            graphics.pose().popMatrix();
            return;
        }
        var stats = EquipmentSnapshots.snapshot(mc.player, character);
        double staminaMax = stats.get(PlayerModifierId.MAX_STAMINA);
        double manaMax = stats.get(PlayerModifierId.MAX_MANA);
        bar(graphics, mc, 35, Component.translatable("hud.mkm.stamina",
                        Math.round(combat.stamina()), Math.round(staminaMax)),
                combat.stamina(), staminaMax,
                options.distinctColors() ? 0xFFF2C45B : 0xFF5CCCA4,
                options.distinctColors(), options.highContrast(), true);
        bar(graphics, mc, 61, Component.translatable("hud.mkm.mana",
                        Math.round(mana.mana()), Math.round(manaMax)),
                mana.mana(), manaMax,
                options.distinctColors() ? 0xFF65B5F5 : 0xFF938BF1,
                options.distinctColors(), options.highContrast(), false);

        Component phase = timeline.idle()
                ? Component.translatable("hud.mkm.phase.idle")
                : Component.translatable("hud.mkm.phase." + timeline.phase().name().toLowerCase(java.util.Locale.ROOT));
        drawText(graphics, mc, Component.translatable("hud.mkm.combat", phase), 7, 84, primary, 210);
        drawText(graphics, mc, Component.translatable("hud.mkm.stance.basic"), 7, 96, dim, 210);

        long now = mc.level.getGameTime();
        var ledger = AbilityRules.reconcileClock(mana, now);
        long remaining = Math.max(0L, ledger.readyTick(AbilityCatalog.RENEWAL.id()) - now);
        double cost = AbilityRules.scaledCost(AbilityCatalog.RENEWAL.baseCost(),
                stats.get(PlayerModifierId.MANA_COST));
        var state = RenewalEligibility.check(
                mc.player.isAlive() && !mc.player.isPassenger(),
                timeline.idle() && now >= combat.heavyReadyTick(),
                mc.player.getHealth() < mc.player.getMaxHealth() - 0.001F,
                ledger, now, cost);
        Component status = switch (state) {
            case READY -> Component.translatable("hud.mkm.renewal.ready");
            case INVALID_ACTOR, BUSY -> Component.translatable("hud.mkm.renewal.busy");
            case FULL_HEALTH -> Component.translatable("hud.mkm.renewal.full_health");
            case LOW_MANA -> Component.translatable("hud.mkm.renewal.low_mana");
            case COOLDOWN -> Component.translatable("hud.mkm.renewal.cooldown",
                    String.format(java.util.Locale.ROOT, "%.1f", remaining / 20D));
        };
        // Text and marker, not red/green alone: ready/block reasons remain visible in every palette.
        drawText(graphics, mc, Component.translatable("hud.mkm.ability", status),
                7, 107, state == RenewalEligibility.Result.READY ? primary : 0xFFFFD797, 210);
        graphics.pose().popMatrix();
    }

    private static void bar(GuiGraphicsExtractor graphics, Minecraft mc, int y,
                            Component name, double value, double max, int color,
                            boolean pattern, boolean highContrast, boolean stamina) {
        drawText(graphics, mc, name, 7, y - 7, highContrast ? 0xFFFFFFFF : 0xFFE4E9F0, 210);
        graphics.fill(7, y + 5, 217, y + 12, highContrast ? 0xFF555555 : 0xFF252A34);
        float ratio = Double.isFinite(value) && Double.isFinite(max) && max > 0
                ? (float) Math.clamp(value / max, 0D, 1D) : 0F;
        int filled = Math.round(210 * ratio);
        if (filled > 0) graphics.fill(7, y + 5, 7 + filled, y + 12, highContrast
                ? (stamina ? 0xFFFFFF00 : 0xFF00FFFF) : color);
        if (pattern) {
            // Stamina: solid with visible ticks; Mana: dashed dark markers.
            int step = stamina ? 21 : 14;
            for (int xx = 7 + step; xx < 217; xx += step) {
                graphics.fill(xx, y + 5, xx + (stamina ? 1 : 3), y + 12,
                        highContrast ? 0xFF000000 : 0xFF172031);
            }
        }
    }
    private static void drawText(GuiGraphicsExtractor graphics, Minecraft mc, Component text,
                                  int x, int y, int color, int maxWidth) {
        float desired = MkmAccessibility.get().textScale() / 100F;
        float fitting = maxWidth / (float) Math.max(1, mc.font.width(text));
        float s = Math.max(0.7F, Math.min(desired, fitting));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(s, s);
        graphics.text(mc.font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }
}
