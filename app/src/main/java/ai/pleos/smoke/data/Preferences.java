package ai.pleos.smoke.data;
import android.content.Context;
import android.content.SharedPreferences;
import ai.pleos.smoke.core.Tariff;
import org.json.JSONObject;
public final class Preferences {
    private final SharedPreferences prefs;
    public Preferences(Context c){prefs=c.getSharedPreferences("carpool",Context.MODE_PRIVATE);}
    public boolean automatic(){return prefs.getBoolean("outsideAuto",true);}
    public boolean applied(){return prefs.getBoolean("outsideApplied",false);}
    public void setOutside(boolean automatic,boolean applied){prefs.edit().putBoolean("outsideAuto",automatic).putBoolean("outsideApplied",applied).apply();}
    public String character(){return prefs.getString("character","horse");}
    public String theme(){return prefs.getString("theme","auto");}
    public void setAppearance(String character,String theme){prefs.edit().putString("character",character).putString("theme",theme).apply();}
    public Tariff custom(){try{return TariffCodec.decode(new JSONObject(prefs.getString("custom","")));}catch(Exception e){return Tariff.customDefault();}}
    public boolean isCustom(){return prefs.getBoolean("customSelected",false);}
    public void selectCustom(boolean value){prefs.edit().putBoolean("customSelected",value).apply();}
    public Tariff tariff(){return isCustom()?custom():Tariff.seoul();}
    public void saveCustom(Tariff tariff){prefs.edit().putString("custom",TariffCodec.encode(tariff).toString()).apply();}
}
