package org.astral;

/** HYG J2000 star data. Optional measurements are null when unavailable. */
public record Star(
        String name,
        double rightAscensionHours,
        double declinationDegrees,
        Double magnitude,
        Double colorIndex,
        String hygId,
        String hipId,
        String hdId,
        String hrId,
        Double properMotionRaMasPerYear,
        Double properMotionDecMasPerYear,
        Double distanceParsecs,
        Double radialVelocityKmPerSec) {

    public Star(String name, double ra, double dec, double mag) {
        this(name, ra, dec, mag, 0.0);
    }

    public Star(String name, double ra, double dec, double mag, double colorIndex) {
        this(name, ra, dec, mag, colorIndex, null, null, null, null,
                null, null, null, null);
    }
}
