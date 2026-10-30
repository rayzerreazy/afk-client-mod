package com.afk.client.util;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads and writes prices like 1500, 1,500, $1.5M, 10m, 2 million. */
public final class Prices {
    private static final Pattern NUMBER = Pattern.compile(
        "([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(?:([kmbt])(?![a-z])|(thousand|million|billion|trillion))?",
        Pattern.CASE_INSENSITIVE);

    private Prices() { }

    /** The first number in the text, with K/M/B/T (or thousand/million/...) applied. -1 if there is none. */
    public static double parse(String text) {
        if (text == null) return -1;
        Matcher m = NUMBER.matcher(text.replace("$", " "));
        if (!m.find()) return -1;

        double value;
        try {
            value = Double.parseDouble(m.group(1).replace(",", ""));
        } catch (NumberFormatException e) {
            return -1;
        }

        double multiplier = 1;
        String letter = m.group(2);
        String word = m.group(3);
        if (letter != null) {
            switch (Character.toLowerCase(letter.charAt(0))) {
                case 'k' -> multiplier = 1e3;
                case 'm' -> multiplier = 1e6;
                case 'b' -> multiplier = 1e9;
                case 't' -> multiplier = 1e12;
                default -> { }
            }
        } else if (word != null) {
            switch (word.toLowerCase(Locale.ROOT)) {
                case "thousand" -> multiplier = 1e3;
                case "million" -> multiplier = 1e6;
                case "billion" -> multiplier = 1e9;
                case "trillion" -> multiplier = 1e12;
                default -> { }
            }
        }
        value *= multiplier;
        return value;
    }

    /** 1500000 -> "1.5M" */
    public static String format(double value) {
        if (value >= 1e12) return trim(value / 1e12) + "T";
        if (value >= 1e9) return trim(value / 1e9) + "B";
        if (value >= 1e6) return trim(value / 1e6) + "M";
        if (value >= 1e3) return trim(value / 1e3) + "K";
        return trim(value);
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }
}
