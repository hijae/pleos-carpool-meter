package dev.hijae.carpoolmeter.core;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;

public class FareEngineTest {
  private Tariff tariff(long step, double basic) {
    return new Tariff(
        "테스트",
        "1",
        "",
        "",
        1000,
        basic,
        step,
        100,
        10000,
        15,
        20,
        Tariff.Stacking.ADDITIVE,
        List.of(),
        1,
        "Asia/Seoul");
  }

  @Test
  public void basicAndIncrementBoundary() {
    FareEngine e = new FareEngine(tariff(100, 100), 0, 0, false);
    e.advance(1000, 1000, 100, 40, true, false);
    assertEquals(1000, e.totalWon());
    e.advance(2000, 2000, 64, 40, true, false);
    assertEquals(64, e.accumulatedWon());
    assertEquals(.64, e.progress(), .0001);
    e.advance(3000, 3000, 36, 40, true, false);
    assertEquals(1100, e.totalWon());
    assertEquals(0, e.accumulatedWon());
  }

  @Test
  public void monetaryProgressUsesFareStep() {
    FareEngine e = new FareEngine(tariff(200, 0), 0, 0, false);
    e.advance(1000, 1000, 64, 40, true, false);
    assertEquals(128, e.accumulatedWon());
    assertEquals(200, e.nextIncrementWon(0, false));
  }

  @Test
  public void pausedMissingAndDuplicateSamplesDoNotCharge() {
    FareEngine e = new FareEngine(tariff(100, 0), 0, 0, false);
    e.advance(1000, 1000, 0, 0, false, false);
    assertEquals(0, e.accumulatedWon());
    e.pause(1000);
    e.advance(2000, 2000, 100, 40, true, false);
    e.resume(2000);
    e.advance(3000, 3000, 50, 40, true, false);
    e.advance(3000, 3000, 50, 40, true, false);
    assertEquals(50, e.accumulatedWon());
    assertEquals(50, e.distanceMeters(), 0);
    e.finish();
    e.advance(4000, 4000, 100, 40, true, false);
    assertEquals(1000, e.totalWon());
  }

  @Test
  public void lowSpeedTimeAndDistanceAreCombined() {
    FareEngine e = new FareEngine(tariff(100, 0), 0, 0, false);
    e.advance(5000, 5000, 50, 5, true, false);
    assertEquals(1100, e.totalWon());
  }

  @Test
  public void sampleSubdivisionDoesNotChangeFare() {
    FareEngine a = new FareEngine(tariff(100, 0), 0, 0, false),
        b = new FareEngine(tariff(100, 0), 0, 0, false);
    for (int i = 1; i <= 100; i++) a.advance(i * 1000, i * 1000, 1, 20, true, false);
    for (int i = 1; i <= 10; i++) b.advance(i * 10000, i * 10000, 10, 20, true, false);
    assertEquals(a.totalWon(), b.totalWon());
    assertEquals(1100, a.totalWon());
  }

  @Test
  public void nanAndNegativeMotionIsSkippedWithoutBilling() {
    long wall = java.time.Instant.parse("2026-11-14T03:00:00Z").toEpochMilli();
    FareEngine e = new FareEngine(Tariff.seoul(), 0, wall, false);
    e.advance(1_000, wall + 1_000, Double.NaN, 10, true, false);
    e.advance(2_000, wall + 2_000, -5, 10, true, false);
    e.advance(3_000, wall + 3_000, 5, Double.NaN, true, false);
    e.advance(4_000, wall + 4_000, 5, -1, true, false);
    assertEquals(0, e.snapshot().distanceMeters(), 0);
    assertEquals(4_800, e.totalWon());
    assertEquals(4_000, e.elapsedMillis());
  }
}
