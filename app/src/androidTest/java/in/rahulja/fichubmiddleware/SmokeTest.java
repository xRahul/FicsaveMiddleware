package in.rahulja.fichubmiddleware;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Instrumented smoke tests, which will execute on an Android device. */
@RunWith(AndroidJUnit4.class)
public class SmokeTest {

  @Test
  public void targetContextMatchesApplicationId() {
    Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

    assertEquals(BuildConfig.APPLICATION_ID, appContext.getPackageName());
  }

  @Test
  public void mainActivityLaunchesWithWebView() {
    try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
      scenario.onActivity(
          activity -> assertNotNull(activity.findViewById(R.id.main_webview)));
    }
  }
}
