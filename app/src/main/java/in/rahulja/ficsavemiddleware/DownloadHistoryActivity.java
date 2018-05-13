package in.rahulja.ficsavemiddleware;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.io.FileInputStream;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DownloadHistoryActivity extends AppCompatActivity {

  public static final String DOWNLOAD_HISTORY_FILENAME = "fm_download_history.json";
  private ListView lv;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download_history);

    lv = (ListView) findViewById(R.id.history_list_view);
  }

  @Override
  public void onResume() {
    super.onResume();  // Always call the superclass method first

    FileInputStream fis;
    JSONArray jsonArray = new JSONArray();
    try {
      fis = openFileInput(DOWNLOAD_HISTORY_FILENAME);
      String historyFileData = FicsaveDownloadListener.convertStreamToString(fis);
      jsonArray = new JSONArray(historyFileData);
      Log.d("FM/HISTaCTIV", jsonArray.toString());
      fis.close();
    } catch (Exception e) {
      Log.e("FM/error", e.toString());
    }

    ArrayList<String> listdata = new ArrayList<>();
    for (int i = jsonArray.length() - 1; i >= 0; i--) {
      try {
        JSONObject tempObject = jsonArray.getJSONObject(i);

        listdata.add(
            String.valueOf(tempObject.get("datetime"))
                + "\n"
                + tempObject.get("name")
        );
      } catch (JSONException e) {
        Log.e("FM/error", e.toString());
      }
    }

    Log.d("FM/historyList", listdata.toString());

    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
        this,
        R.layout.history_item,
        R.id.history_list_item_textview,
        listdata);

    lv.setAdapter(arrayAdapter);
  }
}
