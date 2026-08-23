package in.rahulja.fichubmiddleware;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class FicHubWebViewClient extends WebViewClient {

  private static final String TAG = "fichubM";
  private final MainActivity mainActivity;

  FicHubWebViewClient(MainActivity activity) {
    mainActivity = activity;
  }

  @Override
  public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
    Uri uri = request.getUrl();
    String host = uri.getHost();

    // fichub.net URLs stay in the webview, everything else goes to the browser
    if (host != null && (host.equals(FicHubUrls.HOST)
        || host.endsWith("." + FicHubUrls.HOST))) {
      return false;
    }

    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
    mainActivity.startActivity(intent);

    return true;
  }

  @Override
  public void onPageStarted(WebView view, String url, Bitmap favicon) {
    mainActivity.showTitleProgressSpinner();
  }

  @Override
  public void onPageFinished(WebView view, String url) {
    mainActivity.hideTitleProgressSpinner();

    mainActivity.maybeInjectAutoClickJs(view);
  }

  @Override
  public void onReceivedError(WebView view, WebResourceRequest request,
      WebResourceError error) {
    Log.d(TAG + "/ErrorLoading",
        "Url: " + request.getUrl() + " Reason: " + error.getDescription());
  }
}
