package com.repkap11.browserwrapper;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.PointerIcon;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.Autocomplete;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.OrientationController;
import org.mozilla.geckoview.SlowScriptResponse;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;
import org.mozilla.geckoview.WebNotification;
import org.mozilla.geckoview.WebNotificationDelegate;
import org.mozilla.geckoview.WebRequestError;
import org.mozilla.geckoview.WebResponse;

import java.util.List;

public class BrowserGeckoActivity extends AppCompatActivity {
    private static final String TAG = BrowserGeckoActivity.class.getSimpleName();

    public static final String EXTRA_URL = BrowserWebViewActivity.EXTRA_URL;
    public static final String EXTRA_LIMIT_HORIZONTAL_SCROLL = BrowserWebViewActivity.EXTRA_LIMIT_HORIZONTAL_SCROLL;

    private static GeckoRuntime sRuntime;
    private GeckoView mGeckoView;
    private GeckoSession mGeckoSession;
    private OnBackPressedCallback mOnBackPressedCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String url = getIntent().getStringExtra(EXTRA_URL);
        boolean limit_h_scroll = getIntent().getBooleanExtra(EXTRA_LIMIT_HORIZONTAL_SCROLL, true);

        InsetHelper.activityOnCreate(this, false, true);
        setContentView(R.layout.activity_browser_gecko);
        mGeckoView = findViewById(R.id.gecko_view);

        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(android.R.id.background, typedValue, true);
        int themeBackgroundColor = typedValue.data;

        // If the above doesn't yield a result, you can fallback to a hardcoded dark grey
        if (themeBackgroundColor == Color.WHITE || themeBackgroundColor == 0) {
            themeBackgroundColor = Color.parseColor("#121212");
        }

        mGeckoView.coverUntilFirstPaint(themeBackgroundColor);
        mGeckoView.setActivityContextDelegate(new GeckoView.ActivityContextDelegate() {
            @Nullable
            @Override
            public Context getActivityContext() {
                return BrowserGeckoActivity.this;
            }
        });

        GeckoSessionSettings settings = new GeckoSessionSettings.Builder()
                .usePrivateMode(true)
                .useTrackingProtection(true)
                .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
//                .userAgentOverride("")
                .suspendMediaWhenInactive(false)
                .allowJavascript(true)
                .build();
        mGeckoSession = new GeckoSession(settings);
        mGeckoSession.setContentDelegate(new MyContentDelegate());
        mGeckoSession.setNavigationDelegate(new MyNavigationDelegate());
        if (sRuntime == null) {
            setupRuntime(this);
        }
        mGeckoSession.open(sRuntime);
        mGeckoView.setSession(mGeckoSession);
        Log.i(TAG, "onCreate: Loading extension");

        WebExtensionController wec = sRuntime.getWebExtensionController();
        if (limit_h_scroll) {
            wec.ensureBuiltIn("resource://android/assets/h_scroll_blocker/", "h_scroll_blocker").accept(new GeckoResult.Consumer<WebExtension>() {
                @Override
                public void accept(@Nullable WebExtension webExtension) {
                    Log.i(TAG, "h_scroll_blocker accept: Good");
                }
            }, new GeckoResult.Consumer<Throwable>() {
                @Override
                public void accept(@Nullable Throwable throwable) {
                    Log.i(TAG, "h_scroll_blocker accept: Bad:" + (throwable != null ? throwable.getMessage() : null));
                }
            });
        }

        wec.ensureBuiltIn("resource://android/assets/ublock_origin/", "ublock_origin").accept(new GeckoResult.Consumer<WebExtension>() {
            //            sRuntime.getWebExtensionController().ensureBuiltIn("resource://android/assets/add_skipper/", "add_skipper").accept(new GeckoResult.Consumer<WebExtension>() {
            @Override
            public void accept(@Nullable WebExtension webExtension) {
                Log.i(TAG, "ublock_origin accept: Good");
            }
        }, new GeckoResult.Consumer<Throwable>() {
            @Override
            public void accept(@Nullable Throwable throwable) {
                Log.i(TAG, "ublock_origin accept: Bad:" + (throwable != null ? throwable.getMessage() : null));
            }
        });

        mOnBackPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mGeckoSession.goBack();
            }
        };
        getOnBackPressedDispatcher().addCallback(mOnBackPressedCallback);
        mGeckoView.setHorizontalScrollBarEnabled(false);
        mGeckoView.setVerticalScrollBarEnabled(false);

        InsetHelper.setOnApplyWindowInsetsListener(mGeckoView, InsetHelper.ALL);


        // Load the URL
        if (url == null) {
            Log.e(TAG, "onCreate: No url set");
            return;
        }
        mGeckoView.getSession().loadUri(url);
    }

    private static void setupRuntime(Context context) {
        // GeckoRuntime can only be initialized once per process

        GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder()
                .allowInsecureConnections(GeckoRuntimeSettings.ALLOW_ALL)
                .consoleOutput(true)
                .javaScriptEnabled(true)
                .extensionsProcessEnabled(true)
                .preferredColorScheme(GeckoRuntimeSettings.COLOR_SCHEME_DARK)
                .build();

        sRuntime = GeckoRuntime.create(context, settings);
        sRuntime.getWebExtensionController().setExtensionProcessDelegate(new MyExtensionProcessDelegate());
        sRuntime.getOrientationController().setDelegate(new MyOrientationDelegate());
        sRuntime.getWebExtensionController().setDebuggerDelegate(new MyDebuggerDelegate());
        sRuntime.setWebNotificationDelegate(new MyWebNotificationDelegate());
        sRuntime.setDelegate(new MyDelegate());
        sRuntime.getWebExtensionController().setPromptDelegate(new MyPromptDelegate());
        sRuntime.setServiceWorkerDelegate(new MyServiceWorkerDelegate());
        sRuntime.setAutocompleteStorageDelegate(new MyStorageDelegate());
        sRuntime.setActivityDelegate(new GeckoRuntime.ActivityDelegate() {
            @Nullable
            @Override
            public GeckoResult<Intent> onStartActivityForResult(@NonNull PendingIntent intent) {
                Log.d(TAG, "onStartActivityForResult() called with: intent = [" + intent + "]");
                return null;
            }
        });
    }

    @Override
    protected void onDestroy() {
        mGeckoSession.close();
        super.onDestroy();
    }

    private static class MyDebuggerDelegate implements WebExtensionController.DebuggerDelegate {
        @Override
        public void onExtensionListUpdated() {
            Log.d(TAG, "onExtensionListUpdated() called");
            WebExtensionController.DebuggerDelegate.super.onExtensionListUpdated();
        }
    }

    private static class MyExtensionProcessDelegate implements WebExtensionController.ExtensionProcessDelegate {
        @Override
        public void onDisabledProcessSpawning() {
            Log.d(TAG, "onDisabledProcessSpawning() called");
            WebExtensionController.ExtensionProcessDelegate.super.onDisabledProcessSpawning();
        }
    }

    private static class MyWebNotificationDelegate implements WebNotificationDelegate {
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
    }

    private static class MyDelegate implements GeckoRuntime.Delegate {
        @Override
        public void onShutdown() {
            Log.d(TAG, "onShutdown() called");
        }
    }

    private static class MyPromptDelegate implements WebExtensionController.PromptDelegate {
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
    }

    private static class MyOrientationDelegate implements OrientationController.OrientationDelegate {
        @Nullable
        @Override
        public GeckoResult<AllowOrDeny> onOrientationLock(@NonNull int aOrientation) {
            Log.d(TAG, "onOrientationLock() called with: aOrientation = [" + aOrientation + "]");
            return OrientationController.OrientationDelegate.super.onOrientationLock(aOrientation);
        }

        @Nullable
        @Override
        public void onOrientationUnlock() {
            Log.d(TAG, "onOrientationUnlock() called");
            OrientationController.OrientationDelegate.super.onOrientationUnlock();
        }

    }

    private static class MyServiceWorkerDelegate implements GeckoRuntime.ServiceWorkerDelegate {
        @NonNull
        @Override
        public GeckoResult<GeckoSession> onOpenWindow(@NonNull String url) {
            Log.d(TAG, "onOpenWindow() called with: url = [" + url + "]");
            return null;
        }
    }

    private static class MyStorageDelegate implements Autocomplete.StorageDelegate {
        @Nullable
        @Override
        public GeckoResult<Autocomplete.LoginEntry[]> onLoginFetch(@NonNull String domain) {
            return Autocomplete.StorageDelegate.super.onLoginFetch(domain);
        }

        @Nullable
        @Override
        public GeckoResult<Autocomplete.LoginEntry[]> onLoginFetch() {
            return Autocomplete.StorageDelegate.super.onLoginFetch();
        }

        @Nullable
        @Override
        public GeckoResult<Autocomplete.CreditCard[]> onCreditCardFetch() {
            return Autocomplete.StorageDelegate.super.onCreditCardFetch();
        }

        @Nullable
        @Override
        public GeckoResult<Autocomplete.Address[]> onAddressFetch() {
            return Autocomplete.StorageDelegate.super.onAddressFetch();
        }

        @Override
        public void onLoginSave(@NonNull Autocomplete.LoginEntry login) {
            Autocomplete.StorageDelegate.super.onLoginSave(login);
        }

        @Override
        public void onCreditCardSave(@NonNull Autocomplete.CreditCard creditCard) {
            Autocomplete.StorageDelegate.super.onCreditCardSave(creditCard);
        }

        @Override
        public void onAddressSave(@NonNull Autocomplete.Address address) {
            Autocomplete.StorageDelegate.super.onAddressSave(address);
        }

        @Override
        public void onLoginUsed(@NonNull Autocomplete.LoginEntry login, int usedFields) {
            Autocomplete.StorageDelegate.super.onLoginUsed(login, usedFields);
        }
    }

    private class MyContentDelegate implements GeckoSession.ContentDelegate {
        @Override
        public void onTitleChange(@NonNull GeckoSession session, @Nullable String title) {
            Log.d(TAG, "onTitleChange() called with: session = [" + session + "], title = [" + title + "]");
            GeckoSession.ContentDelegate.super.onTitleChange(session, title);
        }

        @Override
        public void onPreviewImage(@NonNull GeckoSession session, @NonNull String previewImageUrl) {
            Log.d(TAG, "onPreviewImage() called with: session = [" + session + "], previewImageUrl = [" + previewImageUrl + "]");
            GeckoSession.ContentDelegate.super.onPreviewImage(session, previewImageUrl);
        }

        @Override
        public void onFocusRequest(@NonNull GeckoSession session) {
            Log.d(TAG, "onFocusRequest() called with: session = [" + session + "]");
            GeckoSession.ContentDelegate.super.onFocusRequest(session);
        }

        @Override
        public void onCloseRequest(@NonNull GeckoSession session) {
            Log.d(TAG, "onCloseRequest() called with: session = [" + session + "]");
            GeckoSession.ContentDelegate.super.onCloseRequest(session);
        }

        @Override
        public void onFullScreen(@NonNull GeckoSession session, boolean fullScreen) {
            Log.d(TAG, "onFullScreen() called with: session = [" + session + "], fullScreen = [" + fullScreen + "]");
            if (fullScreen) {
//                getSupportActionBar().hide();
//                mGeckoView.setDynamicToolbarMaxHeight(0);
                WindowUtils.enterImmersiveMode(getWindow());
            } else {
//                getSupportActionBar().show();
//                mGeckoView.setDynamicToolbarMaxHeight(getSupportActionBar().getHeight());
                WindowUtils.exitImmersiveMode(getWindow());
            }
        }

        @Override
        public void onMetaViewportFitChange(@NonNull GeckoSession session, @NonNull String viewportFit) {
            Log.d(TAG, "onMetaViewportFitChange() called with: session = [" + session + "], viewportFit = [" + viewportFit + "]");
            GeckoSession.ContentDelegate.super.onMetaViewportFitChange(session, viewportFit);
        }

        @Override
        public void onContextMenu(@NonNull GeckoSession session, int screenX, int screenY, @NonNull ContextElement element) {
            Log.d(TAG, "onContextMenu() called with: session = [" + session + "], screenX = [" + screenX + "], screenY = [" + screenY + "], element = [" + element + "]");
            GeckoSession.ContentDelegate.super.onContextMenu(session, screenX, screenY, element);
        }

        @Override
        public void onExternalResponse(@NonNull GeckoSession session, @NonNull WebResponse response) {
            Log.d(TAG, "onExternalResponse() called with: session = [" + session + "], response = [" + response + "]");
            GeckoSession.ContentDelegate.super.onExternalResponse(session, response);
        }

        @Override
        public void onCrash(@NonNull GeckoSession session) {
            Log.d(TAG, "onCrash() called with: session = [" + session + "]");
            GeckoSession.ContentDelegate.super.onCrash(session);
        }

        @Override
        public void onKill(@NonNull GeckoSession session) {
            Log.d(TAG, "onKill() called with: session = [" + session + "]");
            GeckoSession.ContentDelegate.super.onKill(session);
        }

        @Override
        public void onFirstComposite(@NonNull GeckoSession session) {
            Log.d(TAG, "onFirstComposite() called with: session = [" + session + "]");
            GeckoSession.ContentDelegate.super.onFirstComposite(session);
        }

        @Override
        public void onFirstContentfulPaint(@NonNull GeckoSession session) {
            Log.d(TAG, "onFirstContentfulPaint() called with: session = [" + session + "]");
            GeckoSession.ContentDelegate.super.onFirstContentfulPaint(session);
        }

        @Override
        public void onPaintStatusReset(@NonNull GeckoSession session) {
            Log.d(TAG, "onPaintStatusReset() called with: session = [" + session + "]");
            GeckoSession.ContentDelegate.super.onPaintStatusReset(session);
        }

        @Override
        public void onPointerIconChange(@NonNull GeckoSession session, @NonNull PointerIcon icon) {
            Log.d(TAG, "onPointerIconChange() called with: session = [" + session + "], icon = [" + icon + "]");
            GeckoSession.ContentDelegate.super.onPointerIconChange(session, icon);
        }

        @Override
        public void onWebAppManifest(@NonNull GeckoSession session, @NonNull JSONObject manifest) {
            Log.d(TAG, "onWebAppManifest() called with: session = [" + session + "], manifest = [" + manifest + "]");
            GeckoSession.ContentDelegate.super.onWebAppManifest(session, manifest);
        }

        @Nullable
        @Override
        public GeckoResult<SlowScriptResponse> onSlowScript(@NonNull GeckoSession geckoSession, @NonNull String scriptFileName) {
            Log.d(TAG, "onSlowScript() called with: geckoSession = [" + geckoSession + "], scriptFileName = [" + scriptFileName + "]");
            return GeckoSession.ContentDelegate.super.onSlowScript(geckoSession, scriptFileName);
        }

        @Override
        public void onShowDynamicToolbar(@NonNull GeckoSession geckoSession) {
            Log.d(TAG, "onShowDynamicToolbar() called with: geckoSession = [" + geckoSession + "]");
            GeckoSession.ContentDelegate.super.onShowDynamicToolbar(geckoSession);
        }

        @Override
        public void onHideDynamicToolbar(@NonNull GeckoSession geckoSession) {
            Log.d(TAG, "onHideDynamicToolbar() called with: geckoSession = [" + geckoSession + "]");
            GeckoSession.ContentDelegate.super.onHideDynamicToolbar(geckoSession);
        }

        @Override
        public void onCookieBannerDetected(@NonNull GeckoSession session) {
            Log.d(TAG, "onCookieBannerDetected() called with: session = [" + session + "]");
            GeckoSession.ContentDelegate.super.onCookieBannerDetected(session);
        }

        @Override
        public void onCookieBannerHandled(@NonNull GeckoSession session) {
            Log.d(TAG, "onCookieBannerHandled() called with: session = [" + session + "]");
            GeckoSession.ContentDelegate.super.onCookieBannerHandled(session);
        }
    }

    private class MyNavigationDelegate implements GeckoSession.NavigationDelegate {
        @Override
        public void onLocationChange(@NonNull GeckoSession session, @Nullable String url, @NonNull List<GeckoSession.PermissionDelegate.ContentPermission> perms, @NonNull Boolean hasUserGesture) {
//            Log.d(TAG, "onLocationChange() called with: session = [" + session + "], url = [" + url + "], perms = [" + perms + "], hasUserGesture = [" + hasUserGesture + "]");
            GeckoSession.NavigationDelegate.super.onLocationChange(session, url, perms, hasUserGesture);
        }

        @Override
        public void onCanGoBack(@NonNull GeckoSession session, boolean canGoBack) {
            mOnBackPressedCallback.setEnabled(canGoBack);
//            Log.d(TAG, "onCanGoBack() called with: session = [" + session + "], canGoBack = [" + canGoBack + "]");
            GeckoSession.NavigationDelegate.super.onCanGoBack(session, canGoBack);
        }

        @Override
        public void onCanGoForward(@NonNull GeckoSession session, boolean canGoForward) {
//            Log.d(TAG, "onCanGoForward() called with: session = [" + session + "], canGoForward = [" + canGoForward + "]");
            GeckoSession.NavigationDelegate.super.onCanGoForward(session, canGoForward);
        }

        @Nullable
        @Override
        public GeckoResult<AllowOrDeny> onLoadRequest(@NonNull GeckoSession session, @NonNull LoadRequest request) {
            Log.d(TAG, "onLoadRequest() called with: session = [" + session + "], request = [" + request + "]");
            return GeckoSession.NavigationDelegate.super.onLoadRequest(session, request);
        }

        @Nullable
        @Override
        public GeckoResult<AllowOrDeny> onSubframeLoadRequest(@NonNull GeckoSession session, @NonNull LoadRequest request) {
            Log.d(TAG, "onSubframeLoadRequest() called with: session = [" + session + "], request = [" + request + "]");
            return GeckoSession.NavigationDelegate.super.onSubframeLoadRequest(session, request);
        }

        @Nullable
        @Override
        public GeckoResult<GeckoSession> onNewSession(@NonNull GeckoSession session, @NonNull String uri) {
            Log.d(TAG, "onNewSession() called with: session = [" + session + "], uri = [" + uri + "]");
            return GeckoSession.NavigationDelegate.super.onNewSession(session, uri);
        }

        @Nullable
        @Override
        public GeckoResult<String> onLoadError(@NonNull GeckoSession session, @Nullable String uri, @NonNull WebRequestError error) {
            Log.d(TAG, "onLoadError() called with: session = [" + session + "], uri = [" + uri + "], error = [" + error + "]");
            return GeckoSession.NavigationDelegate.super.onLoadError(session, uri, error);
        }
    }

}