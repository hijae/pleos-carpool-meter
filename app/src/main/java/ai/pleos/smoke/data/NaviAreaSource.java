package ai.pleos.smoke.data;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import ai.pleos.playground.navi.helper.NaviHelper;
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener;
import ai.pleos.playground.navi.data.CurrentLocationInfo;
import ai.pleos.playground.navi.constants.NaviErrorCode;
import ai.pleos.smoke.core.AdministrativeArea;
import kotlin.Unit;

/** getCurrentLocationInfo is request/response, so refresh every five seconds while visible. */
public final class NaviAreaSource {
    public static final String PERMISSION = "pleos.car.permission.NAVI_ROUTE";
    public interface Observer { void onArea(String key, String address, long receivedElapsed); void onNaviStatus(String message); }
    private final Context context;
    private final Observer observer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private NaviHelper navi;
    private NaviHelperEventListener listener;
    private long generation;
    private boolean active;
    private long lastReply;
    private final Runnable poll = new Runnable() {
        public void run() {
            if (!active || navi == null) return;
            if (android.os.SystemClock.elapsedRealtime()-lastReply > 15_000) observer.onNaviStatus("行政区 응답 대기 · 자동 판정 보류".replace("行政区", "행정구역"));
            try { navi.getCurrentLocationInfo(); } catch (Exception e) { observer.onNaviStatus("내비 위치 오류: "+e.getMessage()); }
            handler.postDelayed(this, 5000);
        }
    };
    public NaviAreaSource(Context context, Observer observer) { this.context=context.getApplicationContext();this.observer=observer; }
    public void start() {
        if (active) return;
        if (context.checkSelfPermission(PERMISSION)!=PackageManager.PERMISSION_GRANTED) {observer.onNaviStatus("내비 위치 권한 필요");return;}
        try {
            navi=new NaviHelper(context);active=true;long token=++generation;
            listener=new NaviHelperEventListener() {
                public void onCurrentLocationInfo(CurrentLocationInfo info) { handler.post(()->{
                    if (!active || token!=generation) return;
                    lastReply=android.os.SystemClock.elapsedRealtime();
                    String address=info==null?null:info.getAddress(); String key=AdministrativeArea.fromAddress(address);
                    observer.onNaviStatus(key==null?"행정구역 식별 불가 · 자동 판정 보류":key);
                    observer.onArea(key,address,lastReply);
                }); }
                public void onError(NaviErrorCode error) {handler.post(()->{if(active&&token==generation)observer.onNaviStatus("내비 오류: "+error);});}
            };
            navi.initialize(()->{handler.post(()->{
                if (!active||token!=generation) return;
                navi.addListener(listener);lastReply=android.os.SystemClock.elapsedRealtime();handler.post(poll);
            });return Unit.INSTANCE;},error->{handler.post(()->{
                if (active&&token==generation) observer.onNaviStatus("내비 연결 실패: "+error.getMessage());
            });return Unit.INSTANCE;});
        } catch (Exception e) {stop();observer.onNaviStatus("내비 초기화 실패: "+e.getMessage());}
    }
    public void stop() {
        active=false;generation++;handler.removeCallbacks(poll);
        if(navi!=null){try{if(listener!=null)navi.removeListener(listener);}catch(Exception ignored){}try{navi.release();}catch(Exception ignored){}}
        navi=null;listener=null;
    }
}
