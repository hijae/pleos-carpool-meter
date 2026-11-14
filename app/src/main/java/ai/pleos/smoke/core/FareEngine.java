package ai.pleos.smoke.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Deterministic distance + low-speed time meter. UI never drives billing by animation frames. */
public final class FareEngine {
    private static final BigDecimal ONE=BigDecimal.ONE, ZERO=BigDecimal.ZERO;
    public final Tariff tariff;
    private BigDecimal basicRemaining, progress=ZERO, pendingWon=ZERO;
    private long totalWon, elapsedMillis, lastElapsed;
    private double distanceMeters;
    private boolean paused, ended;

    public FareEngine(Tariff tariff,long startElapsed,long startWall,boolean outside){
        this.tariff=tariff;this.lastElapsed=startElapsed;
        basicRemaining=divide(tariff.baseMeters,tariff.distanceUnitMeters);
        totalWon=BigDecimal.valueOf(tariff.baseWon).multiply(BigDecimal.valueOf(tariff.multiplier(startWall,outside)))
            .divide(BigDecimal.valueOf(tariff.baseRoundingWon),0,RoundingMode.HALF_UP).longValueExact()*tariff.baseRoundingWon;
    }
    private static BigDecimal divide(double n,double d){return BigDecimal.valueOf(n).divide(BigDecimal.valueOf(d),12,RoundingMode.HALF_UP);}
    private long round(BigDecimal amount){return amount.divide(BigDecimal.valueOf(tariff.roundingWon),0,RoundingMode.HALF_UP).longValueExact()*tariff.roundingWon;}

    public void advance(long endElapsed,long endWall,double distance,double speedKmh,boolean usable,boolean outside){
        long dt=endElapsed-lastElapsed;
        if(dt<=0)return;
        lastElapsed=endElapsed;
        if(paused||ended)return;
        elapsedMillis+=dt;
        // Do not bill through location outages or giant gaps; still count elapsed trip duration.
        if(!usable||dt>10_000||!Double.isFinite(distance)||distance<0||!Double.isFinite(speedKmh)||speedKmh<0)return;
        distanceMeters+=distance;
        BigDecimal units=divide(distance,tariff.distanceUnitMeters);
        if(speedKmh<tariff.lowSpeedKmh)units=units.add(divide(dt,tariff.timeUnitMillis));
        // Split at wall-clock minute boundaries, so a callback straddling a night band
        // charges only the part after the boundary at the new rate.
        long cursor=endWall-dt;
        BigDecimal remainingUnits=units;
        while(cursor<endWall){
            long boundary=Math.floorDiv(cursor,60_000)*60_000+60_000;
            long end=Math.min(boundary,endWall);
            BigDecimal part=end==endWall?remainingUnits:units.multiply(divide(end-cursor,dt));
            consume(part,tariff.multiplier(cursor,outside));
            remainingUnits=remainingUnits.subtract(part);cursor=end;
        }
    }
    private void consume(BigDecimal units,double multiplier){
        if(basicRemaining.signum()>0){BigDecimal used=units.min(basicRemaining);basicRemaining=basicRemaining.subtract(used);units=units.subtract(used);}
        BigDecimal step=BigDecimal.valueOf(tariff.stepWon).multiply(BigDecimal.valueOf(multiplier));
        while(units.signum()>0){
            BigDecimal used=units.min(ONE.subtract(progress));
            progress=progress.add(used);pendingWon=pendingWon.add(used.multiply(step));units=units.subtract(used);
            if(progress.compareTo(ONE)>=0){totalWon+=round(pendingWon);progress=ZERO;pendingWon=ZERO;}
        }
    }
    public void pause(long elapsed){lastElapsed=Math.max(elapsed,lastElapsed);paused=true;}
    public void resume(long elapsed){lastElapsed=Math.max(elapsed,lastElapsed);paused=false;}
    public void finish(){ended=true;}
    public long totalWon(){return totalWon;}
    public long elapsedMillis(){return elapsedMillis;}
    public double distanceMeters(){return distanceMeters;}
    public double progress(){return progress.doubleValue();}
    public long accumulatedWon(){return pendingWon.setScale(0,RoundingMode.DOWN).longValue();}
    public long nextIncrementWon(long wall,boolean outside){
        return round(pendingWon.add(ONE.subtract(progress).multiply(BigDecimal.valueOf(tariff.stepWon*tariff.multiplier(wall,outside)))));
    }
    public boolean isPaused(){return paused;}
    public boolean isEnded(){return ended;}
    public double basicRemainingMeters(){return basicRemaining.doubleValue()*tariff.distanceUnitMeters;}
    public record Snapshot(long totalWon,long elapsedMillis,double distanceMeters,String basicRemaining,String progress,String pendingWon){}
    public Snapshot snapshot(){return new Snapshot(totalWon,elapsedMillis,distanceMeters,basicRemaining.toPlainString(),progress.toPlainString(),pendingWon.toPlainString());}
    public static FareEngine restore(Tariff tariff,Snapshot s,long nowElapsed){
        FareEngine e=new FareEngine(tariff,nowElapsed,0,false);e.totalWon=s.totalWon();e.elapsedMillis=s.elapsedMillis();e.distanceMeters=s.distanceMeters();
        e.basicRemaining=new BigDecimal(s.basicRemaining());e.progress=new BigDecimal(s.progress());e.pendingWon=new BigDecimal(s.pendingWon());
        if(e.totalWon<0||e.elapsedMillis<0||!Double.isFinite(e.distanceMeters)||e.distanceMeters<0||e.basicRemaining.signum()<0
            ||e.progress.signum()<0||e.progress.compareTo(ONE)>=0||e.pendingWon.signum()<0)throw new IllegalArgumentException("운행 복원 데이터 오류");
        e.paused=true;return e;
    }
}
