package in.rahulja.ficsavemiddleware;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class FicsaveWebViewClient extends WebViewClient {

  private static final String URL_LABEL = "Url: ";
  private MainActivity mainActivity;

  FicsaveWebViewClient(MainActivity activity) {
    mainActivity = activity;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean shouldOverrideUrlLoading(WebView view, String url) {

    // if URL's host is ficsave.xyz, open in webview, else let android open it somewhere else
    if (Uri.parse(url).getHost().equals(mainActivity.getString(R.string.ficsave_host))) {
      return false;
    }

    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    mainActivity.startActivity(intent);

    return true;
  }

  @Override
  public void onPageStarted(WebView view, String url, Bitmap favicon) {
    mainActivity.showTitleProgressSpinner();
  }

  @Override
  public void onPageFinished(final WebView view, String url) {
    mainActivity.hideTitleProgressSpinner();

    mainActivity.runJSonPage(url);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    Log.d("ficsaveM/ErrorLoading", URL_LABEL + failingUrl + " Reason" + description);
  }
}
