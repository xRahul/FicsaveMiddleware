package in.rahulja.ficsavemiddleware;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class FicSettingsActivity extends PreferenceActivity {

  private Tracker gaTracker;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new SettingsFragment())
        .commit();
    FicsaveMiddlewareApplication application = (FicsaveMiddlewareApplication) getApplication();
    gaTracker = application.getDefaultGoogleAnalyticsTracker();
  }

  @Override
  public void onResume() {
    super.onResume();
    gaTracker.setScreenName("Settings");
    gaTracker.send(new HitBuilders.ScreenViewBuilder().build());
  }
}
