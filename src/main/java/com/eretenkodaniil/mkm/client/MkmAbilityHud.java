package com.eretenkodaniil.mkm.client;

import com.eretenkodaniil.mkm.abilities.AbilityAttachments;
import com.eretenkodaniil.mkm.abilities.AbilityCatalog;
import com.eretenkodaniil.mkm.abilities.AbilityGrantPolicy;
import com.eretenkodaniil.mkm.abilities.AbilityModifierAccess;
import com.eretenkodaniil.mkm.abilities.AbilityRules;
import com.eretenkodaniil.mkm.abilities.ManaLedger;
import com.eretenkodaniil.mkm.abilities.RenewalEligibility;
import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginAttachments;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffects;

/** Compact F5-friendly owner HUD, never source of casting authority. */
public final class MkmAbilityHud {
    private static final int HEIGHT = 7;
    private static final int WHITE = 0xFFE7E9EF;
    private static final int READY = 0xFF86E5B9;
    private static final int WARN = 0xFFC2A7DD;
    private MkmAbilityHud() {}

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gui.screen() != null
                || client.player.isSpectator()) return;
        ManaLedger ledger = client.player.getAttached(AbilityAttachments.PLAYER);
        var character = client.player.getAttached(CharacterAttachments.CHARACTER);
        var combat = client.player.getAttached(CombatAttachments.PLAYER);
        var timeline = client.player.getAttached(CombatAttachments.TIMELINE);
        if (ledger == null || character == null || combat == null || timeline == null) return;
        int w = client.getWindow().getGuiScaledWidth();
        int h = client.getWindow().getGuiScaledHeight();
        if (w < 120 || h < 140) return;
        ResourceHudLayout layout = ResourceHudLayout.forViewport(w, h);
        var stats = AbilityModifierAccess.snapshot(client.player);
        double max = stats.get(PlayerModifierId.MAX_MANA);
        double percent = max > 0 ? Math.clamp(ledger.mana() / max, 0D, 1D) : 0D;
        int x = layout.x(), y = layout.manaY(), width = layout.width();
        graphics.fill(x - 1, y - 1, x + width + 1, y + HEIGHT + 1, 0xC8000000);
        graphics.fill(x, y, x + width, y + HEIGHT, 0xCC1D2033);
        graphics.fill(x, y, x + (int) Math.round(width * percent), y + HEIGHT, 0xFF827BD0);
        String manaLabel = (layout.narrow() ? "MP " : "MKM MP ")
                + Math.round(ledger.mana()) + "/" + Math.round(max);
        graphics.text(client.font, Component.literal(
                ResourceHudText.fit(manaLabel, width, client.font::width)),
                x, y - 12, WHITE);

        long now = client.level.getGameTime();
        ManaLedger current = AbilityRules.reconcileClock(ledger, now);
        boolean active = client.player.isAlive() && !client.player.isPassenger();
        boolean idle = timeline.idle() && now >= combat.heavyReadyTick();
        boolean injured = client.player.getHealth() < client.player.getMaxHealth() - 0.001F;
        double costMultiplier = stats.get(PlayerModifierId.MANA_COST);

        var renewal = AbilityCatalog.RENEWAL;
        var heal = RenewalEligibility.check(active, idle, injured, current, now,
                AbilityRules.scaledCost(renewal.baseCost(), costMultiplier));
        String renewalStatus = switch (heal) {
            case READY -> "READY";
            case INVALID_ACTOR, BUSY -> "BUSY";
            case FULL_HEALTH -> "HP FULL";
            case LOW_MANA -> "LOW MP";
            case COOLDOWN -> String.format(java.util.Locale.ROOT, "%.1fs",
                    Math.max(0D, current.readyTick(renewal.id()) - now) / 20D);
        };
        String healKey = MkmClient.RENEWAL.getTranslatedKeyMessage().getString();
        draw(graphics, client, x, y + 11, width,
                healKey + " HEAL: " + renewalStatus, heal == RenewalEligibility.Result.READY);

        var origin = client.player.getAttached(CharacterOriginAttachments.PROFILE);
        var grant = AbilityGrantPolicy.fromSaved(character, origin);
        boolean licensed = AbilityGrantPolicy.executable(grant, "mkm:arcane_barrier");
        var ward = AbilityCatalog.get("mkm:arcane_barrier");
        double wardCost = AbilityRules.scaledCost(ward.baseCost(), costMultiplier);
        String wardStatus;
        boolean wardReady = false;
        if (!licensed) wardStatus = "MAGE ONLY";
        else if (!active || !idle) wardStatus = "BUSY";
        else if (client.player.hasEffect(MobEffects.ABSORPTION)) wardStatus = "ACTIVE";
        else if (now < current.readyTick(ward.id()))
            wardStatus = String.format(java.util.Locale.ROOT, "%.1fs",
                    (current.readyTick(ward.id()) - now) / 20D);
        else if (current.mana() < wardCost) wardStatus = "LOW MP";
        else { wardStatus = "READY"; wardReady = true; }
        String wardKey = MkmClient.ARCANE_WARD.getTranslatedKeyMessage().getString();
        draw(graphics, client, x, y + 23, width,
                wardKey + " WARD: " + wardStatus, wardReady);
    }

    private static void draw(GuiGraphicsExtractor graphics, Minecraft client,
            int x, int y, int width, String label, boolean ready) {
        graphics.text(client.font,
                Component.literal(ResourceHudText.fit(label, width, client.font::width)),
                x, y, ready ? READY : WARN);
    }
}
