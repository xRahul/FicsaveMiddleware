package in.rahulja.ficsavemiddleware;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

class FicsaveWebViewClient extends WebViewClient {

  private static final String URL_LABEL = "Url: ";
  private MainActivity mActivity;
  private Tracker mGTracker;
  private FirebaseAnalytics mFTracker;

  FicsaveWebViewClient(MainActivity activity) {
    mActivity = activity;
    FicsaveMiddlewareApplication application =
        (FicsaveMiddlewareApplication) mActivity.getApplication();
    mGTracker = application.getDefaultGATracker();
    mFTracker = application.getDefaultFATracker();
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean shouldOverrideUrlLoading(WebView view, String url) {

    // if URL's host is ficsave.xyz, open in webview, else let android open it somewhere else
    if (Uri.parse(url).getHost().equals(mActivity.getString(R.string.ficsave_host))) {
      return false;
    }

    mGTracker.send(new HitBuilders.EventBuilder()
        .setCategory("WebViewClientCategory")
        .setAction("Other Url than ficsave Opened")
        .setLabel(URL_LABEL + url)
        .setValue(1)
        .build());
    Bundle bundle = new Bundle();
    bundle.putString("Url", url);
    mFTracker.logEvent("OtherUrlthanficsaveOpened", bundle);

    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    mActivity.startActivity(intent);

    return true;
  }

  @Override
  public void onPageStarted(WebView view, String url, Bitmap favicon) {
    mActivity.showTitleProgressSpinner();
  }

  @Override
  public void onPageFinished(final WebView view, String url) {
    mActivity.hideTitleProgressSpinner();

    mActivity.runJSonPage(url);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    Log.d("ficsaveM/ErrorLoading", URL_LABEL + failingUrl + " Reason" + description);
    mGTracker.send(new HitBuilders.EventBuilder()
        .setCategory("WebViewClientCategory")
        .setAction("Page Load Error")
        .setLabel(URL_LABEL + failingUrl + " Reason" + description)
        .setValue(1)
        .build());
    Bundle bundle = new Bundle();
    bundle.putString("Url", failingUrl);
    bundle.putString("Reason", description);
    mFTracker.logEvent("PageLoadError", bundle);
  }
}
