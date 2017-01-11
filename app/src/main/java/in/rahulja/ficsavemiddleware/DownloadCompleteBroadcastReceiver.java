package in.rahulja.ficsavemiddleware;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public class DownloadCompleteBroadcastReceiver extends BroadcastReceiver {
    private final long fileId;

//    public DownloadCompleteBroadcastReceiver(long downlaodFileId) {
//        fileId = downlaodFileId;
//    }

    public DownloadCompleteBroadcastReceiver() {
        fileId = 1;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.

        if (fileId == -1)
            return;

        Intent fileIntent = new Intent(Intent.ACTION_VIEW);

        DownloadManager mDownloadManager = (DownloadManager) context
                .getSystemService(Context.DOWNLOAD_SERVICE);

        // Grabs the Uri for the file that was downloaded.
        Uri mostRecentDownload =
                mDownloadManager.getUriForDownloadedFile(fileId);
        // DownloadManager stores the Mime Type. Makes it really easy for us.
        String mimeType =
                mDownloadManager.getMimeTypeForDownloadedFile(fileId);



        fileIntent.setDataAndType(mostRecentDownload, mimeType);
        fileIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(fileIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No handler for this type of file.",
                    Toast.LENGTH_LONG).show();
        }
    }
}
