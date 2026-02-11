package com.repkap11.browserwrapper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FaviconManager {
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public interface FaviconCallback {
        void onResult(Bitmap bitmap);
    }

    public void fetchFavicon(String siteUrl, FaviconCallback callback) {
        executor.execute(() -> {
            Bitmap bitmap = null;

            // 1. Try Google first if it's likely a public site
            if (!isLanAddress(siteUrl)) {
                bitmap = downloadFromGoogle(siteUrl);
            }

            // 2. Fallback to Local Fetch (LAN or Google failed)
            if (bitmap == null) {
                bitmap = fetchLocally(siteUrl);
            }

            callback.onResult(bitmap);
        });
    }

    private boolean isLanAddress(String url) {
        return url.contains("192.168.") || url.contains("10.") || url.contains("172.") || url.contains(".local") || url.contains("localhost");
    }

    private Bitmap downloadFromGoogle(String siteUrl) {
        String googleApi = "https://www.google.com/s2/favicons?sz=256&domain_url=" + siteUrl;
        return downloadBitmap(googleApi);
    }

    private Bitmap fetchLocally(String siteUrl) {
        try {
            Request request = new Request.Builder().url(siteUrl).build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return tryRootFavicon(siteUrl);
                }

                String html = response.body().string();
                String iconUrl = extractIconUrl(html, siteUrl);

                if (iconUrl != null) {
                    return downloadBitmap(iconUrl);
                }
            }
        } catch (Exception e) {
            Log.e("Favicon", "Local fetch failed: " + e.getMessage());
        }

        // Final fallback to domain/favicon.ico
        return tryRootFavicon(siteUrl);
    }

    private String extractIconUrl(String html, String siteUrl) {
        // Regex to find <link> tags with rel containing "icon" and extract the href
        // This looks for: <link ... rel="...icon..." ... href="url" ... >
        Pattern pattern = Pattern.compile("<link[^>]+rel=[\"'](?i)[^\"']*(?:icon)[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(html);

        if (matcher.find()) {
            String href = matcher.group(1);
            return resolveUrl(siteUrl, href);
        }
        return null;
    }

    private String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            URL base = new URL(baseUrl);
            return new URL(base, relativeUrl).toString();
        } catch (Exception e) {
            return relativeUrl;
        }
    }

    private Bitmap tryRootFavicon(String siteUrl) {
        try {
            URL url = new URL(siteUrl);
            String rootIcon = url.getProtocol() + "://" + url.getHost() + "/favicon.ico";
            return downloadBitmap(rootIcon);
        } catch (Exception e) {
            return null;
        }
    }

    private Bitmap downloadBitmap(String url) {
        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                InputStream is = response.body().byteStream();
                return BitmapFactory.decodeStream(is);
            }
        } catch (Exception e) {
            Log.e("Favicon", "Download failed for " + url);
        }
        return null;
    }
}