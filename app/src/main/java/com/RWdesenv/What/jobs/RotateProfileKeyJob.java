package com.RWdesenv.What.jobs;

import androidx.annotation.NonNull;

import org.signal.zkgroup.profiles.ProfileKey;
import com.RWdesenv.What.crypto.ProfileKeyUtil;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.database.RecipientDatabase;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobmanager.Data;
import com.RWdesenv.What.jobmanager.Job;
import com.RWdesenv.What.jobmanager.impl.NetworkConstraint;
import com.RWdesenv.What.profiles.AvatarHelper;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.util.FeatureFlags;
import com.RWdesenv.What.util.TextSecurePreferences;
import org.whispersystems.signalservice.api.SignalServiceAccountManager;
import org.whispersystems.signalservice.api.push.exceptions.PushNetworkException;
import org.whispersystems.signalservice.api.util.StreamDetails;

import java.util.UUID;

public class RotateProfileKeyJob extends BaseJob {

  public static String KEY = "RotateProfileKeyJob";

  public RotateProfileKeyJob() {
    this(new Job.Parameters.Builder()
                           .setQueue("__ROTATE_PROFILE_KEY__")
                           .addConstraint(NetworkConstraint.KEY)
                           .setMaxAttempts(25)
                           .setMaxInstances(1)
                           .build());
  }

  private RotateProfileKeyJob(@NonNull Job.Parameters parameters) {
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
  public void onRun() throws Exception {
    SignalServiceAccountManager accountManager    = ApplicationDependencies.getSignalServiceAccountManager();
    RecipientDatabase           recipientDatabase = DatabaseFactory.getRecipientDatabase(context);
    ProfileKey                  profileKey        = ProfileKeyUtil.createNew();
    Recipient                   self              = Recipient.self();

    recipientDatabase.setProfileKey(self.getId(), profileKey);
     try (StreamDetails avatarStream = AvatarHelper.getSelfProfileAvatarStream(context)) {
      if (FeatureFlags.VERSIONED_PROFILES) {
        accountManager.setVersionedProfile(self.getUuid().get(),
                                           profileKey,
                                           Recipient.self().getProfileName().serialize(),
                                           avatarStream);
      } else {
        accountManager.setProfileName(profileKey, Recipient.self().getProfileName().serialize());
        accountManager.setProfileAvatar(profileKey, avatarStream);
      }
    }

    ApplicationDependencies.getJobManager().add(new RefreshAttributesJob());
  }

  @Override
  public void onFailure() {

  }

  @Override
  protected boolean onShouldRetry(@NonNull Exception exception) {
    return exception instanceof PushNetworkException;
  }

  public static final class Factory implements Job.Factory<RotateProfileKeyJob> {
    @Override
    public @NonNull RotateProfileKeyJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new RotateProfileKeyJob(parameters);
    }
  }
}
