package in.rahulja.fichubmiddleware;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class DownloadHistoryStore {

  public static final String DOWNLOAD_HISTORY_FILENAME = "fm_download_history.json";
  static final int MAX_ENTRIES = 100;
  private static final String FM_ERROR = "FM/Error";

  private final File file;

  DownloadHistoryStore(File file) {
    this.file = file;
  }

  List<Entry> read() {
    JSONArray jsonArray = new JSONArray();
    try (FileInputStream fis = new FileInputStream(file)) {
      jsonArray = new JSONArray(convertStreamToString(fis));
    } catch (Exception e) {
      Log.e(FM_ERROR, e.toString());
    }
    List<Entry> entries = new ArrayList<>();
    for (int i = 0; i < jsonArray.length(); i++) {
      try {
        JSONObject obj = jsonArray.getJSONObject(i);
        entries.add(new Entry(
            String.valueOf(obj.get("name")),
            String.valueOf(obj.get("path")),
            String.valueOf(obj.get("datetime"))));
      } catch (JSONException e) {
        Log.e(FM_ERROR, e.toString());
      }
    }
    return entries;
  }

  void append(String name, String path, String datetime) {
    List<Entry> entries = read();
    entries.add(new Entry(name, path, datetime));
    while (entries.size() > MAX_ENTRIES) {
      entries.remove(0);
    }
    JSONArray jsonArray = serialize(entries);
    // Atomic-ish write: fill a temp file first, then swap it in so a crash or
    // IO failure mid-write cannot truncate the existing history.
    File tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
    try (OutputStreamWriter writer = new OutputStreamWriter(
        new FileOutputStream(tmpFile), StandardCharsets.UTF_8)) {
      writer.write(jsonArray.toString());
    } catch (IOException e) {
      Log.e(FM_ERROR, e.toString());
      tmpFile.delete();
      return;
    }
    if (!tmpFile.renameTo(file)) {
      Log.e(FM_ERROR, "Could not replace history file with " + tmpFile);
      tmpFile.delete();
    }
  }

  private JSONArray serialize(List<Entry> entries) {
    JSONArray jsonArray = new JSONArray();
    for (Entry entry : entries) {
      JSONObject obj = new JSONObject();
      try {
        obj.put("name", entry.getName());
        obj.put("path", entry.getPath());
        obj.put("datetime", entry.getDatetime());
      } catch (JSONException e) {
        Log.e(FM_ERROR, e.toString());
      }
      jsonArray.put(obj);
    }
    return jsonArray;
  }

  @SuppressWarnings("WeakerAccess")
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

  static class Entry {

    private final String name;
    private final String path;
    private final String datetime;

    Entry(String name, String path, String datetime) {
      this.name = name;
      this.path = path;
      this.datetime = datetime;
    }

    public String getName() {
      return name;
    }

    public String getPath() {
      return path;
    }

    public String getDatetime() {
      return datetime;
    }
  }
}
