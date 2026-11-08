package com.eretenkodaniil.mkm.client.input;

import com.eretenkodaniil.mkm.abilities.AbilityCatalog;
import com.eretenkodaniil.mkm.abilities.net.CastAbilityRequest;
import com.eretenkodaniil.mkm.client.gui.MkmActionPaletteScreen;
import com.eretenkodaniil.mkm.combat.DodgeDirection;
import com.eretenkodaniil.mkm.combat.net.ComboRequest;
import com.eretenkodaniil.mkm.combat.net.DodgeRequest;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;

/** One owner of the three first-party action intents, regardless of input source. */
public final class MkmActionRouter {
    private static final ClientActionGate GATE = new ClientActionGate();
    private MkmActionRouter() {}

    public static void tick(Minecraft client) {
        GATE.session(client.player, client.level);
    }

    /**
     * A palette click explicitly owns its GUI context; normal gameplay keys
     * never fire through menus or while the attack key is held for mining.
     */
    public static boolean request(Minecraft client, ClientActionGate.Action action, boolean palette) {
        if (client.player == null || client.level == null || !client.player.isAlive()
                || client.player.isSpectator() || client.player.isPassenger()) return false;
        if (palette) {
            if (!(client.gui.screen() instanceof MkmActionPaletteScreen)) return false;
        } else if (client.gui.screen() != null || client.options.keyAttack.isDown()) {
            return false;
        }
        boolean supported = switch (action) {
            case COMBO -> ClientPlayNetworking.canSend(ComboRequest.TYPE);
            case DODGE -> ClientPlayNetworking.canSend(DodgeRequest.TYPE);
            case RENEWAL -> ClientPlayNetworking.canSend(CastAbilityRequest.TYPE);
        };
        if (!supported) return false;
        GATE.session(client.player, client.level);
        if (!GATE.allow(action, client.level.getGameTime())) return false;
        switch (action) {
            case COMBO -> ClientPlayNetworking.send(new ComboRequest((byte) 1));
            case RENEWAL -> ClientPlayNetworking.send(new CastAbilityRequest(AbilityCatalog.RENEWAL.id()));
            case DODGE -> {
                var keys = client.player.input.keyPresses;
                byte direction = DodgeDirection.fromMovement(
                        keys.forward(), keys.backward(), keys.left(), keys.right());
                ClientPlayNetworking.send(new DodgeRequest(direction));
            }
        }
        return true;
    }
}
