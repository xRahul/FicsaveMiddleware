package in.rahulja.fichubmiddleware;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import android.util.Log;
import android.util.Patterns;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;

public class SettingsFragment extends PreferenceFragmentCompat {

  public static final String DOWNLOAD_FILE_PREFERENCE = "download_file_preference";
  public static final String OPEN_FILE_PREFERENCE = "open_file_preference";
  public static final String SEND_EMAIL_DEVICE_PREFERENCE = "send_email_device_preference";
  public static final String EMAIL_ADDRESS_TO_SEND_TO = "email_address_to_send_to";
  public static final String PREF_VERSION = "version";
  public static final String PREF_DEVELOPER = "developer";
  public static final String PREF_PRIVACY_POLICY = "privacy_policy";
  public static final String INVALID_EMAIL = "Invalid Email";
  private static final String RELEASES_API_URL =
      "https://api.github.com/repos/xRahul/FicsaveMiddleware/releases/latest";
  private OnSharedPreferenceChangeListener listener;
  private Preference emailAddressPref;
  private SharedPreferences prefs;
  private String versionSummary;
  private Preference versionPref;
  private Preference developerPref;
  private Preference privacyPolicyPref;
  private final ExecutorService versionCheckExecutor = Executors.newSingleThreadExecutor();

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    prefs = PreferenceManager.getDefaultSharedPreferences(this.getActivity());

    addPreferencesFromResource(R.xml.preferences);
    initPreferenceView();
    checkLatestAppVersion();
    updatePreferenceView();
  }

  private void initPreferenceView() {
    emailAddressPref = findPreference(EMAIL_ADDRESS_TO_SEND_TO);
    versionPref = findPreference(PREF_VERSION);
    developerPref = findPreference(PREF_DEVELOPER);
    privacyPolicyPref = findPreference(PREF_PRIVACY_POLICY);

    initializePreferenceListener();
  }

  private void updatePreferenceView() {
    String emailAddress = prefs.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
    if (emailAddress.isEmpty()) {
      emailAddressPref.setSummary(R.string.email_address_summary);
    } else {
      emailAddressPref.setSummary(emailAddress);
    }

    versionPref.setSummary(versionSummary);

    findPreference(DOWNLOAD_FILE_PREFERENCE).setSummary("Documents");
  }

  private void checkLatestAppVersion() {
    versionSummary = BuildConfig.VERSION_NAME;
    versionCheckExecutor.execute(new Runnable() {
      @Override
      public void run() {
        try {
          URL url = new URL(RELEASES_API_URL);
          HttpURLConnection httpUrlConnection = (HttpURLConnection) url.openConnection();
          httpUrlConnection.setRequestProperty("Accept", "application/vnd.github+json");
          httpUrlConnection.setConnectTimeout(10_000);
          httpUrlConnection.setReadTimeout(10_000);
          StringBuilder responseBody = new StringBuilder();
          try (BufferedReader reader = new BufferedReader(new InputStreamReader(
              httpUrlConnection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
              responseBody.append(line);
            }
          }
          String latestTag =
              new JSONObject(responseBody.toString()).getString("tag_name");
          String latestVersion =
              latestTag.startsWith("v") ? latestTag.substring(1) : latestTag;
          Log.d("ficsaveM/updateUrl", latestVersion);
          boolean isLatest = latestVersion.equals(BuildConfig.VERSION_NAME);

          if (getActivity() != null && !isDetached() && !isRemoving()) {
            getActivity().runOnUiThread(new Runnable() {
              @Override
              public void run() {
                if (versionPref != null) {
                  if (isLatest) {
                    versionSummary = BuildConfig.VERSION_NAME + " "
                        + getString(R.string.version_summary_latest);
                  } else {
                    versionSummary = BuildConfig.VERSION_NAME + " "
                        + "(" + getString(R.string.version_summary_changed_latest)
                        + latestVersion + ")";
                  }
                  versionPref.setSummary(versionSummary);
                  Log.d("ficsaveM/versionChecked", versionSummary);
                }
              }
            });
          }
        } catch (Exception e) {
          Log.e("FM/SettingsFragment", Arrays.toString(e.getStackTrace()));
        }
      }
    });
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
        return true;
      }
    });
  }

  private void initializeVersionPreferenceClickListener() {
    versionPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
      public boolean onPreferenceClick(Preference versionPref) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
            Uri.parse(getString(R.string.latest_release_url)));
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

      showInvalidEmailAlert("Please enter a valid email address");
      sharedPreferences.edit().putString(EMAIL_ADDRESS_TO_SEND_TO, "").apply();
    } else if (emailAddress.isEmpty()) {
      sharedPreferences.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
    } else {
      emailAddressPref.setSummary(emailAddress);
    }
  }

  private void showInvalidEmailAlert(String message) {
    if (getActivity() == null) {
      return;
    }
    final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setTitle(INVALID_EMAIL);
    builder.setMessage(message);
    builder.setPositiveButton(android.R.string.ok, null);
    builder.show();
  }

  private void sendEmailDeviceSharedPreferenceChange(SharedPreferences sharedPreferences) {
    Boolean sendEmailFromDevice = sharedPreferences.getBoolean(SEND_EMAIL_DEVICE_PREFERENCE, true);
    if (sendEmailFromDevice) {
      String emailAddress = sharedPreferences.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
      if (!Patterns.EMAIL_ADDRESS.matcher(emailAddress).matches()) {
        showInvalidEmailAlert("Please enter a valid email address first!");
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
    if (!downloadFileToDevice) {
      sharedPreferences.edit().putBoolean(OPEN_FILE_PREFERENCE, false).apply();
      sharedPreferences.edit().putBoolean(SEND_EMAIL_DEVICE_PREFERENCE, false).apply();
    }
  }

  @Override
  public void onResume() {
    super.onResume();

    prefs.registerOnSharedPreferenceChangeListener(listener);
  }

  @Override
  public void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(listener);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (versionCheckExecutor != null) {
      versionCheckExecutor.shutdown();
    }
  }
}
