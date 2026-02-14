package com.repkap11.browserwrapper;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;

import java.util.UUID;

public class FragmentMain extends Fragment {

    private static final String TAG = FragmentMain.class.getSimpleName();
    private EditText mEditText_name;
    private EditText mEditText_url;
    private String mInitialUrl = null;
    private String mInitialName = null;
    private Handler mMainHandler;
    private FaviconManager mFavIconManager;
    private ImageView mImageViewFavIcon;
    private Bitmap mCurrentFavicon;
    private SwitchCompat mSwitchUseGeckoView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainHandler = new Handler(Looper.getMainLooper());
        mFavIconManager = new FaviconManager();
        Activity activity = requireActivity();
        handleIntent(activity.getIntent());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mEditText_name = rootView.findViewById(R.id.name);
        mEditText_url = rootView.findViewById(R.id.url);
        mSwitchUseGeckoView = rootView.findViewById(R.id.switch_use_gecko);

        mImageViewFavIcon = rootView.findViewById(R.id.favicon_preview);
        if (mCurrentFavicon != null) {
            mImageViewFavIcon.setImageBitmap(mCurrentFavicon);
        }
        if (TextUtils.isEmpty(mEditText_url.getText())) {
            Log.i(TAG, "onCreateView: Setting url:" + mInitialUrl);
            mEditText_url.setText(mInitialUrl);
        }
        if (TextUtils.isEmpty(mEditText_name.getText())) {
            Log.i(TAG, "onCreateView: Setting name:" + mInitialName);
            mEditText_name.setText(mInitialName);
        }
        Button buttonGenerateIcon = rootView.findViewById(R.id.button_generate_icon);
        buttonGenerateIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = mEditText_url.getText().toString();
                // 1. Get the system's preferred launcher icon size
                int iconSize = getIconSize(requireContext());

                mFavIconManager.fetchFavicon(url, new FaviconManager.FaviconCallback() {
                    @Override
                    public void onResult(Bitmap bitmap) {
                        // 2. Scale the bitmap to that size
                        // filter = true ensures a smoother bilinear scaling
                        if (bitmap == null) {
                            mCurrentFavicon = null;
                            return;
                        }
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true);
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mCurrentFavicon = scaledBitmap;
                                mImageViewFavIcon.setImageBitmap(mCurrentFavicon);
                            }
                        });
                    }
                });

            }
        });
        Button buttonCreate = rootView.findViewById(R.id.button_create);
        buttonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mEditText_name.getText().toString();
                String url = mEditText_url.getText().toString();
                boolean useGecko = mSwitchUseGeckoView.isChecked();
                onIconClicked(name, url, false, useGecko);
            }
        });
        Button buttonCreateAdaptive = rootView.findViewById(R.id.button_create_adaptive);
        buttonCreateAdaptive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mEditText_name.getText().toString();
                String url = mEditText_url.getText().toString();
                boolean useGecko = mSwitchUseGeckoView.isChecked();
                onIconClicked(name, url, true, useGecko);
            }
        });
        return rootView;
    }

    private int getIconSize(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return am.getLauncherLargeIconSize();
    }

//    private static IconCompat getAppIcon(Context context) {
//        Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.ic_launcher_background);
//
//        if (drawable instanceof BitmapDrawable) {
//            return IconCompat.createWithBitmap(((BitmapDrawable) drawable).getBitmap());
//        }
//        int appIconSize = getAppIconSize(context);
////        int appIconDensity = getAppIconDensity(context);
//

    /// /        final float screenDensity = context.getResources().getDisplayMetrics().density;
//        final int adaptiveIconOuterSides = (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics()));
//        final int adaptiveIconSize = appIconSize + adaptiveIconOuterSides;
//
//        final Bitmap bitmap = Bitmap.createBitmap(adaptiveIconSize, adaptiveIconSize, Bitmap.Config.ARGB_8888);
//        final Canvas canvas = new Canvas(bitmap);
//        canvas.drawColor(Color.WHITE);
//        drawable.setBounds(adaptiveIconOuterSides, adaptiveIconOuterSides, adaptiveIconSize - adaptiveIconOuterSides, adaptiveIconSize - adaptiveIconOuterSides);
//        drawable.draw(canvas);
//        return IconCompat.createWithAdaptiveBitmap(bitmap);
//    }
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            String sharedUrlText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedUrlText != null) {
                // Handle the URL here
                Log.i(TAG, "Received shared URL: " + sharedUrlText);
                mInitialUrl = sharedUrlText;
            }
            String sharedNameText = intent.getStringExtra(Intent.EXTRA_SUBJECT);
            if (sharedNameText != null) {
                // Handle the URL here
                Log.i(TAG, "Received shared URL: " + sharedNameText);
                mInitialName = sharedNameText;
            }
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri != null) {
                // Handle the URL here
                Log.i(TAG, "Received deep link URL: " + uri.toString());
                mInitialUrl = uri.toString();
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    }

    public IconCompat createLauncherIcon(boolean adaptive) {
        Bitmap bitmap = mCurrentFavicon;
        if (bitmap == null) {
            Drawable drawable = AppCompatResources.getDrawable(requireContext(), R.mipmap.ic_launcher);

            int appIconSize = getIconSize(requireContext());
            final int adaptiveIconOuterSides = (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics()));
            final int adaptiveIconSize = appIconSize + adaptiveIconOuterSides;

            bitmap = Bitmap.createBitmap(adaptiveIconSize, adaptiveIconSize, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            canvas.drawColor(Color.WHITE);
            drawable.setBounds(adaptiveIconOuterSides, adaptiveIconOuterSides, adaptiveIconSize - adaptiveIconOuterSides, adaptiveIconSize - adaptiveIconOuterSides);
            drawable.draw(canvas);
        }
        if (adaptive) {
            return IconCompat.createWithAdaptiveBitmap(bitmap);
        } else {
            return IconCompat.createWithBitmap(bitmap);
        }
    }

    public void onIconClicked(String name, String url, boolean adaptive, boolean useGecko) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(url)) {
            return;
        }
        Context context = requireContext().getApplicationContext();
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Pinned Shortcuts Not Supported", Toast.LENGTH_SHORT).show();
            return;
        }

        String settingsPackage;
        String settingsClass;
        if (useGecko) {
            settingsPackage = BrowserGeckoActivity.class.getPackageName();
            settingsClass = BrowserGeckoActivity.class.getName();
        } else {
            settingsPackage = BrowserWebViewActivity.class.getPackageName();
            settingsClass = BrowserWebViewActivity.class.getName();
        }


        String uuid = UUID.randomUUID().toString();


        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(settingsPackage);
        launchIntent.setClassName(settingsPackage, settingsClass);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        Uri uri = Uri.parse("urn:uuid:" + uuid);
        launchIntent.setData(uri);
        launchIntent.putExtra(BrowserGeckoActivity.EXTRA_URL, url);


        IconCompat icon = createLauncherIcon(adaptive);

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, context.getPackageName() + ":" + FragmentMain.class.getName() + ":" + uuid).setShortLabel(name).setIcon(icon).setAlwaysBadged().setIntent(launchIntent).build();

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
    }
}
