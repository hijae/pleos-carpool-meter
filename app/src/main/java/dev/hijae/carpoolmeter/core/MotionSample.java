package dev.hijae.carpoolmeter.core;

/** One per-second motion update for the fare engine. Invalid samples never bill. */
public record MotionSample(
    boolean valid, double speedKmh, double distanceMeters, long intervalMillis, String status) {}
