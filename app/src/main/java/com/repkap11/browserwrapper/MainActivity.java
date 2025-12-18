package com.repkap11.browserwrapper;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private FrameLayout fullScreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    // Target URL
    private static final String TARGET_URL = "https://repkap11.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InsetHelper.activityOnCreate(this, false, true);

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webview);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setVerticalScrollBarEnabled(false);

        fullScreenContainer = findViewById(R.id.fullscreen_container);

        InsetHelper.setOnApplyWindowInsetsListener(webView, InsetHelper.ALL);
//        setupWindowInsets();
        setupWebView();
        setupBackPressed();

        showSystemBars(null);
        // Load the URL
        webView.loadUrl(TARGET_URL);
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);

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
    }

    private void setupBackPressed() {
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