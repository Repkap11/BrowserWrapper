package com.repkap11.browserwrapper;

import android.content.res.Configuration;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class InsetHelper {

    public static final int NONE = 0;
    public static final int LEFT = 1 << 0;
    public static final int TOP = 1 << 1;
    public static final int RIGHT = 1 << 2;
    public static final int BOTTOM = 1 << 3;
    public static final int ALL = LEFT | TOP | RIGHT | BOTTOM;
    public static final int TOOLBAR = LEFT | TOP | RIGHT;

    private static final String TAG = InsetHelper.class.getSimpleName();

    public static void activityOnCreate(@NonNull ComponentActivity activity, boolean hasLightTopBar, boolean supportDarkTheme) {
        SystemBarStyle sys_style;
        boolean isNightMode = supportDarkTheme && (activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        if (hasLightTopBar) {
            if (isNightMode) {
                sys_style = SystemBarStyle.dark(activity.getColor(android.R.color.transparent));
            } else {
                sys_style = SystemBarStyle.light(activity.getColor(android.R.color.transparent), activity.getColor(android.R.color.transparent));
            }
        } else {
            sys_style = SystemBarStyle.dark(activity.getColor(android.R.color.transparent));
        }
        SystemBarStyle nav_style = SystemBarStyle.light(activity.getColor(android.R.color.transparent), activity.getColor(android.R.color.transparent));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            EdgeToEdge.enable(activity, sys_style, nav_style);
        }
    }

    public static void setOnApplyWindowInsetsListener(View view, int dir_mask) {
        setOnApplyWindowInsetsListener(view, dir_mask, false);
    }

    public static void setOnApplyWindowInsetsListener(View view, int dir_mask, boolean usePadding) {
        ViewCompat.setOnApplyWindowInsetsListener(view, new OnApplyWindowInsetsListener() {

            private boolean firstRun = true;
            private int cached_padding_left = 0;
            private int cached_padding_top = 0;
            private int cached_padding_right = 0;
            private int cached_padding_bottom = 0;

            private int cached_margin_left = 0;
            private int cached_margin_top = 0;
            private int cached_margin_right = 0;
            private int cached_margin_bottom = 0;

            @Override
            public @NonNull WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat windowInsets) {
                String idName = v.getResources().getResourceName(v.getId());
                if (idName != null) {
                    String[] parts = idName.split("/");
                    if (parts.length == 2) {
                        idName = parts[1];
                    }
                } else {
                    idName = "N/A";
                }
                Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());

                int inset_left = 0, inset_top = 0, inset_right = 0, inset_bottom = 0;
                if ((dir_mask & LEFT) != 0) {
                    inset_left = insets.left;
                }
                if ((dir_mask & TOP) != 0) {
                    inset_top = insets.top;
                }
                if ((dir_mask & RIGHT) != 0) {
                    inset_right = insets.right;
                }
                if ((dir_mask & BOTTOM) != 0) {
                    inset_bottom = insets.bottom;
                }
                if (firstRun) {
                    cached_padding_left = v.getPaddingLeft();
                    cached_padding_top = v.getPaddingTop();
                    cached_padding_right = v.getPaddingRight();
                    cached_padding_bottom = v.getPaddingBottom();

                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    cached_margin_left = mlp.leftMargin;
                    cached_margin_top = mlp.topMargin;
                    cached_margin_right = mlp.rightMargin;
                    cached_margin_bottom = mlp.bottomMargin;
                }
                firstRun = false;

                if (usePadding) {
                    int left, top, right, bottom;
                    if ((dir_mask & LEFT) != 0) {
                        left = cached_padding_left + inset_left;
                    } else {
                        left = cached_padding_left;
                    }
                    if ((dir_mask & TOP) != 0) {
                        top = cached_padding_top + inset_top;
                    } else {
                        top = cached_padding_top;
                    }
                    if ((dir_mask & RIGHT) != 0) {
                        right = cached_padding_right + inset_right;
                    } else {
                        right = cached_padding_right;
                    }
                    if ((dir_mask & BOTTOM) != 0) {
                        bottom = cached_padding_bottom + inset_bottom;
                    } else {
                        bottom = cached_padding_bottom;
                    }
                    v.setPadding(left, top, right, bottom);

                } else {
                    ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                    if ((dir_mask & LEFT) != 0) {
                        mlp.leftMargin = cached_margin_left + inset_left;
                    } else {
                        mlp.leftMargin = cached_margin_left;
                    }
                    if ((dir_mask & TOP) != 0) {
                        mlp.topMargin = cached_margin_top + inset_top;
                    } else {
                        mlp.topMargin = cached_margin_top;
                    }
                    if ((dir_mask & RIGHT) != 0) {
                        mlp.rightMargin = cached_margin_right + inset_right;
                    } else {
                        mlp.rightMargin = cached_margin_right;
                    }
                    if ((dir_mask & BOTTOM) != 0) {
                        mlp.bottomMargin = cached_margin_bottom + inset_bottom;
                    } else {
                        mlp.bottomMargin = cached_margin_bottom;
                    }
                    v.setLayoutParams(mlp);
                }
                firstRun = false;
                return windowInsets.inset(inset_left, inset_top, inset_right, inset_bottom);

            }
        });
    }
}
