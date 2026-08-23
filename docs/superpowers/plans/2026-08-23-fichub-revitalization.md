# FicHub Middleware Revitalization — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the WebView wrapper work against the live **fichub.net** backend again (ficsave.xyz is dead), preserving all real features, while modernizing build/toolchain/security/CI and adding a test suite. Full rename ficsavemiddleware → fichubmiddleware.

**Architecture:** Keep the proven model: Android WebView loads FicHub's native auto-export URL (`https://fichub.net/?b=1&q=<encoded fic url>` — verified live 2026-08-23: page JS (`/js/_.js`) auto-calls `/api/v0/epub?q=` and renders plain `<a href>` download links into `#i`). Existing `DownloadManager` interception handles downloads unchanged. Fragile DOM-scraping JS injection (fill input, set dropdown, submit click) is replaced by one small self-polling "click my preferred format link" script. Stay Java.

**Tech stack:** Java 17 · AGP 8.7.3 / Gradle 8.9 wrapper · compileSdk/targetSdk 36 · minSdk 24 (unchanged) · androidx appcompat 1.7.x, preference 1.2.1, cardview 1.0.0 · JUnit4 + Robolectric (unit), androidx.test + Espresso (instrumented) · GitHub Actions CI/CD.

## Global Constraints

- **applicationId stays `in.rahulja.ficsavemiddleware`** — existing installs must keep updating. Source namespace/package becomes `in.rahulja.fichubmiddleware`.
- Naming convention: package lowercase `fichubmiddleware`; classes `FicHub*` (e.g., `FicHubWebViewClient`, `FicHubDownloadListener`, `FicHubMiddlewareApplication`); display name "FicHub Middleware".
- **Feature parity** (user-confirmed): share-intent auto-export ✓, deep link ✓, default-format pref (auto-click) ✓, download-to-device ✓, open-after-download ✓, email-from-device ✓, download history ✓, terms screen ✓, version check ✓, developer link ✓.
- **Removed by user decision:** "send ebook as email from website" setting (FicHub has no such feature).
- History file name unchanged (`fm_download_history.json`) so old installs keep their history.
- HTTPS only — zero cleartext config.
- Every code task ends green: `./gradlew assembleDebug testDebugUnitTest lintDebug`.
- Dependencies: drop unused material, legacy-support-v4 (both lines), vectordrawable (verified zero usages); KEEP cardview (used by history_item.xml).
- No new dependencies beyond: robolectric, androidx.test.ext:junit (test scope).

---

### Task 1: Cruft purge

**Files:** Delete `app/APKs/` (22MB), `.travis.yml`, `sonar-project.properties`, `app/google-services (demo).json`, `.idea/**`; Modify `.gitignore`.

Steps:
- [ ] `git rm -r` the listed paths
- [ ] `.gitignore`: append `.idea/`, `*.iml`, `local.properties`, `build/`, `.codegraph/`, `.opencode/`
- [ ] Verify: `git status` shows only intended deletions
- [ ] Commit: `chore: remove dead CI config, committed APKs, IDE files`

### Task 2: Toolchain uplift (AGP 3.5.3 → 8.7.3)

**Files:** Modify `settings.gradle`, `build.gradle`, `app/build.gradle`, `gradle/wrapper/gradle-wrapper.properties`, `app/src/main/AndroidManifest.xml`, `gradle.properties`.

Steps:
- [ ] Wrapper → `gradle-8.9-bin.zip`
- [ ] Root `build.gradle`: plugins DSL `id 'com.android.application' version '8.7.3' apply false`; repositories mavenCentral + google (drop jcenter); ext block: compileSdkVersion 36, minSdkVersion 24, targetSdkVersion 36, applicationId `"in.rahulja.ficsavemiddleware"` (unchanged), versionCode 300, versionName `"3.0.0"`
- [ ] `app/build.gradle`: add `namespace 'in.rahulja.ficsavemiddleware'` (rename to fichubmiddleware happens in Task 3); deps → appcompat 1.7.0, preference 1.2.1, cardview 1.0.0; REMOVE material, legacy-support-v4 (both duplicate lines), vectordrawable; testImplementation junit 4.13.2 + org.robolectric:robolectric:4.14.1; androidTestImplementation androidx.test.ext:junit:1.2.1, espresso-core 3.6.1, runner 1.6.2, rules 1.6.1; `compileOptions { sourceCompatibility JavaVersion.VERSION_17; targetCompatibility JavaVersion.VERSION_17 }`; `testOptions { unitTests { includeAndroidResources = true } }`
- [ ] Manifest: delete `package=` attr; explicit `android:exported="true"` on MainActivity, `android:exported="false"` on the other three activities; `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28"/>`; deep-link intent-filter data → `<data android:scheme="https" android:host="fichub.net"/>` (remove http+ficsave.xyz); remove `android:networkSecurityConfig` attr; MainActivity `launchMode` singleInstance → singleTask; add inside `<manifest>` before `<application>`:
```xml
<queries>
    <intent><action android:name="android.intent.action.VIEW"/><data android:scheme="https"/></intent>
    <intent><action android:name="android.intent.action.SEND"/><data android:mimeType="message/rfc822"/></intent>
</queries>
```
- [ ] Iterate upgrade errors until green
- [ ] Verify: `./gradlew assembleDebug` PASS
- [ ] Commit: `build!: upgrade to AGP 8.7.3/Gradle 8.9, SDK 36, modern manifest`

