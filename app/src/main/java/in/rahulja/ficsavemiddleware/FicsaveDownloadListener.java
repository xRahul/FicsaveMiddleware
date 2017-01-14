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
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.File;

class FicsaveDownloadListener implements DownloadListener {
    public static final String OPEN_FILE_PREFERENCE = "open_file_preference";
    public static final String SEND_EMAIL_DEVICE_PREFERENCE = "send_email_device_preference";
    public static final String EMAIL_ADDRESS_TO_SEND_TO = "email_address_to_send_to";
    private final SharedPreferences prefs;
    private MainActivity mContext;
    private DownloadManager mDownloadManager;

    private String downloadUrl;
    private String downloadUserAgent;
    private String downloadContentDisposition;
    private String downloadMimeType;

    private long fileDownloadId;
    private String fileName;

    FicsaveDownloadListener(MainActivity context) {
        mContext = context;
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
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

        /*

        mRequest = new DownloadManager.Request(Uri.parse(url));
        // Limits the download to only over WiFi. Optional.
        mRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI);
        // Makes download visible in notifications while downloading, but disappears after download completes. Optional.
        mRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
        mRequest.setMimeType(mimetype);

        // If necessary for a security check. I needed it, but I don't think it's mandatory.
        String cookie = CookieManager.getInstance().getCookie(url);
        mRequest.addRequestHeader("Cookie", cookie);

        // Grabs the file name from the Content-Disposition
        String filename = null;
        Pattern regex = Pattern.compile("(?<=filename=\").*?(?=\")");
        Matcher regexMatcher = regex.matcher(contentDisposition);
        if (regexMatcher.find()) {
            filename = regexMatcher.group();
        }

        // Sets the file path to save to, including the file name. Make sure to have the WRITE_EXTERNAL_STORAGE permission!!
        mRequest.setDestinationInExternalFilesDir(mContext, Environment.DIRECTORY_DOWNLOADS, filename);
        // Sets the title of the notification and how it appears to the user in the saved directory.
        mRequest.setTitle(filename);

        // Adds the request to the DownloadManager queue to be executed at the next available opportunity.
        mDownloadedFileID = mDownloadManager.enqueue(mRequest);
        */
    }

    private void listenToDownloadComplete() {
        // Function is called once download completes.
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Log.d("ficsaveM/Dcomplete", intent.toString());
                for (String key : intent.getExtras().keySet()) {
                    Object value = intent.getExtras().get(key);
                    if (value != null) {
                        Log.d("ficsaveM/DCextras", key + " " + value.toString());
                    }
                }

                // Prevents the occasional unintentional call. I needed this.
                if (fileDownloadId == -1 || fileDownloadId != (long) intent.getExtras().get("extra_download_id"))
                    return;

                // Grabs the Uri for the file that was downloaded.
                Uri mostRecentDownload =
                        mDownloadManager.getUriForDownloadedFile(fileDownloadId);

                if (prefs.getBoolean(OPEN_FILE_PREFERENCE, true)) {
                    Intent fileIntent = new Intent(Intent.ACTION_VIEW);
                    // DownloadManager stores the Mime Type. Makes it really easy for us.
                    String mimeType =
                            mDownloadManager.getMimeTypeForDownloadedFile(fileDownloadId);
                    fileIntent.setDataAndType(mostRecentDownload, mimeType);
                    fileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        mContext.startActivity(fileIntent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(mContext, "No handler for this type of file.",
                                Toast.LENGTH_LONG).show();
                    }
                }

                if (prefs.getBoolean(SEND_EMAIL_DEVICE_PREFERENCE, true)) {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    emailIntent.setType("message/rfc822");
                    String emailAddress = prefs.getString(EMAIL_ADDRESS_TO_SEND_TO, "");
                    String to[] = {emailAddress};
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

                // Sets up the prevention of an unintentional call. I found it necessary. Maybe not for others.
                fileDownloadId = -1;

                mContext.unregisterReceiver(this);
            }
        };
        // Registers function to listen to the completion of the download.
        mContext.registerReceiver(onComplete, new
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void downloadFile() {
        // Guess file name from metadata
        fileName = URLUtil.guessFileName(downloadUrl, downloadContentDisposition, downloadMimeType);
        Request request = new Request(Uri.parse(downloadUrl));

        // Download only over wifi
        // request.setAllowedNetworkTypes(Request.NETWORK_WIFI);

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
        Toast.makeText(mContext, R.string.downloading_file_toast_msg, //To notify the Client that the file is being downloaded
                Toast.LENGTH_LONG).show();
    }
}