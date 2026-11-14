package ai.pleos.smoke;

import ai.pleos.smoke.data.TariffCodec;
import ai.pleos.smoke.data.TripStore;
import android.os.Bundle;
import android.widget.LinearLayout;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

public final class HistoryActivity extends UiActivity {
  public void onCreate(Bundle state) {
    super.onCreate(state);
    showList();
  }

  private String date(long millis) {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.of("Asia/Seoul"))
        .format(DateTimeFormatter.ofPattern("MM.dd HH:mm"));
  }

  private String summary(TripStore.Trip trip) {
    return String.format(
        Locale.KOREA,
        "%s   %,d원\n%.2f km · %d분 %d초",
        date(trip.start()),
        trip.won(),
        trip.meters() / 1000,
        trip.duration() / 60000,
        (trip.duration() / 1000) % 60);
  }

  private void showList() {
    LinearLayout l = column();
    l.addView(text("주행 내역", 26));
    l.addView(button("미터로 돌아가기", this::finish));
    try (TripStore store = new TripStore(this)) {
      var list = store.list();
      if (list.isEmpty()) l.addView(text("아직 주행 내역이 없습니다.", 18));
      for (var trip : list) l.addView(button(summary(trip), () -> detail(trip)));
    } catch (Exception e) {
      l.addView(text("주행 내역을 읽지 못했습니다. 다시 시도하세요.", 18));
      l.addView(button("다시 읽기", this::showList));
    }
    scroll(l);
  }

  private void detail(TripStore.Trip trip) {
    LinearLayout l = column();
    l.addView(text("운행 상세", 26));
    l.addView(button("목록", this::showList));
    l.addView(text(summary(trip), 24));
    l.addView(text("종료 " + date(trip.end()), 16));
    try {
      var tariff = TariffCodec.decode(new JSONObject(trip.tariff()));
      l.addView(
          text(
              tariff.name
                  + "\n요금표 "
                  + tariff.version
                  + "\n기본 "
                  + tariff.baseWon
                  + "원 / "
                  + tariff.baseMeters
                  + "m\n추가 "
                  + tariff.stepWon
                  + "원 / "
                  + tariff.distanceUnitMeters
                  + "m · 저속 "
                  + tariff.timeUnitMillis / 1000
                  + "초",
              16));
      JSONArray events = new JSONArray(trip.events());
      l.addView(text("할증 변경 기록", 20));
      for (int i = 0; i < events.length(); i++) {
        JSONObject e = events.getJSONObject(i);
        l.addView(
            text(
                date(e.getLong("at"))
                    + " · 심야 "
                    + e.getInt("nightPercent")
                    + "% · 시외 "
                    + (e.getBoolean("outside") ? "적용" : "꺼짐")
                    + " · "
                    + e.getString("mode")
                    + "\n"
                    + e.optString("reason"),
                15));
      }
    } catch (Exception e) {
      l.addView(text("요금제 또는 할증 기록 형식을 읽지 못했습니다.", 16));
    }
    scroll(l);
  }
}
