package com.eretenkodaniil.mkm.client.animation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class R07ModeGateTest {
    private static R07ModeGate.Context eligible(boolean f8,boolean f9){
        return new R07ModeGate.Context(f8,f9,true,true,true,true,
                true,true,true,true);
    }
    @Test void threeModesHaveExclusiveGeometryOwners(){
        assertEquals(R07ModeGate.Choice.C3_FALLBACK,
                R07ModeGate.select(eligible(false,false)));
        assertEquals(R07ModeGate.Choice.SIX_DERIVED,
                R07ModeGate.select(eligible(true,false)));
        assertEquals(R07ModeGate.Choice.ARTICULATED_17,
                R07ModeGate.select(eligible(true,true)));
    }
    @Test void unsupportedContextAlwaysFallsBackToOldPlayerModel(){
        var p=eligible(true,true);
        for(int missing=0;missing<7;missing++){
            var c=new R07ModeGate.Context(true,true,
                    missing!=0,missing!=1,missing!=2,missing!=3,
                    missing!=4,missing!=5,missing!=6,true);
            assertEquals(R07ModeGate.Choice.C3_FALLBACK,R07ModeGate.select(c));
        }
        assertThrows(IllegalArgumentException.class,()->R07ModeGate.select(null));
    }
    @Test void assetMissingNeverCausesInvisibleAvatar(){
        var c=new R07ModeGate.Context(true,true,false,false,true,true,
                true,true,true,true);
        assertEquals(R07ModeGate.Choice.C3_FALLBACK,R07ModeGate.select(c));
    }
}
