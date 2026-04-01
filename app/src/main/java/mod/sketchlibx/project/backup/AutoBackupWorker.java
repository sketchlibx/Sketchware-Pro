package mod.sketchlibx.project.backup;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import a.a.a.lC;
import mod.hey.studios.project.backup.BackupFactory;
import pro.sketchware.R;

public class AutoBackupWorker extends Worker {

    private static final String TAG = "AutoBackupWorker";
    private static final String CHANNEL_ID = "cloud_backup_channel";
    private static final int NOTIFICATION_ID = 9988;

    private NotificationManager notificationManager;

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createChannel();
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            Log.e(TAG, "User not signed in. Cannot perform cloud backup.");
            return Result.failure();
        }

        CloudBackupManager cloudManager = new CloudBackupManager(context, account);

        // Fetch projects to backup (In Auto-Backup, we do ALL projects, manual backup does specific)
        ArrayList<HashMap<String, Object>> projects = lC.a();
        if (projects == null || projects.isEmpty()) {
            return Result.success();
        }

        int total = projects.size();
        boolean allSuccess = true;
        
        for (int i = 0; i < total; i++) {
            HashMap<String, Object> project = projects.get(i);
            String scId = (String) project.get("sc_id");
            String projectName = (String) project.get("my_app_name");

            if (scId == null || projectName == null) continue;

            // Show notification progress
            updateNotification("Backing up: " + projectName, i + 1, total);

            BackupFactory backupFactory = new BackupFactory(scId);
            backupFactory.setBackupLocalLibs(true);
            backupFactory.setBackupCustomBlocks(true);
            
            backupFactory.backup(null, projectName);
            File swbFile = backupFactory.getOutFile();

            if (swbFile != null && swbFile.exists()) {
                try {
                    uploadSync(cloudManager, swbFile, projectName);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to upload: " + projectName, e);
                    allSuccess = false;
                }
            } else {
                allSuccess = false;
            }
        }

        updateNotification("Backup Complete!", total, total);
        return allSuccess ? Result.success() : Result.retry();
    }

    private void updateNotification(String text, int current, int total) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_sync) // Native sync icon
                .setContentTitle("Sketchware Cloud Backup")
                .setContentText(text)
                .setProgress(total, current, false)
                .setOngoing(current < total)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // For WorkManager Foreground Service (Required for Android 12+)
        if (current == 1) {
            try {
                setForegroundAsync(new ForegroundInfo(NOTIFICATION_ID, builder.build()));
            } catch (Exception ignored){}
        } else {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Cloud Backup", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Shows progress of cloud backups");
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void uploadSync(CloudBackupManager cloudManager, File swbFile, String projectName) throws Exception {
        final Object lock = new Object();
        final Exception[] uploadError = {null};

        cloudManager.uploadBackupToCloud(swbFile, projectName, new CloudBackupManager.BackupCallback() {
            @Override
            public void onSuccess(String message) {
                synchronized (lock) { lock.notify(); }
            }
            @Override
            public void onError(String error) {
                uploadError[0] = new Exception(error);
                synchronized (lock) { lock.notify(); }
            }
        });

        synchronized (lock) { lock.wait(); }
        if (uploadError[0] != null) throw uploadError[0];
    }
}