### Task 3: Full rename to fichubmiddleware

**Files:** `git mv app/src/main/java/in/rahulja/ficsavemiddleware app/src/main/java/in/rahulja/fichubmiddleware` (+ same for `app/src/test/...` if exists, `app/src/androidTest/...`), then rewrite package/import statements; Modify `app/build.gradle` namespace → `'in.rahulja.fichubmiddleware'`; layouts `tools:context`; class renames: `FicsaveMiddlewareApplication`→`FicHubMiddlewareApplication`, `FicsaveDownloadListener`→`FicHubDownloadListener`, `FicsaveWebViewClient`→`FicHubWebViewClient`, `FicsaveWebChromeClient`→`FicHubWebChromeClient`. Manifest activity refs updated. Keep `fm_download_history.json` filename constant.

Steps:
- [ ] Move directories, update all package/import statements
- [ ] Namespace + manifest + layout tools:context updates
- [ ] Verify: clean `./gradlew assembleDebug testDebugUnitTest lintDebug` PASS
- [ ] Commit: `refactor!: rename ficsavemiddleware to fichubmiddleware`

### Task 4: `FicHubUrls` utility (TDD)

**Files:** Create `app/src/main/java/in/rahulja/fichubmiddleware/FicHubUrls.java` + `app/src/test/java/in/rahulja/fichubmiddleware/FicHubUrlsTest.java`.

Interfaces produced: `public static final String HOST = "fichub.net"`, `public static String home()`, `public static String autoExport(String ficUrl)` (throws IllegalArgumentException on null/empty).

Steps:
- [ ] Write failing tests first: home() returns `https://fichub.net/`; autoExport encodes fic URL with query params (`https://www.fanfiction.net/s/123/1/Title?param=1&x=y` → q param percent-encoded, whole URL preserved when decoded); throws on null and empty
- [ ] Run: `./gradlew testDebugUnitTest --tests '*FicHubUrlsTest*'` expect FAIL (class missing)
- [ ] Implement:
```java
public final class FicHubUrls {
  public static final String HOST = "fichub.net";
  private static final String BASE = "https://" + HOST;

  private FicHubUrls() { }

  public static String home() { return BASE + "/"; }

  public static String autoExport(String ficUrl) {
    if (ficUrl == null || ficUrl.isEmpty()) {
      throw new IllegalArgumentException("ficUrl required");
    }
    return BASE + "/?b=1&q=" + Uri.encode(ficUrl, null);
  }
}
```
- [ ] Run tests → PASS
- [ ] Commit: `feat: FicHub auto-export URL builder with unit tests`

### Task 5: MainActivity + clients rewired to FicHub

**Files:** Modify `MainActivity.java`, `FicHubWebViewClient.java`, `FicHubWebChromeClient.java`, `res/values/strings.xml`, `res/values/arrays.xml`.

