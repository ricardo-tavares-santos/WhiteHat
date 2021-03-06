package com.RWdesenv.What.jobs;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;

import com.RWdesenv.What.database.ThreadDatabase;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.storage.StorageSyncHelper;
import com.RWdesenv.What.storage.StorageSyncModels;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.database.RecipientDatabase;
import com.RWdesenv.What.database.StorageKeyDatabase;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobmanager.Data;
import com.RWdesenv.What.jobmanager.Job;
import com.RWdesenv.What.jobmanager.impl.NetworkConstraint;
import com.RWdesenv.What.keyvalue.SignalStore;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.recipients.RecipientId;
import com.RWdesenv.What.transport.RetryLaterException;
import com.RWdesenv.What.util.TextSecurePreferences;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.storage.StorageId;
import org.whispersystems.signalservice.api.storage.StorageKey;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.storage.SignalStorageManifest;
import org.whispersystems.signalservice.api.storage.SignalStorageRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Forces remote storage to match our local state. This should only be done when we detect that the
 * remote data is badly-encrypted (which should only happen after re-registering without a PIN).
 */
public class StorageForcePushJob extends BaseJob {

  public static final String KEY = "StorageForcePushJob";

  private static final String TAG = Log.tag(StorageForcePushJob.class);

  public StorageForcePushJob() {
    this(new Parameters.Builder().addConstraint(NetworkConstraint.KEY)
                                 .setQueue(StorageSyncJob.QUEUE_KEY)
                                 .setMaxInstances(1)
                                 .setLifespan(TimeUnit.DAYS.toMillis(1))
                                 .build());
  }

  private StorageForcePushJob(@NonNull Parameters parameters) {
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
  protected void onRun() throws IOException, RetryLaterException {
    StorageKey                  storageServiceKey  = SignalStore.storageServiceValues().getOrCreateStorageMasterKey().deriveStorageServiceKey();
    SignalServiceAccountManager accountManager     = ApplicationDependencies.getSignalServiceAccountManager();
    RecipientDatabase           recipientDatabase  = DatabaseFactory.getRecipientDatabase(context);
    StorageKeyDatabase          storageKeyDatabase = DatabaseFactory.getStorageKeyDatabase(context);

    long                        currentVersion = accountManager.getStorageManifestVersion();
    Map<RecipientId, StorageId> oldStorageKeys = recipientDatabase.getContactStorageSyncIdsMap();

    long                        newVersion         = currentVersion + 1;
    Map<RecipientId, StorageId> newStorageKeys     = generateNewKeys(oldStorageKeys);
    Set<RecipientId>            archivedRecipients = DatabaseFactory.getThreadDatabase(context).getArchivedRecipients();
    List<SignalStorageRecord>   inserts            = Stream.of(oldStorageKeys.keySet())
                                                           .map(recipientDatabase::getRecipientSettings)
                                                           .withoutNulls()
                                                           .map(s -> StorageSyncModels.localToRemoteRecord(s, Objects.requireNonNull(newStorageKeys.get(s.getId())).getRaw(), archivedRecipients))
                                                           .toList();
    inserts.add(StorageSyncHelper.buildAccountRecord(context, StorageId.forAccount(Recipient.self().fresh().getStorageServiceId())));

    SignalStorageManifest manifest = new SignalStorageManifest(newVersion, new ArrayList<>(newStorageKeys.values()));

    try {
      if (newVersion > 1) {
        Log.i(TAG, String.format(Locale.ENGLISH, "Force-pushing data. Inserting %d keys.", inserts.size()));
        if (accountManager.resetStorageRecords(storageServiceKey, manifest, inserts).isPresent()) {
          Log.w(TAG, "Hit a conflict. Trying again.");
          throw new RetryLaterException();
        }
      } else {
        Log.i(TAG, String.format(Locale.ENGLISH, "First version, normal push. Inserting %d keys.", inserts.size()));
        if (accountManager.writeStorageRecords(storageServiceKey, manifest, inserts, Collections.emptyList()).isPresent()) {
          Log.w(TAG, "Hit a conflict. Trying again.");
          throw new RetryLaterException();
        }
      }
    } catch (InvalidKeyException e) {
      Log.w(TAG, "Hit an invalid key exception, which likely indicates a conflict.");
      throw new RetryLaterException(e);
    }

    Log.i(TAG, "Force push succeeded. Updating local manifest version to: " + newVersion);
    TextSecurePreferences.setStorageManifestVersion(context, newVersion);
    recipientDatabase.applyStorageIdUpdates(newStorageKeys);
    storageKeyDatabase.deleteAll();
  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception e) {
    return e instanceof PushNetworkException || e instanceof RetryLaterException;
  }

  @Override
  public void onFailure() {
  }

  private static @NonNull Map<RecipientId, StorageId> generateNewKeys(@NonNull Map<RecipientId, StorageId> oldKeys) {
    Map<RecipientId, StorageId> out = new HashMap<>();

    for (Map.Entry<RecipientId, StorageId> entry : oldKeys.entrySet()) {
      out.put(entry.getKey(), entry.getValue().withNewBytes(StorageSyncHelper.generateKey()));
    }

    return out;
  }

  public static final class Factory implements Job.Factory<StorageForcePushJob> {
    @Override
    public @NonNull StorageForcePushJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new StorageForcePushJob(parameters);
    }
  }
}
