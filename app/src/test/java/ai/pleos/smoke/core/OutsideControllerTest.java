package ai.pleos.smoke.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class OutsideControllerTest {
    @Test public void leavingReturningAndJitter(){
        OutsideController c=new OutsideController(true,false);c.beginTrip("서울특별시");
        c.onArea("경기도/고양시",1000,1000);assertFalse(c.applied());
        c.onArea("서울특별시",2000,2000);c.onArea("경기도/고양시",3000,3000);assertFalse(c.applied());
        c.onArea("경기도/고양시",13000,13000);assertTrue(c.applied());
        c.onArea("서울특별시",14000,14000);c.onArea("서울특별시",24000,24000);assertFalse(c.applied());
    }
    @Test public void mainButtonChangesModeAndIgnoresFurtherAutomaticUpdates(){
        OutsideController c=new OutsideController(true,false);c.beginTrip("서울");c.toggleManual();
        assertFalse(c.automatic());assertTrue(c.applied());
        c.onArea("서울",1000,1000);c.onArea("서울",11000,11000);assertTrue(c.applied());
        c.endTrip();c.beginTrip("서울");assertFalse(c.automatic());assertTrue(c.applied());
        c.setAutomatic(true);c.onArea("서울",12000,12000);c.onArea("서울",22000,22000);assertFalse(c.applied());
    }
    @Test public void missingStaleOrUnknownBaselineNeverEnablesSurcharge(){
        OutsideController c=new OutsideController(true,false);c.beginTrip("서울");
        c.onArea("경기",1000,20000);c.onArea(null,21000,21000);assertFalse(c.applied());
        c.endTrip();c.beginTrip(null);c.onArea("경기",22000,22000);c.onArea("경기",32000,32000);assertFalse(c.applied());
    }
}
