package com.RWdesenv.What.megaphone;

import android.content.Context;

import androidx.annotation.VisibleForTesting;

import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.keyvalue.SignalStore;
import com.RWdesenv.What.push.SignalServiceNetworkAccess;
import com.RWdesenv.What.util.CensorshipUtil;
import com.RWdesenv.What.util.FeatureFlags;
import com.RWdesenv.What.util.TextSecurePreferences;
import com.RWdesenv.What.util.Util;

import java.util.concurrent.TimeUnit;

class PinsForAllSchedule implements MegaphoneSchedule {

  @VisibleForTesting
  static final long DAYS_UNTIL_FULLSCREEN = 8L;

  @VisibleForTesting
  static final long DAYS_REMAINING_MAX    = DAYS_UNTIL_FULLSCREEN - 1;

  private final MegaphoneSchedule schedule = new RecurringSchedule(TimeUnit.DAYS.toMillis(2));

  static boolean shouldDisplayFullScreen(long firstVisible, long currentTime) {
    return false;
    // TODO [greyson] [pins] Maybe re-enable if we ever do a blocking flow again
//    if (pinCreationFailedDuringRegistration()) {
//      return true;
//    }
//
//    if (firstVisible == 0L) {
//      return false;
//    } else {
//      return currentTime - firstVisible >= TimeUnit.DAYS.toMillis(DAYS_UNTIL_FULLSCREEN);
//    }
  }

  static long getDaysRemaining(long firstVisible, long currentTime) {
    if (firstVisible == 0L) {
      return DAYS_REMAINING_MAX;
    } else {
      return Util.clamp(DAYS_REMAINING_MAX - TimeUnit.MILLISECONDS.toDays(currentTime - firstVisible), 0, DAYS_REMAINING_MAX);
    }
  }

  @Override
  public boolean shouldDisplay(int seenCount, long lastSeen, long firstVisible, long currentTime) {
    if (!isEnabled()) return false;

    if (shouldDisplayFullScreen(firstVisible, currentTime)) {
      return true;
    } else {
      return schedule.shouldDisplay(seenCount, lastSeen, firstVisible, currentTime);
    }
  }

  private static boolean isEnabled() {
    if (FeatureFlags.pinsForAllMegaphoneKillSwitch()) {
      return false;
    }

    if (pinCreationFailedDuringRegistration()) {
      return true;
    }

    if (newlyRegisteredV1PinUser()) {
      return true;
    }

    if (SignalStore.registrationValues().pinWasRequiredAtRegistration()) {
      return false;
    }

    if (SignalStore.kbsValues().hasMigratedToPinsForAll()) {
      return false;
    }

    return FeatureFlags.pinsForAll();
  }

  private static boolean pinCreationFailedDuringRegistration() {
    return SignalStore.registrationValues().pinWasRequiredAtRegistration() &&
           !SignalStore.kbsValues().isV2RegistrationLockEnabled()          &&
           !TextSecurePreferences.isV1RegistrationLockEnabled(ApplicationDependencies.getApplication());
  }

  private static final boolean newlyRegisteredV1PinUser() {
    return SignalStore.registrationValues().pinWasRequiredAtRegistration() && TextSecurePreferences.isV1RegistrationLockEnabled(ApplicationDependencies.getApplication());
  }

}
