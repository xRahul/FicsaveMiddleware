package in.rahulja.ficsavemiddleware;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
  private ListView listView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_download_history);
    listView = findViewById(R.id.history_list_view);
  }

  @Override
  public void onResume() {
    super.onResume();  // Always call the superclass method first
    JSONArray jsonArray = getHistoryDataFromFile();
    ArrayList<String> listData = createHistoryList(jsonArray);
    setHistoryListToListView(listData);
  }

  private void setHistoryListToListView(ArrayList<String> listData) {
    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
        this,
        R.layout.history_item,
        R.id.history_list_item_textview,
        listData);
    listView.setAdapter(arrayAdapter);
  }

  @NonNull private ArrayList<String> createHistoryList(JSONArray jsonArray) {
    ArrayList<String> listData = new ArrayList<>();
    for (int i = jsonArray.length() - 1; i >= 0; i--) {
      try {
        JSONObject tempObject = jsonArray.getJSONObject(i);
        listData.add(
            String.valueOf(tempObject.get("datetime"))
                + "\n"
                + tempObject.get("name")
        );
      } catch (JSONException e) {
        Log.e("FM/error", e.toString());
      }
    }
    Log.d("FM/historyList", listData.toString());
    return listData;
  }

  @NonNull private JSONArray getHistoryDataFromFile() {
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
    return jsonArray;
  }
}
