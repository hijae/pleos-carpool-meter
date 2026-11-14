package ai.pleos.smoke;

import ai.pleos.smoke.data.Preferences;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public abstract class UiActivity extends Activity {
  protected int paper, ink, muted;
  protected Preferences preferences;

  protected void onCreate(Bundle state) {
    super.onCreate(state);
    preferences = new Preferences(this);
    applyPalette();
  }

  protected void applyPalette() {
    String theme = preferences.theme();
    boolean night =
        theme.equals("night")
            || (theme.equals("auto")
                && (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                    == Configuration.UI_MODE_NIGHT_YES);
    setTheme(
        night
            ? android.R.style.Theme_Material_NoActionBar
            : android.R.style.Theme_Material_Light_NoActionBar);
    paper = night ? Color.rgb(5, 7, 8) : Color.rgb(246, 246, 241);
    ink = night ? Color.WHITE : Color.rgb(18, 23, 26);
    muted = night ? Color.rgb(173, 181, 185) : Color.rgb(79, 88, 92);
    getWindow().setStatusBarColor(paper);
    getWindow().setNavigationBarColor(paper);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            night
                ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  }

  protected int dp(float value) {
    return Math.round(value * getResources().getDisplayMetrics().density);
  }

  protected LinearLayout column() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setPadding(dp(20), dp(16), dp(20), dp(16));
    l.setBackgroundColor(paper);
    return l;
  }

  protected TextView text(String value, float sp) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(sp);
    t.setTextColor(ink);
    t.setPadding(0, dp(6), 0, dp(6));
    return t;
  }

  protected Button button(String title, Runnable action) {
    Button b = new Button(this);
    b.setText(title);
    b.setTextColor(ink);
    b.setMinHeight(dp(48));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  protected void scroll(LinearLayout content) {
    ScrollView s = new ScrollView(this);
    s.setFillViewport(true);
    s.setBackgroundColor(paper);
    s.addView(content);
    setContentView(s);
  }
}
