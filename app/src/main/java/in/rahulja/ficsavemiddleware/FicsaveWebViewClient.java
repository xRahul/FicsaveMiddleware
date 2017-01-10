package in.rahulja.ficsavemiddleware;


import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import static android.widget.Toast.LENGTH_SHORT;


class FicsaveWebViewClient extends WebViewClient {

    private Context mContext;
    private Activity mActivity;
    private String ficUrl;

    FicsaveWebViewClient(Context context, Activity activity, String ficurl) {
        mContext = context;
        mActivity = activity;
        ficUrl = ficurl;
    }

    public void onPageFinished(final WebView view, String url) {
        mActivity.setProgressBarIndeterminateVisibility(false);
        if (!url.contains("http://ficsave.xyz/download/") && !ficUrl.isEmpty()) {
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
                                    mContext,
                                    "Script run successfully!",
                                    LENGTH_SHORT
                            ).show();
                        }
                    });
                }
            }, 1000);
        }
    }
}
