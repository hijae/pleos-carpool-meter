package ai.pleos.smoke;

import ai.pleos.smoke.data.Preferences;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public abstract class UiActivity extends Activity {
  protected int paper, ink, muted;
  protected boolean dark;
  protected Preferences preferences;

  /** Scales dp/sp up on large head units (e.g. 2560x1440 @160dpi); unchanged on small screens. */
  @Override
  protected void attachBaseContext(Context base) {
    Configuration config = new Configuration(base.getResources().getConfiguration());
    float scale = Math.max(1f, Math.min(2f, config.smallestScreenWidthDp / 960f));
    if (scale > 1.01f) {
      config.densityDpi = Math.round(config.densityDpi * scale);
      super.attachBaseContext(base.createConfigurationContext(config));
    } else {
      super.attachBaseContext(base);
    }
  }

  protected void onCreate(Bundle state) {
    super.onCreate(state);
    preferences = new Preferences(this);
    applyPalette();
  }

  protected void applyPalette() {
    // Day/night always follows the vehicle (system ui mode); the app has no manual override.
    boolean night =
        (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
            == Configuration.UI_MODE_NIGHT_YES;
    setTheme(
        night
            ? android.R.style.Theme_Material_NoActionBar
            : android.R.style.Theme_Material_Light_NoActionBar);
    dark = night;
    // Pleos Connect color tokens: background_secondary, text, surface_high/accent.
    paper = night ? Color.parseColor("#1D1E21") : Color.parseColor("#F7F8FA");
    ink = night ? Color.WHITE : Color.parseColor("#131417");
    muted = night ? Color.rgb(173, 181, 185) : Color.parseColor("#44464E");
    getWindow().setStatusBarColor(paper);
    getWindow().setNavigationBarColor(paper);
    getWindow()
        .getDecorView()
        .setSystemUiVisibility(
            night
                ? 0
                : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  }

  // Pleos Connect typography scale (px on the 160dpi head unit).
  protected static final int HEADLINE_L = 60, TITLE_M = 36, TITLE_S = 32;
  protected static final int BODY_L = 30, BODY_M = 28, BODY_S = 26, LABEL_M = 24, LABEL_S = 20;

  /** Maps the old ad-hoc sizes onto the nearest Pleos typography step. */
  protected static int snap(float legacy) {
    if (legacy <= 14) return LABEL_S;
    if (legacy <= 16) return LABEL_M;
    if (legacy <= 20) return BODY_M;
    if (legacy <= 25) return BODY_L;
    return TITLE_M;
  }

  protected static void size(TextView t, int px) {
    t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, px);
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
    int px = snap(sp);
    size(t, px);
    if (px >= TITLE_M) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
    t.setTextColor(ink);
    t.setPadding(0, dp(6), 0, dp(6));
    return t;
  }

  protected Button button(String title, Runnable action) {
    return button(title, action, false);
  }

  /** Pleos Connect style: flat tonal fill, rounded corners, no shadow; primary is #0064FF. */
  protected Button button(String title, Runnable action, boolean primary) {
    Button b = new Button(this);
    b.setText(title);
    b.setAllCaps(false);
    b.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
    b.setStateListAnimator(null);
    size(b, BODY_M);
    b.setMinHeight(dp(56));
    b.setPadding(dp(16), 0, dp(16), 0);
    int fill = primary ? Color.rgb(0, 100, 255) : Color.parseColor(dark ? "#313236" : "#EDEEF2");
    int pressed =
        primary ? Color.rgb(0, 80, 214) : Color.parseColor(dark ? "#44464E" : "#E0E1E6");
    int off = Color.parseColor(dark ? "#26272A" : "#EDEEF2");
    android.graphics.drawable.StateListDrawable states =
        new android.graphics.drawable.StateListDrawable();
    states.addState(new int[] {-android.R.attr.state_enabled}, rounded(off));
    states.addState(new int[] {android.R.attr.state_pressed}, rounded(pressed));
    states.addState(new int[] {}, rounded(fill));
    b.setBackground(new android.graphics.drawable.InsetDrawable(states, dp(4)));
    int text = primary ? Color.WHITE : ink;
    b.setTextColor(
        new android.content.res.ColorStateList(
            new int[][] {{-android.R.attr.state_enabled}, {}},
            new int[] {mix(paper, muted, 0.6f), text}));
    b.setOnClickListener(v -> action.run());
    return b;
  }

  /** Pleos Connect switch: green track when on, grey when off, round white knob. */
  protected Switch pleosSwitch(String label, boolean checked) {
    Switch s = new Switch(this);
    s.setText(label);
    s.setTextColor(ink);
    size(s, BODY_M);
    s.setShowText(false);
    s.setSwitchMinWidth(dp(56));
    s.setSwitchPadding(dp(16));
    int on = Color.parseColor(dark ? "#00E074" : "#02C265");
    int off = Color.parseColor(dark ? "#44464E" : "#B8B8B9");
    int disabled = Color.parseColor(dark ? "#313236" : "#E7E7E8");
    int[] enabledOn = {android.R.attr.state_enabled, android.R.attr.state_checked};
    int[] disabledAny = {-android.R.attr.state_enabled};
    android.graphics.drawable.StateListDrawable track =
        new android.graphics.drawable.StateListDrawable();
    track.addState(disabledAny, pill(disabled));
    track.addState(enabledOn, pill(on));
    track.addState(new int[] {}, pill(off));
    int knob = Color.WHITE;
    int knobOff = dark ? Color.parseColor("#A4A8AE") : Color.WHITE;
    android.graphics.drawable.StateListDrawable thumb =
        new android.graphics.drawable.StateListDrawable();
    thumb.addState(new int[] {android.R.attr.state_checked}, knob(knob));
    thumb.addState(new int[] {}, knob(knobOff));
    s.setTrackDrawable(track);
    s.setThumbDrawable(thumb);
    s.setChecked(checked);
    return s;
  }

  private android.graphics.drawable.GradientDrawable pill(int color) {
    android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(16));
    d.setSize(dp(56), dp(32));
    return d;
  }

  private android.graphics.drawable.Drawable knob(int color) {
    android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
    d.setShape(android.graphics.drawable.GradientDrawable.OVAL);
    d.setColor(color);
    d.setSize(dp(26), dp(26));
    return new android.graphics.drawable.InsetDrawable(d, dp(3));
  }

  private android.graphics.drawable.GradientDrawable rounded(int color) {
    android.graphics.drawable.GradientDrawable d = new android.graphics.drawable.GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(18));
    return d;
  }

  private static int mix(int base, int over, float amount) {
    return Color.rgb(
        Math.round(Color.red(base) * (1 - amount) + Color.red(over) * amount),
        Math.round(Color.green(base) * (1 - amount) + Color.green(over) * amount),
        Math.round(Color.blue(base) * (1 - amount) + Color.blue(over) * amount));
  }

  protected void scroll(LinearLayout content) {
    ScrollView s = new ScrollView(this);
    s.setFillViewport(true);
    s.setBackgroundColor(paper);
    s.addView(content);
    setContentView(s);
  }
}
