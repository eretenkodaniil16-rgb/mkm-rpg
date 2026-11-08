package com.eretenkodaniil.mkm.client.input;

/** Pure event edge decoding: no C2S from held buttons or on connect. */
public final class GamepadButtonEdges {
    public static final int DODGE = 1, COMBO = 2, RENEWAL = 4;
    private int previous;
    public void reset(int initial) { previous = initial & 7; }
    public int rising(int current) {
        current &= 7;
        int pressed = current & ~previous;
        previous = current;
        return pressed;
    }
}
