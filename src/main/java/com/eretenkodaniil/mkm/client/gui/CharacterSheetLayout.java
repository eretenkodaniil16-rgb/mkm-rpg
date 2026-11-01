package com.eretenkodaniil.mkm.client.gui;

/**
 * Pure viewport geometry for the character UI; avoids clipping on standard 320x240 GUI scales.
 * The small-screen layout omits decorative tabs/long modifier descriptions, not core controls.
 */
public record CharacterSheetLayout(
        int left, int top, int width, int height, boolean compact, boolean narrow,
        boolean micro, int contentTop, int statsTop, int footerTop) {

    public static CharacterSheetLayout forViewport(int viewportWidth, int viewportHeight) {
        int width = Math.max(1, Math.min(460, viewportWidth - 8));
        int height = Math.max(1, Math.min(330, viewportHeight - 8));
        int left = (viewportWidth - width) / 2;
        int top = (viewportHeight - height) / 2;
        boolean compact = height < 320;
        boolean micro = height < 225;
        boolean narrow = width < 310;
        int contentTop = top + (compact ? 27 : 75);
        // 7 entries, 14 pixels apart; each row's widget ends before footerTop.
        int statsTop = top + (compact ? height - 137 : 169);
        int footerTop = top + height - (compact ? 26 : 30);
        return new CharacterSheetLayout(left, top, width, height, compact, narrow,
                micro, contentTop, statsTop, footerTop);
    }

    public int attributeButtonTop(int index) {
        return statsTop + 14 + 14 * index;
    }

    public int attributeTextTop(int index) {
        return statsTop + 17 + 14 * index;
    }
}
