package in.rahulja.fichubmiddleware;

import static org.junit.Assert.assertEquals;

import android.net.Uri;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class FicHubUrlsTest {

  @Test
  public void homeReturnsBaseUrlWithTrailingSlash() {
    assertEquals("https://fichub.net/", FicHubUrls.home());
  }

  @Test
  public void autoExportEncodesFicUrlRoundTrip() {
    String ficUrl = "https://www.fanfiction.net/s/123/1/Some-Title?param=1&x=y z";
    String url = FicHubUrls.autoExport(ficUrl);
    Uri parsed = Uri.parse(url);
    assertEquals(ficUrl, parsed.getQueryParameter("q"));
  }

  @Test(expected = IllegalArgumentException.class)
  public void autoExportThrowsOnNull() {
    FicHubUrls.autoExport(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void autoExportThrowsOnEmpty() {
    FicHubUrls.autoExport("");
  }
}
