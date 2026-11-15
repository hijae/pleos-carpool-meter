package dev.hijae.carpoolmeter.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class OdometerIntegratorTest {
  @Test
  public void increasingOdometerBecomesDistanceAndSpeed() {
    OdometerIntegrator o = new OdometerIntegrator();
    o.drain(1_000);
    o.accept(100.000, 1_000);
    for (int i = 1; i <= 10; i++) o.accept(100.000 + i * 0.01, 1_000 + i * 1_000L); // 10 m/s
    MotionSample r = o.drain(11_000);
    assertTrue(r.valid());
    assertEquals(100.0, r.distanceMeters(), 1e-6);
    assertEquals(36.0, r.speedKmh(), 0.01);
  }

  @Test
  public void unchangedReadingsAreStationaryNotAnOutage() {
    OdometerIntegrator o = new OdometerIntegrator();
    o.drain(1_000);
    for (int i = 0; i <= 5; i++) o.accept(12345.6, 1_000 + i * 1_000L);
    MotionSample r = o.drain(6_000);
    assertTrue(r.valid());
    assertEquals(0, r.distanceMeters(), 0);
    assertEquals(0, r.speedKmh(), 0);
  }

  @Test
  public void missingReadingsNeverBill() {
    OdometerIntegrator o = new OdometerIntegrator();
    o.drain(1_000);
    o.accept(10, 1_000);
    o.accept(10.01, 2_000);
    assertTrue(o.drain(2_000).valid());
    MotionSample stale = o.drain(10_000);
    assertFalse(stale.valid());
    assertEquals(0, stale.distanceMeters(), 0);
  }

  @Test
  public void gapBetweenReadingsIsNotBridged() {
    OdometerIntegrator o = new OdometerIntegrator();
    o.drain(1_000);
    o.accept(10.0, 1_000);
    o.accept(10.0, 2_000);
    o.accept(10.5, 8_000); // 6 s silence: the 500 m jump is not billed
    assertEquals(0, o.drain(8_000).distanceMeters(), 0);
  }

  @Test
  public void coarseStepsStillAddUp() {
    OdometerIntegrator o = new OdometerIntegrator();
    o.drain(1_000);
    double total = 0;
    o.accept(1000.0, 1_000);
    for (int i = 1; i <= 30; i++) {
      o.accept(1000.0 + (i / 10) * 0.1, 1_000 + i * 1_000L); // 0.1 km steps every 10 s
      total += o.drain(1_000 + i * 1_000L).distanceMeters();
    }
    assertEquals(300.0, total, 1e-6);
  }

  @Test
  public void nanAndNegativeReadingsAreSkipped() {
    OdometerIntegrator o = new OdometerIntegrator();
    o.drain(1_000);
    o.accept(Double.NaN, 1_000);
    o.accept(-1, 1_100);
    assertFalse(o.drain(2_000).valid());
    o.accept(10.0, 2_000);
    o.accept(Double.NaN, 2_500);
    o.accept(10.01, 3_000);
    assertEquals(10.0, o.drain(3_000).distanceMeters(), 1e-6);
  }

  @Test
  public void backwardsReadingIsNotBilledAsNegativeDistance() {
    OdometerIntegrator o = new OdometerIntegrator();
    o.drain(1_000);
    o.accept(500.0, 1_000);
    o.accept(0.0, 2_000); // odometer reset
    o.accept(0.01, 3_000);
    assertEquals(10.0, o.drain(3_000).distanceMeters(), 1e-6);
  }
}
