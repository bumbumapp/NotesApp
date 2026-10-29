package com.bumbumapps.mynotes;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * The PIN keys (blue_color_pf) sit on the always-dark lock screen with white digits,
 * so in every theme they must stay dark or the digits disappear.
 */
public class LockScreenColorsTest {

    private static final File RES = new File("src/main/res");

    @Test
    public void pinKeysStayDarkInEveryTheme() throws Exception {
        String styles = read("values/styles.xml");
        Matcher m = Pattern.compile("name=\"blue_color_pf\">@color/(\\w+)<").matcher(styles);
        int found = 0;
        while (m.find()) {
            found++;
            String hex = colorHex(m.group(1));
            assertTrue("blue_color_pf -> " + m.group(1) + " " + hex + " is too light for white digits",
                    luminance(hex) < 0.2);
        }
        assertTrue("no blue_color_pf in styles.xml", found > 0);
    }

    private static String colorHex(String name) throws Exception {
        String colors = read("values/colors.xml");
        Matcher m = Pattern.compile("<color name=\"" + name + "\">([^<]+)<").matcher(colors);
        assertTrue("color " + name + " not found", m.find());
        String v = m.group(1).trim();
        return v.startsWith("@color/") ? colorHex(v.substring(7)) : v;
    }

    private static double luminance(String hex) {
        String h = hex.substring(hex.length() - 6);
        double r = channel(Integer.parseInt(h.substring(0, 2), 16));
        double g = channel(Integer.parseInt(h.substring(2, 4), 16));
        double b = channel(Integer.parseInt(h.substring(4, 6), 16));
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(int c) {
        double s = c / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(new File(RES, path).toPath()), StandardCharsets.UTF_8);
    }
}
