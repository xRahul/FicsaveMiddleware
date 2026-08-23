package in.rahulja.fichubmiddleware;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

  private static final String TAG = "fichubM";
  private static final String FILE_TYPES_PREFERENCE = "file_types_preference";
  private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
  private ProgressBar pbHorizontal;
  private ProgressBar pbCircle;
  private WebView webView;
  private SharedPreferences prefs;
  private String ficUrl = "";
  private boolean pendingAutoFormat = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    if (!prefs.getBoolean("terms_shown", false)) {
      Intent intent = new Intent(this, TermsActivity.class);
      startActivity(intent);
    }

    // get progress bars
    pbHorizontal = findViewById(R.id.progressBarHorizontal);
    pbCircle = findViewById(R.id.progressBarCircle);
    webView = findViewById(R.id.main_webview);

    // Attaching the layout to the toolbar object
    Toolbar toolbar = findViewById(R.id.tool_bar);
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
    // WRITE_EXTERNAL_STORAGE only exists up to Android 9 (API 28)
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
      // check permission of external storage
      if (ContextCompat.checkSelfPermission(getApplicationContext(),
          Manifest.permission.WRITE_EXTERNAL_STORAGE)
          != PackageManager.PERMISSION_GRANTED) {
        // ask for permission
        ActivityCompat.requestPermissions(this,
            new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
            PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
      } else {
        // initialize the app if permission exists
        init();
      }
    } else {
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
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
      startActivity(new Intent(this, FicSettingsActivity.class));
      return true;
    }

    if (id == R.id.download_history_settings) {
      startActivity(new Intent(this, DownloadHistoryActivity.class));
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  /**
   * Show Title Progress Spinner.
   */
  public void showTitleProgressSpinner() {
    // Show progress item
    if (pbCircle != null) {
      pbCircle.setVisibility(View.VISIBLE);
    }
  }

  /**
   * Hide Title Progress Spinner.
   */
  public void hideTitleProgressSpinner() {
    // Hide progress item
    if (pbCircle != null) {
      pbCircle.setVisibility(View.INVISIBLE);
    }
  }

  /**
   * Set Progress bar position.
   *
   * @param position progress
   */
  public void progressHorizontalLoader(int position) {
    // progress the bar
    if (pbHorizontal != null) {
      pbHorizontal.setProgress(position);
    }
  }

  /**
   * Hide Progress Bar.
   */
  public void hideHorizontalLoader() {
    // Hide progress item
    if (pbHorizontal != null) {
      pbHorizontal.setVisibility(View.INVISIBLE);
    }
  }

  /**
   * Show Progress Bar.
   */
  public void showHorizontalLoader() {
    // Show progress item
    if (pbHorizontal != null) {
      pbHorizontal.setVisibility(View.VISIBLE);
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void init() {

    // set fanfic url as main
    setIntentFicUrl();

    String intentViewUrl = getIntentViewUrl();

    if (webView != null) {
      // set listeners and clients to webView
      webView.setWebViewClient(new FicHubWebViewClient(this));
      webView.setWebChromeClient(new FicHubWebChromeClient(this));
      webView.setDownloadListener(new FicHubDownloadListener(this));

      // enable external javascript to run on page
      webView.getSettings().setJavaScriptEnabled(true);

      String urlToLoad;
      if (!intentViewUrl.isEmpty()) {
        urlToLoad = intentViewUrl;
      } else if (!ficUrl.isEmpty()) {
        if (!Patterns.WEB_URL.matcher(ficUrl).matches()) {
          Toast.makeText(
              getApplicationContext(),
              getString(R.string.invalid_fic_url) + " " + ficUrl,
              Toast.LENGTH_LONG
          ).show();
        }
        urlToLoad = FicHubUrls.autoExport(ficUrl);
        pendingAutoFormat = true;
      } else {
        urlToLoad = FicHubUrls.home();
      }

      String currentUrl = webView.getUrl();
      if (urlToLoad.equals(currentUrl)) {
        Log.d(TAG + "/load", "already on " + urlToLoad + ", skipping reload");
        maybeInjectAutoClickJs(webView);
      } else {
        Log.d(TAG + "/load", urlToLoad);
        webView.loadUrl(urlToLoad);
      }
    }
  }

  private String getIntentViewUrl() {
    String url = "";
    Intent intent = getIntent();
    String intentAction = intent.getAction();
    if (Intent.ACTION_VIEW.equals(intentAction) && intent.getData() != null) {
      url = intent.getData().toString();
      Log.d(TAG + "/deepLink", url + " " + intent.toString());
    }
    return url;
  }

  private void setIntentFicUrl() {
    ficUrl = "";
    Intent intent = getIntent();
    if (intent != null) {
      String intentType = intent.getType();
      String intentAction = intent.getAction();
      Log.d(TAG + "/intentReceived", intentAction + " " + intent.toString());
      if (Intent.ACTION_SEND.equals(intentAction) && "text/plain".equals(
          intentType)) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        Matcher m = Patterns.WEB_URL.matcher(sharedText != null ? sharedText : "");
        while (m.find()) {
          ficUrl = m.group();
          Log.d(TAG + "/setIntFicUrl", "URL extracted: " + ficUrl);
          break;
        }
      }
    }
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode,
      @NonNull String[] permissions,
      @NonNull int[] grantResults
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
      if (grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        init();
      } else {
        Log.d(TAG + "/permission", "WRITE_EXTERNAL_STORAGE Permission Denied");
        Toast.makeText(
            getApplicationContext(),
            R.string.give_permission_toast_msg,
            Toast.LENGTH_LONG
        ).show();
      }
    }
  }

  /**
   * Inject the format auto-click script once per pending shared fic URL,
   * after fichub.net finished rendering its export results.
   *
   * @param view the WebView that finished loading
   */
  void maybeInjectAutoClickJs(WebView view) {
    if (!pendingAutoFormat) {
      return;
    }

    pendingAutoFormat = false;

    String currentUrl = view.getUrl();
    if (currentUrl == null || !currentUrl.contains("q=")) {
      return;
    }

    String format = prefs.getString(FILE_TYPES_PREFERENCE, "epub");
    Log.d(TAG + "/JSrun", "auto-click inject, format: " + format);
    view.evaluateJavascript(buildAutoClickJs(format), null);
  }

  private String buildAutoClickJs(String format) {
    return "(function(){var want='" + format + "',tries=0;"
        + "function go(){var as=document.querySelectorAll('#i a');"
        + "for(var i=0;i<as.length;i++){var t=as[i].textContent||'';"
        + "if((want=='epub'&&/EPUB/i.test(t))||(want=='html'&&/zipped HTML/i.test(t))"
        + "||(want=='mobi'&&/MOBI/i.test(t))||(want=='pdf'&&/PDF/i.test(t))){as[i].click();return;}}"
        + "if(++tries<60)setTimeout(go,1000);}go();})()";
  }
}
