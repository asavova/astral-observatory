package org.astral;

import java.time.Instant;

/** A basic, educational J2000 equatorial -> horizontal coordinate conversion. */
public final class SkyMath {
    private SkyMath() {}

    public record Horizontal(double altitudeDegrees, double azimuthDegrees) {}

    public static Horizontal toHorizontal(Star star, double latitudeDegrees, double longitudeDegrees, Instant utc) {
        if (!Double.isFinite(latitudeDegrees) || latitudeDegrees < -90 || latitudeDegrees > 90) {
            throw new IllegalArgumentException("Latitude must be between -90 and 90 degrees");
        }
        if (!Double.isFinite(longitudeDegrees) || longitudeDegrees < -180 || longitudeDegrees > 180) {
            throw new IllegalArgumentException("Longitude must be between -180 and 180 degrees");
        }
        double jd = 2440587.5 + (utc.getEpochSecond() + utc.getNano() / 1e9) / 86400.0;
        double t = (jd - 2451545.0) / 36525.0;
        double gmst = 280.46061837 + 360.98564736629 * (jd - 2451545.0)
                + 0.000387933 * t * t - t * t * t / 38710000.0;
        double hourAngle = Math.toRadians(normalizeDegrees(gmst + longitudeDegrees
                - star.rightAscensionHours() * 15.0));
        double latitude = Math.toRadians(latitudeDegrees);
        double declination = Math.toRadians(star.declinationDegrees());

        double sinAltitude = Math.sin(declination) * Math.sin(latitude)
                + Math.cos(declination) * Math.cos(latitude) * Math.cos(hourAngle);
        double altitude = Math.asin(Math.max(-1.0, Math.min(1.0, sinAltitude)));
        // Azimuth is clockwise from north: N=0, E=90, S=180, W=270.
        double azimuth = Math.atan2(-Math.sin(hourAngle) * Math.cos(declination),
                Math.sin(declination) * Math.cos(latitude)
                - Math.cos(declination) * Math.sin(latitude) * Math.cos(hourAngle));
        return new Horizontal(Math.toDegrees(altitude), normalizeDegrees(Math.toDegrees(azimuth)));
    }

    static double normalizeDegrees(double degrees) {
        double result = degrees % 360.0;
        result = result < 0 ? result + 360.0 : result;
        return result >= 360.0 - 1e-10 ? 0.0 : result;
    }
}
