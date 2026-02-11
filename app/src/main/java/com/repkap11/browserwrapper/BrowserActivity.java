package com.repkap11.browserwrapper;

import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
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

import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.BuildConfig;
import org.mozilla.geckoview.ContentBlocking;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;
import org.mozilla.geckoview.WebNotification;
import org.mozilla.geckoview.WebNotificationDelegate;
import org.webrtc.CryptoOptions;

public class BrowserActivity extends AppCompatActivity {
    private static final String TAG = BrowserActivity.class.getSimpleName();

    public static final String EXTRA_URL = "url";
    private static final boolean USE_GECKO = true;
    private View mBaseView;

    private FrameLayout fullScreenContainer;
    private View customView;

    private WebChromeClient.CustomViewCallback customViewCallback;
    private CookieManager mCookieManager;

    private static GeckoRuntime sRuntime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCookieManager = CookieManager.getInstance();
        mCookieManager.setAcceptCookie(true);

        InsetHelper.activityOnCreate(this, false, true);

        WebView webView = null;
        GeckoView geckoView = null;

        if (USE_GECKO) {
            setContentView(R.layout.activity_browser_gecko);
            geckoView = findViewById(R.id.gecko_view);
            GeckoSession session = new GeckoSession();
            session.setContentDelegate(new GeckoSession.ContentDelegate() {
            });
            if (sRuntime == null) {
                // GeckoRuntime can only be initialized once per process

                sRuntime = GeckoRuntime.create(this);
                sRuntime.getWebExtensionController().setExtensionProcessDelegate(new WebExtensionController.ExtensionProcessDelegate() {
                    @Override
                    public void onDisabledProcessSpawning() {
                        Log.d(TAG, "onDisabledProcessSpawning() called");
                        WebExtensionController.ExtensionProcessDelegate.super.onDisabledProcessSpawning();
                    }
                });
                sRuntime.getWebExtensionController().setDebuggerDelegate(new WebExtensionController.DebuggerDelegate() {
                    @Override
                    public void onExtensionListUpdated() {
                        Log.d(TAG, "onExtensionListUpdated() called");
                        WebExtensionController.DebuggerDelegate.super.onExtensionListUpdated();
                    }
                });
                sRuntime.setWebNotificationDelegate(new WebNotificationDelegate() {
                    @Override
                    public void onShowNotification(@NonNull WebNotification notification) {
                        Log.d(TAG, "onShowNotification() called with: notification = [" + notification + "]");
                        WebNotificationDelegate.super.onShowNotification(notification);
                    }

                    @Override
                    public void onCloseNotification(@NonNull WebNotification notification) {
                        Log.d(TAG, "onCloseNotification() called with: notification = [" + notification + "]");
                        WebNotificationDelegate.super.onCloseNotification(notification);
                    }
                });
                sRuntime.setDelegate(new GeckoRuntime.Delegate() {
                    @Override
                    public void onShutdown() {
                        Log.d(TAG, "onShutdown() called");
                    }
                });
                sRuntime.getWebExtensionController().setPromptDelegate(new WebExtensionController.PromptDelegate() {
                    @Nullable
                    @Override
                    public GeckoResult<WebExtension.PermissionPromptResponse> onInstallPromptRequest(@NonNull WebExtension extension, @NonNull String[] permissions, @NonNull String[] origins, @NonNull String[] dataCollectionPermissions) {
//                    return WebExtensionController.PromptDelegate.super.onInstallPromptRequest(extension, permissions, origins, dataCollectionPermissions);
                        Log.d(TAG, "onInstallPromptRequest() called with: extension = [" + extension + "], permissions = [" + permissions + "], origins = [" + origins + "], dataCollectionPermissions = [" + dataCollectionPermissions + "]");
                        return GeckoResult.fromValue(new WebExtension.PermissionPromptResponse(true, true, true));
                    }

                    @Nullable
                    @Override
                    public GeckoResult<AllowOrDeny> onUpdatePrompt(@NonNull WebExtension extension, @NonNull String[] newPermissions, @NonNull String[] newOrigins, @NonNull String[] newDataCollectionPermissions) {
                        Log.d(TAG, "onUpdatePrompt() called with: extension = [" + extension + "], newPermissions = [" + newPermissions + "], newOrigins = [" + newOrigins + "], newDataCollectionPermissions = [" + newDataCollectionPermissions + "]");
                        return WebExtensionController.PromptDelegate.super.onUpdatePrompt(extension, newPermissions, newOrigins, newDataCollectionPermissions);
                    }

                    @Nullable
                    @Override
                    public GeckoResult<AllowOrDeny> onOptionalPrompt(@NonNull WebExtension extension, @NonNull String[] permissions, @NonNull String[] origins, @NonNull String[] dataCollectionPermissions) {
                        Log.d(TAG, "onOptionalPrompt() called with: extension = [" + extension + "], permissions = [" + permissions + "], origins = [" + origins + "], dataCollectionPermissions = [" + dataCollectionPermissions + "]");
                        return WebExtensionController.PromptDelegate.super.onOptionalPrompt(extension, permissions, origins, dataCollectionPermissions);
                    }
                });

            }
            session.open(sRuntime);
            geckoView.setSession(session);

            Log.i(TAG, "onCreate: Loading extension");

            sRuntime.getWebExtensionController().ensureBuiltIn("resource://android/assets/ublock_origin/", "ublock_origin").accept(new GeckoResult.Consumer<WebExtension>() {
                //            sRuntime.getWebExtensionController().ensureBuiltIn("resource://android/assets/add_skipper/", "add_skipper").accept(new GeckoResult.Consumer<WebExtension>() {
                @Override
                public void accept(@Nullable WebExtension webExtension) {
                    Log.i(TAG, "accept: Good");
                }
            }, new GeckoResult.Consumer<Throwable>() {
                @Override
                public void accept(@Nullable Throwable throwable) {
                    Log.i(TAG, "accept: Bad:" + (throwable != null ? throwable.getMessage() : null));
                }
            });

            mBaseView = geckoView;

        } else {
            setContentView(R.layout.activity_browser_webview);
            webView = findViewById(R.id.webview);
            mBaseView = webView;
        }
        mBaseView.setHorizontalScrollBarEnabled(false);
        mBaseView.setVerticalScrollBarEnabled(false);
        fullScreenContainer = findViewById(R.id.fullscreen_container);
        InsetHelper.setOnApplyWindowInsetsListener(mBaseView, InsetHelper.ALL);
        if (USE_GECKO) {
        } else {
            setupWebView(webView);
            setupBackPressed(webView);
        }
        showSystemBars(null);

        String url = getIntent().getStringExtra(EXTRA_URL);
        // Load the URL
        if (url == null) {
            Log.e(TAG, "onCreate: No url set");
            return;
        }

        if (USE_GECKO) {
            geckoView.getSession().loadUri(url);
        } else {
            webView.loadUrl(url);
        }

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