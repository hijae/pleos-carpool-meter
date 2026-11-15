package ai.pleos.smoke.data;

import ai.pleos.smoke.core.MotionSample;
import ai.pleos.smoke.core.SpeedIntegrator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Vehicle-reported speed (PERF_VEHICLE_SPEED), integrated into distance once per second. Needs
 * android.car.permission.CAR_SPEED (dangerous).
 */
public final class VehicleSpeedSource {
  public static final String PERMISSION = "android.car.permission.CAR_SPEED";
  private static final long FIRST_SAMPLE_TIMEOUT_MILLIS = 5_000;

  public interface Observer {
    void onMotion(MotionSample sample);

    void onStatus(String status);
  }

  private final Context context;
  private final Observer observer;
  private final Runnable onUnavailable;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final SpeedIntegrator integrator = new SpeedIntegrator();
  private CarSpeedBackend backend;
  private boolean running;
  private boolean gotSample;
  private long startedAt;
  private long generation;

  private final Runnable tick =
      new Runnable() {
        public void run() {
          if (!running) return;
          long now = SystemClock.elapsedRealtime();
          if (!gotSample && now - startedAt > FIRST_SAMPLE_TIMEOUT_MILLIS) {
            stop();
            onUnavailable.run();
            return;
          }
          MotionSample sample = integrator.drain(now);
          observer.onStatus(sample.status());
          observer.onMotion(sample);
          handler.postDelayed(this, 1000);
        }
      };

  /** @param onUnavailable called on the main thread when the vehicle never reports a speed. */
  public VehicleSpeedSource(Context context, Observer observer, Runnable onUnavailable) {
    this.context = context.getApplicationContext();
    this.observer = observer;
    this.onUnavailable = onUnavailable;
  }

  public boolean isPermitted() {
    return context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED;
  }

  /** Returns false when speed cannot be used, so the caller should use the odometer instead. */
  public boolean start() {
    if (running) return true;
    if (!isPermitted()) return false;
    try {
      long token = ++generation;
      backend =
          new CarSpeedBackend(
              context,
              speed ->
                  handler.post(
                      () -> {
                        if (!running || token != generation) return;
                        gotSample = true;
                        integrator.accept(speed, SystemClock.elapsedRealtime());
                      }));
    } catch (Throwable unavailable) {
      backend = null;
      return false;
    }
    running = true;
    gotSample = false;
    startedAt = SystemClock.elapsedRealtime();
    integrator.reset();
    observer.onStatus("차량 속도 응답 대기");
    handler.postDelayed(tick, 1000);
    return true;
  }

  public void stop() {
    running = false;
    generation++;
    handler.removeCallbacks(tick);
    if (backend != null) {
      backend.close();
      backend = null;
    }
    integrator.reset();
  }
}
