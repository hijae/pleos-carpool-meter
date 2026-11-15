package dev.hijae.carpoolmeter.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class SpeedIntegratorTest {
  private static final double KMH_10 = 10 / 3.6;

  @Test
  public void constantSpeedIntegratesToDistance() {
    SpeedIntegrator s = new SpeedIntegrator();
    s.drain(1_000);
    for (long t = 1_000; t <= 11_000; t += 100) s.accept(KMH_10, t);
    MotionSample r = s.drain(11_000);
    assertTrue(r.valid());
    assertEquals(10.0, r.speedKmh(), 0.001);
    assertEquals(KMH_10 * 10, r.distanceMeters(), 0.05);
    assertEquals(10_000, r.intervalMillis());
  }

  @Test
  public void trapezoidUsesAverageOfTwoSamples() {
    SpeedIntegrator s = new SpeedIntegrator();
    s.drain(500);
    s.accept(0, 1_000);
    s.accept(10, 2_000); // 0 -> 10 m/s over 1 s
    MotionSample r = s.drain(2_000);
    assertEquals(5.0, r.distanceMeters(), 1e-9);
    assertEquals(36.0, r.speedKmh(), 1e-9);
  }

  @Test
  public void stationaryIsValidWithZeroDistance() {
    SpeedIntegrator s = new SpeedIntegrator();
    s.drain(1_000);
    s.accept(0, 1_500);
    s.accept(0, 2_000);
    MotionSample r = s.drain(2_000);
    assertTrue(r.valid());
    assertEquals(0, r.speedKmh(), 0);
    assertEquals(0, r.distanceMeters(), 0);
  }

  @Test
  public void gapIsNeverBridgedOrCountedAsStationary() {
    SpeedIntegrator s = new SpeedIntegrator();
    s.drain(1_000);
    s.accept(20, 1_000);
    s.accept(20, 2_000);
    s.drain(2_000);
    s.accept(20, 8_000); // 6 s silence: no distance for the gap
    MotionSample r = s.drain(8_000);
    assertEquals(0, r.distanceMeters(), 0);
    MotionSample stale = s.drain(20_000);
    assertFalse(stale.valid());
    assertEquals(0, stale.distanceMeters(), 0);
  }

  @Test
  public void noSampleYetIsInvalid() {
    SpeedIntegrator s = new SpeedIntegrator();
    MotionSample r = s.drain(1_000);
    assertFalse(r.valid());
    assertEquals(0, r.distanceMeters(), 0);
  }

  @Test
  public void duplicateOrReversedTimestampsAreIgnored() {
    SpeedIntegrator s = new SpeedIntegrator();
    s.drain(1_000);
    s.accept(10, 2_000);
    s.accept(10, 2_000);
    s.accept(10, 1_500);
    s.accept(10, 3_000);
    assertEquals(10.0, s.drain(3_000).distanceMeters(), 1e-9);
  }

  @Test
  public void drainResetsAccumulatedDistance() {
    SpeedIntegrator s = new SpeedIntegrator();
    s.drain(1_000);
    s.accept(10, 1_000);
    s.accept(10, 2_000);
    assertEquals(10.0, s.drain(2_000).distanceMeters(), 1e-9);
    s.accept(10, 3_000);
    assertEquals(10.0, s.drain(3_000).distanceMeters(), 1e-9);
  }

  @Test
  public void distanceBehindTheLastSampleCarriesIntoTheNextDrain() {
    SpeedIntegrator s = new SpeedIntegrator();
    s.drain(1_000);
    s.accept(10, 1_000);
    s.accept(10, 1_900); // last sample 100 ms before the tick
    double first = s.drain(2_000).distanceMeters();
    s.accept(10, 2_100);
    s.accept(10, 3_000);
    double second = s.drain(3_000).distanceMeters();
    assertEquals(10.0 * 2.0, first + second, 1e-9); // 1.0 s .. 3.0 s at 10 m/s, nothing lost
  }

  @Test
  public void nanInfiniteAndNegativeSpeedsAreSkipped() {
    SpeedIntegrator s = new SpeedIntegrator();
    s.drain(1_000);
    s.accept(Double.NaN, 1_000);
    s.accept(Double.POSITIVE_INFINITY, 1_100);
    s.accept(-3, 1_200);
    assertFalse(s.drain(2_000).valid()); // nothing usable yet
    s.accept(5, 2_000);
    s.accept(5, 3_000);
    assertEquals(5.0, s.drain(3_000).distanceMeters(), 1e-9);
  }
}
