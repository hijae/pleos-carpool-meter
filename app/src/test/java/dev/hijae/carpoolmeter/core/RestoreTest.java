package dev.hijae.carpoolmeter.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class RestoreTest {
  @Test
  public void restartRestoresAsPausedWithoutBillingGap() {
    Tariff t = Tariff.customDefault();
    FareEngine a = new FareEngine(t, 0, 0, false);
    a.advance(1000, 1000, 1650, 40, true, false);
    FareEngine b = FareEngine.restore(t, a.snapshot(), 1000000);
    assertTrue(b.isPaused());
    assertEquals(a.totalWon(), b.totalWon());
    assertEquals(a.accumulatedWon(), b.accumulatedWon());
    b.advance(1001000, 1001000, 100, 40, true, false);
    assertEquals(a.totalWon(), b.totalWon());
  }
}
