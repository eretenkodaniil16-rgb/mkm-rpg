package com.eretenkodaniil.mkm.client.gui;

import com.eretenkodaniil.mkm.character.origin.CharacterOriginAttachments;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginData;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginRules;
import com.eretenkodaniil.mkm.character.origin.OriginCatalog;
import com.eretenkodaniil.mkm.character.origin.net.OriginSelectionRequest;
import com.eretenkodaniil.mkm.character.origin.net.OriginSelectionResult;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Lightweight real character creation UI, opened via K character sheet.
 * Stage changes and bonuses remain purely preview until the server confirms one-time selection.
 * Existing progressed v4 worlds are NEVER forced to recreate or lose their stats.
 */
public final class CharacterOriginScreen extends Screen {
    private static final int BACKGROUND = 0xE8161B23;
    private static final int BORDER = 0xFF947752;
    private static final int LIGHT = 0xFFE8E7DA;
    private static final int MUTED = 0xFFACB3BF;

    private String raceId = OriginCatalog.HUMAN.id();
    private String classId = OriginCatalog.WARRIOR.id();
    private Button human, dwarf, warrior, mage, confirm;
    private boolean awaiting;
    private boolean confirmedByServer;
    private int requestId;
    private Component status;

    public CharacterOriginScreen() {
        super(Component.translatable("screen.mkm.origin.title"));
    }

    private CharacterOriginData current() {
        return minecraft == null || minecraft.player == null ? null
                : minecraft.player.getAttached(CharacterOriginAttachments.PROFILE);
    }

    @Override
    protected void init() {
        super.init();
        OriginScreenLayout layout = OriginScreenLayout.forViewport(width, height);
        int left = layout.left();
        int panelWidth = layout.width();
        int top = layout.top();
        int column = layout.columnWidth();
        int rightX = layout.classX();

        human = addRenderableWidget(Button.builder(
                Component.translatable("screen.mkm.origin.human"), b -> chooseRace(OriginCatalog.HUMAN.id()))
                .size(column, 20).pos(layout.raceX(), layout.firstY()).build());
        dwarf = addRenderableWidget(Button.builder(
                Component.translatable("screen.mkm.origin.dwarf"), b -> chooseRace(OriginCatalog.DWARF.id()))
                .size(column, 20).pos(layout.raceX(), layout.secondY()).build());
        warrior = addRenderableWidget(Button.builder(
                Component.translatable("screen.mkm.origin.warrior"), b -> chooseClass(OriginCatalog.WARRIOR.id()))
                .size(column, 20).pos(rightX, layout.firstY()).build());
        mage = addRenderableWidget(Button.builder(
                Component.translatable("screen.mkm.origin.mage"), b -> chooseClass(OriginCatalog.MAGE.id()))
                .size(column, 20).pos(rightX, layout.secondY()).build());
        confirm = addRenderableWidget(Button.builder(
                Component.translatable("screen.mkm.origin.confirm"), b -> submit())
                .size(panelWidth / 2 - 12, 20).pos(left + 8, layout.footerY()).build());
        addRenderableWidget(Button.builder(
                Component.translatable("screen.mkm.origin.back"), b -> onClose())
                .size(panelWidth / 2 - 12, 20)
                .pos(left + panelWidth / 2 + 4, layout.footerY()).build());
        updateButtons();
    }

    private void chooseRace(String id) {
        if (!awaiting && (current() == null || !current().selected())) {
            raceId = id;
            status = null;
            updateButtons();
        }
    }

    private void chooseClass(String id) {
        if (!awaiting && (current() == null || !current().selected())) {
            classId = id;
            status = null;
            updateButtons();
        }
    }

    private void updateButtons() {
        CharacterOriginData data = current();
        boolean locked = awaiting || confirmedByServer || (data != null && data.selected());
        if (human != null) {
            human.active = !locked;
            dwarf.active = !locked;
            warrior.active = !locked;
            mage.active = !locked;
            human.setMessage(Component.literal(raceId.equals(OriginCatalog.HUMAN.id()) ? "✓ " : "")
                    .append(Component.translatable("screen.mkm.origin.human")));
            dwarf.setMessage(Component.literal(raceId.equals(OriginCatalog.DWARF.id()) ? "✓ " : "")
                    .append(Component.translatable("screen.mkm.origin.dwarf")));
            warrior.setMessage(Component.literal(classId.equals(OriginCatalog.WARRIOR.id()) ? "✓ " : "")
                    .append(Component.translatable("screen.mkm.origin.warrior")));
            mage.setMessage(Component.literal(classId.equals(OriginCatalog.MAGE.id()) ? "✓ " : "")
                    .append(Component.translatable("screen.mkm.origin.mage")));
        }
        if (confirm != null) {
            confirm.active = !locked && data != null
                    && ClientPlayNetworking.canSend(OriginSelectionRequest.TYPE);
        }
    }

