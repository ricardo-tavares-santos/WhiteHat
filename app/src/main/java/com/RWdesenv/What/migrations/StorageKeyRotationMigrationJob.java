package com.RWdesenv.What.migrations;

import androidx.annotation.NonNull;

import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobmanager.Data;
import com.RWdesenv.What.jobmanager.Job;
import com.RWdesenv.What.jobmanager.JobManager;
import com.RWdesenv.What.jobs.MultiDeviceKeysUpdateJob;
import com.RWdesenv.What.jobs.MultiDeviceStorageSyncRequestJob;
import com.RWdesenv.What.jobs.StorageForcePushJob;
import com.RWdesenv.What.jobs.StorageSyncJob;
import com.RWdesenv.What.keyvalue.SignalStore;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.util.TextSecurePreferences;

public class StorageKeyRotationMigrationJob extends MigrationJob {

  private static final String TAG = Log.tag(StorageKeyRotationMigrationJob.class);

  public static final String KEY = "StorageKeyRotationMigrationJob";

  StorageKeyRotationMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private StorageKeyRotationMigrationJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public boolean isUiBlocking() {
    return false;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void performMigration() {
    JobManager jobManager = ApplicationDependencies.getJobManager();
    SignalStore.storageServiceValues().rotateStorageMasterKey();

    if (TextSecurePreferences.isMultiDevice(context)) {
      Log.i(TAG, "Multi-device.");
      jobManager.startChain(new StorageForcePushJob())
                .then(new MultiDeviceKeysUpdateJob())
                .then(new MultiDeviceStorageSyncRequestJob())
                .enqueue();
    } else {
      Log.i(TAG, "Single-device.");
      jobManager.add(new StorageForcePushJob());
    }
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  public static class Factory implements Job.Factory<StorageKeyRotationMigrationJob> {
    @Override
    public @NonNull StorageKeyRotationMigrationJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new StorageKeyRotationMigrationJob(parameters);
    }
  }
}
