package ai.pleos.smoke;
import android.app.Application;
import android.os.SystemClock;
import ai.pleos.smoke.core.FareEngine;
import ai.pleos.smoke.core.OutsideController;
import ai.pleos.smoke.data.Preferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.UUID;

/** Process-owned trip state survives Activity recreation. Background billing is paused. */
public final class MeterApplication extends Application {
    public FareEngine engine;
    public OutsideController outside;
    public String tripId,area,motionStatus="위치 수신 대기",naviStatus="행정구역 수신 대기";
    public long startedWall,startedElapsed,pausedAt,totalPaused,areaReceived,lastMotion;
    public double speed;
    public JSONArray events=new JSONArray();
    private int lastNight=-1;
    private boolean lastOutside,lastAuto;
    public void onCreate(){super.onCreate();Preferences p=new Preferences(this);outside=new OutsideController(p.automatic(),p.applied());}
    public boolean active(){return engine!=null&&!engine.isEnded();}
    public void begin(){
        Preferences p=new Preferences(this);outside.setAutomatic(p.automatic());
        if(!p.automatic()&&outside.applied()!=p.applied())outside.toggleManual();
        String startArea=SystemClock.elapsedRealtime()-areaReceived<=15000?area:null;outside.beginTrip(startArea);
        tripId=UUID.randomUUID().toString();startedWall=System.currentTimeMillis();startedElapsed=SystemClock.elapsedRealtime();pausedAt=0;totalPaused=0;
        engine=new FareEngine(p.tariff(),startedElapsed,startedWall,outside.applied());events=new JSONArray();lastNight=-1;recordRates("운행 시작");
    }
    public void recordRates(String reason){
        if(!active())return;int night=engine.tariff.nightPercent(System.currentTimeMillis());
        if(night==lastNight&&lastOutside==outside.applied()&&lastAuto==outside.automatic())return;
        try{events.put(new JSONObject().put("at",System.currentTimeMillis()).put("nightPercent",night)
            .put("outside",outside.applied()).put("mode",outside.automatic()?"자동":"수동").put("reason",reason));}catch(Exception ignored){}
        lastNight=night;lastOutside=outside.applied();lastAuto=outside.automatic();
    }
    public void pause(){if(!active()||engine.isPaused())return;long now=SystemClock.elapsedRealtime();engine.advance(now,System.currentTimeMillis(),0,0,false,outside.applied());engine.pause(now);pausedAt=now;}
    public void resume(){if(!active()||!engine.isPaused())return;long now=SystemClock.elapsedRealtime();totalPaused+=now-pausedAt;pausedAt=0;engine.resume(now);}
    public long duration(){if(engine==null)return 0;long end=pausedAt>0?pausedAt:SystemClock.elapsedRealtime();return Math.max(0,end-startedElapsed-totalPaused);}
    public void syncSettings(){Preferences p=new Preferences(this);if(outside.automatic()!=p.automatic())outside.setAutomatic(p.automatic());if(!outside.automatic()&&outside.applied()!=p.applied())outside.toggleManual();recordRates("설정 변경");}
    public void saveOutside(){new Preferences(this).setOutside(outside.automatic(),outside.applied());}
}
