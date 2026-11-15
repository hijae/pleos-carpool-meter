package dev.hijae.carpoolmeter.data;

import dev.hijae.carpoolmeter.core.Tariff;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;

public final class TariffCodec {
  public static JSONObject encode(Tariff t) {
    try {
      JSONObject j = new JSONObject();
      j.put("name", t.name);
      j.put("version", t.version);
      j.put("source", t.source);
      j.put("effectiveDate", t.effectiveDate);
      j.put("baseWon", t.baseWon);
      j.put("baseMeters", t.baseMeters);
      j.put("stepWon", t.stepWon);
      j.put("distanceUnitMeters", t.distanceUnitMeters);
      j.put("timeUnitMillis", t.timeUnitMillis);
      j.put("lowSpeedKmh", t.lowSpeedKmh);
      j.put("outsidePercent", t.outsidePercent);
      j.put("stacking", t.stacking.name());
      j.put("roundingWon", t.roundingWon);
      j.put("baseRoundingWon", t.baseRoundingWon);
      j.put("zone", t.zone.getId());
      JSONArray bands = new JSONArray();
      for (var b : t.nightBands)
        bands.put(
            new JSONObject()
                .put("start", b.startMinute())
                .put("end", b.endMinute())
                .put("percent", b.percent()));
      j.put("bands", bands);
      return j;
    } catch (Exception e) {
      throw new IllegalArgumentException("요금표 저장 실패", e);
    }
  }

  public static Tariff decode(JSONObject j) {
    try {
      ArrayList<Tariff.NightBand> bands = new ArrayList<>();
      JSONArray arr = j.getJSONArray("bands");
      for (int i = 0; i < arr.length(); i++) {
        JSONObject b = arr.getJSONObject(i);
        bands.add(new Tariff.NightBand(b.getInt("start"), b.getInt("end"), b.getInt("percent")));
      }
      return new Tariff(
          j.getString("name"),
          j.getString("version"),
          j.getString("source"),
          j.getString("effectiveDate"),
          j.getLong("baseWon"),
          j.getDouble("baseMeters"),
          j.getLong("stepWon"),
          j.getDouble("distanceUnitMeters"),
          j.getLong("timeUnitMillis"),
          j.getDouble("lowSpeedKmh"),
          j.getInt("outsidePercent"),
          Tariff.Stacking.valueOf(j.getString("stacking")),
          bands,
          j.getLong("roundingWon"),
          j.optLong("baseRoundingWon", j.getLong("roundingWon")),
          j.getString("zone"));
    } catch (Exception e) {
      throw new IllegalArgumentException("저장된 요금표가 유효하지 않습니다", e);
    }
  }
}
