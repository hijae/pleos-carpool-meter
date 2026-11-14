package ai.pleos.smoke;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import ai.pleos.playground.vehicle.Vehicle;
import ai.pleos.playground.vehicle.api.Odometer;
import kotlin.Unit;

public class MainActivity extends Activity {
    private Vehicle vehicle;
    private Odometer odometer;
    private TextView status;
    private Button readButton;
    private boolean closed;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.rgb(18, 27, 42));
        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(24);
        status.setGravity(Gravity.CENTER);
        layout.addView(status);
        readButton = new Button(this);
        readButton.setText("Read odometer");
        layout.addView(readButton);
        setContentView(layout);
        readButton.setOnClickListener(view -> readOdometer());
        try {
            vehicle = new Vehicle(getApplicationContext());
            vehicle.initialize();
            odometer = vehicle.getOdometer();
            status.setText("Pleos Vehicle SDK 2.3.2 initialized.\nVehicle service connection is not yet verified.\nTap Read odometer to query.");
        } catch (Exception error) {
            status.setText("SDK initialization failed: " + error);
            readButton.setEnabled(false);
        }
    }

    private void readOdometer() {
        if (odometer == null || closed) return;
        readButton.setEnabled(false);
        status.setText("Reading vehicle odometer...");
        try {
            odometer.getCurrentDistanceDriven(value -> {
                showResult("Odometer API response: " + value + "\n(raw SDK value; unit not resolved)");
                return Unit.INSTANCE;
            }, error -> {
                showResult("Odometer query failed: " + error);
                return Unit.INSTANCE;
            });
        } catch (Exception error) {
            showResult("Odometer query failed: " + error);
        }
    }

    private void showResult(String message) {
        runOnUiThread(() -> {
            if (closed) return;
            status.setText(message);
            readButton.setEnabled(true);
        });
    }

    @Override protected void onDestroy() {
        closed = true;
        if (vehicle != null) vehicle.release();
        super.onDestroy();
    }
}
