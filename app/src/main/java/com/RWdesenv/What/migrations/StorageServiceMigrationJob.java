package com.RWdesenv.What.migrations;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;

import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobmanager.Data;
import com.RWdesenv.What.jobmanager.Job;
import com.RWdesenv.What.jobmanager.JobManager;
import com.RWdesenv.What.jobs.MultiDeviceKeysUpdateJob;
import com.RWdesenv.What.jobs.StickerPackDownloadJob;
import com.RWdesenv.What.jobs.StorageSyncJob;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.stickers.BlessedPacks;
import com.RWdesenv.What.util.TextSecurePreferences;

import java.util.Arrays;
import java.util.List;

public class StorageServiceMigrationJob extends MigrationJob {

  private static final String TAG = Log.tag(StorageServiceMigrationJob.class);

  public static final String KEY = "StorageServiceMigrationJob";

  StorageServiceMigrationJob() {
    this(new Parameters.Builder().build());
  }

  private StorageServiceMigrationJob(@NonNull Parameters parameters) {
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

    if (TextSecurePreferences.isMultiDevice(context)) {
      Log.i(TAG, "Multi-device.");
      jobManager.startChain(new StorageSyncJob())
                .then(new MultiDeviceKeysUpdateJob())
                .enqueue();
    } else {
      Log.i(TAG, "Single-device.");
      jobManager.add(new StorageSyncJob());
    }
  }

  @Override
  boolean shouldRetry(@NonNull Exception e) {
    return false;
  }

  public static class Factory implements Job.Factory<StorageServiceMigrationJob> {
    @Override
    public @NonNull StorageServiceMigrationJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new StorageServiceMigrationJob(parameters);
    }
  }
}
