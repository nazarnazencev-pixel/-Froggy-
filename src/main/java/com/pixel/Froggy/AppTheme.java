package com.pixel.Froggy;

import java.awt.*;
import java.util.List;

public class AppTheme {

    public enum Mode { LIGHT, DARK }

    private static Mode current = Mode.LIGHT;

    private static final List<Color> LIGHT_COLORS = List.of(
            new Color(0x4DD9C0), new Color(0x6ECFB0), new Color(0xE05FA0),
            new Color(0xF78FB3), new Color(0xF9A66C), new Color(0xF6C94E),
            new Color(0x7EC8E3), new Color(0xA78BFA), new Color(0x67D48A),
            new Color(0xFF8C69), new Color(0x5BC8F5), new Color(0xFFB347)
    );

    private static final List<Color> DARK_COLORS = List.of(
            new Color(0x0077CC), new Color(0x00A86B), new Color(0xE05000),
            new Color(0xCC1B1B), new Color(0xB8860B), new Color(0x007B8A),
            new Color(0x7B2FBE), new Color(0xC2185B), new Color(0x2E7D32),
            new Color(0x00695C), new Color(0x1565C0), new Color(0xAD6800)
    );

    public static Mode getMode()          { return current; }
    public static void setMode(Mode mode) { current = mode; }
    public static boolean isDark()        { return current == Mode.DARK; }

    public static Color background()         { return isDark() ? new Color(0x1E1E1E) : Color.WHITE; }
    public static Color topPanelBackground() { return isDark() ? new Color(0x252526) : new Color(0xF0F0F0); }
    public static Color fieldBackground()    { return isDark() ? new Color(0x2D2D30) : Color.WHITE; }
    public static Color fieldForeground()    { return isDark() ? new Color(0xE8E8E8) : new Color(0x1A1A1A); }
    public static Color fieldBorder()        { return isDark() ? new Color(0x555555) : new Color(0xAAAAAA); }
    public static Color fileText()           { return isDark() ? new Color(0xE8E8E8) : new Color(0x1A1A1A); }
    public static Color dateText()           { return isDark() ? new Color(0x9E9E9E) : new Color(0x666666); }
    public static Color fileHover()          { return isDark() ? new Color(0x2D2D30) : new Color(0xEEEEF8); }
    public static Color buttonBackground()   { return isDark() ? new Color(0x3C3C3C) : new Color(0xE0E0E0); }
    public static Color buttonForeground()   { return isDark() ? new Color(0xE8E8E8) : new Color(0x1A1A1A); }
    public static Color buttonBorder()       { return isDark() ? new Color(0x555555) : new Color(0xAAAAAA); }
    public static Color scrollThumb()        { return isDark() ? new Color(0x5A5A5A) : new Color(0xBBBBBB); }
    public static Color scrollTrack()        { return isDark() ? new Color(0x2D2D2D) : new Color(0xF0F0F0); }
    public static Color titleBarBackground() { return isDark() ? new Color(0x1A1A1A) : new Color(0xF0F0F0); }
    public static Color titleBarForeground() { return isDark() ? Color.WHITE         : new Color(0x1A1A1A); }

    public static Color groupHeaderText() { return isDark() ? Color.WHITE : new Color(0x1A1A1A); }
    public static Color arrowColor()      { return isDark() ? new Color(255, 255, 255, 200) : new Color(0, 0, 0, 160); }

    public static Color groupColor(String ext) {
        List<Color> palette = isDark() ? DARK_COLORS : LIGHT_COLORS;
        int idx = Math.abs(ext.hashCode()) % palette.size();
        return palette.get(idx);
    }
}