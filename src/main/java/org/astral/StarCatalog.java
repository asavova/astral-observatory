package org.astral;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the fixed four-field JSON format of the bundled HYG bright-star subset. */
public final class StarCatalog {
    private StarCatalog() {}

    private static final String NUMBER = "([-+]?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?)";
    private static final Pattern ROW = Pattern.compile(
            "\\{\\s*\"ra\"\\s*:\\s*" + NUMBER
            + "\\s*,\\s*\"dec\"\\s*:\\s*" + NUMBER
            + "\\s*,\\s*\"mag\"\\s*:\\s*" + NUMBER
            + "\\s*,\\s*\"ci\"\\s*:\\s*" + NUMBER + "\\s*\\}");

    public static List<Star> load(Path file) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8).trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IOException("Expected a JSON array in " + file);
        }
        String body = json.substring(1, json.length() - 1).trim();
        if (body.isEmpty()) return List.of();

        List<Star> stars = new ArrayList<>();
        for (String entry : body.split("(?<=\\})\\s*,\\s*(?=\\{)")) {
            Matcher row = ROW.matcher(entry.trim());
            if (!row.matches()) throw new IOException("Invalid star record at index " + stars.size());
            double ra = Double.parseDouble(row.group(1)); // HYG subset uses degrees.
            double dec = Double.parseDouble(row.group(2));
            double mag = Double.parseDouble(row.group(3));
            double colorIndex = Double.parseDouble(row.group(4));
            if (!Double.isFinite(ra) || ra < 0 || ra > 360
                    || !Double.isFinite(dec) || dec < -90 || dec > 90
                    || !Double.isFinite(mag) || !Double.isFinite(colorIndex)) {
                throw new IOException("Invalid star coordinates at index " + stars.size());
            }
            stars.add(new Star("Star " + (stars.size() + 1), ra / 15.0, dec, mag));
        }
        return List.copyOf(stars);
    }
}
