package com.RWdesenv.What.lock.v2;

import android.content.Context;

import androidx.annotation.NonNull;

import com.RWdesenv.What.keyvalue.SignalStore;
import com.RWdesenv.What.util.CensorshipUtil;
import com.RWdesenv.What.util.FeatureFlags;
import com.RWdesenv.What.util.TextSecurePreferences;

public final class PinUtil {

  private PinUtil() {}

  public static boolean userHasPin(@NonNull Context context) {
    return TextSecurePreferences.isV1RegistrationLockEnabled(context) || SignalStore.kbsValues().isV2RegistrationLockEnabled();
  }

  public static boolean shouldShowPinCreationDuringRegistration(@NonNull Context context) {
    return FeatureFlags.pinsForAll() && !PinUtil.userHasPin(context);
  }
}
