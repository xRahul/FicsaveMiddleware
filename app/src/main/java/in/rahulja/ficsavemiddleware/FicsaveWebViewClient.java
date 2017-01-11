package in.rahulja.ficsavemiddleware;


import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;


class FicsaveWebViewClient extends WebViewClient {

    private MainActivity mActivity;
    private String ficUrl;

    FicsaveWebViewClient(MainActivity activity, String ficurl) {
        mActivity = activity;
        ficUrl = ficurl;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {

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

        if (!url.contains(mActivity.getString(R.string.ficsave_download_url)) && !ficUrl.isEmpty()) {
            final String jsString =
                    "document.getElementById('url').value = \"" + ficUrl + "\"; " +
                            "document.getElementsByClassName('select-dropdown')[0].value = \"MOBI\";" +
                            "document.getElementsByClassName('dropdown-content select-dropdown')[0].getElementsByTagName('li')[0].className = \"\";" +
                            "document.getElementsByClassName('dropdown-content select-dropdown')[0].getElementsByTagName('li')[1].className = \"active\";" +
                            "document.getElementsByName('format')[0].value = \"mobi\";" +
                            "document.getElementById(\"download-submit\").click();";

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    view.evaluateJavascript(jsString, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            Toast.makeText(
                                    mActivity,
                                    R.string.script_run_success,
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
                }
            }, 1000);
        }
    }
}