Core changes:
- [ ] strings.xml: `ficsave_host` → `fichub_host` = `fichub.net`; delete `ficsave_download_url`; app_name → "FicHub Middleware"; filter_title_open_ficsave_site → filter_title_open_fichub_site = "FicHub Middleware"; privacy_policy_html text updated (mentions fichub.net, no tracking)
- [ ] arrays.xml FileTypes entries → EPUB/MOBI/PDF/Zipped HTML, values epub/mobi/pdf/html (default epub)
- [ ] MainActivity: delete `getJsScript()` and old fill/submit logic; `setIntentFicUrl` extracts FIRST url match then breaks; `init()` loads `FicHubUrls.autoExport(ficUrl)` when sharing a fic else `home()`; skip reload when webView already on same URL
- [ ] Replace deprecated callbacks (minSdk 24 ⇒ API 24 signatures exist): `shouldOverrideUrlLoading(WebView, WebResourceRequest)` — host equals `FicHubUrls.HOST` (or endsWith(".fichub.net")) → false (load in webview); otherwise ACTION_VIEW browser intent + true. `onReceivedError(WebResourceRequest req, WebResourceError err)` logging variant.
- [ ] Format auto-click injection from onPageFinished, once per pending share (`pendingAutoFormat` boolean field cleared after injection):
```java
private String buildAutoClickJs(String format) {
  return "(function(){var want='" + format + "',tries=0;"
      + "function go(){var as=document.querySelectorAll('#i a');"
      + "for(var i=0;i<as.length;i++){var t=as[i].textContent||'';"
      + "if((want=='epub'&&/EPUB/i.test(t))||(want=='html'&&/zipped HTML/i.test(t))"
      + "||(want=='mobi'&&/MOBI/i.test(t))||(want=='pdf'&&/PDF/i.test(t))){as[i].click();return;}}"
      + "if(++tries<60)setTimeout(go,1000);}go();})()";
}
```
Inject via `webView.evaluateJavascript(js, null)` after page finished when pendingAutoFormat true AND current URL contains `q=` param. format = prefs FILE_TYPES_PREFERENCE value (epub default).
- [ ] Remove WRITE_EXTERNAL_STORAGE runtime request for SDK >= 29 (`prepare()` gates permission flow behind `Build.VERSION.SDK_INT <= Build.VERSION_CODES.P`)
- [ ] Verify: `./gradlew assembleDebug testDebugUnitTest lintDebug` PASS
- [ ] Commit: `feat!: migrate core flow from ficsave.xyz to fichub.net auto-export`

### Task 6: Download listener hardening + history store (TDD)

**Files:** Rewrite `FicHubDownloadListener.java`; Create `DownloadHistoryStore.java` (+ `app/src/test/java/in/rahulja/fichubmiddleware/DownloadHistoryStoreTest.java` Robolectric).

Interfaces produced: `DownloadHistoryStore(File file)` with `List<Entry> read()` and `void append(String name, String path, String datetime)` capped at MAX_ENTRIES=100 (drops oldest); `Entry` = simple holder (name, path, datetime getters). Uses `DOWNLOAD_HISTORY_FILENAME` moved here (was on DownloadHistoryActivity — keep constant name/value `fm_download_history.json`).

Steps:
- [ ] Failing Robolectric tests first (@RunWith(RobolectricTestRunner.class)): append+read round-trip preserves order; cap at 100 drops oldest; corrupt/nonexistent file read → empty list, no throw
- [ ] Implement store with org.json JSONArray
- [ ] Listener rewrite: uses DownloadHistoryStore; receiver registered via `ContextCompat.registerReceiver(ctx, onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED)`; unregister inside try/finally in onReceive; keep id-guard against double fire
- [ ] Add `Intent.FLAG_GRANT_READ_URI_PERMISSION` to open-file and email intents
- [ ] Verify: `./gradlew testDebugUnitTest` PASS, assembleDebug PASS
- [ ] Commit: `fix: download receiver lifecycle, URI grants, extract capped history store`

### Task 7: Settings → androidx.preference

**Files:** Modify `FicSettingsActivity.java`, `SettingsFragment.java`, `res/values/styles.xml`, `res/values-v21/styles.xml`, `res/xml/preferences.xml`.

Changes:
- [ ] preferences.xml: remove `send_email_site_preference` SwitchPreference entirely; ListPreference key `file_types_preference` keeps key/defaultValue changed to `epub`; About section unchanged keys
- [ ] `FicSettingsActivity extends AppCompatActivity`, hosts `SettingsFragment extends PreferenceFragmentCompat` via getSupportFragmentManager replacing android.R.id.content
- [ ] styles: AppTheme gains `<item name="preferenceTheme">@style/PreferenceThemeOverlay</item>` (both values/ and values-v21/)
- [ ] Remove site-email coupling logic; preserve email validation + device-email/open-file mutual exclusion (open_file on ⇒ send_email_device off)
- [ ] Kill removeAll()+re-add rebuild jank — update summaries directly via findPreference
- [ ] Version check: single-thread executor GETs `https://api.github.com/repos/xRahul/FicsaveMiddleware/releases/latest` with `Accept: application/vnd.github+json`, parses `tag_name`, compares to BuildConfig.VERSION_NAME, posts summary via runOnUiThread; failure silently logs
- [ ] Drop deprecated `Environment.getExternalStorageDirectory()` download-folder summary → static text "Documents"
- [ ] Verify: `./gradlew assembleDebug testDebugUnitTest lintDebug` PASS
- [ ] Commit: `refactor: migrate preferences to androidx.preference, drop dead site-email feature`

### Task 8: Instrumented test refresh

**Files:** Rewrite `app/src/androidTest/java/in/rahulja/fichubmiddleware/ExampleInstrumentedTest.java` → `SmokeTest.java`.

