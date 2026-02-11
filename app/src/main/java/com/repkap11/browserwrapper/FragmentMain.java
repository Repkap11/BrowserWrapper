package com.repkap11.browserwrapper;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
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
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.fragment.app.Fragment;

import java.util.UUID;

public class FragmentMain extends Fragment {

    private static final String TAG = FragmentMain.class.getSimpleName();
    private EditText mEditText_name;
    private EditText mEditText_url;
    private Button mButtonCreate;
    private String mInitialUrl = null;
    private String mInitialName = null;
    private Button mButtonGenerateIcon;
    private Handler mMainHandler;
    private FaviconManager mFavIconManager;
    private ImageView mImageViewFavIcon;
    private Bitmap mCurrentFavicon;

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
        mButtonGenerateIcon = rootView.findViewById(R.id.button_generate_icon);
        mButtonGenerateIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = mEditText_url.getText().toString();
                // 1. Get the system's preferred launcher icon size
                ActivityManager am = (ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
                int iconSize = am.getLauncherLargeIconSize();

                mFavIconManager.fetchFavicon(url, new FaviconManager.FaviconCallback() {
                    @Override
                    public void onResult(Bitmap bitmap) {
                        // 2. Scale the bitmap to that size
                        // filter = true ensures a smoother bilinear scaling
                        if (bitmap == null){
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
        mButtonCreate = rootView.findViewById(R.id.button_create);
        mButtonCreate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mEditText_name.getText().toString();
                String url = mEditText_url.getText().toString();
                onIconClicked(name, url);
            }
        });
        return rootView;
    }

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


    public IconCompat createLauncherIcon() {
        if (mCurrentFavicon == null) {
            return null;
        }
        // 3. Create the IconCompat
        // Use 'createWithBitmap' to maintain the exact look of the favicon
        return IconCompat.createWithBitmap(mCurrentFavicon);
    }

    public void onIconClicked(String name, String url) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(url)) {
            return;
        }
        Context context = requireContext().getApplicationContext();
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Pinned Shortcuts Not Supported", Toast.LENGTH_SHORT).show();
            return;
        }

        String settingsPackage = BrowserActivity.class.getPackageName();
        String settingsClass = BrowserActivity.class.getName();

        String uuid = UUID.randomUUID().toString();


        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(settingsPackage);
        launchIntent.setClassName(settingsPackage, settingsClass);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        Uri uri = Uri.parse("urn:uuid:" + uuid);
        launchIntent.setData(uri);
        launchIntent.putExtra(BrowserActivity.EXTRA_URL, url);

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, context.getPackageName() + ":" + FragmentMain.class.getName() + ":" + uuid).setShortLabel(name).setIcon(createLauncherIcon()).setAlwaysBadged().setIntent(launchIntent).build();

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
    }
}
