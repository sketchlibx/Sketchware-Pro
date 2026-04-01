package mod.sketchlibx.project.backup;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import a.a.a.lC;
import mod.hey.studios.project.backup.BackupFactory;

public class AutoBackupWorker extends Worker {

    private static final String TAG = "AutoBackupWorker";

    public AutoBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Auto Backup Started in background...");

        Context context = getApplicationContext();
        
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            Log.e(TAG, "User not signed in. Cannot perform cloud backup.");
            return Result.failure();
        }

        CloudBackupManager cloudManager = new CloudBackupManager(context, account);

        ArrayList<HashMap<String, Object>> projects = lC.a();
        if (projects == null || projects.isEmpty()) {
            Log.d(TAG, "No projects found to backup.");
            return Result.success();
        }

        boolean allSuccess = true;
        for (HashMap<String, Object> project : projects) {
            String scId = (String) project.get("sc_id");
            String projectName = (String) project.get("my_app_name");

            if (scId == null || projectName == null) continue;

            BackupFactory backupFactory = new BackupFactory(scId);
            backupFactory.setBackupLocalLibs(true);
            backupFactory.setBackupCustomBlocks(true);
            
            backupFactory.backup(null, projectName);
            File swbFile = backupFactory.getOutFile();

            if (swbFile != null && swbFile.exists()) {
                try {
                    uploadSync(cloudManager, swbFile, projectName);
                    Log.d(TAG, "Successfully backed up: " + projectName);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to upload: " + projectName, e);
                    allSuccess = false;
                }
            } else {
                Log.e(TAG, "Failed to generate local SWB for: " + projectName);
                allSuccess = false;
            }
        }

        return allSuccess ? Result.success() : Result.retry();
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

        synchronized (lock) {
            lock.wait(); 
        }

        if (uploadError[0] != null) {
            throw uploadError[0];
        }
    }
}
