package com.repkap11.browserwrapper;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
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
        fullScreenContainer = findViewById(R.id.fullscreen_container);

        InsetHelper.setOnApplyWindowInsetsListener(webView, InsetHelper.ALL);
//        setupWindowInsets();
        setupWebView();
        setupBackPressed();

        // Load the URL
        webView.loadUrl(TARGET_URL);
    }

    private void setupWindowInsets() {
        // Apply insets to the WebView so content isn't hidden behind system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.webview), (v, windowInsets) -> {
            // Only apply insets if we are NOT in full screen mode
            if (customView == null) {
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
                v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
            }
            return WindowInsetsCompat.CONSUMED;
        });
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
                showSystemBars();
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
//        WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
//        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
    }

    // Helper to restore bars
    private void showSystemBars() {
//        WindowInsetsControllerCompat windowInsetsController = ViewCompat.getWindowInsetsController(getWindow().getDecorView());
//        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        WindowInsetsControllerCompat windowInsetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());

        windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
    }

//    private void hideSystemBars() {
//        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
//        windowInsetsController.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
//        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars());
//    }

//    private void showSystemBars() {
//        WindowInsetsControllerCompat windowInsetsController = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
//        windowInsetsController.show(WindowInsetsCompat.Type.systemBars());
//    }


}