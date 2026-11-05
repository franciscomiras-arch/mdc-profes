package es.iesmdc.profes;

import org.json.JSONException;
import org.json.JSONObject;

/** Un acceso: predeterminado (de la web del centro) o creado por el profesor. */
final class LinkItem {
  final String id;
  final String section;
  final String icon;
  final String title;
  final String url;
  final boolean custom;

  LinkItem(String id, String section, String icon, String title, String url, boolean custom) {
    this.id = id;
    this.section = section;
    this.icon = icon;
    this.title = title;
    this.url = url;
    this.custom = custom;
  }

  JSONObject toJson() throws JSONException {
    JSONObject o = new JSONObject();
    o.put("id", id);
    o.put("section", section);
    o.put("icon", icon);
    o.put("title", title);
    o.put("url", url);
    return o;
  }

  static LinkItem fromJson(JSONObject o) {
    return new LinkItem(
        o.optString("id", "c_" + System.nanoTime()),
        o.optString("section", Defaults.MINE),
        o.optString("icon", "🔗"),
        o.optString("title", ""),
        o.optString("url", ""),
        true);
  }

  /** Línea de exportación legible: icono | título | url | sección */
  String toLine() {
    return icon + " | " + title.replace("|", "/") + " | " + url + " | " + section.replace("|", "/");
  }
}
