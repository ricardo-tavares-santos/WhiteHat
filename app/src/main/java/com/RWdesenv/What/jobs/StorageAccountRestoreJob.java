package com.RWdesenv.What.jobs;

import androidx.annotation.NonNull;

import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobmanager.Data;
import com.RWdesenv.What.jobmanager.Job;
import com.RWdesenv.What.jobmanager.impl.NetworkConstraint;
import com.RWdesenv.What.keyvalue.SignalStore;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.storage.StorageSyncHelper;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.storage.SignalAccountRecord;
import org.whispersystems.signalservice.api.storage.SignalStorageManifest;
import org.whispersystems.signalservice.api.storage.SignalStorageRecord;
import org.whispersystems.signalservice.api.storage.StorageId;
import org.whispersystems.signalservice.api.storage.StorageKey;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Restored the AccountRecord present in the storage service, if any. This will overwrite any local
 * data that is stored in AccountRecord, so this should only be done immediately after registration.
 */
public class StorageAccountRestoreJob extends BaseJob {

  public static String KEY = "StorageAccountRestoreJob";

  public static long LIFESPAN = TimeUnit.SECONDS.toMillis(10);

  private static final String TAG = Log.tag(StorageAccountRestoreJob.class);

  public StorageAccountRestoreJob() {
    this(new Parameters.Builder()
                       .setQueue(StorageSyncJob.QUEUE_KEY)
                       .addConstraint(NetworkConstraint.KEY)
                       .setMaxInstances(1)
                       .setMaxAttempts(1)
                       .setLifespan(LIFESPAN)
                       .build());
  }

  private StorageAccountRestoreJob(@NonNull Parameters parameters) {
    super(parameters);
  }

  @Override
  public @NonNull Data serialize() {
    return Data.EMPTY;
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  protected void onRun() throws Exception {
    SignalServiceAccountManager accountManager    = ApplicationDependencies.getSignalServiceAccountManager();
    StorageKey                  storageServiceKey = SignalStore.storageServiceValues().getOrCreateStorageMasterKey().deriveStorageServiceKey();

    Optional<SignalStorageManifest> manifest = accountManager.getStorageManifest(storageServiceKey);

    if (!manifest.isPresent()) {
      Log.w(TAG, "Manifest did not exist or was undecryptable (bad key). Not restoring.");
      return;
    }

    Optional<StorageId> accountId = manifest.get().getAccountStorageId();

    if (!accountId.isPresent()) {
      Log.w(TAG, "Manifest had no account record! Not restoring.");
      return;
    }

    List<SignalStorageRecord> records = accountManager.readStorageRecords(storageServiceKey, Collections.singletonList(accountId.get()));
    SignalStorageRecord       record  = records.size() > 0 ? records.get(0) : null;

    if (record == null) {
      Log.w(TAG, "Could not find account record, even though we had an ID! Not restoring.");
      return;
    }

    SignalAccountRecord accountRecord = record.getAccount().orNull();
    if (accountRecord == null) {
      Log.w(TAG, "The storage record didn't actually have an account on it! Not restoring.");
      return;
    }

    StorageId selfStorageId = StorageId.forAccount(Recipient.self().getStorageServiceId());
    StorageSyncHelper.applyAccountStorageSyncUpdates(context, selfStorageId, accountRecord);

    if (accountRecord.getAvatarUrlPath().isPresent()) {
      RetrieveProfileAvatarJob avatarJob = new RetrieveProfileAvatarJob(Recipient.self(), accountRecord.getAvatarUrlPath().get());
      try {
        avatarJob.setContext(context);
        avatarJob.onRun();
      } catch (IOException e) {
        Log.w(TAG, "Failed to download avatar. Scheduling for later.");
        ApplicationDependencies.getJobManager().add(avatarJob);
      }
    }
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof PushNetworkException;
  }

  @Override
  public void onFailure() {
  }

  public static class Factory implements Job.Factory<StorageAccountRestoreJob> {
    @Override
    public @NonNull
    StorageAccountRestoreJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new StorageAccountRestoreJob(parameters);
    }
  }
}
