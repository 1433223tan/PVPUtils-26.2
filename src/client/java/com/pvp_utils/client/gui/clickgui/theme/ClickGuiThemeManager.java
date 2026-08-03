package com.pvp_utils.client.gui.clickgui.theme;

import com.pvp_utils.client.gui.clickgui.theme.vape.VapeTheme;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ClickGuiThemeManager {
    private static final Map<String, ClickGuiTheme> THEMES = new LinkedHashMap<>();
    private static final ClickGuiTheme FALLBACK = new DefaultClickGuiTheme();
    private static ClickGuiTheme currentTheme;

    static {
        register(FALLBACK);
        register(new VapeTheme());
        currentTheme = FALLBACK;
    }

    private ClickGuiThemeManager() {
    }

    public static void register(ClickGuiTheme theme) {
        if (theme == null || theme.id() == null || theme.id().isBlank()) return;
        THEMES.put(theme.id(), theme);
    }

    public static Collection<ClickGuiTheme> themes() {
        return List.copyOf(THEMES.values());
    }

    public static ClickGuiTheme current() {
        return currentTheme;
    }

    public static boolean select(String id) {
        ClickGuiTheme theme = THEMES.get(id);
        if (theme == null) return false;
        currentTheme = theme;
        return true;
    }

    public static Optional<ClickGuiTheme> find(String id) {
        return Optional.ofNullable(THEMES.get(id));
    }
}
