package com.repkap11.browserwrapper;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class BrowserWebViewActivity extends AppCompatActivity {
    private static final String TAG = BrowserWebViewActivity.class.getSimpleName();

    public static final String EXTRA_URL = "url";

    private FrameLayout fullScreenContainer;
    private View customView;

    private WebChromeClient.CustomViewCallback customViewCallback;
    private CookieManager mCookieManager;
    private WebView mWebView;
    private Intent mPendingDownloadIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCookieManager = CookieManager.getInstance();
        mCookieManager.setAcceptCookie(true);

        InsetHelper.activityOnCreate(this, false, true);


        setContentView(R.layout.activity_browser_webview);
        mWebView = findViewById(R.id.webview);

        mWebView.setHorizontalScrollBarEnabled(false);
        mWebView.setVerticalScrollBarEnabled(false);
        fullScreenContainer = findViewById(R.id.fullscreen_container);
        InsetHelper.setOnApplyWindowInsetsListener(mWebView, InsetHelper.ALL);

        setupWebView(mWebView);
        setupBackPressed(mWebView);

        showSystemBars(null);

        String url = getIntent().getStringExtra(EXTRA_URL);
        // Load the URL
        if (url == null) {
            Log.e(TAG, "onCreate: No url set");
            return;
        }
        mWebView.loadUrl(url);
    }

    @Override
    protected void onPause() {
        mCookieManager.flush();
        super.onPause();
    }

    private void setupWebView(WebView webView) {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setMediaPlaybackRequiresUserGesture(true);

        // Standard WebViewClient to keep navigation inside the app
        webView.setWebViewClient(new WebViewClient());

        // 2) WebChromeClient for Full Screen support
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                // If a view is already shown, terminate the new one
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                customView = view;
                customViewCallback = callback;

                // Hide WebView, Show FullScreen Container
                webView.setVisibility(View.GONE);
                fullScreenContainer.setVisibility(View.VISIBLE);
                view.setKeepScreenOn(true);
                fullScreenContainer.addView(view);

                // Hide System Bars (Immersive Mode)
                hideSystemBars();
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;

                // Remove the custom view
                fullScreenContainer.removeView(customView);
                customView = null;

                // Hide Container, Show WebView
                fullScreenContainer.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);

                // Notify callback
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }

                // Show System Bars again
                showSystemBars(null);
            }
        });
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Intent intent = new Intent(BrowserWebViewActivity.this, DownloadService.class);
                String fileName = getSmartFileName(url, contentDisposition, mimetype);
                intent.putExtra("url", url);
                intent.putExtra("fileName", fileName);
                intent.putExtra("mimeType", mimetype);
                intent.putExtra("contentLength", contentLength);
                Log.i(TAG, "downloadFileWithProgress: Download contentDisposition:" + contentDisposition);
                Log.i(TAG, "downloadFileWithProgress: Download mimetype:" + mimetype);
                Log.i(TAG, "downloadFileWithProgress: Download file:" + fileName + " size:" + contentLength);


                checkNotificationPermissionAndDownload(intent);
            }
        });
    }

    public String getSmartFileName(String url, String contentDisposition, String mimeType) {
        // 1. Try to get the filename from URLUtil first
        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);

        // 2. Check if it ended up with .bin or is missing an extension
        if (fileName.endsWith(".bin") || !fileName.contains(".")) {
            // Look at the actual URL path (e.g., https://example.com/video.mp4)
            String urlPath = Uri.parse(url).getLastPathSegment();

            if (urlPath != null && urlPath.contains(".")) {
                String extension = urlPath.substring(urlPath.lastIndexOf("."));

                // 3. If the URL has a valid-looking extension, swap it
                // This fixes "video.bin" -> "video.mp4"
                if (fileName.contains(".")) {
                    fileName = fileName.substring(0, fileName.lastIndexOf(".")) + extension;
                } else {
                    fileName = fileName + extension;
                }
            }
        }
        return fileName;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult() called with: requestCode = [" + requestCode + "], permissions = [" + permissions + "], grantResults = [" + grantResults + "]");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled! You'll see download progress.", Toast.LENGTH_SHORT).show();
                if (mPendingDownloadIntent != null) {
                    Intent intent = mPendingDownloadIntent;
                    mPendingDownloadIntent = null;
                    startServiceSafely(intent);
                }
            } else {
                Toast.makeText(this, "Note: You won't see download progress in the notification bar.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void checkNotificationPermissionAndDownload(Intent downloadIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                mPendingDownloadIntent = downloadIntent;
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            } else {
                // Permission already granted, start the service
                startServiceSafely(downloadIntent);
            }
        } else {
            // API < 33, no runtime permission needed
            startServiceSafely(downloadIntent);
        }
    }

    private void startServiceSafely(Intent intent) {
        String fileName = intent.getStringExtra("fileName");
        Toast.makeText(BrowserWebViewActivity.this, "Download: " + fileName, Toast.LENGTH_SHORT).show();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    private void setupBackPressed(WebView webView) {
        // 1) Modern OnBackPressedCallback
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Priority 1: Exit full screen video if active
                if (customView != null) {
                    webView.getWebChromeClient().onHideCustomView();
                }
                // Priority 2: Navigate back in WebView history
                else if (webView.canGoBack()) {
                    webView.goBack();
                }
                // Priority 3: Standard system back (close activity)
                else {
                    setEnabled(false); // Disable this callback
                    getOnBackPressedDispatcher().onBackPressed(); // Call default behavior
                }
            }
        });
    }

    // Helper to hide bars for full screen experience
    private void hideSystemBars() {
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
    }

    // Helper to restore bars
    private void showSystemBars(@Nullable Configuration config) {
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (config == null) {
            config = getResources().getConfiguration();
        }

        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            windowInsetsController.show(WindowInsetsCompat.Type.navigationBars());
            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            windowInsetsController.hide(WindowInsetsCompat.Type.statusBars());
        } else {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
            windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (customView == null) {
            showSystemBars(newConfig);
        }
    }

}