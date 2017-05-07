package in.rahulja.ficsavemiddleware;


import android.app.DialogFragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;


public class SettingsFragment extends PreferenceFragment
        implements DeveloperDialogFragment.DeveloperDialogListener {

    public static final String DOWNLOAD_FILE_PREFERENCE = "download_file_preference";
    public static final String SEND_EMAIL_SITE_PREFERENCE = "send_email_site_preference";
    public static final String OPEN_FILE_PREFERENCE = "open_file_preference";
    public static final String SEND_EMAIL_DEVICE_PREFERENCE = "send_email_device_preference";
    public static final String EMAIL_ADDRESS_TO_SEND_TO = "email_address_to_send_to";
    public static final String PREF_VERSION = "version";
    public static final String PREF_DEVELOPER = "developer";
    public static final String PREF_PRIVACY_POLICY = "privacy_policy";
    public static final String SETTINGS_CATEGORY = "SettingsCategory";
    public static final String INVALID_EMAIL = "Invalid Email";
    private OnSharedPreferenceChangeListener listener;
    private Preference emailAddressPref;
    private SharedPreferences prefs;
    private Tracker mGTracker;
    private FirebaseAnalytics mFTracker;
    private String versionSummary;
    private Preference versionPref;
    private String latestVersionUrl;
    private Preference developerPref;
    private Preference privacyPolicyPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FicsaveMiddlewareApplication application = (FicsaveMiddlewareApplication) getActivity().getApplication();
        mGTracker = application.getDefaultGATracker();
        mFTracker = application.getDefaultFATracker();

        prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

        addPreferencesFromResource(R.xml.preferences);
        initPreferenceView();
        checkLatestAppVersion();
        updatePreferenceView();
    }

    private void initPreferenceView() {
        latestVersionUrl = getString(R.string.latest_release_url);
        emailAddressPref = findPreference(EMAIL_ADDRESS_TO_SEND_TO);
        versionPref = findPreference(PREF_VERSION);
        developerPref = findPreference(PREF_DEVELOPER);
        privacyPolicyPref = findPreference(PREF_PRIVACY_POLICY);
    }

    private void updatePreferenceView() {

        String emailAddress = prefs.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
        CharSequence tempSummary;
        if (emailAddress.isEmpty()) {
            tempSummary = emailAddressPref.getSummary();
        } else {
            tempSummary = emailAddress;
        }

        getPreferenceScreen().removeAll();
        addPreferencesFromResource(R.xml.preferences);

        versionPref = findPreference(PREF_VERSION);
        versionPref.setSummary(versionSummary);

        Preference downloadFolderPref = findPreference(DOWNLOAD_FILE_PREFERENCE);
        String defaultDownloadFolder = Environment.getExternalStorageDirectory()
                + "/" + Environment.DIRECTORY_DOCUMENTS;
        downloadFolderPref.setSummary(defaultDownloadFolder);
        emailAddressPref = findPreference(EMAIL_ADDRESS_TO_SEND_TO);
        emailAddressPref.setSummary(tempSummary);
        developerPref = findPreference(PREF_DEVELOPER);
        privacyPolicyPref = findPreference(PREF_PRIVACY_POLICY);

        initializePreferenceListener();
    }

    private void checkLatestAppVersion() {
        versionSummary = BuildConfig.VERSION_NAME;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(getString(R.string.latest_release_url));
                    HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
                    ucon.setInstanceFollowRedirects(false);
                    URL secondURL = new URL(ucon.getHeaderField("Location"));
                    String secondUrl = String.valueOf(secondURL);
                    String latestVersion = Uri.parse(secondUrl).getLastPathSegment();
                    Log.d("ficsaveM/updateUrl", secondUrl);
                    trackAppUrlFetch(secondUrl);
                    if (getActivity() != null) {
                        String checkUrl = getString(R.string.current_release_url_prefix) + BuildConfig.VERSION_NAME;
                        if (secondUrl.equals(checkUrl)) {
                            versionSummary = BuildConfig.VERSION_NAME + " " +
                                    getString(R.string.version_summary_latest);
                        } else {
                            versionSummary = BuildConfig.VERSION_NAME + " " +
                                    "(" + getString(R.string.version_summary_changed_latest) + latestVersion + ")";
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                versionPref.setSummary(versionSummary);
                                Log.d("ficsaveM/versionChecked", versionSummary);
                                mGTracker.send(new HitBuilders.EventBuilder()
                                        .setCategory(SETTINGS_CATEGORY)
                                        .setAction("versionSummaryChanged")
                                        .setLabel(versionSummary)
                                        .setValue(1)
                                        .build());
                                Bundle bundle = new Bundle();
                                bundle.putString("Summary", versionSummary);
                                mFTracker.logEvent("versionSummaryChanged", bundle);
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }

    private void trackAppUrlFetch(String secondUrl) {
        mGTracker.send(new HitBuilders.EventBuilder()
                .setCategory(SETTINGS_CATEGORY)
                .setAction("latestAppUrlFetched")
                .setLabel(secondUrl)
                .setValue(1)
                .build());
        Bundle bundle = new Bundle();
        bundle.putString("Url", secondUrl);
        mFTracker.logEvent("latestAppUrlFetched", bundle);
    }

    private void initializePreferenceListener() {
        Log.d("ficsaveM/initPref", "Listener");

        initializeSharedPreferenceListener();

        initializePreferenceClickListener();
    }

    private void initializePreferenceClickListener() {
        initializeVersionPreferenceClickListener();

        initializeDeveloperPreferenceClickListener();

        initializePrivacyPolicyPreferenceClickListener();
    }

    private void initializeDeveloperPreferenceClickListener() {
        developerPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference developerPref) {
                Log.d("ficsaveM/developerClick", developerPref.toString());
                mGTracker.send(new HitBuilders.EventBuilder()
                        .setCategory(SETTINGS_CATEGORY)
                        .setAction("developerPreferenceClicked")
                        .setLabel(developerPref.toString())
                        .setValue(1)
                        .build());
                Bundle bundle = new Bundle();
                bundle.putString("pref", developerPref.toString());
                mFTracker.logEvent("developerPreferenceClicked", bundle);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.developer_url)));
                startActivity(intent);
                // DialogFragment dialog = new DeveloperDialogFragment();
                // dialog.show((getActivity()).getFragmentManager(), "DeveloperDialogFragment");
                return true;
            }
        });
    }

    private void initializeVersionPreferenceClickListener() {
        versionPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference versionPref) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(latestVersionUrl));
                startActivity(intent);

                Log.d("ficsaveM/versionClick", versionPref.toString());
                mGTracker.send(new HitBuilders.EventBuilder()
                        .setCategory(SETTINGS_CATEGORY)
                        .setAction("versionPreferenceClicked")
                        .setLabel(versionPref.toString())
                        .setValue(1)
                        .build());
                Bundle bundle = new Bundle();
                bundle.putString("pref", versionPref.toString());
                mFTracker.logEvent("versionPreferenceClicked", bundle);
                return true;
            }
        });
    }

    private void initializePrivacyPolicyPreferenceClickListener() {
        privacyPolicyPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference privacyPolicyPref) {
                Intent intent = new Intent(getActivity().getBaseContext(), TermsActivity.class);
                startActivity(intent);

                Log.d("ficsaveM/privacyClick", privacyPolicyPref.toString());
                mGTracker.send(new HitBuilders.EventBuilder()
                        .setCategory(SETTINGS_CATEGORY)
                        .setAction("privacyPreferenceClicked")
                        .setLabel(privacyPolicyPref.toString())
                        .setValue(1)
                        .build());
                Bundle bundle = new Bundle();
                bundle.putString("pref", privacyPolicyPref.toString());
                mFTracker.logEvent("privacyPreferenceClicked", bundle);
                return true;
            }
        });
    }

    private void initializeSharedPreferenceListener() {
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
                                builder.setTitle(INVALID_EMAIL);
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
                                builder.setTitle(INVALID_EMAIL);
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
                            builder.setTitle(INVALID_EMAIL);
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
                    default:
                        break;
                }
                updatePreferenceView();

                Map<String, ?> logPrefs = sPrefs.getAll();
                logPrefs.remove(EMAIL_ADDRESS_TO_SEND_TO);

                mGTracker.send(new HitBuilders.EventBuilder()
                        .setCategory(SETTINGS_CATEGORY)
                        .setAction("SharedPreferenceChanged: " + key)
                        .setLabel(logPrefs.toString())
                        .setValue(1)
                        .build());
                Bundle bundle = new Bundle();
                bundle.putString("Key", key);
                bundle.putString("Preferences", logPrefs.toString());
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

    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {
        // empty
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // empty
    }
}

