package ai.pleos.smoke;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;

/** Small monochrome pixel characters, rendered at <= 15 fps. Idle cadence never reaches zero. */
public final class CharacterView extends View {
  private final Paint paint = new Paint();
  private static final int[] FOUR_FEET = {18, 25, 35, 41}, TWO_FEET = {24, 36};
  private String character = "horse";
  private int color;
  private double speed;
  private boolean attached;

  public CharacterView(Context context) {
    super(context);
    setContentDescription("달리는 캐릭터");
    setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
  }

  public void update(String character, int color, double speed) {
    this.character = character;
    this.color = color;
    this.speed = Math.max(0, Math.min(150, speed));
    invalidate();
  }

  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    attached = true;
    invalidate();
  }

  protected void onDetachedFromWindow() {
    attached = false;
    super.onDetachedFromWindow();
  }

  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    paint.setColor(color);
    boolean animate =
        Settings.Global.getFloat(
                getContext().getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, 1)
            != 0;
    long period = (long) Math.max(170, 1100 / (1 + speed / 22));
    int phase = animate ? (int) (SystemClock.uptimeMillis() / (period / 2) % 2) : 0;
    float scale = Math.min(getWidth() / 90f, getHeight() / 56f);
    canvas.save();
    canvas.translate((getWidth() - 90 * scale) / 2, (getHeight() - 56 * scale) / 2);
    canvas.scale(scale, scale);
    canvas.translate(16, 10 - phase);
    switch (character) {
      case "cat" -> {
        rect(canvas, 14, 17, 28, 12);
        rect(canvas, 38, 9, 14, 13);
        rect(canvas, 38, 5, 4, 7);
        rect(canvas, 48, 5, 4, 7);
        rect(canvas, 8, 11, 5, 14);
        rect(canvas, 5, 8, 7, 4);
      }
      case "rabbit" -> {
        rect(canvas, 15, 15, 27, 15);
        rect(canvas, 35, 8, 15, 15);
        rect(canvas, 36, -1, 4, 13);
        rect(canvas, 44, -2, 4, 14);
        rect(canvas, 9, 18, 7, 7);
      }
      case "duck" -> {
        rect(canvas, 13, 16, 30, 16);
        rect(canvas, 35, 9, 12, 17);
        rect(canvas, 38, 5, 11, 10);
        rect(canvas, 48, 11, 10, 4);
        rect(canvas, 6, 15, 10, 6);
      }
      default -> {
        rect(canvas, 14, 15, 30, 14);
        rect(canvas, 37, 5, 8, 16);
        rect(canvas, 42, 3, 13, 8);
        rect(canvas, 43, 0, 3, 5);
        rect(canvas, 35, 4, 3, 17);
        rect(canvas, 6, 12, 10, 4);
        rect(canvas, 3, 10, 5, 5);
      }
    }
    int[] feet = character.equals("duck") ? TWO_FEET : FOUR_FEET;
    for (int i = 0; i < feet.length; i++) {
      int off = i % 2 == phase ? 3 : -3;
      rect(canvas, feet[i], 27, 3, 8);
      rect(canvas, feet[i] + Math.min(off, 0), 34, 6, 3);
    }
    canvas.restore();
    if (attached && isShown() && animate) postInvalidateDelayed(70);
  }

  private void rect(Canvas canvas, float x, float y, float w, float h) {
    canvas.drawRect(x, y, x + w, y + h, paint);
  }
}