Steps:
- [ ] Use `androidx.test.ext.junit.runners.AndroidJUnit4` + `InstrumentationRegistry.getInstrumentation().getTargetContext()`; assert package name is `in.rahulja.ficsavemiddleware` (applicationId!) and ActivityScenario launches MainActivity with webview present
- [ ] Verify: `./gradlew help` resolves test deps (connected run happens in CI emulator job / Task 13)
- [ ] Commit: `test: modernize instrumented smoke test to androidx.test`

### Task 9: Lint + version gate

Steps:
- [ ] Fix lint errors (UnusedResources from removed features, NewApi)
- [ ] Confirm versionCode 300 / versionName 3.0.0
- [ ] Full gate: `./gradlew clean assembleDebug assembleRelease testDebugUnitTest lintDebug` all green (release unsigned OK)
- [ ] Commit: `chore: lint fixes, bump to 3.0.0`

### Task 10: GitHub Actions CI + Dependabot

**Files:** Create `.github/workflows/ci.yml`, `.github/dependabot.yml`.

ci.yml content:
```yaml
name: CI
on:
  push: { branches: [master] }
  pull_request: {}
jobs:
  build-test-lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17' }
      - uses: gradle/actions/setup-gradle@v4
      - run: ./gradlew assembleDebug testDebugUnitTest lintDebug --stacktrace
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: reports-apk
          path: |
            app/build/outputs/apk/debug/app-debug.apk
            app/build/reports/
```
Dependabot: version 2, gradle + github-actions ecosystems, weekly.
- [ ] Verify YAML parses (yamllint or python yaml.safe_load)
- [ ] Commit: `ci: GitHub Actions build/test/lint + dependabot`

### Task 11: Release workflow

**Files:** Create `.github/workflows/release.yml`; Modify README signing section placeholder (full docs in Task 12).

release.yml:
- [ ] Trigger `on: push: tags: ['v*']`
- [ ] Steps: checkout, setup-java 17, setup-gradle, decode secret `KEYSTORE_BASE64` → `keystore.jks` (guarded: only if secret present), `assembleRelease` with env `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` wired into signingConfig read from env (add to app/build.gradle release buildType, guarded so absent env vars still produce unsigned APK)
- [ ] Attach `app-release.apk` to GitHub Release via softprops/action-gh-release@v2 with tag_name
- [ ] Commit: `ci: signed release pipeline on tags`

### Task 12: Docs

**Files:** Rewrite `README.md`.

Content requirements:
- Badges: GH Actions CI, GitHub release, license (drop Travis/Codacy/CodeClimate/Sonar/codebeat/libraries.io badges)
- What it does: wrapper around https://fichub.net/ (successor to ficsave.xyz) exporting fanfiction to EPUB/MOBI/PDF/Zipped HTML
- Features list (updated), supported sites (from fichub.net: SpaceBattles/SufficientVelocity/QuestionableQuesting, FFN/FictionPress, AO3, HPFA, SIYE, AdultFanfiction, Worm/Ward), screenshots section (existing Screenshots/screens.jpg), requirements (JDK 17, Android SDK 36), build/test commands, release/signing secrets table (KEYSTORE_BASE64, SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, SIGNING_KEY_PASSWORD), privacy note, credits (original ficsave.xyz by waylaidwanderer; FicHub backend github.com/FicHub/fichub.net; note about renaming the GitHub repo optional step for owner)
- [ ] Commit: `docs: rewrite README for FicHub backend and modern toolchain`

### Task 13: Final verification gate (QA pass)

Steps:
- [ ] Fresh-clone simulation: `git stash -u` n/a → `git clone . /tmp/opencode/fcm-verify && cd there && ./gradlew clean assembleDebug testDebugUnitTest lintDebug` green
- [ ] Emulator/device run `connectedDebugAndroidTest` if device available; else record skipped-with-reason
- [ ] Manual QA checklist executed & results documented in report: cold start/terms-once; share FFN/AO3/SpaceBattles URL → export auto-runs → preferred format downloads to Documents; open-file pref opens reader; device-email attaches; history lists entry + persists; settings format/email validation; fichub.net deep link opens app; external link exits to browser; download notification appears
- [ ] Fix findings; final commit: `chore: verification fixes`

## Self-review notes

- Spec coverage: all user asks mapped to tasks (feature working=Tasks 4-6; modernize=2,3; security=2,5,6; speed=singleTask/no-rebuild-jank; CI/CD=10,11; docs=12; tests=4,6,8,13; rename=3).
- Type consistency: `FicHubUrls.HOST` referenced in Task 5 client logic; `DownloadHistoryStore` filename constant moves to store class; applicationId string appears in Task 8 assertion deliberately.
