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
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.text.TextUtilsCompat;
import androidx.fragment.app.Fragment;

import java.util.UUID;

public class FragmentMain extends Fragment {

    private static final String TAG = FragmentMain.class.getSimpleName();
    private EditText mEditText_name;
    private EditText mEditText_url;
    private Button mButton;
    private String mInitialUrl = null;
    private String mInitialName = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity activity = requireActivity();
        handleIntent(activity.getIntent());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mEditText_name = rootView.findViewById(R.id.name);
        mEditText_url = rootView.findViewById(R.id.url);
        if (TextUtils.isEmpty(mEditText_url.getText())) {
            Log.i(TAG, "onCreateView: Setting url:" + mInitialUrl);
            mEditText_url.setText(mInitialUrl);
        }
        if (TextUtils.isEmpty(mEditText_name.getText())) {
            Log.i(TAG, "onCreateView: Setting name:" + mInitialName);
            mEditText_name.setText(mInitialName);
        }
        mButton = rootView.findViewById(R.id.button);
        mButton.setOnClickListener(new View.OnClickListener() {
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

    public static int getAppIconSize(Context context) {
        ActivityManager activityManager = context.getSystemService(ActivityManager.class);
        return activityManager.getLauncherLargeIconSize();
    }

    private static IconCompat getAppIcon(Context context) {
        Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.ic_launcher_background);

        if (drawable instanceof BitmapDrawable) {
            return IconCompat.createWithBitmap(((BitmapDrawable) drawable).getBitmap());
        }
        int appIconSize = getAppIconSize(context);
//        int appIconDensity = getAppIconDensity(context);

//        final float screenDensity = context.getResources().getDisplayMetrics().density;
        final int adaptiveIconOuterSides = (int) Math.ceil(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics()));
        final int adaptiveIconSize = appIconSize + adaptiveIconOuterSides;

        final Bitmap bitmap = Bitmap.createBitmap(adaptiveIconSize, adaptiveIconSize, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        drawable.setBounds(adaptiveIconOuterSides, adaptiveIconOuterSides, adaptiveIconSize - adaptiveIconOuterSides, adaptiveIconSize - adaptiveIconOuterSides);
        drawable.draw(canvas);
        return IconCompat.createWithAdaptiveBitmap(bitmap);
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

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, context.getPackageName() + ":" + FragmentMain.class.getName() + ":" + uuid).setShortLabel(name).setIcon(getAppIcon(context)).setAlwaysBadged().setIntent(launchIntent).build();

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
    }
}
