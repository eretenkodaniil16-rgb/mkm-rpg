package com.eretenkodaniil.mkm.client.gui;

import com.eretenkodaniil.mkm.client.input.ClientActionGate;
import com.eretenkodaniil.mkm.client.input.MkmActionRouter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Clickable single-key alternative; all requests use the same route as MKM key mappings. */
public final class MkmActionPaletteScreen extends Screen {
    private Component result;
    public MkmActionPaletteScreen() {
        super(Component.translatable("screen.mkm.actions.title"));
    }
    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        super.init();
        int w = Math.max(1, Math.min(width - 12, 256));
        int x = (width - w) / 2;
        int start = Math.max(2, (height - 164) / 2);
        addRenderableWidget(Button.builder(Component.translatable("screen.mkm.actions.combo"),
                b -> send(ClientActionGate.Action.COMBO)).pos(x, start + 23).size(w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.mkm.actions.dodge"),
                b -> send(ClientActionGate.Action.DODGE)).pos(x, start + 47).size(w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.mkm.actions.renewal"),
                b -> send(ClientActionGate.Action.RENEWAL)).pos(x, start + 71).size(w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.mkm.access.open"),
                b -> minecraft.gui.setScreen(new MkmAccessibilityScreen(this)))
                .pos(x, start + 95).size(w, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.mkm.actions.close"),
                b -> onClose()).pos(x, start + 119).size(w, 20).build());
    }
    private void send(ClientActionGate.Action action) {
        result = Component.translatable(MkmActionRouter.request(minecraft, action, true)
                ? "screen.mkm.actions.sent" : "screen.mkm.actions.blocked");
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int y = Math.max(2, (height - 164) / 2);
        graphics.fill(0, 0, width, height, 0xA0000000);
        graphics.centeredText(font, title, width / 2, y + 3, 0xFFFFFFFF);
        if (result != null) graphics.centeredText(font, result, width / 2, y + 145, 0xFFFFFFA0);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }
    @Override
    public void onClose() { minecraft.gui.setScreen(null); }
}
