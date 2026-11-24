package org.astral;

/** Equatorial catalogue position at the J2000 reference epoch. */
public record Star(String name, double rightAscensionHours, double declinationDegrees, double magnitude) {
}
