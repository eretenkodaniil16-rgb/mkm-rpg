package com.eretenkodaniil.mkm.client.gui;

import com.eretenkodaniil.mkm.client.accessibility.MkmAccessibility;
import com.eretenkodaniil.mkm.client.accessibility.MkmAccessibility.Options;
import com.eretenkodaniil.mkm.client.accessibility.MkmAccessibility.Setting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Three tabbed A/B/C groups fit narrow Minecraft GUI resolutions. */
public final class MkmAccessibilityScreen extends Screen {
    private enum Page { READABILITY, MOTION, INPUT }
    private final Screen parent;
    private final Page page;
    public MkmAccessibilityScreen(Screen parent) { this(parent, Page.READABILITY); }
    private MkmAccessibilityScreen(Screen parent, Page page) {
        super(Component.translatable("screen.mkm.access.title"));
        this.parent = parent;
        this.page = page;
    }
    @Override
    public boolean isPauseScreen() { return false; }
    @Override
    protected void init() {
        super.init();
        int w = Math.max(1, Math.min(width - 12, 320));
        int left = (width - w) / 2;
        int top = Math.max(2, (height - 184) / 2);
        int tw = Math.max(1, (w - 8) / 3);
        addRenderableWidget(Button.builder(Component.literal("A · UI"),
                b -> select(Page.READABILITY)).pos(left, top + 24).size(tw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("B · FX"),
                b -> select(Page.MOTION)).pos(left + tw + 4, top + 24).size(tw, 18).build());
        addRenderableWidget(Button.builder(Component.literal("C · Keys"),
                b -> select(Page.INPUT)).pos(left + 2 * (tw + 4), top + 24).size(tw, 18).build());
        Setting[] entries = switch (page) {
            case READABILITY -> new Setting[] {Setting.HUD_SCALE, Setting.TEXT_SCALE,
                    Setting.HIGH_CONTRAST, Setting.DISTINCT_COLORS};
            case MOTION -> new Setting[] {Setting.REDUCED_SHAKE, Setting.REDUCED_FLASH, Setting.VFX};
            case INPUT -> new Setting[] {Setting.SIMPLE_INPUT};
        };
        for (int i = 0; i < entries.length; i++) {
            Setting setting = entries[i];
            Button button = addRenderableWidget(Button.builder(label(setting),
                    b -> { MkmAccessibility.change(setting); b.setMessage(label(setting)); })
                    .pos(left, top + 47 + i * 21).size(w, 19).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("screen.mkm.access.done"),
                b -> onClose()).pos(left, Math.min(height - 20, top + 138)).size(w, 19).build());
    }
    private void select(Page next) {
        minecraft.gui.setScreen(new MkmAccessibilityScreen(parent, next));
    }
    private static Component label(Setting setting) {
        Options o = MkmAccessibility.get();
        return switch (setting) {
            case HUD_SCALE -> Component.translatable("screen.mkm.access.hudscale", o.hudScale());
            case TEXT_SCALE -> Component.translatable("screen.mkm.access.textscale", o.textScale());
            case HIGH_CONTRAST -> Component.translatable("screen.mkm.access.contrast", onOff(o.highContrast()));
            case DISTINCT_COLORS -> Component.translatable("screen.mkm.access.distinct", onOff(o.distinctColors()));
            case REDUCED_SHAKE -> Component.translatable("screen.mkm.access.shake", onOff(o.reducedShake()));
            case REDUCED_FLASH -> Component.translatable("screen.mkm.access.flash", onOff(o.reducedFlash()));
            case VFX -> Component.translatable("screen.mkm.access.vfx." + o.vfx().name().toLowerCase(java.util.Locale.ROOT));
            case SIMPLE_INPUT -> Component.translatable("screen.mkm.access.simple", onOff(o.simpleInput()));
        };
    }
    private static Component onOff(boolean value) {
        return Component.translatable(value ? "screen.mkm.access.on" : "screen.mkm.access.off");
    }
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
        int top = Math.max(2, (height - 184) / 2);
        graphics.fill(0, 0, width, height, 0xCC090F1C);
        graphics.centeredText(font, title, width / 2, top + 8, 0xFFFFFFFF);
        if (page == Page.MOTION) {
            graphics.centeredText(font, Component.translatable("screen.mkm.access.motion_note"),
                    width / 2, top + 120, 0xFFFFCF89);
        }
        if (page == Page.INPUT) {
            graphics.centeredText(font, Component.translatable("screen.mkm.access.mapping"),
                    width / 2, top + 81, 0xFFE1E7F1);
            graphics.centeredText(font, Component.translatable("screen.mkm.access.controller_map"),
                    width / 2, top + 98, 0xFFE1E7F1);
            graphics.centeredText(font, Component.translatable("screen.mkm.access.controller_blocked"),
                    width / 2, top + 114, 0xFFFFCF89);
        }
        super.extractRenderState(graphics, mx, my, delta);
    }
    @Override
    public void onClose() { minecraft.gui.setScreen(parent); }
}
