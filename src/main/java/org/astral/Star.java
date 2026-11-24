package org.astral;

/** Stellar position and apparent magnitude at the J2000 catalogue epoch. */
public record Star(String name, double rightAscensionHours, double declinationDegrees,
                   double magnitude, double colorIndex) {
    public Star(String name, double rightAscensionHours, double declinationDegrees, double magnitude) {
        this(name, rightAscensionHours, declinationDegrees, magnitude, 0.0);
    }
}
