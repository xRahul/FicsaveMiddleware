package in.rahulja.ficsavemiddleware;

import android.app.Application;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

public class FicsaveMiddlewareApplication extends Application {
  private Tracker gaTracker;
  private FirebaseAnalytics firebaseTracker;

  /**
   * Gets the default Google Analytics {@link Tracker} for this {@link Application}.
   *
   * @return gaTracker
   */
  public synchronized Tracker getDefaultGoogleAnalyticsTracker() {
    if (gaTracker == null) {
      GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
      gaTracker = analytics.newTracker(R.xml.global_tracker);
      gaTracker.enableAdvertisingIdCollection(true);
      gaTracker.enableExceptionReporting(true);
    }
    return gaTracker;
  }

  /**
   * Gets the default {@link FirebaseAnalytics} tracker for this {@link Application}.
   *
   * @return firebaseTracker
   */
  public synchronized FirebaseAnalytics getDefaultFirebaseTracker() {
    if (firebaseTracker == null) {
      firebaseTracker = FirebaseAnalytics.getInstance(this);
      firebaseTracker.setAnalyticsCollectionEnabled(false);
    }
    return firebaseTracker;
  }
}
