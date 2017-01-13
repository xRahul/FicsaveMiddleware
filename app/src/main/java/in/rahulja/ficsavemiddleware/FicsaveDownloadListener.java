package in.rahulja.ficsavemiddleware;

import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.widget.Toast;

class FicsaveDownloadListener implements DownloadListener {
    private MainActivity mContext;
    private DownloadManager mDownloadManager;

    private String downloadUrl;
    private String downloadUserAgent;
    private String downloadContentDisposition;
    private String downloadMimeType;

    private long fileDownloadId;

    FicsaveDownloadListener(MainActivity context) {
        mContext = context;
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
                Log.d("ficsaveM/DCextras", intent.getExtras().toString());

                // Prevents the occasional unintentional call. I needed this.
                if (fileDownloadId == -1 || fileDownloadId != (long) intent.getExtras().get("extra_download_id"))
                    return;
                Intent fileIntent = new Intent(Intent.ACTION_VIEW);

                // Grabs the Uri for the file that was downloaded.
                Uri mostRecentDownload =
                        mDownloadManager.getUriForDownloadedFile(fileDownloadId);
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
        String fileName = URLUtil.guessFileName(downloadUrl, downloadContentDisposition, downloadMimeType);
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