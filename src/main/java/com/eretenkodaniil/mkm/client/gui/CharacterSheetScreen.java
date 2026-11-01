package com.eretenkodaniil.mkm.client.gui;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.ProgressionRules;
import com.eretenkodaniil.mkm.character.attribute.PrimaryAttribute;
import com.eretenkodaniil.mkm.character.net.AllocationRequest;
import com.eretenkodaniil.mkm.character.net.AllocationResult;
import com.eretenkodaniil.mkm.character.attribute.PlayerModifierId;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierCalculator;
import com.eretenkodaniil.mkm.character.modifier.PlayerModifierSnapshot;
import com.eretenkodaniil.mkm.combat.CombatAttachments;
import com.eretenkodaniil.mkm.combat.CombatState;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.fabricmc.loader.api.FabricLoader;

/**
 * First player-facing MKM RPG screen.
 *
 * <p>Staged build increments are a local preview only; an absolute allocation intent must be
 * validated by the server. The character attachment is the sole source of authoritative state.</p>
 */
public final class CharacterSheetScreen extends Screen {
    private static final int PANEL = 0xEE11151D;
    private static final int PANEL_INNER = 0xDD1A202B;
    private static final int BORDER = 0xFF9A7742;
    private static final int TEXT = 0xFFF1E8D5;
    private static final int MUTED = 0xFFA8A7A2;
    private static final int GOOD = 0xFF7FD39A;
    private static final int WARNING = 0xFFE0B36C;
    private static final int BAR_BG = 0xFF282F3D;
    private static final int BAR_FILL = 0xFF9A7742;

    private final int[] pending = new int[CharacterData.ATTRIBUTE_COUNT];
    private final Button[] plusButtons = new Button[CharacterData.ATTRIBUTE_COUNT];
    private final Button[] minusButtons = new Button[CharacterData.ATTRIBUTE_COUNT];
    private Button applyButton;
    private Button resetButton;
    private List<Integer> observedLedger;
    private boolean awaitingReply;
    private boolean awaitingSync;
    private int requestId;
    private int pendingTicks;
    private Component statusMessage;

    public CharacterSheetScreen() {
        super(Component.translatable("screen.mkm.character.title"));
    }

