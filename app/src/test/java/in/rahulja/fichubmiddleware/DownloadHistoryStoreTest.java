package in.rahulja.fichubmiddleware;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class DownloadHistoryStoreTest {

  @Rule public TemporaryFolder tmp = new TemporaryFolder();

  private File historyFile;
  private DownloadHistoryStore store;

  @Before
  public void setUp() throws IOException {
    historyFile = new File(tmp.getRoot(), DownloadHistoryStore.DOWNLOAD_HISTORY_FILENAME);
    store = new DownloadHistoryStore(historyFile);
  }

  @Test
  public void nonexistentFileReadsAsEmptyList() {
    assertTrue(store.read().isEmpty());
    assertEquals("fm_download_history.json", historyFile.getName());
  }

  @Test
  public void corruptFileReadsAsEmptyListWithoutThrowing() throws IOException {
    try (OutputStreamWriter writer = new OutputStreamWriter(
        Files.newOutputStream(historyFile.toPath()), StandardCharsets.UTF_8)) {
      writer.write("{this is not valid json][");
    }
    assertTrue(store.read().isEmpty());
  }

  @Test
  public void appendThenReadPreservesOrder() {
    store.append("first.epub", "/storage/first.epub", "Jan 1, 2026 10:00 AM");
    store.append("second.epub", "/storage/second.epub", "Jan 2, 2026 11:00 AM");
    store.append("third.epub", "/storage/third.epub", "Jan 3, 2026 12:00 PM");

    List<DownloadHistoryStore.Entry> entries = store.read();
    assertEquals(3, entries.size());
    assertEquals("first.epub", entries.get(0).getName());
    assertEquals("/storage/first.epub", entries.get(0).getPath());
    assertEquals("Jan 1, 2026 10:00 AM", entries.get(0).getDatetime());
    assertEquals("second.epub", entries.get(1).getName());
    assertEquals("/storage/second.epub", entries.get(1).getPath());
    assertEquals("third.epub", entries.get(2).getName());
    assertEquals("/storage/third.epub", entries.get(2).getPath());
  }

  @Test
  public void appendPastCapDropsOldest() {
    for (int i = 1; i <= DownloadHistoryStore.MAX_ENTRIES + 1; i++) {
      store.append("file" + i + ".epub", "/path/" + i, "dt" + i);
    }

    List<DownloadHistoryStore.Entry> entries = store.read();
    assertEquals(DownloadHistoryStore.MAX_ENTRIES, entries.size());
    assertEquals("file2.epub", entries.get(0).getName());
    assertEquals(
        "file" + (DownloadHistoryStore.MAX_ENTRIES + 1) + ".epub",
        entries.get(entries.size() - 1).getName());
  }

  @Test
  public void appendLeavesNoTempFileResidue() {
    store.append("a.epub", "/path/a", "dt1");
    store.append("b.epub", "/path/b", "dt2");

    assertEquals(2, store.read().size());
    File[] tmpFiles = tmp.getRoot().listFiles((dir, name) -> name.endsWith(".tmp"));
    assertTrue(tmpFiles != null);
    assertEquals(0, tmpFiles.length);
  }

  @Test
  public void appendKeepsExistingEntriesWhenFileAlreadyHasHistory() throws IOException {
    store.append("old.epub", "/path/old", "dtOld");
    // Simulate a legacy file written by the old listener format (same JSON shape).
    DownloadHistoryStore reopened = new DownloadHistoryStore(historyFile);
    reopened.append("new.epub", "/path/new", "dtNew");

    List<DownloadHistoryStore.Entry> entries = reopened.read();
    assertEquals(2, entries.size());
    assertEquals("old.epub", entries.get(0).getName());
    assertEquals("new.epub", entries.get(1).getName());
  }
}
