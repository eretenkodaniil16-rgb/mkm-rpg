package com.eretenkodaniil.mkm.client.gui;

import com.eretenkodaniil.mkm.quests.net.FordChoiceRequest;
import com.eretenkodaniil.mkm.quests.net.FordDialogueView;
import com.eretenkodaniil.mkm.character.origin.CharacterOriginAttachments;
import com.eretenkodaniil.mkm.character.origin.OriginCatalog;
import net.minecraft.tags.ItemTags;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Lightweight single-NPC scene; stage and PER arrive from the server. */
public final class FordDialogueScreen extends Screen {
    private final FordDialogueView view;
    private boolean submitted;
    private int submittedTicks;
    private static final int TEXT = 0xFFE8DDC5, MUTED = 0xFFB7B4A6;
    public FordDialogueScreen(FordDialogueView view) {
        super(Component.literal("Ильра — смотрительница брода"));
        this.view = view;
    }

    private int panelWidth() { return Math.min(420, Math.max(220, width - 18)); }
    private int panelLeft() { return (width - panelWidth()) / 2; }
    private int panelTop() { return Math.max(8, (height - 218) / 2); }

    @Override protected void init() {
        super.init();
        int x = panelLeft() + 16, y = panelTop() + 80, w = panelWidth() - 32;
        var profile = minecraft == null || minecraft.player == null ? null
                : minecraft.player.getAttached(CharacterOriginAttachments.PROFILE);
        boolean human = profile != null && profile.schemaVersion() == 1
                && profile.raceId().equals(OriginCatalog.HUMAN.id());
        boolean dwarf = profile != null && profile.schemaVersion() == 1
                && profile.raceId().equals(OriginCatalog.DWARF.id());
        if (view.stage() == 0) {
            Button inspect = addChoice(view.tracks() ? "Следы уже изучены"
                    : (view.perception() >= 12 ? "Исследовать следы (PER 12)"
                       : "Требуется PER 12 (" + view.perception() + ")"),
                    FordChoiceRequest.INSPECT, x, y, w);
            inspect.active = !view.tracks() && view.perception() >= 12;
            addChoice("Я помогу каравану", FordChoiceRequest.ACCEPT, x, y + 24, w);
            addChoice("Сейчас не могу", FordChoiceRequest.LEAVE, x, y + 48, w);
            if (human && !view.tracks()) addChoice("Человек: расспросить караванщиков",
                    FordChoiceRequest.HUMAN_PARLEY, x, y + 72, w);
            if (dwarf && !view.tracks()) {
                Button runes = addChoice("Дворф: прочесть руны на камне (кирка)",
                        FordChoiceRequest.DWARF_RUNES, x, y + 72, w);
                runes.active = minecraft.player.getMainHandItem().is(ItemTags.PICKAXES);
            }
        } else if (view.stage() == 1) {
            Button inspect = addChoice(view.tracks() ? "Следы уже изучены"
                    : (view.perception() >= 12 ? "Исследовать следы (PER 12)"
                    : "Требуется PER 12 (" + view.perception() + ")"),
                    FordChoiceRequest.INSPECT, x, y, w);
            inspect.active = !view.tracks() && view.perception() >= 12;
            addChoice("Где искать противника?", FordChoiceRequest.LOCATE, x, y + 24, w);
            addChoice("Я вернусь после боя", FordChoiceRequest.LEAVE, x, y + 48, w);
            if (human && !view.tracks()) addChoice("Человек: узнать тайны каравана",
                    FordChoiceRequest.HUMAN_PARLEY, x, y + 72, w);
            if (dwarf && !view.tracks()) {
                Button runes = addChoice("Дворф: отыскать рунический след (кирка)",
                        FordChoiceRequest.DWARF_RUNES, x, y + 72, w);
                runes.active = minecraft.player.getMainHandItem().is(ItemTags.PICKAXES);
            }
        } else if (view.stage() == 2) {
            addChoice("Получить награду", FordChoiceRequest.CLAIM, x, y, w);
        } else if (view.stage() == 3 && profile != null && profile.selected()) {
            addChoice("Наставничество Ильры: выучить приём (ур. 2 + улика)",
                    FordChoiceRequest.MENTOR_TRAIN, x, y, w);
        }
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), button -> onClose())
                .size(Math.min(100,w), 20).pos(x, panelTop() + 179).build());
    }

    private Button addChoice(String title, byte action, int x, int y, int width) {
        return addRenderableWidget(Button.builder(Component.literal(title), button -> choose(action))
                .size(width, 20).pos(x,y).build());
    }

    private void choose(byte action) {
        if (submitted || !ClientPlayNetworking.canSend(FordChoiceRequest.TYPE)) return;
        submitted = true;
        submittedTicks = 0;
        ClientPlayNetworking.send(new FordChoiceRequest(view.entityId(), view.stage(), action, view.sessionId()));
        if (action == FordChoiceRequest.LEAVE) onClose();
    }

    @Override public void onClose() {
        if (!submitted && ClientPlayNetworking.canSend(FordChoiceRequest.TYPE)) {
            ClientPlayNetworking.send(new FordChoiceRequest(view.entityId(), view.stage(),
                    FordChoiceRequest.CLOSE, view.sessionId()));
        }
        super.onClose();
    }

    @Override public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null) {
            onClose();
            return;
        }
        if (submitted && ++submittedTicks > 100) {
            // A lost response must not permanently freeze all choices.
            submitted = false;
            submittedTicks = 0;
        }
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left=panelLeft(), top=panelTop(), right=left+panelWidth();
        graphics.fill(0,0,width,height,0x88000000);
        graphics.fill(left,top,right,top+218,0xEE19202A);
        graphics.fill(left,top,right,top+2,0xFFD4A45C);
        graphics.centeredText(font,title,width/2,top+12,TEXT);
        String line1 = switch (view.stage()) {
            case 0 -> "У старого брода исчез караван.";
            case 1 -> "Налётчик прячется у брода.";
            case 2 -> "Путь свободен. Ты сдержал слово.";
            case 3 -> "Брод снова открыт благодаря тебе.";
            default -> "Состояние задания требует проверки.";
        };
        String line2 = switch (view.stage()) {
            case 0 -> "Ты поможешь нам разобраться?";
            case 1 -> view.tracks() ? "Следы подтверждают: это иссушённый мертвец." : "Победи налётчика и возвращайся.";
            case 2 -> "Я приготовила награду за помощь.";
            case 3 -> view.tracks() ? "Я помню твою внимательность к следам." : "Караваны вновь могут ходить здесь.";
            default -> "Попробуйте переподключиться или сообщите администратору.";
        };
        graphics.centeredText(font,Component.literal(line1),width/2,top+37,TEXT);
        graphics.centeredText(font,Component.literal(line2),width/2,top+52,MUTED);
        if (view.stage() == 0 || view.stage() == 1)
            graphics.centeredText(font,Component.literal(
                "Восприятие: " + view.perception() + " / 12" + (view.tracks() ? " · следы найдены" : "")
        ), width/2,top+66,view.perception()>=12?0xFF88D7A3:0xFFFFC17E);
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
    }
}
