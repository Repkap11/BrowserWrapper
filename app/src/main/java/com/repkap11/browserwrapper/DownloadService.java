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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

public class DownloadService extends Service {
    private static final String TAG = DownloadService.class.getSimpleName();
    private static final String CHANNEL_ID = "DownloadChannel";

    private final AtomicInteger notificationCounter = new AtomicInteger(1);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");
        final int currentNotificationId = notificationCounter.incrementAndGet();

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
        startForeground(currentNotificationId, notification);

        // Run the download in a background thread to keep the service responsive
        new Thread(() -> {
            long totalSize = intent.getLongExtra("contentLength", -1);
            Uri resultUri = downloadFileWithProgress(url, mimeType, fileName, totalSize, currentNotificationId);

            stopForeground(STOP_FOREGROUND_REMOVE);

            if (resultUri != null) {
                showCompleteNotification(resultUri, fileName, mimeType, currentNotificationId);
            } else {
                NotificationManager nm = getSystemService(NotificationManager.class);
                nm.cancel(currentNotificationId);
            }
            stopSelf(startId);
        }).start();

        return START_NOT_STICKY;
    }

    private NotificationCompat.Builder createBaseNotification(String fileName) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Downloading " + fileName)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setOngoing(true) // User can't swipe it away
                .setOnlyAlertOnce(true) // Prevents the phone from chirping every % update
                .setPriority(NotificationCompat.PRIORITY_LOW);
    }

    private Uri downloadFileWithProgress(String urlString, String mimeType, String fileName, long totalSize, int notificationId) {
        Log.d(TAG, "downloadFileWithProgress() called with: urlString = [" + urlString + "], mimeType = [" + mimeType + "], fileName = [" + fileName + "], totalSize = [" + totalSize + "], notificationId = [" + notificationId + "]");
        ContentResolver resolver = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        NotificationCompat.Builder builder = createBaseNotification(fileName);

        if (uri != null) {
            try (InputStream input = new URL(urlString).openStream();
                 OutputStream output = resolver.openOutputStream(uri)) {

//                byte[] buffer = new byte[8192];
                byte[] buffer = new byte[1024];
                long totalRead = 0;
                int bytesRead;
                int lastProgress = 0;

                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;

                    if (totalSize > 0) {
                        int progress = (int) ((totalRead * 100) / totalSize);
                        // Only update the UI if the percentage has actually increased
                        if (progress > lastProgress) {
                            Log.i(TAG, "downloadFileWithProgress: progress:" + progress);
                            lastProgress = progress;
                            builder.setProgress(100, progress, false).setContentText(progress + "% downloaded");
                            notificationManager.notify(notificationId, builder.build());
//                            Thread.sleep(1000);
                        }
                    } else {
                        // Indeterminate bar if we don't know the file size
                        builder.setProgress(0, 0, true);
                        notificationManager.notify(notificationId, builder.build());
                    }
                }
                return uri;
            } catch (IOException e) {
                Log.i(TAG, "downloadFileWithProgress ERR: " + e);
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

    private void showCompleteNotification(Uri fileUri, String fileName, String mimeType, int notificationId) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        // Intent to open the file
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Critical for MediaStore Uris

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Download Complete")
                .setContentText(fileName)
                .setAutoCancel(true) // Dismisses notification when clicked
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        notificationManager.notify(notificationId, builder.build());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}