package in.rahulja.ficsavemiddleware;


import android.os.Bundle;
import android.preference.PreferenceActivity;


@SuppressWarnings("deprecation")
public class FicSettingsActivity extends PreferenceActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

}
