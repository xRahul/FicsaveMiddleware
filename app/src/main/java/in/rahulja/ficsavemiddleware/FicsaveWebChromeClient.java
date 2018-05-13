package in.rahulja.ficsavemiddleware;

import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

class FicsaveWebChromeClient extends WebChromeClient {
  private MainActivity mActivity;

  FicsaveWebChromeClient(MainActivity activity) {
    mActivity = activity;
  }

  @Override
  public boolean onJsAlert(WebView view, String url, String message,
      final android.webkit.JsResult result) {
    Log.d("ficsaveM/JSalert", message);
    Toast.makeText(mActivity, message, Toast.LENGTH_LONG).show();
    result.confirm();
    return true;
  }

  @Override
  public void onProgressChanged(WebView view, int newProgress) {
    super.onProgressChanged(view, newProgress);

    //Make the bar disappear after URL is loaded, and changes string to Loading...
    mActivity.setTitle("Loading...");
    Log.d("ficsaveM/URLprogress", String.valueOf(newProgress));
    mActivity.showHorizontalLoader();
    mActivity.progressHorizontalLoader(newProgress); //Make the bar disappear after URL is loaded

    // Return the app name after finish loading
    if (newProgress == 100) {
      mActivity.setTitle(R.string.app_name);
      mActivity.hideHorizontalLoader();
    }
  }
}
