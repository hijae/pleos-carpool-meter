package ai.pleos.smoke.core;
import org.junit.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.Assert.*;
public class NightFareTest {
    private long wall(String iso){return Instant.parse(iso).toEpochMilli();}
    private Tariff tariff(Tariff.Stacking stack){return new Tariff("검증","v","","",1000,0,100,100,10000,15,20,stack,List.of(new Tariff.NightBand(1380,120,40)),1,"Asia/Seoul");}
    @Test public void boundaryDoesNotRepricePreviousProgress(){
        long boundary=wall("2026-11-14T14:00:00Z");
        FareEngine e=new FareEngine(tariff(Tariff.Stacking.ADDITIVE),0,boundary-5000,false);
        e.advance(10000,boundary+5000,100,40,true,false);
        assertEquals(1120,e.totalWon()); // 50 won before 23:00 + 70 after.
    }
    @Test public void midnightAndOutsideStacking(){
        long midnight=wall("2026-11-14T15:00:00Z");
        FareEngine sum=new FareEngine(tariff(Tariff.Stacking.ADDITIVE),0,midnight,true);
        sum.advance(1000,midnight+1000,100,40,true,true);assertEquals(1760,sum.totalWon());
        FareEngine product=new FareEngine(tariff(Tariff.Stacking.MULTIPLICATIVE),0,midnight,true);
        product.advance(1000,midnight+1000,100,40,true,true);assertEquals(1848,product.totalWon());
    }
    @Test public void laterRateOnlyAffectsLaterAccrual(){
        long start=wall("2026-11-14T06:00:00Z");
        FareEngine e=new FareEngine(tariff(Tariff.Stacking.ADDITIVE),0,start,false);
        e.advance(1000,start+1000,50,40,true,false);assertEquals(50,e.accumulatedWon());
        e.advance(2000,start+2000,50,40,true,true);assertEquals(1110,e.totalWon());
    }
}
