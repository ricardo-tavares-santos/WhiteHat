package com.RWdesenv.What.jobs;

import androidx.annotation.NonNull;

import com.RWdesenv.What.ApplicationContext;
import com.RWdesenv.What.crypto.IdentityKeyUtil;
import com.RWdesenv.What.crypto.PreKeyUtil;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobmanager.Data;
import com.RWdesenv.What.jobmanager.Job;
import com.RWdesenv.What.jobmanager.impl.NetworkConstraint;
import com.RWdesenv.What.keyvalue.SignalStore;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.util.TextSecurePreferences;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.NonSuccessfulResponseCodeException;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.storage.SignalStorageRecord;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RefreshPreKeysJob extends BaseJob {

  public static final String KEY = "RefreshPreKeysJob";

  private static final String TAG = RefreshPreKeysJob.class.getSimpleName();

  private static final int PREKEY_MINIMUM = 10;

  private static final long REFRESH_INTERVAL = TimeUnit.DAYS.toMillis(3);

  public RefreshPreKeysJob() {
    this(new Job.Parameters.Builder()
                           .setQueue("RefreshPreKeysJob")
                           .addConstraint(NetworkConstraint.KEY)
                           .setMaxInstances(1)
                           .setMaxAttempts(Parameters.UNLIMITED)
                           .setLifespan(TimeUnit.DAYS.toMillis(30))
                           .build());
  }

  public static void scheduleIfNecessary() {
    long timeSinceLastRefresh = System.currentTimeMillis() - SignalStore.getLastPrekeyRefreshTime();

    if (timeSinceLastRefresh > REFRESH_INTERVAL) {
      Log.i(TAG, "Scheduling a prekey refresh. Time since last schedule: " + timeSinceLastRefresh + " ms");
      ApplicationDependencies.getJobManager().add(new RefreshPreKeysJob());
    }
  }

  private RefreshPreKeysJob(@NonNull Job.Parameters parameters) {
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
  public void onRun() throws IOException {
    if (!TextSecurePreferences.isPushRegistered(context)) {
      Log.w(TAG, "Not registered. Skipping.");
      return;
    }

    SignalServiceAccountManager accountManager = ApplicationDependencies.getSignalServiceAccountManager();

    int availableKeys = accountManager.getPreKeysCount();

    Log.i(TAG, "Available keys: " + availableKeys);

    if (availableKeys >= PREKEY_MINIMUM && TextSecurePreferences.isSignedPreKeyRegistered(context)) {
      Log.i(TAG, "Available keys sufficient.");
      SignalStore.setLastPrekeyRefreshTime(System.currentTimeMillis());
      return;
    }

    List<PreKeyRecord> preKeyRecords       = PreKeyUtil.generatePreKeys(context);
    IdentityKeyPair    identityKey         = IdentityKeyUtil.getIdentityKeyPair(context);
    SignedPreKeyRecord signedPreKeyRecord  = PreKeyUtil.generateSignedPreKey(context, identityKey, false);

    Log.i(TAG, "Registering new prekeys...");

    accountManager.setPreKeys(identityKey.getPublicKey(), signedPreKeyRecord, preKeyRecords);

    PreKeyUtil.setActiveSignedPreKeyId(context, signedPreKeyRecord.getId());
    TextSecurePreferences.setSignedPreKeyRegistered(context, true);

    ApplicationDependencies.getJobManager().add(new CleanPreKeysJob());
    SignalStore.setLastPrekeyRefreshTime(System.currentTimeMillis());
    Log.i(TAG, "Successfully refreshed prekeys.");
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    if (exception instanceof NonSuccessfulResponseCodeException) return false;
    if (exception instanceof PushNetworkException)               return true;

    return false;
  }

  @Override
  public void onFailure() {
  }

  public static final class Factory implements Job.Factory<RefreshPreKeysJob> {
    @Override
    public @NonNull RefreshPreKeysJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new RefreshPreKeysJob(parameters);
    }
  }
}
