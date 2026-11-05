package es.iesmdc.profes;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Window;
import android.widget.TextView;

import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/** Colores y utilidades de interfaz, con modo claro y oscuro. */
final class Ui {
  final boolean night;
  final float density;
  final int bg, card, text, sub, accent, header, stroke, ripple;

  Ui(Context c) {
    night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
        == Configuration.UI_MODE_NIGHT_YES;
    density = c.getResources().getDisplayMetrics().density;
    bg = night ? 0xFF121212 : 0xFFF6F3EF;
    card = night ? 0xFF1E1E1E : 0xFFFFFFFF;
    text = night ? 0xFFECECEC : 0xFF222222;
    sub = night ? 0xFFA8A8A8 : 0xFF6B6B6B;
    accent = night ? 0xFFE48A95 : 0xFF8B1E2D;
    header = night ? 0xFF5E1420 : 0xFF8B1E2D;
    stroke = night ? 0xFF2E2E2E : 0xFFE9E3DC;
    ripple = night ? 0x33FFFFFF : 0x228B1E2D;
  }

  int dp(float v) {
    return Math.round(v * density);
  }

  GradientDrawable round(int color, float radiusDp) {
    GradientDrawable g = new GradientDrawable();
    g.setColor(color);
    g.setCornerRadius(radiusDp * density);
    return g;
  }

  /** Fondo de tarjeta con efecto de pulsación. */
  Drawable cardBg(float radiusDp) {
    GradientDrawable shape = round(card, radiusDp);
    shape.setStroke(dp(1), stroke);
    return new RippleDrawable(ColorStateList.valueOf(ripple), shape, round(Color.WHITE, radiusDp));
  }

  Drawable rippleOn(int color, float radiusDp) {
    return new RippleDrawable(ColorStateList.valueOf(ripple), round(color, radiusDp),
        round(Color.WHITE, radiusDp));
  }

  TextView text(Context c, String s, float sp, int color, boolean bold) {
    TextView t = new TextView(c);
    t.setText(s);
    t.setTextSize(sp);
    t.setTextColor(color);
    if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
    return t;
  }

  int dialogTheme() {
    return night ? android.R.style.Theme_Material_Dialog_Alert
        : android.R.style.Theme_Material_Light_Dialog_Alert;
  }

  /** Dibujo de borde a borde (obligatorio con targetSdk 35+): barras transparentes. */
  @SuppressWarnings("deprecation")
  void edgeToEdge(Activity a, boolean lightNavIcons) {
    Window w = a.getWindow();
    WindowCompat.setDecorFitsSystemWindows(w, false);
    w.setStatusBarColor(Color.TRANSPARENT);
    w.setNavigationBarColor(Color.TRANSPARENT);
    WindowInsetsControllerCompat ctl = WindowCompat.getInsetsController(w, w.getDecorView());
    ctl.setAppearanceLightStatusBars(false); // cabecera granate: iconos blancos
    ctl.setAppearanceLightNavigationBars(lightNavIcons);
  }
}
