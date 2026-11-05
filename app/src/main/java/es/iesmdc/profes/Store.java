package es.iesmdc.profes;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/** Datos del profesor guardados solo en el móvil (incluidos en la copia de seguridad de Android). */
final class Store {
  private static final String K_CUSTOM = "custom";
  private static final String K_FAVS = "favs";
  private static final String K_HIDDEN = "hidden";

  private final SharedPreferences p;

  Store(Context c) {
    p = c.getSharedPreferences("mdc_profes", Context.MODE_PRIVATE);
  }

  // ---- Enlaces propios ----
  List<LinkItem> custom() {
    List<LinkItem> out = new ArrayList<>();
    try {
      JSONArray a = new JSONArray(p.getString(K_CUSTOM, "[]"));
      for (int i = 0; i < a.length(); i++) {
        LinkItem it = LinkItem.fromJson(a.getJSONObject(i));
        if (!it.url.isEmpty() && !it.title.isEmpty()) out.add(it);
      }
    } catch (JSONException ignored) {
      // Datos dañados: se devuelve lo que se haya podido leer.
    }
    return out;
  }

  private void saveCustom(List<LinkItem> list) {
    JSONArray a = new JSONArray();
    for (LinkItem it : list) {
      try {
        a.put(it.toJson());
      } catch (JSONException ignored) {
      }
    }
    p.edit().putString(K_CUSTOM, a.toString()).apply();
  }

  void upsert(LinkItem item) {
    List<LinkItem> list = custom();
    boolean replaced = false;
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).id.equals(item.id)) {
        list.set(i, item);
        replaced = true;
      }
    }
    if (!replaced) list.add(item);
    saveCustom(list);
  }

  void addAll(List<LinkItem> items) {
    List<LinkItem> list = custom();
    list.addAll(items);
    saveCustom(list);
  }

  void delete(String id) {
    List<LinkItem> list = custom();
    Iterator<LinkItem> it = list.iterator();
    while (it.hasNext()) {
      if (it.next().id.equals(id)) it.remove();
    }
    saveCustom(list);
    removeFav(id);
  }

  // ---- Favoritos (ordenados) ----
  List<String> favs() {
    List<String> out = new ArrayList<>();
    try {
      JSONArray a = new JSONArray(p.getString(K_FAVS, "[]"));
      for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
    } catch (JSONException ignored) {
    }
    return out;
  }

  private void saveFavs(List<String> ids) {
    p.edit().putString(K_FAVS, new JSONArray(ids).toString()).apply();
  }

  boolean isFav(String id) {
    return favs().contains(id);
  }

  void toggleFav(String id) {
    List<String> f = favs();
    if (!f.remove(id)) f.add(id);
    saveFavs(f);
  }

  void removeFav(String id) {
    List<String> f = favs();
    if (f.remove(id)) saveFavs(f);
  }

  // ---- Accesos predeterminados ocultos ----
  Set<String> hidden() {
    return new HashSet<>(p.getStringSet(K_HIDDEN, Collections.<String>emptySet()));
  }

  void hide(String id) {
    Set<String> s = hidden();
    s.add(id);
    p.edit().putStringSet(K_HIDDEN, s).apply();
    removeFav(id);
  }

  void restoreHidden() {
    p.edit().remove(K_HIDDEN).apply();
  }
}
