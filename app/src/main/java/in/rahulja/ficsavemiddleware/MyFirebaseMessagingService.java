package in.rahulja.ficsavemiddleware;

import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

  private static final String TAG = "FirebaseMsgService";

  @Override
  public void onNewToken(String s) {
    super.onNewToken(s);
    Log.d(TAG, "Refreshed token: " + s);
  }
}