    private void submit() {
        CharacterOriginData state = current();
        if (state == null || state.selected() || awaiting
                || !ClientPlayNetworking.canSend(OriginSelectionRequest.TYPE)) return;
        requestId = requestId == Integer.MAX_VALUE ? 1 : requestId + 1;
        awaiting = true;
        status = Component.translatable("screen.mkm.origin.sending");
        ClientPlayNetworking.send(new OriginSelectionRequest(
                requestId, state.revision(), raceId, classId));
        updateButtons();
    }

    @Override
    public void tick() {
        super.tick();
        CharacterOriginData saved = current();
        if (saved != null && saved.selected() && !awaiting) {
            raceId = saved.raceId();
            classId = saved.classId();
        }
        updateButtons();
    }

    public void onSelectionResult(OriginSelectionResult result) {
        if (!awaiting || result.requestId() != requestId) return;
        awaiting = false;
        if (result.status() == CharacterOriginRules.Outcome.SELECTED
                || result.status() == CharacterOriginRules.Outcome.ALREADY_SELECTED) {
            confirmedByServer = true;
        }
        status = Component.translatable(switch (result.status()) {
            case SELECTED -> "screen.mkm.origin.saved";
            case ALREADY_SELECTED -> "screen.mkm.origin.already";
            case LOCKED -> "screen.mkm.origin.locked";
            case STALE -> "screen.mkm.origin.stale";
            case INVALID -> "screen.mkm.origin.invalid";
            case UNSUPPORTED -> "screen.mkm.origin.unsupported";
        });
        updateButtons();
    }

    @Override
    public void onClose() {
        minecraft.gui.setScreen(new CharacterSheetScreen());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mx, int my, float delta) {
        OriginScreenLayout layout = OriginScreenLayout.forViewport(width, height);
        int panelWidth = layout.width();
        int panelHeight = layout.height();
        int left = layout.left();
        int top = layout.top();
        int rightX = layout.classX();

        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, BACKGROUND);
        graphics.fill(left, top, left + panelWidth, top + 2, BORDER);
        graphics.centeredText(font, title, width / 2, top + 12, LIGHT);
        graphics.centeredText(font, Component.translatable(layout.compact()
                        ? "screen.mkm.origin.short_notice" : "screen.mkm.origin.subtitle"),
                width / 2, top + 30, MUTED);
        graphics.text(font, Component.translatable("screen.mkm.origin.race"),
                layout.raceX(), layout.headingY(), BORDER);
        graphics.text(font, Component.translatable("screen.mkm.origin.clazz"),
                rightX, layout.headingY(), BORDER);

        CharacterOriginData state = current();
        if (state != null && state.selected()) {
            graphics.centeredText(font, Component.translatable(
                    "screen.mkm.origin.saved_pair",
                    Component.translatable(OriginCatalog.race(state.raceId()) == null
                            ? "screen.mkm.origin.unknown"
                            : OriginCatalog.race(state.raceId()).titleKey()),
                    Component.translatable(OriginCatalog.clazz(state.classId()) == null
                            ? "screen.mkm.origin.unknown"
                            : OriginCatalog.clazz(state.classId()).titleKey())),
                    width / 2, layout.labelY(), LIGHT);
            graphics.centeredText(font, Component.translatable("screen.mkm.origin.permanent"),
                    width / 2, layout.firstDescriptionY(), MUTED);
        } else {
            graphics.centeredText(font,
                    Component.translatable("screen.mkm.origin.bonuses"),
                    width / 2, top + 128, LIGHT);
            graphics.centeredText(font, Component.translatable(
                    raceId.equals(OriginCatalog.HUMAN.id())
                            ? (layout.compact() ? "screen.mkm.origin.human_short" : "screen.mkm.origin.human_desc")
                            : (layout.compact() ? "screen.mkm.origin.dwarf_short" : "screen.mkm.origin.dwarf_desc")),
                    width / 2, layout.firstDescriptionY(), MUTED);
            graphics.centeredText(font, Component.translatable(
                    classId.equals(OriginCatalog.WARRIOR.id())
                            ? (layout.compact() ? "screen.mkm.origin.warrior_short" : "screen.mkm.origin.warrior_desc")
                            : (layout.compact() ? "screen.mkm.origin.mage_short" : "screen.mkm.origin.mage_desc")),
                    width / 2, layout.secondDescriptionY(), MUTED);
        }
        if (status != null) graphics.centeredText(font, status, width / 2, layout.statusY(), LIGHT);
        super.extractRenderState(graphics, mx, my, delta);
    }
}
