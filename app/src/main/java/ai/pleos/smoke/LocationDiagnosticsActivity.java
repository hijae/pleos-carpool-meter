package ai.pleos.smoke;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Button;
import ai.pleos.smoke.data.PleosLocationSource;
import ai.pleos.smoke.core.MotionEstimator;
import java.util.Locale;

public final class LocationDiagnosticsActivity extends Activity implements PleosLocationSource.Observer {
    private TextView status, values;
    private PleosLocationSource source;
    private double meters;
    public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(1); layout.setPadding(24,24,24,24);
        status = new TextView(this); values = new TextView(this); status.setTextSize(20); values.setTextSize(24);
        layout.addView(status); layout.addView(values);
        Button permission = new Button(this); permission.setText("위치 권한 요청 / 다시 연결");
        permission.setOnClickListener(v -> requestPermissions(new String[]{PleosLocationSource.PERMISSION}, 1));
        layout.addView(permission); setContentView(layout); source = new PleosLocationSource(this,this);
    }
    protected void onStart() { super.onStart(); source.start(); }
    protected void onStop() { source.stop(); super.onStop(); }
    public void onStatus(String message) { status.setText(message); }
    public void onMotion(MotionEstimator.Result result) {
        meters += result.distanceMeters(); values.setText(String.format(Locale.KOREA,
                "추정 %.1f km/h\n누적 %.1f m\n샘플 간격 %d ms", result.speedKmh(), meters, result.intervalMillis()));
    }
    public void onRequestPermissionsResult(int request, String[] permissions, int[] grants) {
        super.onRequestPermissionsResult(request,permissions,grants); source.stop(); source.start();
    }
}
