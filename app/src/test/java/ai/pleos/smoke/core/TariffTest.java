package ai.pleos.smoke.core;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.List;
import org.junit.Test;

public class TariffTest {
  @Test
  public void midnightRangesAreHalfOpen() {
    Tariff t = Tariff.customDefault();
    assertEquals(20, t.nightPercent(Instant.parse("2026-11-14T13:00:00Z").toEpochMilli()));
    assertEquals(40, t.nightPercent(Instant.parse("2026-11-14T14:00:00Z").toEpochMilli()));
    assertEquals(40, t.nightPercent(Instant.parse("2026-11-14T16:59:59Z").toEpochMilli()));
    assertEquals(20, t.nightPercent(Instant.parse("2026-11-14T17:00:00Z").toEpochMilli()));
    assertEquals(0, t.nightPercent(Instant.parse("2026-11-14T19:00:00Z").toEpochMilli()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void overlappingNightBandsRejected() {
    new Tariff(
        "검증",
        "v",
        "",
        "",
        1000,
        0,
        100,
        100,
        1000,
        10,
        20,
        Tariff.Stacking.ADDITIVE,
        List.of(new Tariff.NightBand(1380, 120, 20), new Tariff.NightBand(60, 180, 40)),
        1,
        "Asia/Seoul");
  }

  @Test
  public void stackingExplicit() {
    assertEquals(
        1.6,
        Tariff.customDefault()
            .multiplier(Instant.parse("2026-11-14T14:00:00Z").toEpochMilli(), true),
        0.0001);
  }
}
