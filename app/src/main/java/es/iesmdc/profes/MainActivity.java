package es.iesmdc.profes;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.File;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public class MainActivity extends ComponentActivity {
  static final String ACTION_OPEN_LINK = "es.iesmdc.profes.OPEN_LINK";
  static final String ACTION_ADD_LINK = "es.iesmdc.profes.ADD_LINK";
  static final String EXTRA_URL = "url";
  private static final String EXPORT_HEADER = "# MdC Profes · enlaces";

  private Ui ui;
  private Store store;
  private LinkOpener opener;
  private LinearLayout header;
  private LinearLayout content;
  private EditText search;
  private String query = "";
  private Bitmap thumb;
  private long thumbStamp = -1;

  // ------------------------------------------------------------------ ciclo de vida

  @Override
  protected void onCreate(Bundle saved) {
    super.onCreate(saved);
    ui = new Ui(this);
    store = new Store(this);
    opener = new LinkOpener(this, ui.header);
    ui.edgeToEdge(this, !ui.night);
    buildLayout();
    if (saved == null) handleIntent(getIntent());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleIntent(intent);
  }

  @Override
  protected void onStart() {
    super.onStart();
    opener.bind();
  }

  @Override
  protected void onResume() {
    super.onResume();
    render(); // refresca la miniatura del horario al volver
  }

  @Override
  protected void onStop() {
    super.onStop();
    opener.unbind();
  }

  // ------------------------------------------------------------------ estructura

  private void buildLayout() {
    LinearLayout root = new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setBackgroundColor(ui.bg);

    header = new LinearLayout(this);
    header.setOrientation(LinearLayout.VERTICAL);
    header.setBackgroundColor(ui.header);

    LinearLayout row = new LinearLayout(this);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);
    LinearLayout titles = new LinearLayout(this);
    titles.setOrientation(LinearLayout.VERTICAL);
    TextView title = ui.text(this, "MdC Profes", 21, Color.WHITE, true);
    TextView subtitle = ui.text(this, "IES Miguel de Cervantes · Murcia", 12, 0xD9FFFFFF, false);
    titles.addView(title);
    titles.addView(subtitle);
    row.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    TextView menu = ui.text(this, "⋮", 26, Color.WHITE, true);
    menu.setPadding(ui.dp(14), ui.dp(4), ui.dp(6), ui.dp(4));
    menu.setContentDescription("Más opciones");
    menu.setOnClickListener(this::showMainMenu);
    row.addView(menu);
    header.addView(row);

    search = new EditText(this);
    search.setHint("🔍  Buscar acceso…");
    search.setHintTextColor(0xB3FFFFFF);
    search.setTextColor(Color.WHITE);
    search.setTextSize(15);
    search.setSingleLine(true);
    search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
    search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    search.setBackground(ui.round(0x33FFFFFF, 22));
    search.setPadding(ui.dp(16), ui.dp(10), ui.dp(16), ui.dp(10));
    search.addTextChangedListener(new TextWatcher() {
      @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
      @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
      @Override public void afterTextChanged(Editable s) {
        query = s.toString();
        render();
      }
    });
    search.setOnEditorActionListener((v, actionId, ev) -> {
      hideKeyboard();
      return true;
    });
    LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    sp.topMargin = ui.dp(12);
    header.addView(search, sp);
    root.addView(header);

    ScrollView scroll = new ScrollView(this);
    scroll.setClipToPadding(false);
    scroll.setFillViewport(true);
    content = new LinearLayout(this);
    content.setOrientation(LinearLayout.VERTICAL);
    scroll.addView(content);
    root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

    ViewCompat.setOnApplyWindowInsetsListener(root, (v, wi) -> {
      Insets bars = wi.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
      Insets ime = wi.getInsets(WindowInsetsCompat.Type.ime());
      header.setPadding(bars.left + ui.dp(16), bars.top + ui.dp(10), bars.right + ui.dp(12), ui.dp(14));
      content.setPadding(bars.left + ui.dp(12), ui.dp(4), bars.right + ui.dp(12),
          Math.max(bars.bottom, ime.bottom) + ui.dp(28));
      return WindowInsetsCompat.CONSUMED;
    });

    setContentView(root);
  }

  // ------------------------------------------------------------------ pintado

  private void render() {
    content.removeAllViews();
    List<LinkItem> all = allLinks();
    String q = norm(query.trim());

    if (!q.isEmpty()) {
      List<LinkItem> hits = new ArrayList<>();
      for (LinkItem it : all) {
        if (norm(it.title + " " + it.section + " " + it.url).contains(q)) hits.add(it);
      }
      sectionTitle("Resultados (" + hits.size() + ")");
      if (hits.isEmpty()) {
        TextView none = ui.text(this, "No hay accesos que coincidan. Puedes añadirlo tú con el menú ⋮.",
            14, ui.sub, false);
        none.setPadding(ui.dp(4), ui.dp(8), ui.dp(4), 0);
        content.addView(none);
      } else {
        grid(hits, false);
      }
      return;
    }

    scheduleCard();

    Map<String, LinkItem> byId = new HashMap<>();
    for (LinkItem it : all) byId.put(it.id, it);
    List<LinkItem> favs = new ArrayList<>();
    for (String id : store.favs()) {
      LinkItem it = byId.get(id);
      if (it != null) favs.add(it);
    }
    if (!favs.isEmpty()) {
      sectionTitle("★ Favoritos");
      grid(favs, false);
    }

    LinkedHashMap<String, List<LinkItem>> sections = new LinkedHashMap<>();
    for (String s : Defaults.SECTIONS) sections.put(s, new ArrayList<>());
    List<LinkItem> mine = new ArrayList<>();
    for (LinkItem it : all) {
      String s = TextUtils.isEmpty(it.section) ? Defaults.MINE : it.section.trim();
      if (s.equalsIgnoreCase(Defaults.MINE)) {
        mine.add(it);
        continue;
      }
      List<LinkItem> list = sections.get(s);
      if (list == null) {
        list = new ArrayList<>();
        sections.put(s, list);
      }
      list.add(it);
    }
    for (Map.Entry<String, List<LinkItem>> e : sections.entrySet()) {
      if (e.getValue().isEmpty()) continue;
      sectionTitle(e.getKey());
      grid(e.getValue(), false);
    }
    sectionTitle(Defaults.MINE);
    grid(mine, true);

    TextView tip = ui.text(this,
        "Consejo: mantén pulsado un acceso para fijarlo en favoritos, ocultarlo o ponerlo en la "
            + "pantalla de inicio. Desde Chrome puedes usar «Compartir › MdC Profes» para guardar "
            + "cualquier página.", 12, ui.sub, false);
    tip.setPadding(ui.dp(4), ui.dp(20), ui.dp(4), 0);
    content.addView(tip);
  }

  private void sectionTitle(String s) {
    TextView t = ui.text(this, s.toUpperCase(Locale.forLanguageTag("es")), 13, ui.accent, true);
    t.setLetterSpacing(0.06f);
    t.setPadding(ui.dp(4), ui.dp(18), 0, ui.dp(6));
    if (Build.VERSION.SDK_INT >= 28) t.setAccessibilityHeading(true);
    content.addView(t);
  }

  private int columns() {
    int w = getResources().getConfiguration().screenWidthDp;
    return Math.max(3, Math.min(6, w / 115));
  }

  private void grid(List<LinkItem> items, boolean withAddTile) {
    int cols = columns();
    GridLayout g = new GridLayout(this);
    g.setColumnCount(cols);
    int n = 0;
    for (LinkItem it : items) {
      g.addView(tile(it), cell());
      n++;
    }
    if (withAddTile) {
      g.addView(addTile(), cell());
      n++;
    }
    if (n == 0) return;
    while (n % cols != 0) { // huecos invisibles para que la última fila no se estire
      g.addView(new View(this), cell());
      n++;
    }
    content.addView(g, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
  }

  private GridLayout.LayoutParams cell() {
    GridLayout.LayoutParams p = new GridLayout.LayoutParams(GridLayout.spec(GridLayout.UNDEFINED),
        GridLayout.spec(GridLayout.UNDEFINED, 1f));
    p.width = 0;
    p.height = ui.dp(100);
    int m = ui.dp(4);
    p.setMargins(m, m, m, m);
    return p;
  }

  private View tile(LinkItem it) {
    LinearLayout t = new LinearLayout(this);
    t.setOrientation(LinearLayout.VERTICAL);
    t.setGravity(Gravity.CENTER);
    t.setPadding(ui.dp(6), ui.dp(8), ui.dp(6), ui.dp(8));
    t.setBackground(ui.cardBg(16));
    t.setElevation(ui.night ? 0 : ui.dp(1));
    TextView ic = ui.text(this, it.icon, 28, ui.text, false);
    ic.setGravity(Gravity.CENTER);
    ic.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    TextView label = ui.text(this, it.title, 12, ui.text, false);
    label.setGravity(Gravity.CENTER);
    label.setMaxLines(2);
    label.setEllipsize(TextUtils.TruncateAt.END);
    label.setPadding(0, ui.dp(4), 0, 0);
    t.addView(ic);
    t.addView(label);
    t.setContentDescription(it.title);
    t.setOnClickListener(v -> opener.open(it.url));
    t.setOnLongClickListener(v -> {
      showTileMenu(v, it);
      return true;
    });
    return t;
  }

  private View addTile() {
    LinearLayout t = new LinearLayout(this);
    t.setOrientation(LinearLayout.VERTICAL);
    t.setGravity(Gravity.CENTER);
    t.setBackground(ui.rippleOn(ui.night ? 0xFF2A1A1D : 0xFFF3E6E8, 16));
    TextView plus = ui.text(this, "+", 30, ui.accent, true);
    plus.setGravity(Gravity.CENTER);
    plus.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    TextView label = ui.text(this, "Añadir enlace", 12, ui.accent, true);
    label.setGravity(Gravity.CENTER);
    t.addView(plus);
    t.addView(label);
    t.setContentDescription("Añadir enlace");
    t.setOnClickListener(v -> showEditDialog(null, "", ""));
    return t;
  }

  private void scheduleCard() {
    LinearLayout card = new LinearLayout(this);
    card.setOrientation(LinearLayout.HORIZONTAL);
    card.setGravity(Gravity.CENTER_VERTICAL);
    card.setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12));
    card.setBackground(ui.cardBg(18));
    card.setElevation(ui.night ? 0 : ui.dp(1));

    Bitmap b = scheduleThumb();
    int size = ui.dp(60);
    LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(size, size);
    if (b != null) {
      ImageView iv = new ImageView(this);
      iv.setImageBitmap(b);
      iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
      iv.setBackground(ui.round(ui.stroke, 12));
      iv.setClipToOutline(true);
      iv.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      card.addView(iv, ip);
    } else {
      TextView ic = ui.text(this, "🗓️", 28, ui.text, false);
      ic.setGravity(Gravity.CENTER);
      ic.setBackground(ui.round(ui.night ? 0xFF2A1A1D : 0xFFF3E6E8, 12));
      ic.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
      card.addView(ic, ip);
    }

    LinearLayout texts = new LinearLayout(this);
    texts.setOrientation(LinearLayout.VERTICAL);
    texts.setPadding(ui.dp(14), 0, ui.dp(8), 0);
    texts.addView(ui.text(this, "Mi horario", 16, ui.text, true));
    texts.addView(ui.text(this, b != null ? "Toca para verlo a pantalla completa"
        : "Añade una foto de tu horario", 13, ui.sub, false));
    card.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
    TextView arrow = ui.text(this, "›", 28, ui.sub, false);
    arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    card.addView(arrow);
    card.setContentDescription(b != null ? "Mi horario" : "Añadir foto de mi horario");
    card.setOnClickListener(v -> startActivity(new Intent(this, ScheduleActivity.class)));

    LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT);
    cp.topMargin = ui.dp(14);
    cp.leftMargin = ui.dp(4);
    cp.rightMargin = ui.dp(4);
    content.addView(card, cp);
  }

  /** Miniatura en caché; solo se vuelve a decodificar si la foto cambia. */
  private Bitmap scheduleThumb() {
    File f = ScheduleActivity.file(this);
    if (!f.exists()) {
      thumb = null;
      thumbStamp = -1;
      return null;
    }
    long stamp = f.lastModified();
    if (thumb != null && stamp == thumbStamp) return thumb;
    BitmapFactory.Options o = new BitmapFactory.Options();
    o.inJustDecodeBounds = true;
    BitmapFactory.decodeFile(f.getAbsolutePath(), o);
    int s = 1;
    while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= 240) s *= 2;
    BitmapFactory.Options o2 = new BitmapFactory.Options();
    o2.inSampleSize = s;
    thumb = BitmapFactory.decodeFile(f.getAbsolutePath(), o2);
    thumbStamp = stamp;
    return thumb;
  }

  // ------------------------------------------------------------------ datos

  private List<LinkItem> allLinks() {
    Set<String> hidden = store.hidden();
    List<LinkItem> list = new ArrayList<>();
    for (LinkItem it : Defaults.LINKS) if (!hidden.contains(it.id)) list.add(it);
    list.addAll(store.custom());
    return list;
  }

  private boolean urlExists(String url, String exceptId) {
    String key = urlKey(url);
    for (LinkItem it : Defaults.LINKS) if (urlKey(it.url).equals(key)) return true;
    for (LinkItem it : store.custom()) {
      if (!it.id.equals(exceptId) && urlKey(it.url).equals(key)) return true;
    }
    return false;
  }

  private static String urlKey(String url) {
    String s = url.trim().toLowerCase(Locale.ROOT).replaceFirst("^https?://", "").replaceFirst("^www\\.", "");
    return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
  }

  private static String norm(String s) {
    return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
  }

  /** Devuelve una URL válida (añade https:// si falta) o null. */
  static String normalizeUrl(String raw) {
    if (raw == null) return null;
    String s = raw.trim();
    if (s.isEmpty() || s.contains(" ")) return null;
    if (!s.matches("(?i)^(https?://|mailto:|tel:).*")) s = "https://" + s;
    if (s.matches("(?i)^(mailto:|tel:).+")) return s;
    android.net.Uri u = android.net.Uri.parse(s);
    String host = u.getHost();
    if (host == null || !host.contains(".") || host.startsWith(".") || host.endsWith(".")) return null;
    return s;
  }

  private static String firstUrl(String text) {
    Matcher m = Patterns.WEB_URL.matcher(text);
    return m.find() ? normalizeUrl(m.group()) : null;
  }

  // ------------------------------------------------------------------ intents

  private void handleIntent(Intent in) {
    if (in == null || in.getAction() == null) return;
    String action = in.getAction();
    if (Intent.ACTION_SEND.equals(action)) {
      String text = in.getStringExtra(Intent.EXTRA_TEXT);
      if (text == null) return;
      if (text.startsWith(EXPORT_HEADER) || (text.contains("|") && text.contains("\n"))) {
        showImportDialog(text);
        return;
      }
      String url = firstUrl(text);
      if (url == null) {
        toast("No se ha encontrado ningún enlace en lo que has compartido");
        return;
      }
      String subject = in.getStringExtra(Intent.EXTRA_SUBJECT);
      showEditDialog(null, subject == null ? "" : subject.trim(), url);
    } else if (ACTION_OPEN_LINK.equals(action)) {
      String url = in.getStringExtra(EXTRA_URL);
      if (url != null) opener.open(url);
    } else if (ACTION_ADD_LINK.equals(action)) {
      showEditDialog(null, "", "");
    }
  }

  // ------------------------------------------------------------------ menús

  private void showMainMenu(View anchor) {
    PopupMenu pm = new PopupMenu(this, anchor);
    Menu m = pm.getMenu();
    m.add(0, 1, 0, "Añadir enlace");
    m.add(0, 2, 1, "Mi horario");
    m.add(0, 3, 2, "Compartir mis enlaces");
    m.add(0, 4, 3, "Importar enlaces");
    int hidden = store.hidden().size();
    m.add(0, 5, 4, "Restaurar accesos ocultos" + (hidden > 0 ? " (" + hidden + ")" : "")).setEnabled(hidden > 0);
    m.add(0, 6, 5, "Acerca de");
    pm.setOnMenuItemClickListener(mi -> {
      switch (mi.getItemId()) {
        case 1: showEditDialog(null, "", ""); break;
        case 2: startActivity(new Intent(this, ScheduleActivity.class)); break;
        case 3: exportLinks(); break;
        case 4: showImportDialog(""); break;
        case 5: store.restoreHidden(); render(); toast("Accesos restaurados"); break;
        case 6: showAbout(); break;
        default: return false;
      }
      return true;
    });
    pm.show();
  }

  private void showTileMenu(View anchor, LinkItem it) {
    PopupMenu pm = new PopupMenu(this, anchor);
    Menu m = pm.getMenu();
    m.add(0, 1, 0, store.isFav(it.id) ? "Quitar de favoritos" : "Añadir a favoritos");
    m.add(0, 2, 1, "Poner en la pantalla de inicio");
    m.add(0, 3, 2, "Copiar enlace");
    m.add(0, 4, 3, "Compartir");
    if (it.custom) {
      m.add(0, 5, 4, "Editar");
      m.add(0, 6, 5, "Eliminar");
    } else {
      m.add(0, 7, 4, "Ocultar");
    }
    pm.setOnMenuItemClickListener(mi -> {
      switch (mi.getItemId()) {
        case 1: store.toggleFav(it.id); render(); break;
        case 2: pinShortcut(it); break;
        case 3: copy(it); break;
        case 4: share(it.title + "\n" + it.url, "Compartir enlace"); break;
        case 5: showEditDialog(it, it.title, it.url); break;
        case 6: confirmDelete(it); break;
        case 7:
          store.hide(it.id);
          render();
          toast("Oculto. Puedes recuperarlo en ⋮ › Restaurar accesos ocultos");
          break;
        default: return false;
      }
      return true;
    });
    pm.show();
  }

  // ------------------------------------------------------------------ alta y edición

  private void showEditDialog(LinkItem editing, String preTitle, String preUrl) {
    LinearLayout box = new LinearLayout(this);
    box.setOrientation(LinearLayout.VERTICAL);
    box.setPadding(ui.dp(22), ui.dp(8), ui.dp(22), 0);

    final String[] icon = {editing != null ? editing.icon : "🔗"};
    TextView preview = ui.text(this, icon[0], 40, ui.text, false);
    preview.setGravity(Gravity.CENTER);
    preview.setContentDescription("Icono elegido");
    box.addView(preview);

    HorizontalScrollView hs = new HorizontalScrollView(this);
    hs.setHorizontalScrollBarEnabled(false);
    LinearLayout icons = new LinearLayout(this);
    for (String e : Defaults.ICONS) {
      TextView t = ui.text(this, e, 26, ui.text, false);
      t.setPadding(ui.dp(8), ui.dp(6), ui.dp(8), ui.dp(6));
      t.setBackground(ui.rippleOn(Color.TRANSPARENT, 10));
      t.setOnClickListener(v -> {
        icon[0] = e;
        preview.setText(e);
      });
      icons.addView(t);
    }
    hs.addView(icons);
    box.addView(hs);

    EditText title = field("Nombre (p. ej. Moodle de Lengua)", preTitle, InputType.TYPE_CLASS_TEXT
        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
    EditText url = field("Dirección web (https://…)", preUrl, InputType.TYPE_CLASS_TEXT
        | InputType.TYPE_TEXT_VARIATION_URI);

    AutoCompleteTextView section = new AutoCompleteTextView(this);
    section.setHint("Sección");
    section.setSingleLine(true);
    section.setThreshold(0);
    section.setText(editing != null ? editing.section : Defaults.MINE);
    Set<String> names = new LinkedHashSet<>();
    names.add(Defaults.MINE);
    for (String s : Defaults.SECTIONS) names.add(s);
    for (LinkItem c : store.custom()) if (!TextUtils.isEmpty(c.section)) names.add(c.section);
    section.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
        new ArrayList<>(names)));
    section.setOnFocusChangeListener((v, has) -> {
      if (has) section.showDropDown();
    });
    section.setOnClickListener(v -> section.showDropDown());

    box.addView(title);
    box.addView(url);
    box.addView(section);
    TextView help = ui.text(this, "Se guarda solo en tu móvil. Elige una sección existente o escribe una nueva.",
        12, ui.sub, false);
    help.setPadding(ui.dp(4), ui.dp(6), 0, 0);
    box.addView(help);

    ScrollView wrap = new ScrollView(this);
    wrap.addView(box);

    AlertDialog dlg = new AlertDialog.Builder(this, ui.dialogTheme())
        .setTitle(editing == null ? "Nuevo enlace" : "Editar enlace")
        .setView(wrap)
        .setPositiveButton("Guardar", null)
        .setNegativeButton("Cancelar", null)
        .create();
    dlg.setOnShowListener(d -> dlg.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
      String t = title.getText().toString().trim();
      String u = normalizeUrl(url.getText().toString());
      String s = section.getText().toString().trim();
      if (t.isEmpty()) {
        title.setError("Escribe un nombre");
        return;
      }
      if (u == null) {
        url.setError("Dirección no válida");
        return;
      }
      if (urlExists(u, editing != null ? editing.id : "")) {
        url.setError("Este enlace ya está en la app");
        return;
      }
      String id = editing != null ? editing.id : "c_" + System.currentTimeMillis();
      store.upsert(new LinkItem(id, s.isEmpty() ? Defaults.MINE : s, icon[0], t, u, true));
      dlg.dismiss();
      render();
      toast(editing == null ? "Enlace añadido" : "Enlace actualizado");
    }));
    dlg.show();
    if (preTitle.isEmpty()) title.requestFocus();
  }

  private EditText field(String hint, String value, int inputType) {
    EditText e = new EditText(this);
    e.setHint(hint);
    e.setText(value);
    e.setSingleLine(true);
    e.setInputType(inputType);
    return e;
  }

  private void confirmDelete(LinkItem it) {
    new AlertDialog.Builder(this, ui.dialogTheme())
        .setTitle("Eliminar «" + it.title + "»")
        .setMessage("Se quitará de la app en este móvil.")
        .setPositiveButton("Eliminar", (d, w) -> {
          store.delete(it.id);
          render();
        })
        .setNegativeButton("Cancelar", null)
        .show();
  }

  // ------------------------------------------------------------------ exportar / importar

  /** Texto legible para pasar enlaces a compañeros por correo, Telegram o WhatsApp. */
  private void exportLinks() {
    List<LinkItem> mine = store.custom();
    if (mine.isEmpty()) {
      toast("Aún no has añadido enlaces propios");
      return;
    }
    StringBuilder sb = new StringBuilder(EXPORT_HEADER).append('\n');
    for (LinkItem it : mine) sb.append(it.toLine()).append('\n');
    share(sb.toString(), "Compartir mis enlaces");
  }

  private void showImportDialog(String prefill) {
    EditText input = new EditText(this);
    input.setHint("Pega aquí: icono | nombre | dirección | sección");
    input.setText(prefill);
    input.setMinLines(5);
    input.setGravity(Gravity.TOP | Gravity.START);
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
    LinearLayout box = new LinearLayout(this);
    box.setPadding(ui.dp(22), ui.dp(8), ui.dp(22), 0);
    box.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    new AlertDialog.Builder(this, ui.dialogTheme())
        .setTitle("Importar enlaces")
        .setView(box)
        .setPositiveButton("Importar", (d, w) -> importLines(input.getText().toString()))
        .setNegativeButton("Cancelar", null)
        .show();
  }

  private void importLines(String text) {
    List<LinkItem> toAdd = new ArrayList<>();
    Set<String> seen = new LinkedHashSet<>();
    int skipped = 0;
    long base = System.currentTimeMillis();
    for (String raw : text.split("\\r?\\n")) {
      String line = raw.trim();
      if (line.isEmpty() || line.startsWith("#")) continue;
      String[] p = line.split("\\|");
      for (int i = 0; i < p.length; i++) p[i] = p[i].trim();
      String icon = "🔗", title, url, section = Defaults.MINE;
      if (p.length == 2) {
        title = p[0]; url = p[1];
      } else if (p.length == 3) {
        icon = p[0]; title = p[1]; url = p[2];
      } else if (p.length >= 4) {
        icon = p[0]; title = p[1]; url = p[2]; section = p[3];
      } else {
        String u = firstUrl(line);
        if (u == null) { skipped++; continue; }
        title = u; url = u;
      }
      String norm = normalizeUrl(url);
      if (norm == null || title.isEmpty() || urlExists(norm, "") || !seen.add(urlKey(norm))) {
        skipped++;
        continue;
      }
      if (icon.isEmpty() || icon.length() > 16) icon = "🔗";
      toAdd.add(new LinkItem("c_" + (base + toAdd.size()), section.isEmpty() ? Defaults.MINE : section,
          icon, title, norm, true));
    }
    store.addAll(toAdd);
    render();
    toast("Importados: " + toAdd.size() + (skipped > 0 ? " · omitidos: " + skipped : ""));
  }

  // ------------------------------------------------------------------ utilidades

  private void pinShortcut(LinkItem it) {
    ShortcutManager sm = getSystemService(ShortcutManager.class);
    if (sm == null || !sm.isRequestPinShortcutSupported()) {
      toast("Tu pantalla de inicio no permite crear accesos directos");
      return;
    }
    Intent si = new Intent(this, MainActivity.class)
        .setAction(ACTION_OPEN_LINK)
        .putExtra(EXTRA_URL, it.url)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    ShortcutInfo info = new ShortcutInfo.Builder(this, "link_" + it.id)
        .setShortLabel(it.title.length() > 20 ? it.title.substring(0, 20) : it.title)
        .setLongLabel(it.title)
        .setIcon(Icon.createWithAdaptiveBitmap(emojiIcon(it.icon)))
        .setIntent(si)
        .build();
    sm.requestPinShortcut(info, null);
  }

  /** Icono adaptativo de 108 dp con el emoji en la zona segura central. */
  private Bitmap emojiIcon(String emoji) {
    int size = ui.dp(108);
    Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(b);
    c.drawColor(0xFFF6F3EF);
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    p.setTextAlign(Paint.Align.CENTER);
    p.setTextSize(size * 0.40f);
    p.setTypeface(Typeface.DEFAULT);
    Paint.FontMetrics fm = p.getFontMetrics();
    c.drawText(emoji, size / 2f, size / 2f - (fm.ascent + fm.descent) / 2f, p);
    return b;
  }

  private void copy(LinkItem it) {
    ClipboardManager cm = getSystemService(ClipboardManager.class);
    if (cm == null) return;
    cm.setPrimaryClip(ClipData.newPlainText(it.title, it.url));
    if (Build.VERSION.SDK_INT < 33) toast("Enlace copiado");
  }

  private void share(String text, String chooserTitle) {
    Intent send = new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text);
    startActivity(Intent.createChooser(send, chooserTitle));
  }

  private void showAbout() {
    String version = "";
    try {
      version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
    } catch (PackageManager.NameNotFoundException ignored) {
    }
    new AlertDialog.Builder(this, ui.dialogTheme())
        .setTitle("MdC Profes " + version)
        .setMessage("Accesos rápidos de la Zona Profesorado de iesmigueldecervantes.com.\n\n"
            + "Tus enlaces, favoritos y la foto de tu horario se guardan solo en este móvil "
            + "y se incluyen en la copia de seguridad de Android de tu cuenta.\n\n"
            + "Incidencias: formulario de Incidencias RMI.")
        .setPositiveButton("Cerrar", null)
        .show();
  }

  private void hideKeyboard() {
    InputMethodManager imm = getSystemService(InputMethodManager.class);
    if (imm != null) imm.hideSoftInputFromWindow(search.getWindowToken(), 0);
    search.clearFocus();
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
  }
}
