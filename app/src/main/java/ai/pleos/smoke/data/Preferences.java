package ai.pleos.smoke.data;

import ai.pleos.smoke.core.Tariff;
import ai.pleos.smoke.core.TariffPresets;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONObject;

public final class Preferences {
  private final SharedPreferences prefs;

  public Preferences(Context c) {
    prefs = c.getSharedPreferences("carpool", Context.MODE_PRIVATE);
  }

  public boolean applied() {
    return prefs.getBoolean("outsideApplied", false);
  }

  public void setOutsideApplied(boolean applied) {
    prefs.edit().putBoolean("outsideApplied", applied).apply();
  }

  public String character() {
    return prefs.getString("character", "horse");
  }

  public void setCharacter(String character) {
    prefs.edit().putString("character", character).apply();
  }

  public Tariff custom() {
    try {
      return TariffCodec.decode(new JSONObject(prefs.getString("custom", "")));
    } catch (Exception e) {
      return Tariff.customDefault();
    }
  }

  /** The vehicle-speed permission dialog is shown once automatically; the main button asks again. */
  public boolean speedPermissionAsked() {
    return prefs.getBoolean("speedPermissionAsked", false);
  }

  public void setSpeedPermissionAsked() {
    prefs.edit().putBoolean("speedPermissionAsked", true).apply();
  }

  public static final String CUSTOM = "custom";

  /** Selected region preset id, or {@link #CUSTOM}. Defaults to Seoul. */
  public String region() {
    String saved = prefs.getString("region", null);
    if (saved != null) return saved;
    return prefs.getBoolean("customSelected", false) ? CUSTOM : TariffPresets.DEFAULT_ID;
  }

  public void setRegion(String id) {
    prefs.edit().putString("region", id).apply();
  }

  public boolean isCustom() {
    return CUSTOM.equals(region());
  }

  public Tariff tariff() {
    return isCustom() ? custom() : TariffPresets.byId(region()).tariff();
  }

  public void saveCustom(Tariff tariff) {
    prefs.edit().putString("custom", TariffCodec.encode(tariff).toString()).apply();
  }
}
