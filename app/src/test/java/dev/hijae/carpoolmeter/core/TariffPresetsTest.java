package dev.hijae.carpoolmeter.core;

import static org.junit.Assert.*;

import java.util.HashSet;
import org.junit.Test;

public class TariffPresetsTest {
  @Test
  public void seoulIsDefaultAndFirst() {
    assertEquals("seoul", TariffPresets.DEFAULT_ID);
    assertEquals("seoul", TariffPresets.all().get(0).id());
    assertEquals(Tariff.seoul().version, TariffPresets.byId("seoul").tariff().version);
    assertEquals("seoul", TariffPresets.byId("unknown-id").id());
  }

  @Test
  public void presetsAreUniqueAndSelfConsistent() {
    var ids = new HashSet<String>();
    for (var p : TariffPresets.all()) {
      assertTrue(p.id(), ids.add(p.id()));
      assertFalse(p.note().isBlank());
      assertTrue(p.tariff().outsidePercent > 0);
      assertEquals(TariffPresets.indexOf(p.id()), TariffPresets.all().indexOf(p));
    }
    assertEquals(8, ids.size());
  }

  @Test
  public void publishedFigures() {
    Tariff busan = TariffPresets.byId("busan").tariff();
    assertEquals(4800, busan.baseWon);
    assertEquals(2000, busan.baseMeters, 0);
    assertEquals(132, busan.distanceUnitMeters, 0);
    assertEquals(33_000, busan.timeUnitMillis);
    Tariff daegu = TariffPresets.byId("daegu").tariff();
    assertEquals(4500, daegu.baseWon);
    assertEquals(1700, daegu.baseMeters, 0);
    assertEquals(125, daegu.distanceUnitMeters, 0);
    assertEquals(35, daegu.outsidePercent);
    Tariff daejeon = TariffPresets.byId("daejeon").tariff();
    assertEquals(4800, daejeon.baseWon);
    assertEquals(1650, daejeon.baseMeters, 0);
    assertEquals(29_000, daejeon.timeUnitMillis);
  }

  @Test
  public void nightBandsFollowEachRegionsClock() {
    Tariff busan = TariffPresets.byId("busan").tariff();
    long at = java.time.ZonedDateTime.of(2026, 11, 15, 1, 0, 0, 0, java.time.ZoneId.of("Asia/Seoul"))
        .toInstant().toEpochMilli();
    assertEquals(30, busan.nightPercent(at));
    long at2230 = java.time.ZonedDateTime.of(2026, 11, 15, 22, 30, 0, 0, java.time.ZoneId.of("Asia/Seoul"))
        .toInstant().toEpochMilli();
    assertEquals(0, busan.nightPercent(at2230));
    assertEquals(20, TariffPresets.byId("ulsan").tariff().nightPercent(at2230));
  }

  @Test
  public void lowSpeedThresholdIsDistanceOverTime() {
    assertEquals(15.72, Tariff.seoul().lowSpeedKmh, 0.001);
    Tariff busan = TariffPresets.byId("busan").tariff();
    assertEquals(14.4, busan.lowSpeedKmh, 0.001);
  }
}
