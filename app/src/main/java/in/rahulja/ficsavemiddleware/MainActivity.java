package in.rahulja.ficsavemiddleware;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.util.regex.Matcher;


public class MainActivity extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    private ProgressBar pbHorizontal;
    private ProgressBar pbCircle;
    private WebView mWebview;

    private String ficUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get progress bars
        pbHorizontal = (ProgressBar) findViewById(R.id.progressBarHorizontal);
        pbCircle = (ProgressBar) findViewById(R.id.progressBarCircle);
        mWebview = (WebView) findViewById(R.id.main_webview);

        // Attaching the layout to the toolbar object
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        // Setting toolbar as the ActionBar with setSupportActionBar() call
        setSupportActionBar(toolbar);

        prepare();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        prepare();
    }

    private void prepare() {
        // check permission of external storage
        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // ask for permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            // initialize the app if permission exists
            init();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Return true to show menu
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        return super.onOptionsItemSelected(item);
    }

    public void showTitleProgressSpinner() {
        // Show progress item
        if(pbCircle != null) {
            pbCircle.setVisibility(View.VISIBLE);
        }
    }

    public void hideTitleProgressSpinner() {
        // Hide progress item
        if(pbCircle != null) {
            pbCircle.setVisibility(View.INVISIBLE);
        }
    }

    public void progressHorizontalLoader(int position) {
        // progress the bar
        if(pbHorizontal != null) {
            pbHorizontal.setProgress(position);
        }
    }

    public void hideHorizontalLoader() {
        // Hide progress item
        if(pbHorizontal != null) {
            pbHorizontal.setVisibility(View.INVISIBLE);
        }
    }

    public void showHorizontalLoader() {
        // Show progress item
        if(pbHorizontal != null) {
            pbHorizontal.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {

        // set fanfic url as main
        setIntentFicUrl();

        if (mWebview != null) {
            // set listeners and clients to webview
            mWebview.setWebViewClient(new FicsaveWebViewClient(this));
            mWebview.setWebChromeClient(new FicsaveWebChromeClient(this));
            mWebview.setDownloadListener(new FicsaveDownloadListener(this));

            // enable external javascript to run on page
            mWebview.getSettings().setJavaScriptEnabled(true);

            // load the ficsave homepage
            String ficsaveHomePage = "http://" + getString(R.string.ficsave_host);
            if (mWebview.getUrl() != null && mWebview.getUrl().contains(ficsaveHomePage)) {
                runJSonPage(mWebview.getUrl());
            } else {
                Log.d("ficsaveM/load", ficsaveHomePage);
                mWebview.loadUrl(ficsaveHomePage);
            }
        }
    }

    private void setIntentFicUrl() {
        ficUrl = "";
        Intent intent = getIntent();
        if (intent != null) {
            String intentType = intent.getType();
            String intentAction = intent.getAction();
            Log.d("ficsaveM/intentReceived", intentAction + " " + intent.toString());
            if (Intent.ACTION_SEND.equals(intentAction) && intentType != null) {
                if (intentType.equals("text/plain")) {
                    Matcher m = Patterns.WEB_URL.matcher(intent.getStringExtra(Intent.EXTRA_TEXT));
                    while (m.find()) {
                        String url = m.group();
                        ficUrl = url;
                        Log.d("ficsaveM/setIntFicUrl", "URL extracted: " + url);
                    }
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String permissions[],
            @NonNull int[] grantResults
    ) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    Log.d("ficsaveM/permission", "WRITE_EXTERNAL_STORAGE Permission Denied");
                    Toast.makeText(
                        getApplicationContext(), 
                        R.string.give_permission_toast_msg,
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
        }
    }

    public void runJSonPage(String url) {
        Log.d("ficsaveM/runJSCalled", url + " " + ficUrl);
        // Check if not loading the download URL and some fanfic url is there to download
        if (!url.contains(getString(R.string.ficsave_download_url)) && !ficUrl.isEmpty()) {

            // Get javascript to run on page
            final String jsString = getJsScript();

            // Execute Javascript on a new thread 2 second after page load
            Log.d("ficsaveM/JSrun", "Start");
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mWebview.evaluateJavascript(jsString, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            Log.d("ficsaveM/JSrun", "Success, Value: " + value);
                            Toast.makeText(
                                    getApplicationContext(),
                                    R.string.script_run_success,
                                    Toast.LENGTH_SHORT
                            ).show();

                            // empty the fanfic url so it won't get downloaded again somehow
                            ficUrl = "";
                        }
                    });
                }
            }, 2000);
        }
    }

    private String getJsScript() {
        return
            "document.getElementById('url').value = \"" + ficUrl + "\"; " +
            "document.getElementsByClassName('select-dropdown')[0].value = \"MOBI\";" +
            "document.getElementsByClassName('dropdown-content select-dropdown')[0].getElementsByTagName('li')[0].className = \"\";" +
            "document.getElementsByClassName('dropdown-content select-dropdown')[0].getElementsByTagName('li')[1].className = \"active\";" +
            "document.getElementsByName('format')[0].value = \"mobi\";" +
            "document.getElementById(\"download-submit\").click();";
    }
}
