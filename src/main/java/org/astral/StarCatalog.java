package org.astral;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the bundled offline HYG catalogue, or its smaller demo subset. */
public final class StarCatalog {
    private StarCatalog() {}

    private static final String NUMBER =
            "([-+]?(?:[0-9]+(?:\\.[0-9]+)?|\\.[0-9]+)(?:[eE][-+]?[0-9]+)?)";
    private static final Pattern ROW = Pattern.compile(
            "\\{\\s*\"ra\"\\s*:\\s*" + NUMBER
            + "\\s*,\\s*\"dec\"\\s*:\\s*" + NUMBER
            + "\\s*,\\s*\"mag\"\\s*:\\s*" + NUMBER
            + "\\s*,\\s*\"ci\"\\s*:\\s*" + NUMBER + "\\s*\\}");

    public static List<Star> loadBestAvailable(Path dataDirectory) throws IOException {
        Path full = dataDirectory.resolve("hyg-full-v41.tsv");
        if (Files.isRegularFile(full)) return load(full);
        return load(dataDirectory.resolve("hyg-bright-v41.json"));
    }

    public static List<Star> load(Path file) throws IOException {
        if (file.toString().endsWith(".tsv")) return loadTsv(file);
        if (file.toString().endsWith(".json")) return loadJson(file);
        throw new IOException("Unsupported catalogue format: " + file);
    }

    private static List<Star> loadTsv(Path file) throws IOException {
        List<Star> stars = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine();
            if (!"id\tra_hours\tdec_deg\tmag\tci\tname".equals(header)) {
                throw new IOException("Unexpected HYG TSV header");
            }
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                ++lineNumber;
                String[] fields = line.split("\t", -1);
                if (fields.length != 6) {
                    throw new IOException("Invalid catalogue row at line " + lineNumber);
                }
                try {
                    double ra = Double.parseDouble(fields[1]);
                    double dec = Double.parseDouble(fields[2]);
                    double mag = Double.parseDouble(fields[3]);
                    double ci = Double.parseDouble(fields[4]);
                    validate(ra, dec, mag, ci, lineNumber);
                    String name = fields[5].isBlank() ? "HYG " + fields[0] : fields[5];
                    stars.add(new Star(name, ra, dec, mag, ci));
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid number at line " + lineNumber, e);
                }
            }
        }
        if (stars.isEmpty()) throw new IOException("Empty star catalogue");
        return List.copyOf(stars);
    }

    private static List<Star> loadJson(Path file) throws IOException {
        String json = Files.readString(file, StandardCharsets.UTF_8).trim();
        if (!json.startsWith("[") || !json.endsWith("]")) {
            throw new IOException("Expected a JSON array");
        }
        String body = json.substring(1, json.length() - 1).trim();
        List<Star> stars = new ArrayList<>();
        if (body.isEmpty()) return List.of();
        for (String entry : body.split("(?<=\\})\\s*,\\s*(?=\\{)")) {
            Matcher row = ROW.matcher(entry.trim());
            if (!row.matches()) {
                throw new IOException("Invalid star record at index " + stars.size());
            }
            double ra = Double.parseDouble(row.group(1)) / 15.0;
            double dec = Double.parseDouble(row.group(2));
            double mag = Double.parseDouble(row.group(3));
            double ci = Double.parseDouble(row.group(4));
            validate(ra, dec, mag, ci, stars.size() + 1);
            stars.add(new Star("HYG sample " + (stars.size() + 1), ra, dec, mag, ci));
        }
        return List.copyOf(stars);
    }

    private static void validate(double ra, double dec, double mag, double ci, int index)
            throws IOException {
        if (!Double.isFinite(ra) || ra < 0 || ra > 24
                || !Double.isFinite(dec) || dec < -90 || dec > 90
                || !Double.isFinite(mag) || !Double.isFinite(ci)) {
            throw new IOException("Invalid star data at index " + index);
        }
    }
}
