package in.rahulja.ficsavemiddleware;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

public class FicSettingsActivity extends PreferenceActivity {

  private Tracker mGTracker;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    getFragmentManager().beginTransaction()
        .replace(android.R.id.content, new SettingsFragment())
        .commit();
    FicsaveMiddlewareApplication application = (FicsaveMiddlewareApplication) getApplication();
    mGTracker = application.getDefaultGATracker();
  }

  @Override
  public void onResume() {
    super.onResume();
    mGTracker.setScreenName("Settings");
    mGTracker.send(new HitBuilders.ScreenViewBuilder().build());
  }
}
