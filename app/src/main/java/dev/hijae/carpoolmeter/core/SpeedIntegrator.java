package dev.hijae.carpoolmeter.core;

/**
 * Turns vehicle speed samples (m/s) into distance by trapezoidal integration. A gap longer than
 * {@link #MAX_GAP_MILLIS} is never bridged and is never treated as stationary time.
 */
public final class SpeedIntegrator {
  public static final long MAX_GAP_MILLIS = 3_000;

  private double lastSpeed;
  private long lastTime = -1;
  private double distance;
  private long lastDrain = -1;

  public synchronized void reset() {
    lastSpeed = 0;
    lastTime = -1;
    distance = 0;
    lastDrain = -1;
  }

  /** Adds a speed sample taken at {@code timeMillis} (monotonic clock). */
  public synchronized void accept(double speedMetersPerSecond, long timeMillis) {
    // NaN, infinite and negative speeds are skipped.
    if (!Double.isFinite(speedMetersPerSecond) || speedMetersPerSecond < 0 || timeMillis <= 0)
      return;
    if (lastTime >= 0) {
      long dt = timeMillis - lastTime;
      if (dt <= 0) return;
      if (dt <= MAX_GAP_MILLIS) distance += (lastSpeed + speedMetersPerSecond) / 2 * (dt / 1000.0);
    }
    lastSpeed = speedMetersPerSecond;
    lastTime = timeMillis;
  }

  /** Distance and speed accumulated since the previous call. */
  public synchronized MotionSample drain(long nowMillis) {
    long previous = lastDrain;
    lastDrain = nowMillis;
    if (lastTime < 0 || nowMillis - lastTime > MAX_GAP_MILLIS) {
      distance = 0;
      lastSpeed = 0;
      return new MotionSample(false, 0, 0, 0, "차량 속도 수신 대기/끊김 · 요금 누적 보류");
    }
    if (previous < 0 || nowMillis <= previous) {
      return new MotionSample(false, lastSpeed * 3.6, 0, 0, "차량 속도 대기");
    }
    double drained = distance;
    distance = 0;
    double kmh = lastSpeed * 3.6;
    if (kmh < 0.5) kmh = 0;
    return new MotionSample(true, kmh, drained, nowMillis - previous, "차량 속도 수신 중");
  }
}
