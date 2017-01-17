package in.rahulja.ficsavemiddleware;


import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Patterns;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;


public class SettingsFragment extends PreferenceFragment {

    public static final String DOWNLOAD_FILE_PREFERENCE = "download_file_preference";
    public static final String SEND_EMAIL_SITE_PREFERENCE = "send_email_site_preference";
    public static final String OPEN_FILE_PREFERENCE = "open_file_preference";
    public static final String SEND_EMAIL_DEVICE_PREFERENCE = "send_email_device_preference";
    public static final String EMAIL_ADDRESS_TO_SEND_TO = "email_address_to_send_to";
    private OnSharedPreferenceChangeListener listener;
    private Preference emailAddressPref;
    private SharedPreferences prefs;
    private Tracker mGTracker;
    private FirebaseAnalytics mFTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        addPreferencesFromResource(R.xml.preferences);
        updatePreferenceView();

        initializePreferenceListener();

        FicsaveMiddlewareApplication application = (FicsaveMiddlewareApplication) getActivity().getApplication();
        mGTracker = application.getDefaultGATracker();
        mFTracker = application.getDefaultFATracker();
    }

    private void updatePreferenceView() {

        emailAddressPref = findPreference(EMAIL_ADDRESS_TO_SEND_TO);
        String emailAddress = prefs.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
        CharSequence tempSummary;
        if (emailAddress.isEmpty()) {
            tempSummary = emailAddressPref.getSummary();
        } else {
            tempSummary = emailAddress;
        }

        setPreferenceScreen(null);
        addPreferencesFromResource(R.xml.preferences);

        Preference downloadFolderPref = findPreference(DOWNLOAD_FILE_PREFERENCE);
        String defaultDownloadFolder = Environment.getExternalStorageDirectory()
                + "/" + Environment.DIRECTORY_DOCUMENTS;
        downloadFolderPref.setSummary(defaultDownloadFolder);
        emailAddressPref = findPreference(EMAIL_ADDRESS_TO_SEND_TO);
        emailAddressPref.setSummary(tempSummary);
    }

    private void initializePreferenceListener() {
        Log.d("ficsaveM/initPref", "Listener");
        listener = new OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences sPrefs, String key) {
                switch (key) {
                    case DOWNLOAD_FILE_PREFERENCE:
                        Boolean downloadFileToDevice = sPrefs.getBoolean(DOWNLOAD_FILE_PREFERENCE, true);
                        if (downloadFileToDevice) {
                            sPrefs.edit().putBoolean(SEND_EMAIL_SITE_PREFERENCE, false).apply();
                        } else {
                            sPrefs.edit().putBoolean(OPEN_FILE_PREFERENCE, false).apply();
                            sPrefs.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
                            sPrefs.edit().putBoolean(SEND_EMAIL_SITE_PREFERENCE, true).apply();
                        }
                        break;
                    case OPEN_FILE_PREFERENCE:
                        Boolean openFileOnDevice = sPrefs.getBoolean(OPEN_FILE_PREFERENCE, true);
                        if (openFileOnDevice) {
                            sPrefs.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
                        }
                        break;
                    case SEND_EMAIL_DEVICE_PREFERENCE:
                        Boolean sendEmailFromDevice = sPrefs.getBoolean(SEND_EMAIL_DEVICE_PREFERENCE, true);
                        if (sendEmailFromDevice) {
                            String emailAddress = sPrefs.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
                            if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setTitle("Invalid Email");
                                builder.setMessage("Please enter a valid email address first!");
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.show();
                                sPrefs.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
                            } else {
                                sPrefs.edit().putBoolean(OPEN_FILE_PREFERENCE, false).apply();
                            }
                        }
                        break;
                    case SEND_EMAIL_SITE_PREFERENCE:
                        Boolean sendEmailFromSite = sPrefs.getBoolean(SEND_EMAIL_SITE_PREFERENCE, true);
                        if (sendEmailFromSite) {
                            String emailAddress = sPrefs.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
                            if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setTitle("Invalid Email");
                                builder.setMessage("Please enter a valid email address first!");
                                builder.setPositiveButton(android.R.string.ok, null);
                                builder.show();
                                sPrefs.edit().putBoolean(SEND_EMAIL_SITE_PREFERENCE, false).apply();
                            } else {
                                sPrefs.edit().putBoolean(DOWNLOAD_FILE_PREFERENCE, false).apply();
                                sPrefs.edit().putBoolean(OPEN_FILE_PREFERENCE, false).apply();
                                sPrefs.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
                            }
                        } else {
                            sPrefs.edit().putBoolean(DOWNLOAD_FILE_PREFERENCE, true).apply();
                        }
                        break;
                    case EMAIL_ADDRESS_TO_SEND_TO:
                        String emailAddress = sPrefs.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
                        if (!emailAddress.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {

                            if (Patterns.EMAIL_ADDRESS.matcher(emailAddress.trim()).matches()) {
                                sPrefs.edit().putString(EMAIL_ADDRESS_TO_SEND_TO, emailAddress.trim()).apply();
                                break;
                            }

                            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("Invalid Email");
                            builder.setMessage("Please enter a valid email address");
                            builder.setPositiveButton(android.R.string.ok, null);
                            builder.show();
                            sPrefs.edit().putString(EMAIL_ADDRESS_TO_SEND_TO, "").apply();
                        } else if (emailAddress.isEmpty()) {
                            sPrefs.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
                            sPrefs.edit().putBoolean(SEND_EMAIL_SITE_PREFERENCE, false).apply();
                            emailAddressPref.setSummary(R.string.email_address_summary);
                        } else {
                            emailAddressPref.setSummary(emailAddress);
                        }
                        break;
                }
                updatePreferenceView();

                mGTracker.send(new HitBuilders.EventBuilder()
                        .setCategory("SettingsCategory")
                        .setAction("SharedPreferenceChanged: " + key)
                        .setLabel(sPrefs.getAll().toString())
                        .setValue(1)
                        .build());
                Bundle bundle = new Bundle();
                bundle.putString("Key", key);
                bundle.putString("Preferences", sPrefs.getAll().toString());
                mFTracker.logEvent("SharedPreferenceChanged", bundle);
            }
        };
    }


    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(listener);
    }
}

