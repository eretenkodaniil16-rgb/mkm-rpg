package com.eretenkodaniil.mkm.client.gui;

import com.eretenkodaniil.mkm.quests.net.FordJournalView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Read-only player-local journal. Data only comes from the server. */
public final class FordJournalScreen extends Screen {
    private final FordJournalView view;
    public FordJournalScreen(FordJournalView view) {
        super(Component.literal("MKM — журнал заданий"));
        this.view=view;
    }
    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .size(100,20).pos((width-100)/2,(height+115)/2).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick) {
        int w=Math.min(410,Math.max(210,width-20)),left=(width-w)/2,top=Math.max(8,(height-175)/2);
        g.fill(0,0,width,height,0x88000000);
        g.fill(left,top,left+w,top+175,0xEE19202A);
        g.fill(left,top,left+w,top+2,0xFFD4A45C);
        g.centeredText(font,title,width/2,top+14,0xFFF0E3CA);
        g.centeredText(font,Component.literal("Смотрительница брода"),width/2,top+42,0xFFD4A45C);
        String objective=switch(view.stage()){
            case 0 -> "Найти Ильру и принять решение.";
            case 1 -> view.encounterLoaded()?"Остановить Разорителя брода.":"Вернуться к броду; противник не загружен.";
            case 2 -> "Вернуться к Ильре за наградой.";
            case 3 -> "Завершено: проход для караванов открыт.";
            default -> "Требуется проверка сохранения.";
        };
        g.centeredText(font,Component.literal(objective),width/2,top+70,0xFFF0E3CA);
        g.centeredText(font,Component.literal(
                view.tracks() ? "Улика: обнаружены следы нежити." : "Улика: следы ещё не исследованы."
        ),width/2,top+92,0xFFACB4C2);
        super.extractRenderState(g,mx,my,tick);
    }
}
