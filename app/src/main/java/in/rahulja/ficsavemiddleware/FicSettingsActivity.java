package in.rahulja.ficsavemiddleware;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;


public class FicSettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
}
