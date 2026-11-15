package dev.hijae.carpoolmeter.core;

/** Out-of-area surcharge is applied only by the user's explicit toggle. */
public final class OutsideController {
  private boolean applied;

  public OutsideController(boolean applied) {
    this.applied = applied;
  }

  public void toggle() {
    applied = !applied;
  }

  public void set(boolean value) {
    applied = value;
  }

  public boolean applied() {
    return applied;
  }
}
