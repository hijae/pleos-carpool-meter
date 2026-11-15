package ai.pleos.smoke;

import ai.pleos.smoke.core.MotionSample;
import ai.pleos.smoke.data.GearWatcher;
import ai.pleos.smoke.data.MotionInput;
import ai.pleos.smoke.data.VehicleSpeedSource;
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
    implements VehicleSpeedSource.Observer {
  private MeterApplication session;
  private MotionInput motion;
  private GearWatcher gear;
  private Boolean inPark; // null: gear unknown
  private Button historyButton, settingsButton;
  // In-app confirmation and notices: system dialogs are sized for the full display, not this panel.
  private TextView notice;
  private LinearLayout confirmRow;
  private boolean confirmingEnd;
  private String noticeText;
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
    motion = new MotionInput(this, this);
    gear =
        new GearWatcher(
            this,
            park -> {
              inPark = park;
              if (visible) refresh();
            });
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    build();
  }

  private void build() {
    applyPalette();
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(24), dp(18), dp(24), dp(18));
    root.setBackgroundColor(paper);
    LinearLayout header = new LinearLayout(this);
    header.setGravity(Gravity.CENTER_VERTICAL);
    TextView title = text("CARPOOL METER", 28);
    header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
    historyButton = button("주행 내역", this::history);
    settingsButton = button("설정", () -> startActivity(new Intent(this, SettingsActivity.class)));
    header.addView(historyButton);
    header.addView(settingsButton);
    root.addView(header);
    rates = text("", 16);
    size(rates, BODY_M);
    root.addView(rates);
    LinearLayout display = new LinearLayout(this);
    display.setGravity(Gravity.CENTER_VERTICAL);
    display.setPadding(0, dp(56), 0, 0);
    LinearLayout money = new LinearLayout(this);
    money.setOrientation(LinearLayout.VERTICAL);
    amount = text("0 원", 56);
    amount.setSingleLine(true);
    amount.setAutoSizeTextTypeUniformWithConfiguration(
        LABEL_M, HEADLINE_L, 2, android.util.TypedValue.COMPLEX_UNIT_PX);
    money.addView(amount, new LinearLayout.LayoutParams(-1, dp(72)));
    increment = text("", 14);
    size(increment, LABEL_M);
    money.addView(increment);
    counter = text("0", 25);
    money.addView(counter);
    progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progress.setMax(10000);
    progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ink));
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
    root.addView(display, new LinearLayout.LayoutParams(-1, -2));
    root.addView(progress, new LinearLayout.LayoutParams(-1, dp(6)));
    metrics = text("", 18);
    root.addView(metrics);
    root.addView(new android.view.View(this), new LinearLayout.LayoutParams(-1, 0, 1));
    LinearLayout controls = new LinearLayout(this);
    outside =
        button(
            "시외할증 켜기",
            () -> {
              session.outside.toggle();
              session.saveOutside();
              session.recordRates("메인 시외 버튼");
              session.checkpoint();
              refresh();
            });
    begin = button("시작", this::startTrip, true);
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
            },
            true);
    controls.addView(pause, new LinearLayout.LayoutParams(0, -2, 1));
    end =
        button(
            "종료",
            () -> {
              confirmingEnd = true;
              noticeText = null;
              refresh();
            });
    controls.addView(end, new LinearLayout.LayoutParams(0, -2, 1));
    // Motion source status, directly above the bottom buttons.
    signal = text("", 13);
    signal.setTextColor(muted);
    root.addView(signal);
    notice = text("", 18);
    root.addView(notice);
    confirmRow = new LinearLayout(this);
    confirmRow.addView(
        button(
            "종료",
            () -> {
              confirmingEnd = false;
              finishTrip();
            },
            true),
        new LinearLayout.LayoutParams(0, -2, 1));
    confirmRow.addView(
        button(
            "취소",
            () -> {
              confirmingEnd = false;
              refresh();
            }),
        new LinearLayout.LayoutParams(0, -2, 1));
    root.addView(confirmRow);
    root.addView(controls);
    root.addView(outside, new LinearLayout.LayoutParams(-1, -2));
    scroll(root);
    refresh();
  }

  protected void onStart() {
    super.onStart();
    visible = true;
    session.syncSettings();
    build();
    motion.start();
    gear.start();
    if (!preferences.speedPermissionAsked()
        && checkSelfPermission(VehicleSpeedSource.PERMISSION) != PackageManager.PERMISSION_GRANTED) {
      preferences.setSpeedPermissionAsked();
      permission();
    }
    handler.post(clock);
  }

  protected void onStop() {
    visible = false;
    handler.removeCallbacks(clock);
    motion.stop();
    gear.stop();
    if (!isChangingConfigurations() && session.active() && !session.engine.isPaused()) {
      session.pause();
      session.motionStatus = "화면 이탈로 일시정지 · 복귀 후 다시 시작";
    }
    session.checkpoint();
    super.onStop();
  }

  private void permission() {
    requestPermissions(new String[] {VehicleSpeedSource.PERMISSION}, 7);
  }

  public void onRequestPermissionsResult(int code, String[] names, int[] values) {
    super.onRequestPermissionsResult(code, names, values);
    if (!visible) return;
    motion.stop();
    motion.start();
  }

  private void startTrip() {
    if (session.active()) return;
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
      session.saveOutside();
      session.clearCheckpoint();
      refresh();
      history();
    } catch (Exception e) {
      session.checkpoint();
      noticeText = "저장 실패 · 운행은 일시정지 상태로 유지됩니다. 다시 종료를 눌러주세요.";
      refresh();
    }
  }

  private void history() {
    startActivity(new Intent(this, HistoryActivity.class));
  }

  public void onMotion(MotionSample result) {
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
            + (session.outside.applied() ? tariff.outsidePercent + "%" : "꺼짐"));
    outside.setText(session.outside.applied() ? "시외할증 끄기" : "시외할증 켜기");
    begin.setVisibility(active ? android.view.View.GONE : android.view.View.VISIBLE);
    pause.setVisibility(active ? android.view.View.VISIBLE : android.view.View.GONE);
    end.setEnabled(active);
    pause.setText(active && e.isPaused() ? "다시 시작" : "일시정지");
    signal.setText(session.motionStatus);
    if (confirmingEnd) {
      notice.setText("운행을 종료할까요?");
      notice.setVisibility(android.view.View.VISIBLE);
      confirmRow.setVisibility(android.view.View.VISIBLE);
    } else {
      confirmRow.setVisibility(android.view.View.GONE);
      notice.setText(noticeText == null ? "" : noticeText);
      notice.setVisibility(noticeText == null ? android.view.View.GONE : android.view.View.VISIBLE);
    }
    // Settings and history are blocked by the system while driving; a tap would cover the meter and
    // pause the trip, so they are only enabled in Park (or when the gear cannot be read).
    boolean screensAllowed = inPark == null || inPark;
    historyButton.setEnabled(screensAllowed);
    settingsButton.setEnabled(screensAllowed);
  }
}
