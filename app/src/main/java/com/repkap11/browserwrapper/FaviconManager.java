package com.repkap11.browserwrapper;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

            Bitmap finalBitmap = bitmap;
            // Return to UI thread (simplified for example)
            callback.onResult(finalBitmap);
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
            // Use Jsoup to parse the HTML
            Document doc = Jsoup.connect(siteUrl).get();
            // Look for link tags with "icon" in the rel attribute
            Element iconElement = doc.head().select("link[rel~=(?i)^(shortcut|apple-touch-)?icon]").first();

            String iconUrl;
            if (iconElement != null) {
                iconUrl = iconElement.attr("abs:href");
            } else {
                // Hard fallback to root favicon.ico
                URL url = new URL(siteUrl);
                iconUrl = url.getProtocol() + "://" + url.getHost() + "/favicon.ico";
            }
            return downloadBitmap(iconUrl);
        } catch (Exception e) {
            Log.e("Favicon", "Local fetch failed: " + e.getMessage());
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