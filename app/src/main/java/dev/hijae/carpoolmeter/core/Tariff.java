package dev.hijae.carpoolmeter.core;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Tariff {
  public enum Stacking {
    ADDITIVE,
    MULTIPLICATIVE,
    HIGHER_ONLY
  }

  public record NightBand(int startMinute, int endMinute, int percent) {
    public NightBand {
      if (startMinute < 0
          || startMinute >= 1440
          || endMinute < 0
          || endMinute >= 1440
          || startMinute == endMinute
          || percent < 0
          || percent > 200) throw new IllegalArgumentException("심야 시간대/할증률을 확인하세요");
    }

    public boolean contains(int minute) {
      return startMinute < endMinute
          ? minute >= startMinute && minute < endMinute
          : minute >= startMinute || minute < endMinute;
    }
  }

  public final String name, version, source, effectiveDate;
  public final long baseWon, stepWon, timeUnitMillis, roundingWon, baseRoundingWon;
  public final double baseMeters, distanceUnitMeters, lowSpeedKmh;
  public final int outsidePercent;
  public final Stacking stacking;
  public final List<NightBand> nightBands;
  public final ZoneId zone;

  public Tariff(
      String name,
      String version,
      String source,
      String effectiveDate,
      long baseWon,
      double baseMeters,
      long stepWon,
      double distanceUnitMeters,
      long timeUnitMillis,
      double lowSpeedKmh,
      int outsidePercent,
      Stacking stacking,
      List<NightBand> bands,
      long roundingWon,
      String zone) {
    this(
        name,
        version,
        source,
        effectiveDate,
        baseWon,
        baseMeters,
        stepWon,
        distanceUnitMeters,
        timeUnitMillis,
        lowSpeedKmh,
        outsidePercent,
        stacking,
        bands,
        roundingWon,
        roundingWon,
        zone);
  }

  public Tariff(
      String name,
      String version,
      String source,
      String effectiveDate,
      long baseWon,
      double baseMeters,
      long stepWon,
      double distanceUnitMeters,
      long timeUnitMillis,
      double lowSpeedKmh,
      int outsidePercent,
      Stacking stacking,
      List<NightBand> bands,
      long roundingWon,
      long baseRoundingWon,
      String zone) {
    if (name == null
        || name.isBlank()
        || version == null
        || version.isBlank()
        || baseWon < 0
        || baseWon > 1_000_000
        || stepWon <= 0
        || stepWon > 100_000
        || !Double.isFinite(baseMeters)
        || baseMeters < 0
        || baseMeters > 100_000
        || !Double.isFinite(distanceUnitMeters)
        || distanceUnitMeters < 1
        || distanceUnitMeters > 10_000
        || timeUnitMillis < 1000
        || timeUnitMillis > 3_600_000
        || !Double.isFinite(lowSpeedKmh)
        || lowSpeedKmh < 0
        || lowSpeedKmh > 100
        || outsidePercent < 0
        || outsidePercent > 200
        || stacking == null
        || roundingWon < 1
        || roundingWon > stepWon
        || baseRoundingWon < 1
        || baseRoundingWon > 1000) throw new IllegalArgumentException("요금/거리/시간/할증 입력값을 확인하세요");
    this.zone = ZoneId.of(zone);
    List<NightBand> copy = new ArrayList<>(bands);
    boolean[] occupied = new boolean[1440];
    for (NightBand b : copy)
      for (int m = 0; m < 1440; m++)
        if (b.contains(m)) {
          if (occupied[m]) throw new IllegalArgumentException("심야 시간대가 겹칩니다");
          occupied[m] = true;
        }
    this.name = name;
    this.version = version;
    this.source = source;
    this.effectiveDate = effectiveDate;
    this.baseWon = baseWon;
    this.baseMeters = baseMeters;
    this.stepWon = stepWon;
    this.distanceUnitMeters = distanceUnitMeters;
    this.timeUnitMillis = timeUnitMillis;
    this.lowSpeedKmh = lowSpeedKmh;
    this.outsidePercent = outsidePercent;
    this.stacking = stacking;
    this.nightBands = Collections.unmodifiableList(copy);
    this.roundingWon = roundingWon;
    this.baseRoundingWon = baseRoundingWon;
  }

  public int nightPercent(long epochMillis) {
    var t = Instant.ofEpochMilli(epochMillis).atZone(zone);
    int minute = t.getHour() * 60 + t.getMinute();
    for (NightBand b : nightBands) if (b.contains(minute)) return b.percent();
    return 0;
  }

  public double multiplier(long wallMillis, boolean outside) {
    return factor(wallMillis, outside).doubleValue();
  }

  public BigDecimal factor(long wallMillis, boolean outside) {
    BigDecimal n = BigDecimal.valueOf(nightPercent(wallMillis), 2),
        o = BigDecimal.valueOf(outside ? outsidePercent : 0, 2);
    return switch (stacking) {
      case ADDITIVE -> BigDecimal.ONE.add(n).add(o);
      case MULTIPLICATIVE -> BigDecimal.ONE.add(n).multiply(BigDecimal.ONE.add(o));
      case HIGHER_ONLY -> BigDecimal.ONE.add(n.max(o));
    };
  }

  public static Tariff customDefault() {
    return new Tariff(
        "직접 설정",
        "custom-1",
        "사용자 직접 설정",
        "",
        4800,
        1600,
        100,
        131,
        30_000,
        15.72,
        20,
        Stacking.ADDITIVE,
        List.of(
            new NightBand(1320, 1380, 20),
            new NightBand(1380, 120, 40),
            new NightBand(120, 240, 20)),
        10,
        "Asia/Seoul");
  }

  public static Tariff seoul() {
    return new Tariff(
        "서울 · 중형 일반",
        "seoul-sedan-2023-02-01-v1",
        "https://english.seoul.go.kr/policy/transportation/modes-of-transport/",
        "2023-02-01",
        4800,
        1600,
        100,
        131,
        30_000,
        15.72,
        20,
        Stacking.ADDITIVE,
        List.of(
            new NightBand(1320, 1380, 20),
            new NightBand(1380, 120, 40),
            new NightBand(120, 240, 20)),
        10,
        100,
        "Asia/Seoul");
  }
}
