package ai.pleos.smoke.core;

/** Mode and applied state are separate. Main toggle always changes the saved mode to manual. */
public final class OutsideController {
    private boolean automatic, applied, trip;
    private String baseline, candidate;
    private long candidateSince, candidateLast;
    public OutsideController(boolean automatic,boolean applied){this.automatic=automatic;this.applied=applied;}
    public void beginTrip(String startArea){trip=true;baseline=startArea;candidate=null;if(automatic)applied=false;}
    public void endTrip(){trip=false;baseline=null;candidate=null;}
    public void toggleManual(){applied=!applied;automatic=false;candidate=null;}
    public void setAutomatic(boolean value){automatic=value;candidate=null;}
    public void onArea(String area,long receivedElapsed,long nowElapsed){
        if(!automatic||!trip||baseline==null||area==null||nowElapsed<receivedElapsed||nowElapsed-receivedElapsed>15000){candidate=null;return;}
        if(!area.equals(candidate)||receivedElapsed-candidateLast>15_000){candidate=area;candidateSince=receivedElapsed;}
        candidateLast=receivedElapsed;
        if(receivedElapsed-candidateSince>=10_000)applied=!baseline.equals(area);
    }
    public boolean automatic(){return automatic;}
    public boolean applied(){return applied;}
    public String baseline(){return baseline;}
}
