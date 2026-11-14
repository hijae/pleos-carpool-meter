package ai.pleos.smoke;

import android.widget.Button;
import android.widget.Switch;
import android.view.View;
import android.view.ViewGroup;
import android.content.Context;
import ai.pleos.smoke.data.Preferences;
import ai.pleos.smoke.data.TripStore;
import ai.pleos.smoke.data.TariffCodec;
import ai.pleos.smoke.core.Tariff;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class AppIntegrationTest {
    @Before public void clear(){Context c=RuntimeEnvironment.getApplication();c.getSharedPreferences("carpool",0).edit().clear().commit();c.getSharedPreferences("checkpoint",0).edit().clear().commit();c.deleteDatabase("trips.db");}
    private Button find(View root,String title){if(root instanceof Button b&&title.contentEquals(b.getText()))return b;if(root instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){Button b=find(g.getChildAt(i),title);if(b!=null)return b;}return null;}
    private Switch automatic(View root){if(root instanceof Switch s&&s.getText().toString().contains("행정구역"))return s;if(root instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){Switch s=automatic(g.getChildAt(i));if(s!=null)return s;}return null;}
    @Test public void mainTogglePersistsManualAndSettingsReflectIt(){
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity main=controller.get();Button toggle=find(main.getWindow().getDecorView(),"시외할증 켜기");assertNotNull(toggle);toggle.performClick();
            Preferences prefs=new Preferences(main);assertFalse(prefs.automatic());assertTrue(prefs.applied());
            try(var settings=Robolectric.buildActivity(SettingsActivity.class).setup()){
                Switch s=automatic(settings.get().getWindow().getDecorView());assertNotNull(s);assertFalse(s.isChecked());
            }
        }
    }
    @Test public void tariffCodecAndTripHistoryPreserveFareSnapshotAndDeduplicate(){
        Tariff original=Tariff.seoul();Tariff decoded=TariffCodec.decode(TariffCodec.encode(original));assertEquals(original.version,decoded.version);assertEquals(100,decoded.baseRoundingWon);
        MeterApplication app=(MeterApplication)RuntimeEnvironment.getApplication();app.begin();app.pause();
        try(TripStore db=new TripStore(app)){db.save(app);db.save(app);assertEquals(1,db.list().size());var trip=db.list().get(0);assertEquals(app.engine.totalWon(),trip.won());assertEquals(original.version,TariffCodec.decode(new org.json.JSONObject(trip.tariff())).version);}
        catch(org.json.JSONException e){throw new AssertionError(e);}
        app.engine.finish();app.clearCheckpoint();
    }
    @Test public void noPermissionStillAllowsMeterAndSettingsToOpen(){
        try(var main=Robolectric.buildActivity(MainActivity.class).setup()){
            assertNotNull(find(main.get().getWindow().getDecorView(),"시작"));
            assertNotNull(find(main.get().getWindow().getDecorView(),"설정"));
        }
        try(var settings=Robolectric.buildActivity(SettingsActivity.class).setup()){
            assertNotNull(find(settings.get().getWindow().getDecorView(),"저장"));
        }
    }
    @Test public void seoulBaseNightRatesMatchPublishedTable(){
        Tariff t=Tariff.seoul();long twentyTwo=java.time.Instant.parse("2026-11-14T13:00:00Z").toEpochMilli();
        assertEquals(5800,new ai.pleos.smoke.core.FareEngine(t,0,twentyTwo,false).totalWon());
        assertEquals(6700,new ai.pleos.smoke.core.FareEngine(t,0,twentyTwo+3600000,false).totalWon());
    }
}
