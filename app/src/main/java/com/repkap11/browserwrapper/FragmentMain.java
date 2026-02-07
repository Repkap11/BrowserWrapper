package com.repkap11.browserwrapper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import androidx.fragment.app.Fragment;

import java.util.UUID;

public class FragmentMain extends Fragment {

    private EditText mEditText_name;
    private EditText mEditText_url;
    private Button mButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        mEditText_name = rootView.findViewById(R.id.name);
        mEditText_url = rootView.findViewById(R.id.url);
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
        Context context = requireContext().getApplicationContext();
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            Toast.makeText(context, "Pinned Shortcuts Not Supported", Toast.LENGTH_SHORT).show();
            return;
        }

        String settingsPackage = BrowserActivity.class.getPackageName();
        String settingsClass = BrowserActivity.class.getName();

        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(settingsPackage);
        launchIntent.setClassName(settingsPackage, settingsClass);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        launchIntent.putExtra(BrowserActivity.EXTRA_URL, url);
        String uuid = UUID.randomUUID().toString();

        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, context.getPackageName() + ":" + FragmentMain.class.getName() + ":" + uuid).setShortLabel(name).setIcon(getAppIcon(context)).setAlwaysBadged().setIntent(launchIntent).build();

        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
    }
}
