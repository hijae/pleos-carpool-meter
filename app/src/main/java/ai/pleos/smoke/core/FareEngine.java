package ai.pleos.smoke.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.TreeMap;

/** Deterministic distance + low-speed time meter. UI never drives billing by animation frames. */
public final class FareEngine {
  private static final BigDecimal ONE = BigDecimal.ONE, ZERO = BigDecimal.ZERO;
  public final Tariff tariff;
  private BigDecimal basicRemaining, progress = ZERO, pendingWon = ZERO;
  private long totalWon, elapsedMillis, lastElapsed;
  private double distanceMeters;
  private boolean paused, ended;
  private boolean previousOutside;
  private final TreeMap<Long, Boolean> outsideChanges = new TreeMap<>();

  public FareEngine(Tariff tariff, long startElapsed, long startWall, boolean outside) {
    this.tariff = tariff;
    this.lastElapsed = startElapsed;
    this.previousOutside = outside;
    basicRemaining = divide(tariff.baseMeters, tariff.distanceUnitMeters);
    totalWon =
        BigDecimal.valueOf(tariff.baseWon)
                .multiply(tariff.factor(startWall, outside))
                .divide(BigDecimal.valueOf(tariff.baseRoundingWon), 0, RoundingMode.HALF_UP)
                .longValueExact()
            * tariff.baseRoundingWon;
  }

  private static BigDecimal divide(double n, double d) {
    return BigDecimal.valueOf(n).divide(BigDecimal.valueOf(d), 12, RoundingMode.HALF_UP);
  }

  private long round(BigDecimal amount) {
    return amount
            .divide(BigDecimal.valueOf(tariff.roundingWon), 0, RoundingMode.HALF_UP)
            .longValueExact()
        * tariff.roundingWon;
  }

  public void advance(
      long endElapsed,
      long endWall,
      double distance,
      double speedKmh,
      boolean usable,
      boolean outside) {
    long fromElapsed = lastElapsed;
    long dt = endElapsed - lastElapsed;
    if (dt <= 0) return;
    lastElapsed = endElapsed;
    if (paused || ended) {
      previousOutside = outside;
      outsideChanges.headMap(endElapsed, true).clear();
      return;
    }
    elapsedMillis += dt;
    // Do not bill through missing data, giant gaps or NaN/negative values; still count elapsed
    // trip duration.
    if (!usable
        || dt > 10_000
        || !Double.isFinite(distance)
        || distance < 0
        || !Double.isFinite(speedKmh)
        || speedKmh < 0) {
      previousOutside = outside;
      outsideChanges.headMap(endElapsed, true).clear();
      return;
    }
    distanceMeters += distance;
    BigDecimal units = divide(distance, tariff.distanceUnitMeters);
    if (speedKmh < tariff.lowSpeedKmh) units = units.add(divide(dt, tariff.timeUnitMillis));
    // Split at wall-clock minute boundaries, so a callback straddling a night band
    // charges only the part after the boundary at the new rate.
    long cursor = fromElapsed;
    boolean segmentOutside = outsideChanges.isEmpty() ? outside : previousOutside;
    BigDecimal remainingUnits = units;
    while (cursor < endElapsed) {
      while (!outsideChanges.isEmpty() && outsideChanges.firstKey() <= cursor)
        segmentOutside = outsideChanges.pollFirstEntry().getValue();
      long wall = endWall - dt + (cursor - fromElapsed);
      long toMinute = Math.floorDiv(wall, 60_000) * 60_000 + 60_000 - wall;
      long end = Math.min(endElapsed, cursor + toMinute);
      if (!outsideChanges.isEmpty()) end = Math.min(end, outsideChanges.firstKey());
      BigDecimal part =
          end == endElapsed ? remainingUnits : units.multiply(divide(end - cursor, dt));
      consume(part, tariff.factor(wall, segmentOutside));
      remainingUnits = remainingUnits.subtract(part);
      cursor = end;
    }
    outsideChanges.headMap(endElapsed, true).clear();
    previousOutside = outside;
  }

  /** Exact monotonic time of a manual/automatic transition, before the next motion callback. */
  public void outsideChanged(boolean applied, long elapsed) {
    if (paused || ended || elapsed <= lastElapsed) {
      previousOutside = applied;
      outsideChanges.clear();
    } else outsideChanges.put(elapsed, applied);
  }

  private void consume(BigDecimal units, BigDecimal multiplier) {
    if (basicRemaining.signum() > 0) {
      BigDecimal used = units.min(basicRemaining);
      basicRemaining = basicRemaining.subtract(used);
      units = units.subtract(used);
    }
    BigDecimal step = BigDecimal.valueOf(tariff.stepWon).multiply(multiplier);
    while (units.signum() > 0) {
      BigDecimal used = units.min(ONE.subtract(progress));
      progress = progress.add(used);
      pendingWon = pendingWon.add(used.multiply(step));
      units = units.subtract(used);
      if (progress.compareTo(ONE) >= 0) {
        totalWon += round(pendingWon);
        progress = ZERO;
        pendingWon = ZERO;
      }
    }
  }

  public void pause(long elapsed) {
    lastElapsed = Math.max(elapsed, lastElapsed);
    paused = true;
  }

  public void resume(long elapsed) {
    lastElapsed = Math.max(elapsed, lastElapsed);
    paused = false;
  }

  public void finish() {
    ended = true;
  }

  public long totalWon() {
    return totalWon;
  }

  public long elapsedMillis() {
    return elapsedMillis;
  }

  public double distanceMeters() {
    return distanceMeters;
  }

  public double progress() {
    return progress.doubleValue();
  }

  public long accumulatedWon() {
    return pendingWon.setScale(0, RoundingMode.DOWN).longValue();
  }

  public long nextIncrementWon(long wall, boolean outside) {
    return round(
        pendingWon.add(
            ONE.subtract(progress)
                .multiply(
                    BigDecimal.valueOf(tariff.stepWon).multiply(tariff.factor(wall, outside)))));
  }

  public boolean isPaused() {
    return paused;
  }

  public boolean isEnded() {
    return ended;
  }

  public double basicRemainingMeters() {
    return basicRemaining.doubleValue() * tariff.distanceUnitMeters;
  }

  public record Snapshot(
      long totalWon,
      long elapsedMillis,
      double distanceMeters,
      String basicRemaining,
      String progress,
      String pendingWon) {}

  public Snapshot snapshot() {
    return new Snapshot(
        totalWon,
        elapsedMillis,
        distanceMeters,
        basicRemaining.toPlainString(),
        progress.toPlainString(),
        pendingWon.toPlainString());
  }

  public static FareEngine restore(Tariff tariff, Snapshot s, long nowElapsed) {
    FareEngine e = new FareEngine(tariff, nowElapsed, 0, false);
    e.totalWon = s.totalWon();
    e.elapsedMillis = s.elapsedMillis();
    e.distanceMeters = s.distanceMeters();
    e.basicRemaining = new BigDecimal(s.basicRemaining());
    e.progress = new BigDecimal(s.progress());
    e.pendingWon = new BigDecimal(s.pendingWon());
    if (e.totalWon < 0
        || e.elapsedMillis < 0
        || !Double.isFinite(e.distanceMeters)
        || e.distanceMeters < 0
        || e.basicRemaining.signum() < 0
        || e.progress.signum() < 0
        || e.progress.compareTo(ONE) >= 0
        || e.pendingWon.signum() < 0) throw new IllegalArgumentException("운행 복원 데이터 오류");
    e.paused = true;
    return e;
  }
}
