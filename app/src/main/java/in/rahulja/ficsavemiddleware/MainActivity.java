package in.rahulja.ficsavemiddleware;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.util.regex.Matcher;


public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    MenuItem fsActionProgressItem;
    // ProgressBar loadingTitleSpinner;
    public ProgressBar pbHorizontal;

    private String ficUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pbHorizontal = (ProgressBar) findViewById(R.id.progressBarHorizontal);

        // Attaching the layout to the toolbar object
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        // Setting toolbar as the ActionBar with setSupportActionBar() call
        setSupportActionBar(toolbar);

        setFicUrl();

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
        } else {
            init();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // Store instance of the menu item containing progress
        fsActionProgressItem = menu.findItem(R.id.fsActionProgress);
        // Extract the action-view from the menu item
        // loadingTitleSpinner =  (ProgressBar) MenuItemCompat.getActionView(fsActionProgressItem);
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
        if(fsActionProgressItem != null) {
            fsActionProgressItem.setVisible(true);
        }
    }

    public void hideTitleProgressSpinner() {
        // Hide progress item
        if(fsActionProgressItem != null) {
            fsActionProgressItem.setVisible(false);
        }
    }

    public void progressHorizontalLoader(int position) {
        // Show progress item
        Log.d("mainProgress", String.valueOf(position));
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
        // Hide progress item
        if(pbHorizontal != null) {
            pbHorizontal.setVisibility(View.VISIBLE);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        final WebView mWebview = (WebView) findViewById(R.id.main_webview);

        if (mWebview != null) {
            mWebview.setDownloadListener(new FicsaveDownloadListener(this));
            mWebview.setWebViewClient(new FicsaveWebViewClient(this, ficUrl));
            mWebview.setWebChromeClient(new FicsaveWebChromeClient(this));
            mWebview.getSettings().setJavaScriptEnabled(true);
            mWebview.loadUrl("http://" + getString(R.string.ficsave_host));
        }
    }

    private void setFicUrl() {
        ficUrl = "";
        Intent intent = getIntent();
        if (intent != null) {
            String intentAction = intent.getAction();
            String intentType = intent.getType();
            if (Intent.ACTION_SEND.equals(intentAction) && intentType != null) {
                if (intentType.equals("text/plain")) {
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
                        R.string.give_permission_toast_msg,
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
        }
    }
}
