package com.eretenkodaniil.mkm.client.gui;

import com.eretenkodaniil.mkm.MkmMod;
import com.eretenkodaniil.mkm.character.CharacterAttachments;
import com.eretenkodaniil.mkm.character.CharacterData;
import com.eretenkodaniil.mkm.character.CharacterDerivedStats;
import com.eretenkodaniil.mkm.character.ProgressionRules;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/**
 * First player-facing MKM RPG screen.
 *
 * <p>The screen is intentionally read-only. Character state is owned by the server and arrives on
 * the client through the synchronized player attachment.</p>
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

        CharacterData data = minecraft.player.getExistingData(CharacterAttachments.CHARACTER).orElse(null);
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

        int statsTop = contentTop + 94;
        int split = left + panelWidth / 2 + 8;

        graphics.text(font, Component.translatable("screen.mkm.character.attributes"), contentLeft, statsTop, BORDER);
        graphics.text(font, Component.translatable("screen.mkm.character.str", data.strength()), contentLeft, statsTop + 17, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.dex", data.dexterity()), contentLeft, statsTop + 31, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.vit", data.vitality()), contentLeft, statsTop + 45, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.end", data.endurance()), contentLeft, statsTop + 59, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.int", data.intelligence()), contentLeft, statsTop + 73, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.wil", data.willpower()), contentLeft, statsTop + 87, TEXT);
        graphics.text(font, Component.translatable("screen.mkm.character.per", data.perception()), contentLeft, statsTop + 101, TEXT);

        CharacterDerivedStats derived = CharacterDerivedStats.from(data);

        graphics.text(font, Component.translatable("screen.mkm.character.derived"), split, statsTop, BORDER);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.max_health", derived.maxHealthPreview()),
                split,
                statsTop + 17,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.physical_power", derived.physicalPowerPercent() + "%"),
                split,
                statsTop + 31,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.attack_speed", derived.attackSpeedPercent() + "%"),
                split,
                statsTop + 45,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.max_stamina", derived.maxStaminaPreview()),
                split,
                statsTop + 59,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.magic_power", derived.magicPowerPercent() + "%"),
                split,
                statsTop + 73,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.max_mana", derived.maxManaPreview()),
                split,
                statsTop + 87,
                TEXT);
        graphics.text(
                font,
                Component.translatable("screen.mkm.character.weak_point_damage", derived.weakPointDamagePercent() + "%"),
                split,
                statsTop + 101,
                TEXT);

        graphics.text(
                font,
                Component.translatable("screen.mkm.character.derived_note"),
                contentLeft,
                top + panelHeight() - 48,
                MUTED);
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
        return ModList.get()
                .getModContainerById(MkmMod.MOD_ID)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("dev");
    }
}
