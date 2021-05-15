package com.RWdesenv.What.lock.v2;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.core.util.Consumer;

import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.keyvalue.KbsValues;
import com.RWdesenv.What.keyvalue.SignalStore;
import com.RWdesenv.What.lock.PinHashing;
import com.RWdesenv.What.lock.RegistrationLockReminders;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.megaphone.Megaphones;
import com.RWdesenv.What.util.TextSecurePreferences;
import com.RWdesenv.What.util.concurrent.SimpleTask;
import org.whispersystems.signalservice.api.KeyBackupService;
import org.whispersystems.signalservice.api.KeyBackupServicePinException;
import org.whispersystems.signalservice.api.KeyBackupSystemNoDataException;
import org.whispersystems.signalservice.api.RegistrationLockData;
import org.whispersystems.signalservice.api.kbs.HashedPin;
import org.whispersystems.signalservice.api.kbs.MasterKey;
import org.whispersystems.signalservice.internal.contacts.crypto.UnauthenticatedResponseException;

import java.io.IOException;

final class ConfirmKbsPinRepository {

  private static final String TAG = Log.tag(ConfirmKbsPinRepository.class);

  void setPin(@NonNull KbsPin kbsPin, @NonNull PinKeyboardType keyboard, @NonNull Consumer<PinSetResult> resultConsumer) {

    Context context  = ApplicationDependencies.getApplication();
    String  pinValue = kbsPin.toString();

    SimpleTask.run(() -> {
      try {
        Log.i(TAG, "Setting pin on KBS");

        KbsValues                         kbsValues        = SignalStore.kbsValues();
        MasterKey                         masterKey        = kbsValues.getOrCreateMasterKey();
        KeyBackupService                  keyBackupService = ApplicationDependencies.getKeyBackupService();
        KeyBackupService.PinChangeSession pinChangeSession = keyBackupService.newPinChangeSession();
        HashedPin                         hashedPin        = PinHashing.hashPin(pinValue, pinChangeSession);
        RegistrationLockData              kbsData          = pinChangeSession.setPin(hashedPin, masterKey);

        kbsValues.setRegistrationLockMasterKey(kbsData, PinHashing.localPinHash(pinValue));
        TextSecurePreferences.clearOldRegistrationLockPin(context);
        TextSecurePreferences.setRegistrationLockLastReminderTime(context, System.currentTimeMillis());
        TextSecurePreferences.setRegistrationLockNextReminderInterval(context, RegistrationLockReminders.INITIAL_INTERVAL);
        SignalStore.kbsValues().setKeyboardType(keyboard);
        ApplicationDependencies.getMegaphoneRepository().markFinished(Megaphones.Event.PINS_FOR_ALL);

        Log.i(TAG, "Pin set on KBS");
        return PinSetResult.SUCCESS;
      } catch (IOException | UnauthenticatedResponseException e) {
        Log.w(TAG, e);
        return PinSetResult.FAILURE;
      }
    }, resultConsumer::accept);
  }

  enum PinSetResult {
    SUCCESS,
    FAILURE
  }
}
