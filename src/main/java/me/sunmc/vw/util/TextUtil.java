package me.sunmc.vw.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class TextUtil {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private TextUtil() {
    }

    /**
     * Converts an ampersand color-coded string into an Adventure {@link Component}.
     */
    public static Component parse(String text) {
        if (text == null) return Component.empty();
        return LEGACY.deserialize(text);
    }
}