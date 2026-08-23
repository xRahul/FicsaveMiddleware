package in.rahulja.fichubmiddleware;

import android.net.Uri;

public final class FicHubUrls {
  public static final String HOST = "fichub.net";
  private static final String BASE = "https://" + HOST;

  private FicHubUrls() { }

  public static String home() { return BASE + "/"; }

  public static String autoExport(String ficUrl) {
    if (ficUrl == null || ficUrl.isEmpty()) {
      throw new IllegalArgumentException("ficUrl required");
    }
    return BASE + "/?b=1&q=" + Uri.encode(ficUrl, null);
  }
}
