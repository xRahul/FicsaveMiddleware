package in.rahulja.ficsavemiddleware;

import android.app.Application;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;


public class FicsaveMiddlewareApplication extends Application {
    private Tracker mGTracker;
    private FirebaseAnalytics mFTracker;

    /**
     * Gets the default {@link Tracker} for this {@link Application}.
     *
     * @return tracker
     */
    synchronized public Tracker getDefaultGATracker() {
        if (mGTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            mGTracker = analytics.newTracker(R.xml.global_tracker);
            mGTracker.enableAdvertisingIdCollection(true);
            mGTracker.enableExceptionReporting(true);
        }
        return mGTracker;
    }

    synchronized public FirebaseAnalytics getDefaultFATracker() {
        if (mFTracker == null) {
            mFTracker = FirebaseAnalytics.getInstance(this);
            mFTracker.setAnalyticsCollectionEnabled(true);
        }
        return mFTracker;
    }
}
