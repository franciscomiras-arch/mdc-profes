package es.iesmdc.profes;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Horario personal: foto desde la cámara o la galería, guardada solo en este móvil. */
public class ScheduleActivity extends ComponentActivity {
  private static final int MAX_SIDE = 2400; // suficiente para leer un horario A4 con zoom
  private static final String STATE_CAMERA = "camera_uri";

  static File file(Context c) {
    return new File(c.getFilesDir(), "horario.jpg");
  }

  private final ExecutorService io = Executors.newSingleThreadExecutor();
  private Ui ui;
  private ZoomImageView image;
  private LinearLayout empty;
  private TextView hint;
  private TextView deleteBtn;
  private ProgressBar progress;
  private Uri pendingCamera;

  private ActivityResultLauncher<PickVisualMediaRequest> pickPhoto;
  private ActivityResultLauncher<Uri> takePhoto;

  @Override
  @SuppressWarnings("deprecation")
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    ui = new Ui(this);
    ui.edgeToEdge(this, false);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    if (saved != null) pendingCamera = saved.getParcelable(STATE_CAMERA);

    pickPhoto = registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
      if (uri != null) importImage(uri);
    });
    takePhoto = registerForActivityResult(new ActivityResultContracts.TakePicture(), ok -> {
      if (Boolean.TRUE.equals(ok) && pendingCamera != null) importImage(pendingCamera);
    });

    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(Color.BLACK);

    image = new ZoomImageView(this);
    image.setContentDescription("Foto de mi horario");
    root.addView(image, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT));

    empty = buildEmptyState();
    root.addView(empty, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

    LinearLayout bar = new LinearLayout(this);
    bar.setOrientation(LinearLayout.HORIZONTAL);
    bar.setGravity(Gravity.CENTER_VERTICAL);
    bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
        new int[] {0xCC000000, 0x00000000}));
    TextView back = ui.text(this, "‹  Mi horario", 19, Color.WHITE, true);
    back.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12));
    back.setContentDescription("Volver");
    back.setOnClickListener(v -> finish());
    bar.addView(back, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    bar.addView(action("📷", "Hacer foto", v -> launchCamera()));
    bar.addView(action("🖼️", "Elegir de la galería", v -> launchPicker()));
    deleteBtn = action("🗑️", "Eliminar horario", v -> confirmDelete());
    bar.addView(deleteBtn);
    root.addView(bar, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP));

    hint = ui.text(this, "Pellizca o toca dos veces para ampliar", 12, 0xCCFFFFFF, false);
    hint.setGravity(Gravity.CENTER);
    hint.setPadding(ui.dp(12), ui.dp(6), ui.dp(12), ui.dp(6));
    hint.setBackground(ui.round(0x66000000, 14));
    FrameLayout.LayoutParams hp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
    root.addView(hint, hp);

    progress = new ProgressBar(this);
    progress.setVisibility(View.GONE);
    root.addView(progress, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

    ViewCompat.setOnApplyWindowInsetsListener(root, (v, wi) -> {
      Insets b = wi.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
      bar.setPadding(b.left + ui.dp(4), b.top + ui.dp(4), b.right + ui.dp(4), ui.dp(16));
      hp.bottomMargin = b.bottom + ui.dp(16);
      hint.setLayoutParams(hp);
      empty.setPadding(b.left + ui.dp(32), 0, b.right + ui.dp(32), 0);
      return WindowInsetsCompat.CONSUMED;
    });

    setContentView(root);
    load();
  }

  @Override
  protected void onSaveInstanceState(Bundle out) {
    super.onSaveInstanceState(out);
    // La cámara puede hacer que Android cierre la app en móviles con poca memoria.
    if (pendingCamera != null) out.putParcelable(STATE_CAMERA, pendingCamera);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    io.shutdown();
  }

  private TextView action(String emoji, String description, View.OnClickListener l) {
    TextView t = ui.text(this, emoji, 22, Color.WHITE, false);
    t.setPadding(ui.dp(12), ui.dp(10), ui.dp(12), ui.dp(10));
    t.setContentDescription(description);
    t.setOnClickListener(l);
    return t;
  }

  private LinearLayout buildEmptyState() {
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setGravity(Gravity.CENTER_HORIZONTAL);
    TextView ic = ui.text(this, "🗓️", 56, Color.WHITE, false);
    ic.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    TextView title = ui.text(this, "Aún no has añadido tu horario", 20, Color.WHITE, true);
    title.setGravity(Gravity.CENTER);
    title.setPadding(0, ui.dp(12), 0, ui.dp(8));
    TextView sub = ui.text(this,
        "Hazle una foto a tu horario impreso o elige una captura de Plumier. "
            + "Se guarda solo en este móvil.", 14, 0xCCFFFFFF, false);
    sub.setGravity(Gravity.CENTER);
    box.addView(ic);
    box.addView(title);
    box.addView(sub);
    box.addView(button("📷  Hacer foto", v -> launchCamera()), buttonParams(24));
    box.addView(button("🖼️  Elegir de la galería", v -> launchPicker()), buttonParams(12));
    return box;
  }

  private Button button(String label, View.OnClickListener l) {
    Button b = new Button(this);
    b.setText(label);
    b.setAllCaps(false);
    b.setTextSize(15);
    b.setTextColor(Color.WHITE);
    b.setBackground(ui.rippleOn(0xFF8B1E2D, 24));
    b.setPadding(ui.dp(24), ui.dp(12), ui.dp(24), ui.dp(12));
    b.setOnClickListener(l);
    return b;
  }

  private LinearLayout.LayoutParams buttonParams(int topDp) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    p.topMargin = ui.dp(topDp);
    return p;
  }

  private void launchPicker() {
    pickPhoto.launch(new PickVisualMediaRequest.Builder()
        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
        .build());
  }

  private void launchCamera() {
    File dir = new File(getCacheDir(), "camera");
    if (!dir.exists() && !dir.mkdirs()) {
      toast("No se pudo preparar la cámara");
      return;
    }
    pendingCamera = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider",
        new File(dir, "captura.jpg"));
    try {
      takePhoto.launch(pendingCamera);
    } catch (ActivityNotFoundException e) {
      toast("No hay ninguna app de cámara disponible");
    }
  }

  private void confirmDelete() {
    new AlertDialog.Builder(this, ui.dialogTheme())
        .setTitle("Eliminar horario")
        .setMessage("Se borrará la foto de tu horario de este móvil.")
        .setPositiveButton("Eliminar", (d, w) -> {
          if (file(this).delete()) toast("Horario eliminado");
          load();
        })
        .setNegativeButton("Cancelar", null)
        .show();
  }

  private void load() {
    File f = file(this);
    boolean has = f.exists();
    empty.setVisibility(has ? View.GONE : View.VISIBLE);
    hint.setVisibility(has ? View.VISIBLE : View.GONE);
    deleteBtn.setVisibility(has ? View.VISIBLE : View.GONE);
    image.setVisibility(has ? View.VISIBLE : View.GONE);
    if (!has) {
      image.setImageDrawable(null);
      return;
    }
    progress.setVisibility(View.VISIBLE);
    io.execute(() -> {
      Bitmap bm = BitmapFactory.decodeFile(f.getAbsolutePath());
      runOnUiThread(() -> {
        progress.setVisibility(View.GONE);
        if (isFinishing()) return;
        if (bm == null) {
          toast("No se pudo abrir la imagen guardada");
          return;
        }
        image.setImageBitmap(bm);
      });
    });
  }

  /** Reduce, endereza (EXIF) y guarda la foto en almacenamiento privado de la app. */
  private void importImage(Uri uri) {
    progress.setVisibility(View.VISIBLE);
    final Context app = getApplicationContext();
    io.execute(() -> {
      boolean ok;
      try {
        ContentResolver cr = app.getContentResolver();
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = cr.openInputStream(uri)) {
          BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Formato no válido");

        int sample = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2;
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = sample;
        Bitmap bm;
        try (InputStream in = cr.openInputStream(uri)) {
          bm = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bm == null) throw new IOException("No se pudo decodificar");

        int rotation = 0;
        try (InputStream in = cr.openInputStream(uri)) {
          if (in != null) rotation = new ExifInterface(in).getRotationDegrees();
        } catch (IOException ignored) {
        }

        float scale = Math.min(1f, (float) MAX_SIDE / Math.max(bm.getWidth(), bm.getHeight()));
        if (rotation != 0 || scale < 1f) {
          Matrix m = new Matrix();
          m.postScale(scale, scale);
          m.postRotate(rotation);
          Bitmap r = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
          if (r != bm) bm.recycle();
          bm = r;
        }

        File tmp = new File(app.getFilesDir(), "horario.tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
          bm.compress(Bitmap.CompressFormat.JPEG, 90, out);
        }
        bm.recycle();
        ok = tmp.renameTo(file(app));
      } catch (IOException | SecurityException | OutOfMemoryError e) {
        ok = false;
      }
      File cam = new File(new File(app.getCacheDir(), "camera"), "captura.jpg");
      if (cam.exists()) cam.delete();

      final boolean saved = ok;
      runOnUiThread(() -> {
        progress.setVisibility(View.GONE);
        pendingCamera = null;
        toast(saved ? "Horario guardado" : "No se pudo guardar la imagen");
        load();
      });
    });
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }
}
