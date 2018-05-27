package in.rahulja.ficsavemiddleware;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.widget.Toast;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.Date;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class FicsaveDownloadListener implements DownloadListener {
  private static final String OPEN_FILE_PREFERENCE = "open_file_preference";
  private static final String SEND_EMAIL_DEVICE_PREFERENCE = "send_email_device_preference";
  private static final String EMAIL_ADDRESS_TO_SEND_TO = "email_address_to_send_to";
  private static final String DOWNLOAD_LISTENER_CATEGORY = "DownloadListenerCategory";
  private static final String FILE_LABEL = "File: ";
  private static final String DOWNLOAD_HISTORY_FILENAME = "fm_download_history.json";
  private static final String FM_ERROR = "FM/Error";
  private final SharedPreferences prefs;
  private MainActivity mContext;
  private DownloadManager mDownloadManager;
  private String downloadUrl;
  private String downloadUserAgent;
  private String downloadContentDisposition;
  private String downloadMimeType;
  private long fileDownloadId;
  private String fileName;
  private Tracker mGTracker;
  private FirebaseAnalytics mFTracker;

  FicsaveDownloadListener(MainActivity context) {
    mContext = context;
    prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    FicsaveMiddlewareApplication application =
        (FicsaveMiddlewareApplication) mContext.getApplication();
    mGTracker = application.getDefaultGATracker();
    mFTracker = application.getDefaultFATracker();
  }

  static String convertStreamToString(InputStream is) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = reader.readLine()) != null) {
      sb.append(line).append("\n");
    }
    reader.close();
    return sb.toString();
  }

  @Override
  public void onDownloadStart(String url, String userAgent, String
      contentDisposition, final String mimetype, long contentLength) {

    downloadUrl = url;
    downloadUserAgent = userAgent;
    downloadContentDisposition = contentDisposition;
    downloadMimeType = mimetype;

    mDownloadManager = (DownloadManager) mContext
        .getSystemService(Context.DOWNLOAD_SERVICE);

    downloadFile();
    listenToDownloadComplete();
  }

  private void listenToDownloadComplete() {
    // Function is called once download completes.
    BroadcastReceiver onComplete = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Log.d("ficsaveM/Dcomplete", intent.toString());
        // Prevents the occasional unintentional call. I needed this.
        if (fileDownloadId == -1
            || intent.getExtras() == null
            || fileDownloadId != (long) intent.getExtras()
            .get("extra_download_id")) {
          return;
        }
        // Grabs the Uri for the file that was downloaded.
        Uri mostRecentDownload = mDownloadManager.getUriForDownloadedFile(fileDownloadId);
        JSONArray jsonArray = new JSONArray();
        try {
          FileInputStream fis = mContext.openFileInput(DOWNLOAD_HISTORY_FILENAME);
          String historyFileData = convertStreamToString(fis);
          jsonArray = new JSONArray(historyFileData);
          fis.close();
        } catch (Exception e) {
          Log.e(FM_ERROR, e.toString());
        }

        JSONObject newFileData = new JSONObject();
        try {
          newFileData.put("name", fileName);
          newFileData.put("path", mostRecentDownload);
          newFileData.put("datetime", DateFormat.getDateTimeInstance().format(new Date()));
          Log.d("FM/FILE_DATA", jsonArray.toString());
          Log.d("FM/NEW_FILE_DATA", newFileData.toString());
          jsonArray.put(newFileData);
        } catch (JSONException e) {
          Log.e(FM_ERROR, e.toString());
        }

        try (OutputStreamWriter writer = new OutputStreamWriter(
            mContext.openFileOutput(DOWNLOAD_HISTORY_FILENAME, Context.MODE_PRIVATE),
            StandardCharsets.UTF_8)
        ) {
          writer.write(jsonArray.toString());
        } catch (IOException e) {
          Log.e(FM_ERROR, e.toString());
        }

        if (prefs.getBoolean(OPEN_FILE_PREFERENCE, true)) {
          openFileOnDevice(mostRecentDownload);
        }
        if (prefs.getBoolean(SEND_EMAIL_DEVICE_PREFERENCE, true)) {
          emailFileFromDevice(mostRecentDownload);
        }
        // Sets up the prevention of an unintentional call. I found it necessary. Maybe not for others.
        fileDownloadId = -1;
        mContext.unregisterReceiver(this);
        trackDownloadComplete();
      }
    };
    // Registers function to listen to the completion of the download.
    mContext.registerReceiver(onComplete, new
        IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
  }

  private void sendGTrackerEvent(String action) {
    mGTracker.send(new HitBuilders.EventBuilder()
        .setCategory(DOWNLOAD_LISTENER_CATEGORY)
        .setAction(action)
        .setLabel(FILE_LABEL + fileName)
        .setValue(1)
        .build());
    Bundle bundle = new Bundle();
    bundle.putString("File", fileName);
    mFTracker.logEvent(action, bundle);
  }

  private void trackDownloadComplete() {
    sendGTrackerEvent("DownloadComplete");
  }

  private void trackFileCannotOpen() {
    sendGTrackerEvent("CannotOpenFile");
  }

  private void openFileOnDevice(Uri mostRecentDownload) {
    Intent fileIntent = new Intent(Intent.ACTION_VIEW);
    // DownloadManager stores the Mime Type. Makes it really easy for us.
    String mimeType =
        mDownloadManager.getMimeTypeForDownloadedFile(fileDownloadId);
    fileIntent.setDataAndType(mostRecentDownload, mimeType);
    fileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    try {
      mContext.startActivity(fileIntent);
    } catch (ActivityNotFoundException e) {
      Log.d("ficsaveM/cantOpenFile", fileName);
      trackFileCannotOpen();
      Toast.makeText(mContext, "You don't have a supported ebook reader",
          Toast.LENGTH_LONG).show();
    }
  }

  private void emailFileFromDevice(Uri mostRecentDownload) {
    Intent emailIntent = new Intent(Intent.ACTION_SEND);
    emailIntent.setType("message/rfc822");
    String emailAddress = prefs.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
    String[] to = { emailAddress };
    emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
    // the attachment
    emailIntent.putExtra(Intent.EXTRA_STREAM, mostRecentDownload);
    // the mail subject
    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "FicsaveMiddleware - " + fileName);
    // the mail content
    String content =
        "Hi" +
            "\n\n" +
            "Hope you enjoy the story!" +
            "\n\n" +
            "Ficsave.xyz is a creation of https://github.com/waylaidwanderer" +
            "\n" +
            "and FicsaveMiddleware is created by https://github.com/xRahul.";
    emailIntent.putExtra(Intent.EXTRA_TEXT, content);
    mContext.startActivity(emailIntent);
  }

  private void downloadFile() {
    // Guess file name from metadata
    fileName = URLUtil.guessFileName(downloadUrl, downloadContentDisposition, downloadMimeType);
    Request request = new Request(Uri.parse(downloadUrl));

    // Make media scanner scan this file so that other apps can use it.
    request.allowScanningByMediaScanner();

    // show notification when downloading and after download completes
    request.setNotificationVisibility(
        Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
    );

    // Set the directory where to save the file
    Log.d("ficsaveM/filePath", Environment.DIRECTORY_DOCUMENTS + "/" + fileName);
    request.setDestinationInExternalPublicDir(
        Environment.DIRECTORY_DOCUMENTS, fileName);

    request.setMimeType(downloadMimeType);
    request.setTitle(fileName);

    // Set headers needed to download the file
    String cookies = CookieManager.getInstance().getCookie(downloadUrl);
    request.addRequestHeader("cookie", cookies);
    request.addRequestHeader("User-Agent", downloadUserAgent);

    fileDownloadId = mDownloadManager.enqueue(request);
    Log.d("ficsaveM/DownloadStartd", "fileID: " + fileDownloadId + ", fileName: " + fileName);
    Toast.makeText(mContext, R.string.downloading_file_toast_msg,
        //To notify the Client that the file is being downloaded
        Toast.LENGTH_LONG).show();

    sendGTrackerEvent("DownloadEnqueued");
  }
}