package com.RWdesenv.What.megaphone;

import com.RWdesenv.What.keyvalue.SignalStore;
import com.RWdesenv.What.util.FeatureFlags;

final class SignalPinReminderSchedule implements MegaphoneSchedule {

  @Override
  public boolean shouldDisplay(int seenCount, long lastSeen, long firstVisible, long currentTime) {
    if (!SignalStore.kbsValues().isV2RegistrationLockEnabled()) {
      return false;
    }

    if (!FeatureFlags.pinsForAll()) {
      return false;
    }

    long lastSuccessTime = SignalStore.pinValues().getLastSuccessfulEntryTime();
    long interval        = SignalStore.pinValues().getCurrentInterval();

    return currentTime - lastSuccessTime >= interval;
  }
}
