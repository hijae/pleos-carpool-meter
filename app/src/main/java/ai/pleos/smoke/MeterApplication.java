package ai.pleos.smoke;
import android.app.Application;
import android.os.SystemClock;
import ai.pleos.smoke.core.FareEngine;
import ai.pleos.smoke.core.OutsideController;
import ai.pleos.smoke.data.Preferences;
import ai.pleos.smoke.data.TariffCodec;
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
    public void onCreate(){super.onCreate();Preferences p=new Preferences(this);outside=new OutsideController(p.automatic(),p.applied());restore();}
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
    public void checkpoint(){if(!active())return;try{
        var s=engine.snapshot();JSONObject state=new JSONObject().put("id",tripId).put("start",startedWall).put("duration",duration()).put("tariff",TariffCodec.encode(engine.tariff))
            .put("total",s.totalWon()).put("elapsed",s.elapsedMillis()).put("meters",s.distanceMeters()).put("basic",s.basicRemaining()).put("progress",s.progress()).put("pending",s.pendingWon())
            .put("baseline",outside.baseline()).put("automatic",outside.automatic()).put("applied",outside.applied()).put("events",events);
        getSharedPreferences("checkpoint",MODE_PRIVATE).edit().putString("active",state.toString()).apply();
    }catch(Exception e){motionStatus="운행 임시 저장 실패 · 종료 시 다시 저장하세요";}}
    public void clearCheckpoint(){getSharedPreferences("checkpoint",MODE_PRIVATE).edit().remove("active").apply();}
    private void restore(){try{
        String raw=getSharedPreferences("checkpoint",MODE_PRIVATE).getString("active",null);if(raw==null)return;JSONObject j=new JSONObject(raw);
        long now=SystemClock.elapsedRealtime();engine=FareEngine.restore(TariffCodec.decode(j.getJSONObject("tariff")),new FareEngine.Snapshot(j.getLong("total"),j.getLong("elapsed"),j.getDouble("meters"),j.getString("basic"),j.getString("progress"),j.getString("pending")),now);
        tripId=j.getString("id");startedWall=j.getLong("start");startedElapsed=now-j.getLong("duration");pausedAt=now;totalPaused=0;events=j.getJSONArray("events");
        outside=new OutsideController(j.getBoolean("automatic"),j.getBoolean("applied"));outside.restoreTrip(j.optString("baseline",null));
        motionStatus="이전 운행 복원 · 일시정지 상태";
    }catch(Exception e){engine=null;motionStatus="이전 운행 복원 실패 · 저장된 기록은 유지됨";}}
}
