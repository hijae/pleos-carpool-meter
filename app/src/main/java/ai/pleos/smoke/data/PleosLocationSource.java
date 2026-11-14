package ai.pleos.smoke.data;

import ai.pleos.playground.location.fused.FusedLocation;
import ai.pleos.playground.location.fused.listener.FusedLocationListener;
import ai.pleos.playground.location.fused.model.LocationData;
import ai.pleos.smoke.core.MotionEstimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

public final class PleosLocationSource {
  public static final String PERMISSION = "pleos.car.permission.FUSED_LOCATION";

  public interface Observer {
    void onMotion(MotionEstimator.Result result);

    void onStatus(String status);
  }

  private final Context context;
  private final Observer observer;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final MotionEstimator estimator = new MotionEstimator();
  private FusedLocation location;
  private boolean running;
  private long generation;
  private long lastArrival;
  private final Runnable watchdog =
      new Runnable() {
        public void run() {
          if (!running) return;
          if (android.os.SystemClock.elapsedRealtime() - lastArrival > 10_000)
            observer.onStatus("위치 수신 대기/끊김 · 요금 누적 보류");
          handler.postDelayed(this, 2000);
        }
      };
  private FusedLocationListener listener;

  public PleosLocationSource(Context context, Observer observer) {
    this.context = context.getApplicationContext();
    this.observer = observer;
  }

  public void start() {
    if (running) return;
    if (context.checkSelfPermission(PERMISSION) != PackageManager.PERMISSION_GRANTED) {
      observer.onStatus("위치 권한 필요");
      return;
    }
    try {
      location = new FusedLocation(context);
      if (!location.isSupported()) {
        observer.onStatus("FusedLocation 미지원");
        stop();
        return;
      }
      location.initialize();
      running = true;
      long token = ++generation;
      estimator.reset();
      lastArrival = android.os.SystemClock.elapsedRealtime();
      listener =
          new FusedLocationListener() {
            public void onChanged(LocationData data) {
              handler.post(
                  () -> {
                    if (!running || token != generation) return;
                    if (data == null) {
                      observer.onStatus("사용 가능한 위치 없음");
                      return;
                    }
                    MotionEstimator.Result result =
                        estimator.accept(
                            new MotionEstimator.Sample(
                                data.getLatitude(),
                                data.getLongitude(),
                                data.getTimestampMillis(),
                                data.getConfidence()),
                            System.currentTimeMillis());
                    if (result.valid()) lastArrival = android.os.SystemClock.elapsedRealtime();
                    observer.onStatus(result.status() + " · 신뢰도 " + data.getConfidence());
                    observer.onMotion(result);
                  });
            }

            public void onError(Exception error) {
              handler.post(
                  () -> {
                    if (running && token == generation)
                      observer.onStatus("위치 오류: " + error.getMessage());
                  });
            }
          };
      location.registerFusedLocationCallback(listener);
      observer.onStatus("위치 서비스 응답 대기");
      handler.post(watchdog);
    } catch (Exception error) {
      stop();
      observer.onStatus("위치 연결 실패: " + error.getMessage());
    }
  }

  public void stop() {
    running = false;
    generation++;
    handler.removeCallbacks(watchdog);
    if (location != null) {
      try {
        if (listener != null) location.unregisterFusedLocationCallback(listener);
      } catch (Exception ignored) {
      }
      try {
        location.release();
      } catch (Exception ignored) {
      }
    }
    location = null;
    listener = null;
    estimator.reset();
  }
}
