package org.astral;

import java.time.Instant;

/** Small smoke tests for the initial coordinate conversion. */
public final class SkyMathTest {
    public static void main(String[] args) {
        Star northPole = new Star("North celestial pole", 0, 90, 0);
        var fromEquator = SkyMath.toHorizontal(northPole, 0, 0, Instant.parse("2026-11-24T20:00:00Z"));
        near(0, fromEquator.altitudeDegrees(), 1e-8, "pole altitude at equator");
        var fromNorth = SkyMath.toHorizontal(northPole, 45, 0, Instant.parse("2026-11-24T20:00:00Z"));
        near(45, fromNorth.altitudeDegrees(), 1e-8, "pole altitude at latitude 45");
        double az = fromNorth.azimuthDegrees();
        if (az < 0 || az >= 360) throw new AssertionError("azimuth range");
        near(10, SkyMath.normalizeDegrees(370), 1e-8, "angle normalization");
        near(350, SkyMath.normalizeDegrees(-10), 1e-8, "negative angle normalization");
        System.out.println("Basic sky-math smoke tests passed.");
    }
    private static void near(double expected, double actual, double tolerance, String name) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }
}
