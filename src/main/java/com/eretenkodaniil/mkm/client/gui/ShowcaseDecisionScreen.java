package com.eretenkodaniil.mkm.client.gui;

import com.eretenkodaniil.mkm.showcase.net.ShowcaseChoiceRequest;
import com.eretenkodaniil.mkm.showcase.net.ShowcaseDecisionView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Rune inscriptions with two consequential server-validated story choices. */
public final class ShowcaseDecisionScreen extends Screen {
    private static final int TEXT=0xFFF0E4CC, SUB=0xFFAAB5C7;
    private final ShowcaseDecisionView data;
    private boolean submitted;
    public ShowcaseDecisionScreen(ShowcaseDecisionView data) {
        super(Component.literal("Сторожевая башня · руническая плита"));
        this.data=data;
    }
    private int panelW(){return Math.min(430,Math.max(210,width-20));}
    private int panelL(){return (width-panelW())/2;}
    private int panelT(){return Math.max(4,(height-206)/2);}
    @Override protected void init() {
        super.init();
        int x=panelL()+12,y=panelT()+103,w=panelW()-24;
        if (data.mode()==ShowcaseDecisionView.SELECT) {
            addRenderableWidget(Button.builder(Component.literal("Защитить переправу · 100 XP"),
                    button->choose(ShowcaseChoiceRequest.DEFEND)).size(w,20).pos(x,y).build());
            Button study=addRenderableWidget(Button.builder(
                    Component.literal(data.studyEligible()
                            ?"Исследовать руны · 125 XP"
                            :"Руны закрыты · требуется PER 12 или класс Маг"),
                    button->choose(ShowcaseChoiceRequest.STUDY))
                    .size(w,20).pos(x,y+24).build());
            study.active=data.studyEligible();
        } else {
            addRenderableWidget(Button.builder(Component.literal(data.branch()==1
                            ?"Укрепить брод и призвать хранителя"
                            :"Расшифровать и запечатать рунический узел"),
                    button->choose(ShowcaseChoiceRequest.FINISH))
                    .size(w,20).pos(x,y).build());
        }
        addRenderableWidget(Button.builder(Component.literal("Отойти"),
                button->onClose()).size(Math.min(100,w),20).pos(x,panelT()+169).build());
    }
    private void choose(byte action) {
        if (submitted || !ClientPlayNetworking.canSend(ShowcaseChoiceRequest.TYPE)) return;
        submitted=true;
        ClientPlayNetworking.send(new ShowcaseChoiceRequest(action,data.nonce()));
        super.onClose();
    }
    @Override public void onClose() {
        if (!submitted && ClientPlayNetworking.canSend(ShowcaseChoiceRequest.TYPE))
            ClientPlayNetworking.send(new ShowcaseChoiceRequest(ShowcaseChoiceRequest.CLOSE,data.nonce()));
        super.onClose();
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float tick) {
        int l=panelL(),t=panelT(),w=panelW();
        g.fill(0,0,width,height,0x99000000);
        g.fill(l,t,l+w,t+206,0xEE171F28);
        g.fill(l,t,l+w,t+2,0xFFC4A06A);
        g.centeredText(font,title,width/2,t+14,TEXT);
        g.centeredText(font,Component.literal("Скрытый дозор пробуждается у башни."),width/2,t+40,TEXT);
        g.centeredText(font,Component.literal(data.mode()==ShowcaseDecisionView.SELECT
                ?"Реши судьбу переправы. Выбор будет сохранён."
                :"Все три стража пали. Заверши выбранный путь."),width/2,t+58,SUB);
        g.centeredText(font,Component.literal("Башня: X "+data.x()+", Z "+data.z()),
                width/2,t+79,0xFFD4B68A);
        super.extractRenderState(g,mx,my,tick);
    }
}
