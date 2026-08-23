package in.rahulja.fichubmiddleware;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadHistoryActivity extends AppCompatActivity {

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
    List<DownloadHistoryStore.Entry> entries = getHistoryEntries();
    ArrayList<String> listData = createHistoryList(entries);
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

  @NonNull private ArrayList<String> createHistoryList(
      List<DownloadHistoryStore.Entry> entries) {
    ArrayList<String> listData = new ArrayList<>();
    for (int i = entries.size() - 1; i >= 0; i--) {
      DownloadHistoryStore.Entry entry = entries.get(i);
      listData.add(entry.getDatetime() + "\n" + entry.getName());
    }
    Log.d("FM/historyList", listData.toString());
    return listData;
  }

  @NonNull private List<DownloadHistoryStore.Entry> getHistoryEntries() {
    DownloadHistoryStore historyStore = new DownloadHistoryStore(
        new File(getFilesDir(), DownloadHistoryStore.DOWNLOAD_HISTORY_FILENAME));
    return historyStore.read();
  }
}
