package ai.pleos.smoke;

import ai.pleos.smoke.core.Tariff;
import ai.pleos.smoke.core.TariffPresets;
import ai.pleos.smoke.data.Preferences;
import android.os.Bundle;
import android.text.InputType;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SettingsActivity extends UiActivity {
  private EditText base, meters, step, unit, seconds, low, outside, round, baseRound;
  private Spinner stacking, character, region;
  private TextView errorText;
  private Switch night;
  private LinearLayout bands;
  private final List<EditText[]> rows = new ArrayList<>();

  public void onCreate(Bundle state) {
    super.onCreate(state);
    build();
  }

  private EditText field(LinearLayout parent, String title, String value) {
    parent.addView(text(title, 15));
    EditText e = new EditText(this);
    e.setSingleLine(true);
    e.setText(value);
    e.setTextColor(ink);
    e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
    parent.addView(e);
    return e;
  }

  private Spinner select(LinearLayout parent, String title, String[] labels, int selection) {
    parent.addView(text(title, 15));
    Spinner s = new Spinner(this);
    ArrayAdapter<String> adapter =
        new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels);
    s.setAdapter(adapter);
    s.setSelection(selection);
    parent.addView(s);
    return s;
  }

  private void build() {
    Tariff t = preferences.custom();
    LinearLayout l = column();
    l.addView(text("설정", 26));
    l.addView(button("미터로 돌아가기", this::save));
    errorText = text("", 15);
    errorText.setVisibility(android.view.View.GONE);
    l.addView(errorText);
    var presets = TariffPresets.all();
    String[] regionNames = new String[presets.size() + 1];
    for (int i = 0; i < presets.size(); i++) regionNames[i] = presets.get(i).tariff().name;
    regionNames[presets.size()] = "직접 설정";
    int customIndex = presets.size();
    region =
        select(
            l,
            "요금 지역",
            regionNames,
            preferences.isCustom() ? customIndex : TariffPresets.indexOf(preferences.region()));
    TextView preset = text(describe(region.getSelectedItemPosition()), 15);
    l.addView(preset);
    LinearLayout editor = column();
    l.addView(editor);
    LinearLayout outer = l;
    l = editor;
    base = field(l, "기본요금 (원)", "" + t.baseWon);
    meters = field(l, "기본거리 (m)", "" + t.baseMeters);
    step = field(l, "추가요금 단위 (원)", "" + t.stepWon);
    unit = field(l, "추가거리 단위 (m)", "" + t.distanceUnitMeters);
    seconds = field(l, "저속 시간 단위 (초)", "" + (t.timeUnitMillis / 1000.0));
    low = field(l, "저속 기준 (km/h)", "" + t.lowSpeedKmh);
    round = field(l, "요금 반올림 단위 (원)", "" + t.roundingWon);
    baseRound = field(l, "기본요금 할증 반올림 단위 (원)", "" + t.baseRoundingWon);
    night = pleosSwitch("심야할증 자동 적용", !t.nightBands.isEmpty());
    l.addView(night);
    l.addView(text("심야 시간대 · 시작~종료 / 할증률", 18));
    bands = new LinearLayout(this);
    bands.setOrientation(LinearLayout.VERTICAL);
    l.addView(bands);
    for (var b : t.nightBands) addBand(b.startMinute(), b.endMinute(), b.percent());
    l.addView(button("시간대 추가", () -> addBand(1320, 1380, 20)));
    outside = field(l, "시외할증률 (%)", "" + t.outsidePercent);
    stacking =
        select(
            l, "심야·시외 중복 적용", new String[] {"할증률 합산", "할증률 곱셈", "더 높은 할증만"}, t.stacking.ordinal());
    l = outer;
    editor.setVisibility(
        region.getSelectedItemPosition() == customIndex
            ? android.view.View.VISIBLE
            : android.view.View.GONE);
    preset.setVisibility(
        region.getSelectedItemPosition() == customIndex
            ? android.view.View.GONE
            : android.view.View.VISIBLE);
    region.setOnItemSelectedListener(
        new android.widget.AdapterView.OnItemSelectedListener() {
          public void onNothingSelected(android.widget.AdapterView<?> p) {}

          public void onItemSelected(
              android.widget.AdapterView<?> p, android.view.View v, int pos, long id) {
            editor.setVisibility(
                pos == customIndex ? android.view.View.VISIBLE : android.view.View.GONE);
            preset.setVisibility(
                pos == customIndex ? android.view.View.GONE : android.view.View.VISIBLE);
            if (pos != customIndex) preset.setText(describe(pos));
          }
        });
    character =
        select(
            l,
            "캐릭터",
            new String[] {"말", "고양이", "토끼", "오리"},
            List.of("horse", "cat", "rabbit", "duck").indexOf(preferences.character()));
    l.addView(text("진행 중인 운행의 요금제는 유지됩니다. 요금표 변경은 다음 운행부터 적용됩니다.", 14));
    scroll(l);
  }

  private void addBand(int start, int end, int percent) {
    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    EditText a = new EditText(this), b = new EditText(this), c = new EditText(this);
    EditText[] values = {a, b, c};
    a.setText(clock(start));
    b.setText(clock(end));
    c.setText("" + percent);
    for (int i = 0; i < 3; i++) {
      EditText e = values[i];
      e.setTextColor(ink);
      e.setSingleLine(true);
      size(e, BODY_M);
      e.setInputType(
          i == 2
              ? InputType.TYPE_CLASS_NUMBER
              : InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
      e.setContentDescription(i == 0 ? "시작 시각 HH:mm" : i == 1 ? "종료 시각 HH:mm" : "할증률 퍼센트");
      row.addView(e, new LinearLayout.LayoutParams(0, dp(52), 1));
    }
    row.addView(
        button(
            "삭제",
            () -> {
              bands.removeView(row);
              rows.remove(values);
            }));
    rows.add(values);
    bands.addView(row);
  }

  private String describe(int index) {
    var p = TariffPresets.all().get(index);
    Tariff t = p.tariff();
    StringBuilder bands = new StringBuilder();
    for (var band : t.nightBands) {
      if (bands.length() > 0) bands.append(" · ");
      bands.append(clock(band.startMinute()) + "~" + clock(band.endMinute()) + " " + band.percent() + "%");
    }
    return String.format(
            Locale.KOREA,
            "%s · %s 적용\n%,d원 / %.2fkm · %.0fm 또는 저속 %d초당 %d원\n심야 %s / 시외 %d%%\n",
            t.name,
            t.effectiveDate,
            t.baseWon,
            t.baseMeters / 1000,
            t.distanceUnitMeters,
            t.timeUnitMillis / 1000,
            t.stepWon,
            bands,
            t.outsidePercent)
        + p.note();
  }

  private String clock(int m) {
    return String.format(Locale.ROOT, "%02d:%02d", m / 60, m % 60);
  }

  private int minute(String value) {
    if (!value.matches("[0-2][0-9]:[0-5][0-9]"))
      throw new IllegalArgumentException("시각은 HH:mm 형식입니다");
    int h = Integer.parseInt(value.substring(0, 2));
    if (h > 23) throw new IllegalArgumentException("시각은 00:00~23:59입니다");
    return h * 60 + Integer.parseInt(value.substring(3));
  }

  private long num(EditText e) {
    return Long.parseLong(e.getText().toString());
  }

  private double decimal(EditText e) {
    return Double.parseDouble(e.getText().toString());
  }

  @Override
  public void onBackPressed() {
    save();
  }

  private void save() {
    try {
      int customIndex = TariffPresets.all().size();
      boolean custom = region.getSelectedItemPosition() == customIndex;
      if (custom) {
        List<Tariff.NightBand> list = new ArrayList<>();
        if (night.isChecked())
          for (EditText[] r : rows)
            list.add(
                new Tariff.NightBand(
                    minute(r[0].getText().toString()),
                    minute(r[1].getText().toString()),
                    Math.toIntExact(num(r[2]))));
        if (night.isChecked() && list.isEmpty())
          throw new IllegalArgumentException("심야 시간대를 추가하세요");
        long time = Math.round(decimal(seconds) * 1000);
        long outsideRate = num(outside);
        if (outsideRate > 200) throw new IllegalArgumentException("시외할증률을 확인하세요");
        Tariff tariff =
            new Tariff(
                "직접 설정",
                "custom-" + System.currentTimeMillis(),
                "사용자 직접 설정",
                "",
                num(base),
                decimal(meters),
                num(step),
                decimal(unit),
                time,
                decimal(low),
                (int) outsideRate,
                Tariff.Stacking.values()[stacking.getSelectedItemPosition()],
                list,
                num(round),
                num(baseRound),
                "Asia/Seoul");
        preferences.saveCustom(tariff);
      }
      preferences.setRegion(
          custom
              ? Preferences.CUSTOM
              : TariffPresets.all().get(region.getSelectedItemPosition()).id());
      preferences.setCharacter(
          new String[] {"horse", "cat", "rabbit", "duck"}[character.getSelectedItemPosition()]);
      finish();
    } catch (Exception e) {
      // Shown in the panel: system dialogs are sized for the full display, not this app panel.
      errorText.setText(e.getMessage() == null ? "입력값을 확인하세요" : e.getMessage());
      errorText.setVisibility(android.view.View.VISIBLE);
    }
  }
}
