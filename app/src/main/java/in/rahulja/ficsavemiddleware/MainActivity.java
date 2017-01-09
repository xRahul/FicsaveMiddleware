package in.rahulja.ficsavemiddleware;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    Context mContext;
    Activity mActivity;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;
        mActivity = this;

        final WebView mWebview = (WebView) findViewById(R.id.main_webview);
        final String ficUrl = "random fic url";

        if (mWebview != null) {

            mWebview.setDownloadListener(new DownloadListener() {
                public void onDownloadStart(String url, String userAgent,
                                            String contentDisposition, String mimetype,
                                            long contentLength) {


//                    String fileName = contentDisposition.substring(21).replaceAll("^\"|\"$", "");
                    String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);

                    if (ContextCompat.checkSelfPermission(mContext,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(mActivity,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
                    }

                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    ); //Notify client once download is completed!
                    request.setDestinationInExternalPublicDir(
                            Environment.DIRECTORY_DOWNLOADS, fileName);
                    request.setMimeType(mimetype);
                    request.setTitle(fileName);

                    String cookies = CookieManager.getInstance().getCookie(url);
                    request.addRequestHeader("cookie", cookies);
                    request.addRequestHeader("User-Agent", userAgent);

                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT); //This is important!
                    intent.addCategory(Intent.CATEGORY_OPENABLE); //CATEGORY.OPENABLE
                    intent.setType("*/*");//any application,any extension
                    Toast.makeText(getApplicationContext(), "Downloading File", //To notify the Client that the file is being downloaded
                            Toast.LENGTH_LONG).show();

                }
            });

            mWebview.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    if (!url.contains("http://ficsave.xyz/download/")) {
                        final String js =
                                "document.getElementById('url').value = \"" + ficUrl + "\"; " +
                                        "document.getElementsByClassName('select-dropdown')[0].value = \"MOBI\";" +
                                        "document.getElementsByClassName('dropdown-content select-dropdown')[0].getElementsByTagName('li')[0].className = \"\";" +
                                        "document.getElementsByClassName('dropdown-content select-dropdown')[0].getElementsByTagName('li')[1].className = \"active\";" +
                                        "document.getElementsByName('format')[0].value = \"mobi\";" +
                                        "document.getElementById(\"download-submit\").click();";

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                mWebview.evaluateJavascript(js, new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        Toast.makeText(mContext, value, LENGTH_SHORT).show();
                                    }

                                });
                            }
                        }, 2000);
                    }
                }
            });
            WebSettings webSettings = mWebview.getSettings();
            webSettings.setJavaScriptEnabled(true);
            mWebview.loadUrl("http://ficsave.xyz");
        }
    }
}
