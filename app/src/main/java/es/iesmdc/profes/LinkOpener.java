package es.iesmdc.profes;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

/**
 * Abre enlaces de la forma recomendada por Android:
 * 1) si hay una app nativa que lo gestione (Drive, Gmail, Moodle...), se abre en ella;
 * 2) si no, en una Custom Tab precalentada (más rápida y conserva las sesiones del navegador).
 */
final class LinkOpener {
  private final Activity activity;
  private final int toolbarColor;
  private CustomTabsServiceConnection connection;
  private CustomTabsSession session;

  LinkOpener(Activity activity, int toolbarColor) {
    this.activity = activity;
    this.toolbarColor = toolbarColor;
  }

  void bind() {
    if (connection != null) return;
    String pkg = CustomTabsClient.getPackageName(activity, null);
    if (pkg == null) return;
    connection = new CustomTabsServiceConnection() {
      @Override
      public void onCustomTabsServiceConnected(@NonNull ComponentName name, @NonNull CustomTabsClient client) {
        client.warmup(0);
        session = client.newSession(null);
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
        session = null;
      }
    };
    try {
      if (!CustomTabsClient.bindCustomTabsService(activity, pkg, connection)) connection = null;
    } catch (SecurityException e) {
      connection = null;
    }
  }

  void unbind() {
    if (connection == null) return;
    try {
      activity.unbindService(connection);
    } catch (IllegalArgumentException ignored) {
    }
    connection = null;
    session = null;
  }

  void open(String url) {
    Uri uri = Uri.parse(url);
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(java.util.Locale.ROOT);

    if (!scheme.equals("http") && !scheme.equals("https")) {
      plainView(uri); // mailto:, tel:...
      return;
    }

    if (Build.VERSION.SDK_INT >= 30) {
      try {
        Intent nativeApp = new Intent(Intent.ACTION_VIEW, uri)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_REQUIRE_NON_BROWSER
                | Intent.FLAG_ACTIVITY_REQUIRE_DEFAULT);
        activity.startActivity(nativeApp);
        return;
      } catch (ActivityNotFoundException ignored) {
        // Ninguna app nativa: seguimos con Custom Tabs.
      }
    }

    try {
      CustomTabsIntent.Builder b = session != null
          ? new CustomTabsIntent.Builder(session) : new CustomTabsIntent.Builder();
      b.setDefaultColorSchemeParams(new CustomTabColorSchemeParams.Builder()
          .setToolbarColor(toolbarColor).build());
      b.setShowTitle(true);
      b.setShareState(CustomTabsIntent.SHARE_STATE_ON);
      b.build().launchUrl(activity, uri);
    } catch (ActivityNotFoundException e) {
      plainView(uri);
    }
  }

  private void plainView(Uri uri) {
    try {
      activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
    } catch (ActivityNotFoundException e) {
      Toast.makeText(activity, "No hay ninguna app para abrir este enlace", Toast.LENGTH_SHORT).show();
    }
  }
}
