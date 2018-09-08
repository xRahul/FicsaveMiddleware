package in.rahulja.ficsavemiddleware;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
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
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;
import java.util.regex.Matcher;

public class MainActivity extends AppCompatActivity {

  private static final String FILE_TYPES_PREFERENCE = "file_types_preference";
  private static final String SEND_EMAIL_SITE_PREFERENCE = "send_email_site_preference";
  private static final String EMAIL_ADDRESS_TO_SEND_TO = "email_address_to_send_to";
  private static final String MAIN_PAGE_CATEGORY = "MainPageCategory";
  private static final String URL_LABEL = "Url: ";
  private static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
  private ProgressBar pbHorizontal;
  private ProgressBar pbCircle;
  private WebView webView;
  private SharedPreferences prefs;
  private Tracker gaTracker;
  private FirebaseAnalytics firebaseTracker;
  private String ficUrl = "";

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

    FicsaveMiddlewareApplication application = (FicsaveMiddlewareApplication) getApplication();
    gaTracker = application.getDefaultGoogleAnalyticsTracker();
    firebaseTracker = application.getDefaultFirebaseTracker();

    // Attaching the layout to the toolbar object
    Toolbar toolbar = findViewById(R.id.tool_bar);
    // Setting toolbar as the ActionBar with setSupportActionBar() call
    setSupportActionBar(toolbar);

