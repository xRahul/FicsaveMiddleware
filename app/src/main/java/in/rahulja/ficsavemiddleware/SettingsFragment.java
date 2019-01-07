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
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
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
  public static final String INVALID_EMAIL = "Invalid Email";
  private OnSharedPreferenceChangeListener listener;
  private Preference emailAddressPref;
  private SharedPreferences prefs;
  private String versionSummary;
  private Preference versionPref;
  private String latestVersionUrl;
  private Preference developerPref;
  private Preference privacyPolicyPref;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

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
          HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
          httpUrlConnection.setInstanceFollowRedirects(false);
          String secondUrl =
              String.valueOf(new URL(httpUrlConnection.getHeaderField("Location")));
          String latestVersion = Uri.parse(secondUrl).getLastPathSegment();
          Log.d("ficsaveM/updateUrl", secondUrl);
          if (getActivity() != null) {
            String checkUrl =
                getString(R.string.current_release_url_prefix) + BuildConfig.VERSION_NAME;
            if (secondUrl.equals(checkUrl)) {
              versionSummary = BuildConfig.VERSION_NAME + " "
                  + getString(R.string.version_summary_latest);
            } else {
              versionSummary = BuildConfig.VERSION_NAME + " "
                  + "(" + getString(R.string.version_summary_changed_latest) + latestVersion + ")";
            }

            getActivity().runOnUiThread(new Runnable() {
              @Override
              public void run() {
                versionPref.setSummary(versionSummary);
                Log.d("ficsaveM/versionChecked", versionSummary);
              }
            });
          }
        } catch (IOException e) {
          Log.e("FM/SettingsFragment", Arrays.toString(e.getStackTrace()));
        }
      }
    });
    thread.start();
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
        Intent intent =
            new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.developer_url)));
        startActivity(intent);
        /* DialogFragment dialog = new DeveloperDialogFragment();
        dialog.show((getActivity()).getFragmentManager(), "DeveloperDialogFragment"); */
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
        return true;
      }
    });
  }

  private void initializeSharedPreferenceListener() {
    listener = new OnSharedPreferenceChangeListener() {
      public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
          case DOWNLOAD_FILE_PREFERENCE:
            downloadFileSharedPreferenceChange(sharedPreferences);
            break;
          case OPEN_FILE_PREFERENCE:
            openFileSharedPreferenceChange(sharedPreferences);
            break;
          case SEND_EMAIL_DEVICE_PREFERENCE:
            sendEmailDeviceSharedPreferenceChange(sharedPreferences);
            break;
          case SEND_EMAIL_SITE_PREFERENCE:
            sendEmailSiteSharedPreferenceChange(sharedPreferences);
            break;
          case EMAIL_ADDRESS_TO_SEND_TO:
            emailAddressToSendToSharedPreferenceChange(sharedPreferences);
            break;
          default:
            break;
        }
        updatePreferenceView();

        Map<String, ?> logPrefs = sharedPreferences.getAll();
        logPrefs.remove(EMAIL_ADDRESS_TO_SEND_TO);
      }
    };
  }

  private void emailAddressToSendToSharedPreferenceChange(SharedPreferences sharedPreferences) {
    String emailAddress = sharedPreferences.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
    if (!emailAddress.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(emailAddress)
        .matches()) {

      if (Patterns.EMAIL_ADDRESS.matcher(emailAddress.trim()).matches()) {
        sharedPreferences.edit().putString(EMAIL_ADDRESS_TO_SEND_TO, emailAddress.trim()).apply();
        return;
      }

      final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
      builder.setTitle(INVALID_EMAIL);
      builder.setMessage("Please enter a valid email address");
      builder.setPositiveButton(android.R.string.ok, null);
      builder.show();
      sharedPreferences.edit().putString(EMAIL_ADDRESS_TO_SEND_TO, "").apply();
    } else if (emailAddress.isEmpty()) {
      sharedPreferences.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
      sharedPreferences.edit().putBoolean(SEND_EMAIL_SITE_PREFERENCE, false).apply();
      emailAddressPref.setSummary(R.string.email_address_summary);
    } else {
      emailAddressPref.setSummary(emailAddress);
    }
  }

  private void sendEmailSiteSharedPreferenceChange(SharedPreferences sharedPreferences) {
    Boolean sendEmailFromSite = sharedPreferences.getBoolean(SEND_EMAIL_SITE_PREFERENCE, true);
    if (sendEmailFromSite) {
      String emailAddress = sharedPreferences.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
      if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
        showInvalidEmailAlert();
        sharedPreferences.edit().putBoolean(SEND_EMAIL_SITE_PREFERENCE, false).apply();
      } else {
        sharedPreferences.edit().putBoolean(DOWNLOAD_FILE_PREFERENCE, false).apply();
        sharedPreferences.edit().putBoolean(OPEN_FILE_PREFERENCE, false).apply();
        sharedPreferences.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
      }
    } else {
      sharedPreferences.edit().putBoolean(DOWNLOAD_FILE_PREFERENCE, true).apply();
    }
  }

  private void showInvalidEmailAlert() {
    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(INVALID_EMAIL);
    builder.setMessage("Please enter a valid email address first!");
    builder.setPositiveButton(android.R.string.ok, null);
    builder.show();
  }

  private void sendEmailDeviceSharedPreferenceChange(SharedPreferences sharedPreferences) {
    Boolean sendEmailFromDevice = sharedPreferences.getBoolean(SEND_EMAIL_DEVICE_PREFERENCE, true);
    if (sendEmailFromDevice) {
      String emailAddress = sharedPreferences.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
      if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
        showInvalidEmailAlert();
        sharedPreferences.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
      } else {
        sharedPreferences.edit().putBoolean(OPEN_FILE_PREFERENCE, false).apply();
      }
    }
  }

  private void openFileSharedPreferenceChange(SharedPreferences sharedPreferences) {
    Boolean openFileOnDevice = sharedPreferences.getBoolean(OPEN_FILE_PREFERENCE, true);
    if (openFileOnDevice) {
      sharedPreferences.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
    }
  }

  private void downloadFileSharedPreferenceChange(SharedPreferences sharedPreferences) {
    Boolean downloadFileToDevice = sharedPreferences.getBoolean(DOWNLOAD_FILE_PREFERENCE, true);
    if (downloadFileToDevice) {
      sharedPreferences.edit().putBoolean(SEND_EMAIL_SITE_PREFERENCE, false).apply();
    } else {
      sharedPreferences.edit().putBoolean(OPEN_FILE_PREFERENCE, false).apply();
      sharedPreferences.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
      sharedPreferences.edit().putBoolean(SEND_EMAIL_SITE_PREFERENCE, true).apply();
    }
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

