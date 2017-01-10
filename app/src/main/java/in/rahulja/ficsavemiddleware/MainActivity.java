package in.rahulja.ficsavemiddleware;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.util.regex.Matcher;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    Activity mActivity;

    private String ficUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mActivity = this;
        setFicUrl(getIntent());

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(mActivity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            init();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        final WebView mWebview = (WebView) findViewById(R.id.main_webview);

        if (mWebview != null) {
            mWebview.setDownloadListener(new MyDownloadListener(this));

            mWebview.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
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
                                mWebview.evaluateJavascript(jsString, new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        Toast.makeText(
                                                getApplicationContext(),
                                                "Script run successfully!",
                                                LENGTH_SHORT
                                        ).show();
                                    }
                                });
                            }
                        }, 1000);
                    }
                }
            });
            WebSettings webSettings = mWebview.getSettings();
            webSettings.setJavaScriptEnabled(true);
            mWebview.loadUrl("http://ficsave.xyz");
        }
    }

    private void setFicUrl(Intent intent) {
        ficUrl = "";
        if (intent != null) {
            String intentAction = intent.getAction();
            String intentType = intent.getType();
            if (Intent.ACTION_SEND.equals(intentAction) && intentType != null) {
                if ("text/plain".equals(intentType)) {
                    Matcher m = Patterns.WEB_URL.matcher(intent.getStringExtra(Intent.EXTRA_TEXT));
                    while (m.find()) {
                        String url = m.group();
                        Log.d("URL", "URL extracted: " + url);
                        ficUrl = url;
                    }
                }
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    Toast.makeText(
                        getApplicationContext(), 
                        "Please give storage permission first!",
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
        }
    }
}
