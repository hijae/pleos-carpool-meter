package dev.hijae.carpoolmeter.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class OutsideControllerTest {
  @Test
  public void surchargeChangesOnlyWhenToggled() {
    OutsideController c = new OutsideController(false);
    assertFalse(c.applied());
    c.toggle();
    assertTrue(c.applied());
    c.toggle();
    assertFalse(c.applied());
  }

  @Test
  public void setOverridesCurrentState() {
    OutsideController c = new OutsideController(true);
    assertTrue(c.applied());
    c.set(false);
    assertFalse(c.applied());
    c.set(true);
    assertTrue(c.applied());
  }
}
