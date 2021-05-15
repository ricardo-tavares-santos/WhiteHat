package com.RWdesenv.What.profiles.edit;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.util.Consumer;

import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobs.MultiDeviceProfileContentUpdateJob;
import com.RWdesenv.What.jobs.MultiDeviceProfileKeyUpdateJob;
import com.RWdesenv.What.jobs.ProfileUploadJob;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.profiles.AvatarHelper;
import com.RWdesenv.What.profiles.ProfileMediaConstraints;
import com.RWdesenv.What.profiles.ProfileName;
import com.RWdesenv.What.profiles.SystemProfileUtil;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientId;
import com.RWdesenv.What.util.Base64;
import com.RWdesenv.What.util.ProfileUtil;
import com.RWdesenv.What.util.TextSecurePreferences;
import com.RWdesenv.What.util.Util;
import com.RWdesenv.What.util.concurrent.ListenableFuture;
import com.RWdesenv.What.util.concurrent.SignalExecutors;
import com.RWdesenv.What.util.concurrent.SimpleTask;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.profiles.SignalServiceProfile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

class EditProfileRepository {

  private static final String TAG = Log.tag(EditProfileRepository.class);

  private final Context context;
  private final boolean excludeSystem;

  EditProfileRepository(@NonNull Context context, boolean excludeSystem) {
    this.context        = context.getApplicationContext();
    this.excludeSystem  = excludeSystem;
  }

  void getCurrentProfileName(@NonNull Consumer<ProfileName> profileNameConsumer) {
    ProfileName storedProfileName = Recipient.self().getProfileName();
    if (!storedProfileName.isEmpty()) {
      profileNameConsumer.accept(storedProfileName);
    } else if (!excludeSystem) {
      SystemProfileUtil.getSystemProfileName(context).addListener(new ListenableFuture.Listener<String>() {
        @Override
        public void onSuccess(String result) {
          if (!TextUtils.isEmpty(result)) {
            profileNameConsumer.accept(ProfileName.fromSerialized(result));
          } else {
            profileNameConsumer.accept(storedProfileName);
          }
        }

        @Override
        public void onFailure(ExecutionException e) {
          Log.w(TAG, e);
          profileNameConsumer.accept(storedProfileName);
        }
      });
    } else {
      profileNameConsumer.accept(storedProfileName);
    }
  }

  void getCurrentAvatar(@NonNull Consumer<byte[]> avatarConsumer) {
    RecipientId selfId = Recipient.self().getId();

    if (AvatarHelper.hasAvatar(context, selfId)) {
      SimpleTask.run(() -> {
        try {
          return Util.readFully(AvatarHelper.getAvatar(context, selfId));
        } catch (IOException e) {
          Log.w(TAG, e);
          return null;
        }
      }, avatarConsumer::accept);
    } else if (!excludeSystem) {
      SystemProfileUtil.getSystemProfileAvatar(context, new ProfileMediaConstraints()).addListener(new ListenableFuture.Listener<byte[]>() {
        @Override
        public void onSuccess(byte[] result) {
          avatarConsumer.accept(result);
        }

        @Override
        public void onFailure(ExecutionException e) {
          Log.w(TAG, e);
          avatarConsumer.accept(null);
        }
      });
    }
  }

  void uploadProfile(@NonNull ProfileName profileName, @Nullable byte[] avatar, @NonNull Consumer<UploadResult> uploadResultConsumer) {
    SimpleTask.run(() -> {
      DatabaseFactory.getRecipientDatabase(context).setProfileName(Recipient.self().getId(), profileName);

      try {
        AvatarHelper.setAvatar(context, Recipient.self().getId(), avatar != null ? new ByteArrayInputStream(avatar) : null);
      } catch (IOException e) {
        return UploadResult.ERROR_FILE_IO;
      }

      ApplicationDependencies.getJobManager()
                             .startChain(new ProfileUploadJob())
                             .then(Arrays.asList(new MultiDeviceProfileKeyUpdateJob(), new MultiDeviceProfileContentUpdateJob()))
                             .enqueue();

      return UploadResult.SUCCESS;
    }, uploadResultConsumer::accept);
  }

  void getCurrentUsername(@NonNull Consumer<Optional<String>> callback) {
    callback.accept(Optional.fromNullable(TextSecurePreferences.getLocalUsername(context)));
    SignalExecutors.UNBOUNDED.execute(() -> callback.accept(getUsernameInternal()));
  }

  @WorkerThread
  private @NonNull Optional<String> getUsernameInternal() {
    try {
      SignalServiceProfile profile = ProfileUtil.retrieveProfile(context, Recipient.self(), SignalServiceProfile.RequestType.PROFILE).getProfile();
      TextSecurePreferences.setLocalUsername(context, profile.getUsername());
      DatabaseFactory.getRecipientDatabase(context).setUsername(Recipient.self().getId(), profile.getUsername());
    } catch (IOException e) {
      Log.w(TAG, "Failed to retrieve username remotely! Using locally-cached version.");
    }
    return Optional.fromNullable(TextSecurePreferences.getLocalUsername(context));
  }

  public enum UploadResult {
    SUCCESS,
    ERROR_FILE_IO
  }

}
