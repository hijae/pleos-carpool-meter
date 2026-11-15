package ai.pleos.smoke.core;

import java.util.ArrayDeque;

/**
 * Turns cumulative odometer readings (km) into distance and a windowed speed. Readings are polled
 * every second, so an unchanged value means "stationary" while missing readings mean "unknown" and
 * never bill. Resolution depends on the vehicle (unverified on a real car).
 */
public final class OdometerIntegrator {
  public static final long MAX_GAP_MILLIS = 3_000;
  public static final long WINDOW_MILLIS = 10_000;

  private record Step(long time, double meters) {}

  private final ArrayDeque<Step> window = new ArrayDeque<>();
  private double lastKm;
  private long lastTime = -1;
  private long firstTime = -1;
  private double pendingMeters;
  private long lastDrain = -1;

  public synchronized void reset() {
    window.clear();
    lastTime = -1;
    firstTime = -1;
    pendingMeters = 0;
    lastDrain = -1;
  }

  /** Adds an odometer reading in kilometres taken at {@code timeMillis} (monotonic clock). */
  public synchronized void accept(double km, long timeMillis) {
    // NaN, infinite and negative readings are skipped.
    if (!Double.isFinite(km) || km < 0 || timeMillis <= 0) return;
    if (lastTime < 0) {
      lastKm = km;
      lastTime = timeMillis;
      firstTime = timeMillis;
      return;
    }
    long dt = timeMillis - lastTime;
    if (dt <= 0) return;
    // A gap between readings is an outage and a backwards reading a reset: rebase without billing.
    double meters = (km - lastKm) * 1000;
    if (dt <= MAX_GAP_MILLIS && meters >= 0) {
      pendingMeters += meters;
      window.addLast(new Step(timeMillis, meters));
    }
    lastKm = km;
    lastTime = timeMillis;
  }

  /** Distance and windowed speed accumulated since the previous call. */
  public synchronized MotionSample drain(long nowMillis) {
    long previous = lastDrain;
    lastDrain = nowMillis;
    if (lastTime < 0 || nowMillis - lastTime > MAX_GAP_MILLIS) {
      pendingMeters = 0;
      window.clear();
      lastTime = -1;
      return new MotionSample(false, 0, 0, 0, "오도미터 응답 대기/끊김 · 요금 누적 보류");
    }
    while (!window.isEmpty() && nowMillis - window.peekFirst().time() > WINDOW_MILLIS)
      window.removeFirst();
    double windowMeters = 0;
    for (Step s : window) windowMeters += s.meters();
    long span = Math.min(WINDOW_MILLIS, Math.max(1_000, nowMillis - firstTime));
    double kmh = windowMeters / (span / 1000.0) * 3.6;
    if (kmh < 0.5) kmh = 0;
    if (previous < 0 || nowMillis <= previous) return new MotionSample(false, kmh, 0, 0, "오도미터 대기");
    double drained = pendingMeters;
    pendingMeters = 0;
    return new MotionSample(true, kmh, drained, nowMillis - previous, "오도미터 수신 중");
  }
}
