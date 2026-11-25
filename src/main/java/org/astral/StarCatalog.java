package org.astral;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the original offline HYG v4.1 catalogue and older compact formats. */
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
        Path original = dataDirectory.resolve("hygdata_v41.csv");
        if (Files.isRegularFile(original)) return load(original);
        Path compact = dataDirectory.resolve("hyg-full-v41.tsv");
        if (Files.isRegularFile(compact)) return load(compact);
        return load(dataDirectory.resolve("hyg-bright-v41.json"));
    }

    public static List<Star> load(Path file) throws IOException {
        String path = file.toString().toLowerCase(java.util.Locale.ROOT);
        if (path.endsWith(".csv")) return loadHygCsv(file);
        if (path.endsWith(".tsv")) return loadTsv(file);
        if (path.endsWith(".json")) return loadJson(file);
        throw new IOException("Unsupported catalogue format: " + file);
    }

    private static List<Star> loadHygCsv(Path file) throws IOException {
        List<Star> stars = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String firstLine = reader.readLine();
            if (firstLine == null) throw new IOException("Empty HYG CSV");
            List<String> columns = parseCsvLine(firstLine.replace("\uFEFF", ""));
            Map<String, Integer> header = new HashMap<>();
            for (int i = 0; i < columns.size(); i++) header.put(columns.get(i).trim(), i);
            for (String required : List.of("id", "hip", "hd", "hr", "ra", "dec", "proper",
                    "mag", "ci", "pmra", "pmdec", "dist", "rv")) {
                if (!header.containsKey(required)) {
                    throw new IOException("Missing HYG CSV column: " + required);
                }
            }
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                ++lineNumber;
                if (line.isBlank()) continue;
                List<String> values = parseCsvLine(line);
                if (values.size() != columns.size()) {
                    throw new IOException("Unexpected column count at line " + lineNumber);
                }
                String hygId = get(values, header, "id");
                Double dist = number(get(values, header, "dist"), lineNumber, "dist");
                // The Sun is a separate Solar System object, not a fixed catalogue star.
                if (dist != null && dist == 0.0) continue;
                double ra = requiredNumber(get(values, header, "ra"), lineNumber, "ra");
                double dec = requiredNumber(get(values, header, "dec"), lineNumber, "dec");
                Double mag = number(get(values, header, "mag"), lineNumber, "mag");
                Double ci = number(get(values, header, "ci"), lineNumber, "ci");
                Double pmra = number(get(values, header, "pmra"), lineNumber, "pmra");
                Double pmdec = number(get(values, header, "pmdec"), lineNumber, "pmdec");
                Double rv = number(get(values, header, "rv"), lineNumber, "rv");
                validate(ra, dec, mag, ci, lineNumber);
                // HYG uses dist >= 100000 as a marker for missing/dubious distance.
                if (dist != null && (dist <= 0 || dist >= 100_000)) dist = null;
                String proper = get(values, header, "proper");
                String name = proper.isBlank() ? "HYG " + hygId : proper;
                stars.add(new Star(name, ra, dec, mag, ci, hygId,
                        optional(get(values, header, "hip")),
                        optional(get(values, header, "hd")),
                        optional(get(values, header, "hr")),
                        pmra, pmdec, dist, rv));
            }
        }
        if (stars.isEmpty()) throw new IOException("No stars loaded from " + file);
        return List.copyOf(stars);
    }

    private static List<String> parseCsvLine(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        if (inQuotes) throw new IOException("Unclosed quoted CSV field");
        fields.add(current.toString());
        return fields;
    }

    private static String get(List<String> row, Map<String, Integer> header, String column) {
        return row.get(header.get(column));
    }

    private static String optional(String value) {
        return value.isBlank() ? null : value;
    }

    private static Double number(String value, int lineNumber, String field) throws IOException {
        if (value.isBlank()) return null;
        try {
            double number = Double.parseDouble(value);
            if (!Double.isFinite(number)) {
                throw new IOException("Non-finite " + field + " at line " + lineNumber);
            }
            return number;
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid " + field + " at line " + lineNumber, ex);
        }
    }

    private static double requiredNumber(String value, int lineNumber, String field)
            throws IOException {
        Double n = number(value, lineNumber, field);
        if (n == null) throw new IOException("Missing " + field + " at line " + lineNumber);
        return n;
    }

    private static List<Star> loadTsv(Path file) throws IOException {
        List<Star> stars = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            if (!"id\tra_hours\tdec_deg\tmag\tci\tname".equals(reader.readLine())) {
                throw new IOException("Unexpected HYG TSV header");
            }
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                ++lineNumber;
                String[] f = line.split("\t", -1);
                if (f.length != 6) throw new IOException("Invalid TSV row at line " + lineNumber);
                double ra = requiredNumber(f[1], lineNumber, "ra");
                double dec = requiredNumber(f[2], lineNumber, "dec");
                Double mag = number(f[3], lineNumber, "mag");
                Double ci = number(f[4], lineNumber, "ci");
                validate(ra, dec, mag, ci, lineNumber);
                if (mag != null && mag == 99.0) mag = null;
                String name = f[5].isBlank() ? "HYG " + f[0] : f[5];
                stars.add(new Star(name, ra, dec, mag, ci, f[0],
                        null, null, null, null, null, null, null));
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
            if (!row.matches()) throw new IOException("Invalid star record at index " + stars.size());
            double ra = Double.parseDouble(row.group(1)) / 15.0;
            double dec = Double.parseDouble(row.group(2));
            double mag = Double.parseDouble(row.group(3));
            double ci = Double.parseDouble(row.group(4));
            validate(ra, dec, mag, ci, stars.size() + 1);
            stars.add(new Star("HYG sample " + (stars.size() + 1), ra, dec, mag, ci));
        }
        return List.copyOf(stars);
    }

    private static void validate(double ra, double dec, Double mag, Double ci, int index)
            throws IOException {
        if (!Double.isFinite(ra) || ra < 0 || ra > 24
                || !Double.isFinite(dec) || dec < -90 || dec > 90
                || (mag != null && !Double.isFinite(mag))
                || (ci != null && !Double.isFinite(ci))) {
            throw new IOException("Invalid star data at index " + index);
        }
    }
}
