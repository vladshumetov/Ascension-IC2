package com.ascension_ic2.client;

import java.util.Locale;

/** Compact number formatting for GUI labels: 1234 -> 1.2k, 524288 -> 524.3k, 4000000 -> 4M. */
public final class GuiNumbers {
    private GuiNumbers() {}

    public static String format(long value) {
        if (value >= 1_000_000_000L) {
            return trim(value / 1_000_000_000.0) + "G";
        }
        if (value >= 1_000_000L) {
            return trim(value / 1_000_000.0) + "M";
        }
        if (value >= 1_000L) {
            return trim(value / 1_000.0) + "k";
        }
        return Long.toString(value);
    }

    private static String trim(double value) {
        String text = String.format(Locale.ROOT, "%.1f", value);
        return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
    }
}
