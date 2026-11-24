package org.astral;

import java.nio.file.Files;
import java.nio.file.Path;

public final class StarCatalogTest {
    public static void main(String[] args) throws Exception {
        var stars = StarCatalog.load(Path.of("data/hyg-bright-v41.json"));
        if (stars.size() != 5070) throw new AssertionError("Expected 5070 stars, got " + stars.size());
        var sirius = stars.get(0);
        near(101.287 / 15.0, sirius.rightAscensionHours(), 1e-8);
        near(-16.716, sirius.declinationDegrees(), 1e-8);
        near(-1.44, sirius.magnitude(), 1e-8);
        for (var star : stars) {
            var position = SkyMath.toHorizontal(star, 42.6977, 23.3219, java.time.Instant.parse("2026-11-24T20:00:00Z"));
            if (!Double.isFinite(position.azimuthDegrees()) || !Double.isFinite(position.altitudeDegrees())) {
                throw new AssertionError("Non-finite sky coordinates");
            }
        }
        Path invalid = Files.createTempFile("bad-catalog", ".json");
        try {
            Files.writeString(invalid, "[{\"ra\":100,\"dec\":120,\"mag\":1,\"ci\":0}]");
            try {
                StarCatalog.load(invalid);
                throw new AssertionError("Invalid declination accepted");
            } catch (java.io.IOException expected) {
                // Expected.
            }
        } finally {
            Files.deleteIfExists(invalid);
        }
        Path tsv = Files.createTempFile("hyg-catalog-test", ".tsv");
        try {
            Files.writeString(tsv, "id\tra_hours\tdec_deg\tmag\tci\tname\n"
                    + "100\t6.7522\t-16.7161\t-1.46\t0.01\tSirius\n"
                    + "101\t2.0\t42.0\t5.0\t1.2\t\n");
            var full = StarCatalog.load(tsv);
            if (full.size() != 2) throw new AssertionError("TSV row count");
            near(-1.46, full.get(0).magnitude(), 1e-8);
            near(1.2, full.get(1).colorIndex(), 1e-8);
            if (!full.get(0).name().equals("Sirius")
                    || !full.get(1).name().equals("HYG 101")) {
                throw new AssertionError("TSV names");
            }
        } finally {
            Files.deleteIfExists(tsv);
        }
        Path fullFile = Path.of("data/hyg-full-v41.tsv");
        if (Files.exists(fullFile)) {
            var full = StarCatalog.load(fullFile);
            if (full.size() < 100_000) throw new AssertionError("Full HYG catalogue is incomplete");
        }
        System.out.println("Catalogue loading and sky-coordinate checks passed.");
    }
    private static void near(double expected, double actual, double tolerance) {
        if (Math.abs(expected - actual) > tolerance) throw new AssertionError(expected + " != " + actual);
    }
}
