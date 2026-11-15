package ai.pleos.smoke.data;

import ai.pleos.smoke.core.MotionSample;
import ai.pleos.smoke.core.OdometerIntegrator;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

/**
 * Pleos Vehicle SDK Odometer (pleos.car.permission.CAR_DRIVE, normal), polled once per second.
 * Used when vehicle speed is unavailable.
 */
public final class OdometerSource {
  private final Context context;
  private final VehicleSpeedSource.Observer observer;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final OdometerIntegrator integrator = new OdometerIntegrator();
  private ai.pleos.playground.vehicle.Vehicle vehicle;
  private boolean running;
  private long generation;
  private String prefix = "";

  private final Runnable tick =
      new Runnable() {
        public void run() {
          if (!running) return;
          request();
          MotionSample sample = integrator.drain(SystemClock.elapsedRealtime());
          observer.onStatus(prefix + sample.status());
          observer.onMotion(sample);
          handler.postDelayed(this, 1000);
        }
      };

  public OdometerSource(Context context, VehicleSpeedSource.Observer observer) {
    this.context = context.getApplicationContext();
    this.observer = observer;
  }

  /** @param reason shown before the odometer status, e.g. why vehicle speed is not used */
  public void start(String reason) {
    if (running) return;
    prefix = reason == null || reason.isEmpty() ? "" : reason + " · ";
    if (context
        .getPackageManager()
        .queryIntentServices(new Intent("ai.pleos.playground.vehicle.VEHICLE_START"), 0)
        .isEmpty()) {
      observer.onStatus(prefix + "Pleos 차량 서비스 없음 · 요금 누적 보류");
      return;
    }
    try {
      vehicle = new ai.pleos.playground.vehicle.Vehicle(context);
      vehicle.initialize();
    } catch (Throwable error) {
      vehicle = null;
      observer.onStatus(prefix + "오도미터 연결 실패: " + error.getMessage());
      return;
    }
    running = true;
    ++generation;
    integrator.reset();
    observer.onStatus(prefix + "오도미터 응답 대기");
    handler.postDelayed(tick, 1000);
  }

  public void stop() {
    running = false;
    generation++;
    handler.removeCallbacks(tick);
    if (vehicle != null) {
      try {
        vehicle.release();
      } catch (Throwable ignored) {
      }
      vehicle = null;
    }
    integrator.reset();
  }

  private void request() {
    long token = generation;
    try {
      vehicle
          .getOdometer()
          .getCurrentDistanceDriven(
              km -> {
                handler.post(
                    () -> {
                      if (running && token == generation && km != null)
                        integrator.accept(km, SystemClock.elapsedRealtime());
                    });
                return kotlin.Unit.INSTANCE;
              },
              error -> kotlin.Unit.INSTANCE);
    } catch (Throwable ignored) {
      // Not bound yet: the missing reading is reported as "no response", never as stationary.
    }
  }
}
