package com.eretenkodaniil.mkm.client.gui;

import com.eretenkodaniil.mkm.quests.net.FordJournalView;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Owner-only snapshot; stage transitions and coordinates are server-supplied. */
public final class FordJournalScreen extends Screen {
    private final FordJournalView view;
    public FordJournalScreen(FordJournalView view) {
        super(Component.literal("MKM — журнал заданий"));
        this.view=view;
    }
    @Override protected void init() {
        super.init();
        int top=Math.max(4,(height-230)/2);
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .size(100,20).pos((width-100)/2,top+199).build());
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick) {
        int w=Math.min(440,Math.max(205,width-16)),l=(width-w)/2,t=Math.max(4,(height-230)/2);
        g.fill(0,0,width,height,0x88000000);
        g.fill(l,t,l+w,t+230,0xEE19202A);
        g.fill(l,t,l+w,t+2,0xFFD4A45C);
        g.centeredText(font,title,width/2,t+10,0xFFF0E3CA);
        g.centeredText(font,Component.literal("Ильра · старый брод"),width/2,t+35,0xFFD4A45C);
        String ford=switch(view.stage()){
            case 0 -> "Отыскать смотрительницу и принять решение.";
            case 1 -> view.encounterLoaded()?"Победить разорителя брода.":"Вернуться к броду: враг может быть выгружен.";
            case 2 -> "Вернуться к Ильре за наградой.";
            case 3 -> "Завершено: проход для караванов открыт.";
            default -> "Требуется проверка сохранения.";
        };
        g.centeredText(font,Component.literal(ford),width/2,t+55,0xFFF0E3CA);
        g.centeredText(font,Component.literal(view.tracks()
                ?"Улика: обнаружены следы нежити.":"Улика: следы ещё не исследованы."),
                width/2,t+72,0xFFACB4C2);
        g.centeredText(font,Component.literal("SHOWCASE · разрушенная башня"),
                width/2,t+99,0xFFD4A45C);
        String objective=switch(view.showcaseStage()){
            case 0 -> "Спросить Ильру или самому найти руническую плиту.";
            case 1 -> "Отыскать башню и выбрать исход у руны.";
            case 2 -> view.remaining()==0
                    ?"Возвращайся к руне: реши судьбу переправы."
                    :"Стражи башни: осталось "+(view.remaining()<0?"?":view.remaining())+".";
            case 3 -> view.showcaseBranch()==1
                    ?"Исход: защищена переправа; появился хранитель."
                    :"Исход: исследован и запечатан рунический узел.";
            default -> "Состояние башни требует проверки.";
        };
        g.centeredText(font,Component.literal(objective),width/2,t+121,0xFFF0E3CA);
        if(view.showcaseStage()!=0) {
            g.centeredText(font,Component.literal(
                    "Метка башни: X "+(view.towerX()+7)+" / Y "+(view.towerY()+5)
                    +" / Z "+(view.towerZ()+6)),width/2,t+140,0xFFACB4C2);
            if (view.showcaseStage()==2)
                g.centeredText(font,Component.literal(view.showcaseBranch()==1
                        ?"Путь: защита брода":"Путь: изучение рун"),width/2,t+159,0xFF9CD1A5);
        }
        super.extractRenderState(g,mx,my,tick);
    }
}
