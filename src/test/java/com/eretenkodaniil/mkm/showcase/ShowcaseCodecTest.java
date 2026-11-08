package com.eretenkodaniil.mkm.showcase;

import com.eretenkodaniil.mkm.showcase.net.ShowcaseChoiceRequest;
import com.eretenkodaniil.mkm.showcase.net.ShowcaseDecisionView;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ShowcaseCodecTest {
    @Test void decisionPacketAndChoiceNonceRoundTrip() {
        var sent=new ShowcaseDecisionView(100,-20,ShowcaseDecisionView.SELECT,true,(byte)0,71334777L);
        var buffer=Unpooled.buffer();
        try {
            ShowcaseDecisionView.CODEC.encode(buffer,sent);
            assertEquals(sent,ShowcaseDecisionView.CODEC.decode(buffer));
            var choice=new ShowcaseChoiceRequest(ShowcaseChoiceRequest.STUDY,sent.nonce());
            ShowcaseChoiceRequest.CODEC.encode(buffer,choice);
            assertEquals(choice,ShowcaseChoiceRequest.CODEC.decode(buffer));
        } finally { buffer.release(); }
    }
    @Test void oneTimeSessionRequiresSameRuneModeAndExpiry() {
        var s=new ShowcaseSessions.Session(775L,new BlockPos(12,70,3),
                ShowcaseDecisionView.SELECT,100,1000);
        assertTrue(s.matches(775L,new BlockPos(12,70,3),ShowcaseDecisionView.SELECT,400));
        assertFalse(s.matches(775L,new BlockPos(12,70,4),ShowcaseDecisionView.SELECT,400));
        assertFalse(s.matches(775L,new BlockPos(12,70,3),ShowcaseDecisionView.RESOLVE,400));
        assertFalse(s.matches(775L,new BlockPos(12,70,3),ShowcaseDecisionView.SELECT,1001));
        assertFalse(s.matches(776L,new BlockPos(12,70,3),ShowcaseDecisionView.SELECT,400));
    }
}
