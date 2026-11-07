package com.eretenkodaniil.mkm.client.gui;

/** Pure responsive geometry for the origin selection screen (no client/render dependencies). */
public record OriginScreenLayout(
        int left, int top, int width, int height, boolean compact,
        int raceX, int classX, int columnWidth,
        int headingY, int firstY, int secondY,
        int labelY, int firstDescriptionY, int secondDescriptionY,
        int statusY, int footerY) {

    public static OriginScreenLayout forViewport(int viewportWidth, int viewportHeight) {
        int width = Math.min(328, Math.max(120, viewportWidth - 12));
        int height = Math.min(228, Math.max(176, viewportHeight - 12));
        int left = (viewportWidth - width) / 2;
        int top = (viewportHeight - height) / 2;
        boolean compact = height < 216;
        int column = Math.min(146, (width - 26) / 2);
        return new OriginScreenLayout(left, top, width, height, compact,
                left + 8, left + width - column - 8, column,
                top + (compact ? 44 : 54),
                top + (compact ? 56 : 69), top + (compact ? 82 : 94),
                top + (compact ? 112 : 128),
                top + (compact ? 124 : 145), top + (compact ? 136 : 158),
                top + (compact ? 148 : 177), top + height - 29);
    }
}
