package in.rahulja.ficsavemiddleware;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.Window;
import android.webkit.WebView;
import android.widget.Toast;

import java.util.regex.Matcher;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    Activity mActivity;

    private String ficUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
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
            mWebview.setDownloadListener(new FicsaveDownloadListener(this));
            mWebview.setWebViewClient(new FicsaveWebViewClient(this, this, ficUrl));
            mWebview.getSettings().setJavaScriptEnabled(true);
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