    @Override
    protected void init() {
        super.init();

        int panelLeft = panelLeft();
        int panelTop = panelTop();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.mkm.character.close"),
                        button -> onClose())
                .size(72, 20)
                .pos(panelLeft + panelWidth - 84, panelTop + panelHeight - 30)
                .build());

        PrimaryAttribute[] attributes = PrimaryAttribute.values();
        int statsTop = panelTop + 169;
        for (int i = 0; i < attributes.length; i++) {
            final int index = i;
            int buttonTop = statsTop + 14 + 14 * i;
            minusButtons[i] = addRenderableWidget(Button.builder(Component.literal("-"),
                    button -> stage(index, -1))
                    .size(18, 14).pos(panelLeft + 91, buttonTop).build());
            plusButtons[i] = addRenderableWidget(Button.builder(Component.literal("+"),
                    button -> stage(index, 1))
                    .size(18, 14).pos(panelLeft + 113, buttonTop).build());
        }
        applyButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.mkm.character.apply"),
                        button -> submitAllocation())
                .size(90, 20).pos(panelLeft + panelWidth - 276, panelTop + panelHeight - 30)
                .build());
        resetButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.mkm.character.reset"),
                        button -> { Arrays.fill(pending, 0); statusMessage = null; refreshButtons(); })
                .size(82, 20).pos(panelLeft + panelWidth - 180, panelTop + panelHeight - 30)
                .build());
        refreshButtons();
    }

    @Override
    public void tick() {
        super.tick();
        CharacterData data = currentCharacter();
        if (data != null) {
            if (observedLedger != null && !observedLedger.equals(data.allocatedPoints())) {
                Arrays.fill(pending, 0);
                awaitingReply = false;
                awaitingSync = false;
                statusMessage = Component.translatable("screen.mkm.character.synced_allocation");
            }
            observedLedger = data.allocatedPoints();
        }
        if ((awaitingReply || awaitingSync) && ++pendingTicks > 100) {
            awaitingReply = false;
            awaitingSync = false;
            statusMessage = Component.translatable("screen.mkm.character.allocation_timeout");
        }
        refreshButtons();
    }

    private CharacterData currentCharacter() {
        return minecraft == null || minecraft.player == null
                ? null : minecraft.player.getAttached(CharacterAttachments.CHARACTER);
    }

    private int pendingTotal() {
        int sum = 0;
        for (int value : pending) sum += value;
        return sum;
    }

    private void stage(int index, int increment) {
        CharacterData data = currentCharacter();
        if (data == null || index < 0 || index >= pending.length) return;
        int next = pending[index] + increment;
        if (next < 0 || next + data.attribute(PrimaryAttribute.values()[index])
                > ProgressionRules.ALLOCATION_ATTRIBUTE_CAP
                || pendingTotal() + increment > data.availablePoints()) return;
        if (awaitingReply || awaitingSync) return;
        pending[index] = next;
        statusMessage = null;
        refreshButtons();
    }

    private void refreshButtons() {
        CharacterData data = currentCharacter();
        int total = pendingTotal();
        for (int i = 0; i < plusButtons.length; i++) {
            if (plusButtons[i] == null || minusButtons[i] == null) continue;
            plusButtons[i].active = !awaitingReply && !awaitingSync && data != null
                    && data.schemaVersion() == CharacterData.CURRENT_SCHEMA_VERSION
                    && total < data.availablePoints()
                    && data.attribute(PrimaryAttribute.values()[i]) + pending[i]
                            < ProgressionRules.ALLOCATION_ATTRIBUTE_CAP;
            minusButtons[i].active = !awaitingReply && !awaitingSync && pending[i] > 0;
        }
        if (applyButton != null) {
            applyButton.active = !awaitingReply && !awaitingSync && data != null
                    && total > 0 && total <= data.availablePoints()
                    && ClientPlayNetworking.canSend(AllocationRequest.TYPE);
        }
        if (resetButton != null) resetButton.active = !awaitingReply && !awaitingSync && total > 0;
    }

    private void submitAllocation() {
        CharacterData data = currentCharacter();
        if (awaitingReply || awaitingSync || data == null || pendingTotal() == 0
                || pendingTotal() > data.availablePoints()
                || !ClientPlayNetworking.canSend(AllocationRequest.TYPE)) return;
        int[] allocations = new int[pending.length];
        for (int i = 0; i < allocations.length; i++) {
            allocations[i] = data.allocatedPoints().get(i) + pending[i];
        }
        // The server validates against its current attachment, never the client preview.
        requestId++;
        awaitingReply = true;
        pendingTicks = 0;
        statusMessage = Component.translatable("screen.mkm.character.allocation_sending");
        ClientPlayNetworking.send(new AllocationRequest(
                requestId, data.spentPoints(), allocations[0], allocations[1], allocations[2],
                allocations[3], allocations[4], allocations[5], allocations[6]));
        refreshButtons();
    }

    /** Acknowledgement never mutates stats; synchronized attachments are authoritative. */
    public void onAllocationResult(AllocationResult result) {
        if (!awaitingReply || result.requestId() != requestId) return;
        awaitingReply = false;
        awaitingSync = result.accepted();
        pendingTicks = 0;
        statusMessage = Component.translatable(result.accepted()
                ? "screen.mkm.character.allocation_accepted"
                : "screen.mkm.character.allocation_rejected");
        refreshButtons();
    }

    private int previewAttribute(CharacterData data, PrimaryAttribute attribute) {
        return data.attribute(attribute) + pending[attribute.ordinal()];
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        int panelWidth = panelWidth();
        int panelHeight = panelHeight();
        int right = left + panelWidth;
        int bottom = top + panelHeight;

        graphics.fill(0, 0, width, height, 0x99000000);
        graphics.fill(left, top, right, bottom, PANEL);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, PANEL_INNER);
        graphics.fill(left, top, right, top + 2, BORDER);
        graphics.fill(left, bottom - 2, right, bottom, BORDER);
        graphics.fill(left, top, left + 2, bottom, BORDER);
        graphics.fill(right - 2, top, right, bottom, BORDER);

        graphics.centeredText(font, title, width / 2, top + 12, TEXT);
        graphics.centeredText(
                font,
                Component.translatable("screen.mkm.character.runtime", modVersion()),
                width / 2,
                top + 27,
                MUTED);

        drawTabs(graphics, left, top, panelWidth);
        drawCharacterContent(graphics, left, top, panelWidth);

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private void drawTabs(GuiGraphicsExtractor graphics, int left, int top, int panelWidth) {
        int tabY = top + 44;
        int usable = panelWidth - 32;
        int tabWidth = usable / 3;
        int tabLeft = left + 16;

        graphics.fill(tabLeft, tabY, tabLeft + tabWidth, tabY + 19, 0xFF2D3544);
        graphics.fill(tabLeft, tabY + 17, tabLeft + tabWidth, tabY + 19, BORDER);
        graphics.centeredText(
                font,
                Component.translatable("screen.mkm.character.tab.character"),
                tabLeft + tabWidth / 2,
                tabY + 5,
                TEXT);

        graphics.centeredText(
                font,
                Component.translatable("screen.mkm.character.tab.skills"),
                tabLeft + tabWidth + tabWidth / 2,
                tabY + 5,
                MUTED);
        graphics.centeredText(
                font,
                Component.translatable("screen.mkm.character.tab.quests"),
                tabLeft + tabWidth * 2 + tabWidth / 2,
                tabY + 5,
                MUTED);
    }

    private void drawCharacterContent(GuiGraphicsExtractor graphics, int left, int top, int panelWidth) {
        if (minecraft == null || minecraft.player == null) {
            graphics.centeredText(
                    font,
                    Component.translatable("screen.mkm.character.no_player"),
                    width / 2,
                    top + 92,
                    WARNING);
            return;
        }

        CharacterData data = minecraft.player.getAttached(CharacterAttachments.CHARACTER);
        int contentTop = top + 75;
        int contentLeft = left + 22;

        graphics.text(
                font,
                Component.translatable("screen.mkm.character.name", minecraft.player.getName()),
                contentLeft,
                contentTop,
                TEXT);

        if (data == null) {
            graphics.text(
                    font,
                    Component.translatable("screen.mkm.character.sync_pending"),
                    contentLeft,
                    contentTop + 20,
                    WARNING);
            graphics.text(
                    font,
                    Component.translatable("screen.mkm.character.sync_hint"),
                    contentLeft,
                    contentTop + 36,
                    MUTED);
            return;
        }

        graphics.text(
                font,
                Component.translatable("screen.mkm.character.synced"),
                contentLeft,
                contentTop + 16,
                GOOD);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.level", data.level()),
                contentLeft,
                contentTop + 36,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.xp", data.experience()),
                contentLeft,
                contentTop + 50,
                TEXT);

        drawExperienceBar(graphics, data, contentLeft, contentTop + 66, panelWidth - 44);

        graphics.text(font,
                Component.translatable("screen.mkm.character.points",
                        data.availablePoints(), pendingTotal()),
                contentLeft, contentTop + 81, pendingTotal() > 0 ? GOOD : TEXT);

        int statsTop = contentTop + 94;
        int split = left + panelWidth / 2 + 8;

        graphics.text(font, Component.translatable("screen.mkm.character.attributes"), contentLeft, statsTop, BORDER);
        graphics.text(font, Component.translatable("screen.mkm.character.str", previewAttribute(data, PrimaryAttribute.STRENGTH)), contentLeft, statsTop + 17, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.dex", previewAttribute(data, PrimaryAttribute.DEXTERITY)), contentLeft, statsTop + 31, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.vit", previewAttribute(data, PrimaryAttribute.VITALITY)), contentLeft, statsTop + 45, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.end", previewAttribute(data, PrimaryAttribute.ENDURANCE)), contentLeft, statsTop + 59, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.int", previewAttribute(data, PrimaryAttribute.INTELLIGENCE)), contentLeft, statsTop + 73, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.wil", previewAttribute(data, PrimaryAttribute.WILLPOWER)), contentLeft, statsTop + 87, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.per", previewAttribute(data, PrimaryAttribute.PERCEPTION)), contentLeft, statsTop + 101, TEXT);

        CharacterData previewData = data;
        for (PrimaryAttribute attribute : PrimaryAttribute.values()) {
            if (pending[attribute.ordinal()] > 0) {
                previewData = previewData.withAttribute(attribute, previewAttribute(data, attribute));
            }
        }
        PlayerModifierSnapshot modifiers = PlayerModifierCalculator.calculate(previewData);
        CombatState combat = minecraft.player.getAttached(CombatAttachments.PLAYER);
        String stamina = combat == null
                ? "? / " + modifiers.formatted(PlayerModifierId.MAX_STAMINA)
                : Math.round(combat.stamina()) + " / " + modifiers.formatted(PlayerModifierId.MAX_STAMINA);

        graphics.text(font, Component.translatable(pendingTotal() > 0
                ? "screen.mkm.character.derived_preview" : "screen.mkm.character.derived"), split, statsTop, BORDER);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.max_health", modifiers.formatted(PlayerModifierId.MAX_HEALTH)),
                split,
                statsTop + 17,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.physical_power", modifiers.formatted(PlayerModifierId.PHYSICAL_POWER)),
                split,
                statsTop + 31,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.attack_speed", modifiers.formatted(PlayerModifierId.ATTACK_SPEED)),
                split,
                statsTop + 45,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.max_stamina", stamina),
                split,
                statsTop + 59,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.magic_power", modifiers.formatted(PlayerModifierId.MAGIC_POWER)),
                split,
                statsTop + 73,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.max_mana", modifiers.formatted(PlayerModifierId.MAX_MANA)),
                split,
                statsTop + 87,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.weak_point_damage", modifiers.formatted(PlayerModifierId.WEAK_POINT_DAMAGE)),
                split,
                statsTop + 101,
                TEXT);

        graphics.text(
                font,
                statusMessage == null
                        ? Component.translatable("screen.mkm.character.derived_note") : statusMessage,
                contentLeft,
                top + panelHeight() - 48,
                statusMessage == null ? MUTED : WARNING);
    }

    private void drawExperienceBar(GuiGraphicsExtractor graphics, CharacterData data, int x, int y, int barWidth) {
        int barHeight = 10;
        graphics.fill(x, y, x + barWidth, y + barHeight, BAR_BG);

        double progress;
        Component label;
        if (ProgressionRules.isMaxLevel(data.experience())) {
            progress = 1.0D;
            label = Component.translatable("screen.mkm.character.max_level");
        } else {
            long currentFloor = ProgressionRules.experienceForLevel(data.level());
            long next = ProgressionRules.experienceForNextLevel(data.experience());
            long inLevel = Math.max(0L, data.experience() - currentFloor);
            long span = Math.max(1L, next - currentFloor);
            progress = Math.min(1.0D, (double) inLevel / (double) span);
            label = Component.translatable("screen.mkm.character.progress", inLevel, span);
        }

        int filled = (int) Math.round(barWidth * progress);
        graphics.fill(x, y, x + filled, y + barHeight, BAR_FILL);
        graphics.centeredText(font, label, x + barWidth / 2, y + 1, TEXT);
    }

    private int panelWidth() {
        return Math.min(460, Math.max(280, width - 32));
    }

    private int panelHeight() {
        return Math.min(330, Math.max(300, height - 32));
    }

    private int panelLeft() {
        return (width - panelWidth()) / 2;
    }

    private int panelTop() {
        return (height - panelHeight()) / 2;
    }

    private static String modVersion() {
        return FabricLoader.getInstance()
                .getModContainer(MkmMod.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("dev");
    }
}
