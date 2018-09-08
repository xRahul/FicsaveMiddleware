package in.rahulja.ficsavemiddleware;

import android.util.Log;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

class FicsaveWebChromeClient extends WebChromeClient {
  private MainActivity mainActivity;

  FicsaveWebChromeClient(MainActivity activity) {
    mainActivity = activity;
  }

  @Override
  public boolean onJsAlert(WebView view, String url, String message,
      final android.webkit.JsResult result) {
    Log.d("ficsaveM/JSalert", message);
    Toast.makeText(mainActivity, message, Toast.LENGTH_LONG).show();
    result.confirm();
    return true;
  }

  @Override
  public void onProgressChanged(WebView view, int newProgress) {
    super.onProgressChanged(view, newProgress);

    //Make the bar disappear after URL is loaded, and changes string to Loading...
    mainActivity.setTitle("Loading...");
    Log.d("ficsaveM/URLprogress", String.valueOf(newProgress));
    mainActivity.showHorizontalLoader();
    mainActivity.progressHorizontalLoader(newProgress); //Make the bar disappear after URL is loaded

    // Return the app name after finish loading
    if (newProgress == 100) {
      mainActivity.setTitle(R.string.app_name);
      mainActivity.hideHorizontalLoader();
    }
  }
}
