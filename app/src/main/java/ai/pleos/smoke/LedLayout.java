package ai.pleos.smoke;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.LinearLayout;

/** Quiet dotted LED frame; no flashing and no per-frame allocation. */
public final class LedLayout extends LinearLayout {
  private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint point = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final float density;

  public LedLayout(Context context) {
    this(context, android.graphics.Color.WHITE);
  }

  public LedLayout(Context context, int color) {
    super(context);
    density = getResources().getDisplayMetrics().density;
    setWillNotDraw(false);
    point.setColor(color);
    glow.setColor(color);
    glow.setAlpha(25);
  }

  protected void onDraw(Canvas c) {
    super.onDraw(c);
    float inset = 5 * density, spacing = 13 * density;
    for (float x = inset + spacing; x < getWidth() - inset - spacing; x += spacing) {
      dot(c, x, inset);
      dot(c, x, getHeight() - inset);
    }
    for (float y = inset + spacing; y < getHeight() - inset - spacing; y += spacing) {
      dot(c, inset, y);
      dot(c, getWidth() - inset, y);
    }
  }

  private void dot(Canvas c, float x, float y) {
    c.drawCircle(x, y, 4 * density, glow);
    c.drawCircle(x, y, 1.2f * density, point);
  }
}
