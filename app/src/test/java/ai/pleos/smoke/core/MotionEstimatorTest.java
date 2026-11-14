package ai.pleos.smoke.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class MotionEstimatorTest {
    private MotionEstimator.Sample sample(double lat, long t) { return new MotionEstimator.Sample(lat,127,t,1); }
    @Test public void rejectsStaleDuplicateAndGapWithoutAddingDistance() {
        MotionEstimator m = new MotionEstimator(); long t=1_000_000;
        assertFalse(m.accept(sample(37,t),t).valid());
        assertFalse(m.accept(sample(37.01,t),t).valid());
        assertFalse(m.accept(sample(37.01,t+20_000),t+20_000).valid());
        assertFalse(m.accept(sample(37,t),t+20_000).valid());
    }
    @Test public void stationaryDriftDoesNotAccumulateAndMovementDoes() {
        MotionEstimator m = new MotionEstimator(); long t=1_000_000;
        m.accept(sample(37,t),t);
        assertEquals(0,m.accept(sample(37.000001,t+1000),t+1000).distanceMeters(),0);
        MotionEstimator.Result moving=m.accept(sample(37.0001,t+2000),t+2000);
        assertTrue(moving.valid()); assertTrue(moving.distanceMeters()>10); assertTrue(moving.speedKmh()>0);
    }
    @Test public void teleportDoesNotAdvanceMeter() {
        MotionEstimator m=new MotionEstimator();m.accept(sample(37,1_000_000),1_000_000);
        assertEquals(0,m.accept(sample(38,1_001_000),1_001_000).distanceMeters(),0);
    }
}
