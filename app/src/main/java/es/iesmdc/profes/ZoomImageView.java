package es.iesmdc.profes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

/** Visor de imagen con zoom por pellizco, arrastre y doble toque. */
@SuppressLint("AppCompatCustomView")
final class ZoomImageView extends ImageView {
  private static final float MAX_ZOOM = 6f;
  private static final float DOUBLE_TAP_ZOOM = 2.5f;

  private final Matrix matrix = new Matrix();
  private final float[] values = new float[9];
  private final RectF rect = new RectF();
  private float baseScale = 1f;
  private final ScaleGestureDetector scaleDetector;
  private final GestureDetector gestureDetector;

  ZoomImageView(Context c) {
    super(c);
    setScaleType(ScaleType.MATRIX);
    scaleDetector = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
      @Override
      public boolean onScale(ScaleGestureDetector d) {
        zoomTo(currentScale() * d.getScaleFactor(), d.getFocusX(), d.getFocusY());
        return true;
      }
    });
    gestureDetector = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
      @Override
      public boolean onDown(MotionEvent e) {
        return true;
      }

      @Override
      public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
        matrix.postTranslate(-dx, -dy);
        apply();
        return true;
      }

      @Override
      public boolean onDoubleTap(MotionEvent e) {
        float target = currentScale() > baseScale * 1.1f ? baseScale : baseScale * DOUBLE_TAP_ZOOM;
        zoomTo(target, e.getX(), e.getY());
        return true;
      }
    });
  }

  @Override
  public void setImageBitmap(Bitmap bm) {
    super.setImageBitmap(bm);
    reset();
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    reset();
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent e) {
    scaleDetector.onTouchEvent(e);
    if (!scaleDetector.isInProgress()) gestureDetector.onTouchEvent(e);
    if (e.getPointerCount() > 1 || currentScale() > baseScale * 1.01f) {
      // Evita que un contenedor padre robe el gesto mientras se amplía.
      if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
    }
    return true;
  }

  private void reset() {
    if (matrix == null) return; // llamado desde el constructor de ImageView
    Drawable d = getDrawable();
    if (d == null || getWidth() == 0 || getHeight() == 0) return;
    float dw = d.getIntrinsicWidth();
    float dh = d.getIntrinsicHeight();
    if (dw <= 0 || dh <= 0) return;
    baseScale = Math.min(getWidth() / dw, getHeight() / dh);
    matrix.reset();
    matrix.postScale(baseScale, baseScale);
    matrix.postTranslate((getWidth() - dw * baseScale) / 2f, (getHeight() - dh * baseScale) / 2f);
    setImageMatrix(matrix);
  }

  private float currentScale() {
    matrix.getValues(values);
    return values[Matrix.MSCALE_X];
  }

  private void zoomTo(float target, float fx, float fy) {
    float clamped = Math.max(baseScale, Math.min(baseScale * MAX_ZOOM, target));
    float factor = clamped / currentScale();
    matrix.postScale(factor, factor, fx, fy);
    apply();
  }

  /** Mantiene la imagen centrada o pegada a los bordes, sin huecos. */
  private void apply() {
    Drawable d = getDrawable();
    if (d == null) return;
    rect.set(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
    matrix.mapRect(rect);
    float w = getWidth(), h = getHeight(), dx = 0, dy = 0;
    if (rect.width() <= w) dx = (w - rect.width()) / 2f - rect.left;
    else if (rect.left > 0) dx = -rect.left;
    else if (rect.right < w) dx = w - rect.right;
    if (rect.height() <= h) dy = (h - rect.height()) / 2f - rect.top;
    else if (rect.top > 0) dy = -rect.top;
    else if (rect.bottom < h) dy = h - rect.bottom;
    matrix.postTranslate(dx, dy);
    setImageMatrix(matrix);
  }
}