    prepare();
  }

  @Override
  protected void onResume() {
    super.onResume();
    gaTracker.setScreenName("Homepage");
    gaTracker.send(new HitBuilders.ScreenViewBuilder().build());
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
          new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE },
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
      webView.setWebViewClient(new FicsaveWebViewClient(this));
      webView.setWebChromeClient(new FicsaveWebChromeClient(this));
      webView.setDownloadListener(new FicsaveDownloadListener(this));

      // enable external javascript to run on page
      webView.getSettings().setJavaScriptEnabled(true);

      // load the ficsave homepage
      String ficsaveHomePage = "http://" + getString(R.string.ficsave_host);
      String urlToLoad = intentViewUrl.isEmpty() ? ficsaveHomePage : intentViewUrl;
      if (webView.getUrl() != null && webView.getUrl().contains(urlToLoad)) {
        runJSonPage(urlToLoad);

        gaTracker.send(new HitBuilders.EventBuilder()
            .setCategory(MAIN_PAGE_CATEGORY)
            .setAction("Running JS - Website already Loaded")
            .setLabel(URL_LABEL + urlToLoad)
            .setValue(1)
            .build());
        Bundle bundle = new Bundle();
        bundle.putString("Url", urlToLoad);
        firebaseTracker.logEvent("RunningJS_SiteAlreadyLoaded", bundle);
      } else {
        Log.d("ficsaveM/load", urlToLoad);
        webView.loadUrl(urlToLoad);

        gaTracker.send(new HitBuilders.EventBuilder()
            .setCategory(MAIN_PAGE_CATEGORY)
            .setAction("Loading Url")
            .setLabel(URL_LABEL + urlToLoad)
            .setValue(1)
            .build());
        Bundle bundle = new Bundle();
        bundle.putString("Url", urlToLoad);
        firebaseTracker.logEvent("LoadingUrl", bundle);
      }
    }
  }

  private String getIntentViewUrl() {
    String url = "";
    Intent intent = getIntent();
    String intentAction = intent.getAction();
    if (Intent.ACTION_VIEW.equals(intentAction) && intent.getData() != null) {
      url = intent.getData().toString();
      Log.d("ficsaveM/deepLink", url + " " + intent.toString());

      gaTracker.send(new HitBuilders.EventBuilder()
          .setCategory(MAIN_PAGE_CATEGORY)
          .setAction("Deep Link Accessed")
          .setLabel(URL_LABEL + url)
          .setValue(1)
          .build());
      Bundle bundle = new Bundle();
      bundle.putString("Url", url);
      firebaseTracker.logEvent("DeepLinkAccessed", bundle);
    }
    return url;
  }

  private void setIntentFicUrl() {
    ficUrl = "";
    Intent intent = getIntent();
    if (intent != null) {
      String intentType = intent.getType();
      String intentAction = intent.getAction();
      Log.d("ficsaveM/intentReceived", intentAction + " " + intent.toString());
      if (Intent.ACTION_SEND.equals(intentAction) && intentType != null && "text/plain".equals(
          intentType)) {
        Matcher m = Patterns.WEB_URL.matcher(intent.getStringExtra(Intent.EXTRA_TEXT));
        while (m.find()) {
          String url = m.group();
          ficUrl = url;
          Log.d("ficsaveM/setIntFicUrl", "URL extracted: " + url);

          gaTracker.send(new HitBuilders.EventBuilder()
              .setCategory(MAIN_PAGE_CATEGORY)
              .setAction("Fic Url Set")
              .setLabel(URL_LABEL + ficUrl)
              .setValue(1)
              .build());
          Bundle bundle = new Bundle();
          bundle.putString("Url", ficUrl);
          firebaseTracker.logEvent("FicUrlSet", bundle);
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
    String trackerResult = "";
    if (requestCode == PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE) {
      if (grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        trackerResult = "Granted";
        init();
      } else {
        trackerResult = "Denied";
        Log.d("ficsaveM/permission", "WRITE_EXTERNAL_STORAGE Permission Denied");
        Toast.makeText(
            getApplicationContext(),
            R.string.give_permission_toast_msg,
            Toast.LENGTH_LONG
        ).show();
      }
    }
    gaTracker.send(new HitBuilders.EventBuilder()
        .setCategory(MAIN_PAGE_CATEGORY)
        .setAction("Permission Requested")
        .setLabel("WRITE_EXTERNAL_STORAGE: " + trackerResult)
        .setValue(1)
        .build());
    Bundle bundle = new Bundle();
    bundle.putString("Permission", "WRITE_EXTERNAL_STORAGE");
    bundle.putString("Access", trackerResult);
    firebaseTracker.logEvent("PermissionRequested", bundle);
  }

  /**
   * Execute JavaScript on the Web Page.
   *
   * @param url URL to execute JS on.
   */
  public void runJSonPage(String url) {
    Log.d("ficsaveM/runJSCalled", url + " " + ficUrl);
    // Check if not loading the download URL and some fanfic url is there to download
    if (!url.contains(getString(R.string.ficsave_download_url))) {

      if (!ficUrl.isEmpty() && !Patterns.WEB_URL.matcher(ficUrl).matches()) {
        Toast.makeText(
            getApplicationContext(),
            R.string.invalid_fic_url + " " + ficUrl,
            Toast.LENGTH_LONG
        ).show();
      }

      // Get javascript to run on page
      final String jsString = getJsScript();

      // Execute Javascript on a new thread 2 second after page load
      Log.d("ficsaveM/JSrun", "Start");

      new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          webView.evaluateJavascript(jsString, new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
              Log.d("ficsaveM/JSrun", "Success, Value: " + value);
              trackJsRunSuccess(value);

              // empty the fanfic url so it won't get downloaded again somehow
              ficUrl = "";
            }
          });
        }
      }, 2000);
    }
  }

  private void trackJsRunSuccess(String value) {
    gaTracker.send(new HitBuilders.EventBuilder()
        .setCategory(MAIN_PAGE_CATEGORY)
        .setAction("JS run success")
        .setLabel(value)
        .setValue(1)
        .build());
    Bundle bundle = new Bundle();
    bundle.putString("Value", value);
    firebaseTracker.logEvent("JSrunSuccess", bundle);
  }

  private String getJsScript() {

    String jsScript = "";

    if (!ficUrl.isEmpty()) {
      jsScript += "document.getElementsByClassName(\"grey-text text-lighten-1\")[0].className = "
          + "\"grey-text text-lighten-1 active\";"
          + "document.getElementById('url').value = \""
          + ficUrl
          + "\";";
    }

    if (prefs.getBoolean(SEND_EMAIL_SITE_PREFERENCE, false)) {
      jsScript += "document.getElementsByClassName(\"grey-text text-lighten-1\")[2].className = "
          + "\"grey-text text-lighten-1 active\";"
          + "document.getElementById('email').value = \""
          + prefs.getString(EMAIL_ADDRESS_TO_SEND_TO, "")
          + "\";";
    }

    switch (prefs.getString(FILE_TYPES_PREFERENCE, "mobi")) {
      case "mobi":
        jsScript += "document.getElementsByClassName('select-dropdown')[0].value = \"MOBI\";"
            + "document.getElementsByName('format')[0].value = \"mobi\";";
        break;
      case "epub":
        jsScript += "document.getElementsByClassName('select-dropdown')[0].value = \"ePub\";"
            + "document.getElementsByName('format')[0].value = \"epub\";";
        break;
      case "txt":
        jsScript += "document.getElementsByClassName('select-dropdown')[0].value = \"Text\";"
            + "document.getElementsByName('format')[0].value = \"txt\";";
        break;
      default:
        break;
    }

    if (!ficUrl.isEmpty()) {
      jsScript += "document.getElementById(\"download-submit\").click();";
    }

    return jsScript;
  }
}
