package ai.pleos.smoke;

import ai.pleos.smoke.core.MotionEstimator;
import ai.pleos.smoke.data.NaviAreaSource;
import ai.pleos.smoke.data.PleosLocationSource;
import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

public final class LocationDiagnosticsActivity extends Activity
    implements PleosLocationSource.Observer, NaviAreaSource.Observer {
  private TextView status, values, areaStatus, address, odometer;
  private PleosLocationSource source;
  private NaviAreaSource navi;
  private ai.pleos.playground.vehicle.Vehicle vehicle;
  private double meters;

  public void onCreate(Bundle state) {
    super.onCreate(state);
    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(24, 24, 24, 24);
    status = new TextView(this);
    values = new TextView(this);
    status.setTextSize(20);
    values.setTextSize(24);
    layout.addView(status);
    layout.addView(values);
    areaStatus = new TextView(this);
    address = new TextView(this);
    odometer = new TextView(this);
    layout.addView(areaStatus);
    layout.addView(address);
    layout.addView(odometer);
    Button permission = new Button(this);
    permission.setText("위치 권한 요청 / 다시 연결");
    permission.setOnClickListener(
        v ->
            requestPermissions(
                new String[] {
                  PleosLocationSource.PERMISSION,
                  NaviAreaSource.PERMISSION,
                  "pleos.car.permission.CAR_DRIVE"
                },
                1));
    layout.addView(permission);
    Button read = new Button(this);
    read.setText("오도미터 원시값 조회");
    read.setOnClickListener(v -> readOdometer());
    layout.addView(read);
    android.widget.ScrollView scroll = new android.widget.ScrollView(this);
    scroll.addView(layout);
    setContentView(scroll);
    source = new PleosLocationSource(this, this);
    navi = new NaviAreaSource(this, this);
  }

  protected void onStart() {
    super.onStart();
    source.start();
    navi.start();
  }

  protected void onStop() {
    source.stop();
    navi.stop();
    if (vehicle != null) {
      try {
        vehicle.release();
      } catch (Exception ignored) {
      }
      vehicle = null;
    }
    super.onStop();
  }

  public void onStatus(String message) {
    status.setText(message);
  }

  public void onMotion(MotionEstimator.Result result) {
    meters += result.distanceMeters();
    values.setText(
        String.format(
            Locale.KOREA,
            "추정 %.1f km/h\n누적 %.1f m\n샘플 간격 %d ms",
            result.speedKmh(),
            meters,
            result.intervalMillis()));
  }

  public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
    super.onRequestPermissionsResult(request, permissions, grants);
    source.stop();
    navi.stop();
    source.start();
    navi.start();
  }

  public void onNaviStatus(String message) {
    areaStatus.setText(message);
  }

  public void onArea(String key, String raw, long time) {
    address.setText("주소: " + raw + "\n행정구역 키: " + key + "\n응답 수신: " + time + " ms (측정 시각 아님)");
  }

  private void readOdometer() {
    try {
      if (vehicle == null) {
        vehicle = new ai.pleos.playground.vehicle.Vehicle(getApplicationContext());
        vehicle.initialize();
      }
      odometer.setText("차량 서비스 응답 대기");
      vehicle
          .getOdometer()
          .getCurrentDistanceDriven(
              value -> {
                runOnUiThread(
                    () -> {
                      if (!isFinishing() && !isDestroyed())
                        odometer.setText("오도미터 원시값: " + value + " · 단위/갱신 주기는 실기 확인 필요");
                    });
                return kotlin.Unit.INSTANCE;
              },
              error -> {
                runOnUiThread(
                    () -> {
                      if (!isFinishing() && !isDestroyed())
                        odometer.setText("오도미터 조회 실패: " + error.getMessage());
                    });
                return kotlin.Unit.INSTANCE;
              });
    } catch (Exception e) {
      odometer.setText("오도미터 연결 실패: " + e.getMessage());
    }
  }
}
