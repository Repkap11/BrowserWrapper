package com.repkap11.browserwrapper;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class DownloadService extends Service {
    private static final String CHANNEL_ID = "DownloadChannel";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra("url");
        String fileName = intent.getStringExtra("fileName");
        String mimeType = intent.getStringExtra("mimeType");

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Downloading File")
                .setContentText(fileName)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        // Android 14+ requires specifying the type (e.g., DATA_SYNC)
        startForeground(1, notification);

        // Run the download in a background thread to keep the service responsive
        new Thread(() -> {
            Uri resultUri = downloadFileToMediaStore(url, mimeType, fileName);

            // Stop the foreground "Downloading" state
            stopForeground(STOP_FOREGROUND_REMOVE);

            if (resultUri != null) {
                showCompleteNotification(resultUri, fileName, mimeType);
            }

            stopSelf();
        }).start();

        return START_NOT_STICKY;
    }

    private Uri downloadFileToMediaStore(String urlString, String mimeType, String fileName) {
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try (InputStream input = new URL(urlString).openStream();
                 OutputStream output = resolver.openOutputStream(uri)) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                return uri; // Success: return the file location
            } catch (IOException e) {
                resolver.delete(uri, null, null);
            }
        }
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void showCompleteNotification(Uri fileUri, String fileName, String mimeType) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        int NOTIFICATION_ID = 2; // Use a different ID than the foreground service

        // Intent to open the file
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Critical for MediaStore Uris

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "DownloadChannel")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete")
                .setContentText(fileName)
                .setAutoCancel(true) // Dismisses notification when clicked
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}