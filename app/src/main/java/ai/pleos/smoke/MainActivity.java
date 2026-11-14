package ai.pleos.smoke;

import ai.pleos.smoke.core.MotionEstimator;
import ai.pleos.smoke.data.NaviAreaSource;
import ai.pleos.smoke.data.PleosLocationSource;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.*;
import java.util.Locale;

public final class MainActivity extends UiActivity
    implements PleosLocationSource.Observer, NaviAreaSource.Observer {
  private MeterApplication session;
  private PleosLocationSource motion;
  private NaviAreaSource navigation;
  private TextView amount, counter, increment, metrics, rates, signal, speedLabel;
  private CharacterView character;
  private ProgressBar progress;
  private Button begin, pause, end, outside;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private boolean visible;
  private final Runnable clock =
      new Runnable() {
        public void run() {
          if (!visible) return;
          session.recordRates("심야 시간대 변경");
          refresh();
          handler.postDelayed(this, 1000);
        }
      };

  public void onCreate(Bundle state) {
    super.onCreate(state);
    session = (MeterApplication) getApplication();
    motion = new PleosLocationSource(this, this);
    navigation = new NaviAreaSource(this, this);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    build();
  }

  private void build() {
    applyPalette();
    LinearLayout root = new LedLayout(this, ink);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(24), dp(18), dp(24), dp(18));
    root.setBackgroundColor(paper);
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = text("CARPOOL METER", 19);
    header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    header.addView(button("주행 내역", this::history));
    header.addView(button("설정", () -> startActivity(new Intent(this, SettingsActivity.class))));
    root.addView(header);
    rates = text("", 16);
    root.addView(rates);
    LinearLayout display = new LinearLayout(this);
    display.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout money = new LinearLayout(this);
    money.setOrientation(LinearLayout.VERTICAL);
    amount = text("0 원", 56);
    amount.setTypeface(android.graphics.Typeface.MONOSPACE);
    amount.setSingleLine(true);
    amount.setAutoSizeTextTypeUniformWithConfiguration(
        20, 56, 2, android.util.TypedValue.COMPLEX_UNIT_SP);
    money.addView(amount, new LinearLayout.LayoutParams(-1, dp(84)));
    increment = text("", 14);
    money.addView(increment);
    counter = text("0", 25);
    counter.setTypeface(android.graphics.Typeface.MONOSPACE);
    money.addView(counter);
    progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setMax(10000);
    progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ink));
    money.addView(progress, new LinearLayout.LayoutParams(-1, dp(6)));
    display.addView(money, new LinearLayout.LayoutParams(0, -2, 1.6f));
    LinearLayout runner = new LinearLayout(this);
    runner.setOrientation(LinearLayout.VERTICAL);
    runner.setGravity(Gravity.CENTER);
    character = new CharacterView(this);
    runner.addView(character, new LinearLayout.LayoutParams(-1, dp(110)));
    speedLabel = text("", 23);
    speedLabel.setGravity(Gravity.CENTER);
    runner.addView(speedLabel);
    display.addView(runner, new LinearLayout.LayoutParams(0, -2, 1));
    root.addView(display);
    metrics = text("", 18);
    root.addView(metrics);
    LinearLayout controls = new LinearLayout(this);
    outside =
        button(
            "시외할증 켜기",
            () -> {
              session.outside.toggleManual();
              session.saveOutside();
              session.recordRates("메인 시외 버튼");
              session.checkpoint();
              refresh();
            });
    controls.addView(outside, new LinearLayout.LayoutParams(0, -2, 1));
    begin = button("시작", this::startTrip);
    controls.addView(begin, new LinearLayout.LayoutParams(0, -2, 1));
    pause =
        button(
            "일시정지",
            () -> {
              if (session.engine.isPaused()) {
                session.resume();
                motion.stop();
                motion.start();
              } else session.pause();
              refresh();
            });
    controls.addView(pause, new LinearLayout.LayoutParams(0, -2, 1));
    end =
        button(
            "종료",
            () ->
                new AlertDialog.Builder(this)
                    .setMessage("운행을 종료할까요?")
                    .setPositiveButton("종료", (d, w) -> finishTrip())
                    .setNegativeButton("취소", null)
                    .show());
    controls.addView(end, new LinearLayout.LayoutParams(0, -2, 1));
    root.addView(controls);
    signal = text("", 13);
    signal.setTextColor(muted);
    root.addView(signal);
    root.addView(button("위치 권한 / 재연결", this::permission));
    scroll(root);
    refresh();
  }

  protected void onStart() {
    super.onStart();
    visible = true;
    session.syncSettings();
    build();
    motion.start();
    navigation.start();
    handler.post(clock);
  }

  protected void onStop() {
    visible = false;
    handler.removeCallbacks(clock);
    motion.stop();
    navigation.stop();
    if (!isChangingConfigurations() && session.active() && !session.engine.isPaused()) {
      session.pause();
      session.motionStatus = "화면 이탈로 일시정지 · 복귀 후 다시 시작";
    }
    session.checkpoint();
    super.onStop();
  }

  private void permission() {
    requestPermissions(new String[] {PleosLocationSource.PERMISSION, NaviAreaSource.PERMISSION}, 7);
  }

  public void onRequestPermissionsResult(int code, String[] names, int[] values) {
    super.onRequestPermissionsResult(code, names, values);
    if (!visible) return;
    motion.stop();
    navigation.stop();
    motion.start();
    navigation.start();
  }

  private void startTrip() {
    if (session.active()) return;
    if (checkSelfPermission(PleosLocationSource.PERMISSION) != PackageManager.PERMISSION_GRANTED) {
      permission();
      return;
    }
    if (preferences.automatic()
        && (session.area == null || SystemClock.elapsedRealtime() - session.areaReceived > 15000)) {
      new AlertDialog.Builder(this)
          .setMessage("자동 시외할증에 필요한 출발 행정구역을 기다리고 있습니다. 위치 권한/내비 연결을 확인하거나 설정에서 수동 모드를 선택하세요.")
          .setPositiveButton(
              "설정", (d, w) -> startActivity(new Intent(this, SettingsActivity.class)))
          .setNegativeButton("확인", null)
          .show();
      return;
    }
    session.begin();
    session.checkpoint();
    motion.stop();
    motion.start();
    refresh();
  }

  private void finishTrip() {
    if (!session.active()) return;
    session.pause();
    try (ai.pleos.smoke.data.TripStore store = new ai.pleos.smoke.data.TripStore(this)) {
      store.save(session);
      session.engine.finish();
      session.outside.endTrip();
      session.saveOutside();
      session.clearCheckpoint();
      refresh();
      history();
    } catch (Exception e) {
      session.checkpoint();
      new AlertDialog.Builder(this)
          .setMessage("저장 실패 · 운행은 일시정지 상태로 유지됩니다. 다시 종료를 눌러주세요.")
          .setPositiveButton("확인", null)
          .show();
    }
  }

  private void history() {
    startActivity(new Intent(this, HistoryActivity.class));
  }

  public void onMotion(MotionEstimator.Result result) {
    session.speed = result.speedKmh();
    if (result.valid()) session.lastMotion = SystemClock.elapsedRealtime();
    if (session.active() && !session.engine.isPaused())
      session.engine.advance(
          SystemClock.elapsedRealtime(),
          System.currentTimeMillis(),
          result.distanceMeters(),
          result.speedKmh(),
          result.valid(),
          session.outside.applied());
    session.checkpoint();
    refresh();
  }

  public void onStatus(String message) {
    session.motionStatus = message;
    if (visible) refresh();
  }

  public void onNaviStatus(String message) {
    session.naviStatus = message;
    if (visible) refresh();
  }

  public void onArea(String key, String address, long elapsed) {
    session.area = key;
    session.areaReceived = elapsed;
    session.outside.onArea(key, elapsed, SystemClock.elapsedRealtime());
    session.saveOutside();
    session.recordRates("행정구역 변경");
    if (visible) refresh();
  }

  private void refresh() {
    if (amount == null) return;
    boolean active = session.active();
    var e = session.engine;
    long now = System.currentTimeMillis();
    var tariff = e == null ? preferences.tariff() : e.tariff;
    amount.setText(String.format(Locale.KOREA, "%,d 원", e == null ? 0 : e.totalWon()));
    counter.setText("" + (e == null ? 0 : e.accumulatedWon()));
    increment.setText(
        "다음 +"
            + (e == null ? tariff.stepWon : e.nextIncrementWon(now, session.outside.applied()))
            + "원까지");
    progress.setProgress(e == null ? 0 : (int) (e.progress() * 10000));
    boolean fresh =
        session.lastMotion > 0 && SystemClock.elapsedRealtime() - session.lastMotion < 10000;
    speedLabel.setText(
        (fresh ? String.format(Locale.KOREA, "%.0f", session.speed) : "—") + " km/h");
    character.update(
        preferences.character(), ink, fresh && active && !e.isPaused() ? session.speed : 0);
    long sec = session.duration() / 1000;
    metrics.setText(
        String.format(
            Locale.KOREA,
            "%.2f km    %02d:%02d    %s",
            e == null ? 0 : e.distanceMeters() / 1000,
            sec / 60,
            sec % 60,
            active ? (e.isPaused() ? "일시정지" : "주행 중") : "운행 대기"));
    rates.setText(
        tariff.name
            + " · 심야 "
            + tariff.nightPercent(now)
            + "% · 시외 "
            + (session.outside.applied() ? tariff.outsidePercent + "%" : "꺼짐")
            + " · "
            + (session.outside.automatic() ? "자동" : "수동"));
    outside.setText(session.outside.applied() ? "시외할증 끄기" : "시외할증 켜기");
    begin.setVisibility(active ? android.view.View.GONE : android.view.View.VISIBLE);
    pause.setVisibility(active ? android.view.View.VISIBLE : android.view.View.GONE);
    end.setEnabled(active);
    pause.setText(active && e.isPaused() ? "다시 시작" : "일시정지");
    signal.setText(session.motionStatus + "\n" + session.naviStatus);
  }
}
