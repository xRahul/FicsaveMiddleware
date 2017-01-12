package in.rahulja.ficsavemiddleware;


import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;


class FicsaveWebViewClient extends WebViewClient {

    private MainActivity mActivity;

    FicsaveWebViewClient(MainActivity activity) {
        mActivity = activity;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {

        // if URL's host is ficsave.xyz, open in webview, else let android open it somewhere else
        if (Uri.parse(url).getHost().equals(mActivity.getString(R.string.ficsave_host))) {
            return false;
        }
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
        Log.d("ficsaveM/ErrorLoading", "Url: " + failingUrl + " Reason" + description);
    }
}
