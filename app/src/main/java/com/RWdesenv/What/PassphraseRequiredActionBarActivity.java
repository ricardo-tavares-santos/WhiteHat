package com.RWdesenv.What;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.RWdesenv.What.crypto.MasterSecretUtil;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobs.PushNotificationReceiveJob;
import com.RWdesenv.What.keyvalue.SignalStore;
import com.RWdesenv.What.lock.v2.CreateKbsPinActivity;
import com.RWdesenv.What.lock.v2.PinUtil;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.migrations.ApplicationMigrationActivity;
import com.RWdesenv.What.migrations.ApplicationMigrations;
import com.RWdesenv.What.profiles.ProfileName;
import com.RWdesenv.What.profiles.edit.EditProfileActivity;
import com.RWdesenv.What.push.SignalServiceNetworkAccess;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.registration.RegistrationNavigationActivity;
import com.RWdesenv.What.service.KeyCachingService;
import com.RWdesenv.What.util.CensorshipUtil;
import com.RWdesenv.What.util.TextSecurePreferences;

import java.util.Locale;

public abstract class PassphraseRequiredActionBarActivity extends BaseActionBarActivity implements MasterSecretListener {
  private static final String TAG = PassphraseRequiredActionBarActivity.class.getSimpleName();

  public static final String LOCALE_EXTRA = "locale_extra";

  private static final int STATE_NORMAL              = 0;
  private static final int STATE_CREATE_PASSPHRASE   = 1;
  private static final int STATE_PROMPT_PASSPHRASE   = 2;
  private static final int STATE_UI_BLOCKING_UPGRADE = 3;
  private static final int STATE_WELCOME_PUSH_SCREEN = 4;
  private static final int STATE_CREATE_PROFILE_NAME = 5;
  private static final int STATE_CREATE_KBS_PIN      = 6;

  private SignalServiceNetworkAccess networkAccess;
  private BroadcastReceiver          clearKeyReceiver;

  @Override
  protected final void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "[" + Log.tag(getClass()) + "] onCreate()");
    this.networkAccess = new SignalServiceNetworkAccess(this);
    onPreCreate();

    final boolean locked = KeyCachingService.isLocked(this);
    routeApplicationState(locked);

    super.onCreate(savedInstanceState);

    if (!isFinishing()) {
      initializeClearKeyReceiver();
      onCreate(savedInstanceState, true);
    }
  }

  protected void onPreCreate() {}
  protected void onCreate(Bundle savedInstanceState, boolean ready) {}

  @Override
  protected void onResume() {
    Log.d(TAG, "[" + Log.tag(getClass()) + "] onResume()");
    super.onResume();

    if (networkAccess.isCensored(this)) {
      ApplicationDependencies.getJobManager().add(new PushNotificationReceiveJob(this));
    }
  }

  @Override
  protected void onStart() {
    Log.d(TAG, "[" + Log.tag(getClass()) + "] onStart()");
    super.onStart();
  }

  @Override
  protected void onPause() {
    Log.d(TAG, "[" + Log.tag(getClass()) + "] onPause()");
    super.onPause();
  }

  @Override
  protected void onStop() {
    Log.d(TAG, "[" + Log.tag(getClass()) + "] onStop()");
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    Log.d(TAG, "[" + Log.tag(getClass()) + "] onDestroy()");
    super.onDestroy();
    removeClearKeyReceiver(this);
  }

  @Override
  public void onMasterSecretCleared() {
    Log.d(TAG, "onMasterSecretCleared()");
    if (ApplicationContext.getInstance(this).isAppVisible()) routeApplicationState(true);
    else                                                     finish();
  }

  protected <T extends Fragment> T initFragment(@IdRes int target,
                                                @NonNull T fragment)
  {
    return initFragment(target, fragment, null);
  }

  protected <T extends Fragment> T initFragment(@IdRes int target,
                                                @NonNull T fragment,
                                                @Nullable Locale locale)
  {
    return initFragment(target, fragment, locale, null);
  }

  protected <T extends Fragment> T initFragment(@IdRes int target,
                                                @NonNull T fragment,
                                                @Nullable Locale locale,
                                                @Nullable Bundle extras)
  {
    Bundle args = new Bundle();
    args.putSerializable(LOCALE_EXTRA, locale);

    if (extras != null) {
      args.putAll(extras);
    }

    fragment.setArguments(args);
    getSupportFragmentManager().beginTransaction()
                               .replace(target, fragment)
                               .commitAllowingStateLoss();
    return fragment;
  }

  private void routeApplicationState(boolean locked) {
    Intent intent = getIntentForState(getApplicationState(locked));
    if (intent != null) {
      startActivity(intent);
      finish();
    }
  }

  private Intent getIntentForState(int state) {
    Log.d(TAG, "routeApplicationState(), state: " + state);

    switch (state) {
      case STATE_CREATE_PASSPHRASE:   return getCreatePassphraseIntent();
      case STATE_PROMPT_PASSPHRASE:   return getPromptPassphraseIntent();
      case STATE_UI_BLOCKING_UPGRADE: return getUiBlockingUpgradeIntent();
      case STATE_WELCOME_PUSH_SCREEN: return getPushRegistrationIntent();
      case STATE_CREATE_KBS_PIN:      return getCreateKbsPinIntent();
      case STATE_CREATE_PROFILE_NAME: return getCreateProfileNameIntent();
      default:                        return null;
    }
  }

  private int getApplicationState(boolean locked) {
    if (!MasterSecretUtil.isPassphraseInitialized(this)) {
      return STATE_CREATE_PASSPHRASE;
    } else if (locked) {
      return STATE_PROMPT_PASSPHRASE;
    } else if (ApplicationMigrations.isUpdate(this) && ApplicationMigrations.isUiBlockingMigrationRunning()) {
      return STATE_UI_BLOCKING_UPGRADE;
    } else if (!TextSecurePreferences.hasPromptedPushRegistration(this)) {
      return STATE_WELCOME_PUSH_SCREEN;
    } else if (userMustSetProfileName()) {
      return STATE_CREATE_PROFILE_NAME;
    } else if (userMustSetKbsPin()) {
      return STATE_CREATE_KBS_PIN;
    } else {
      return STATE_NORMAL;
    }
  }

  private boolean userMustSetKbsPin() {
    // TODO [greyson] [pins] Maybe re-enable in the future
//    return !SignalStore.registrationValues().isRegistrationComplete() && !PinUtil.userHasPin(this);
    return false;
  }

  private boolean userMustSetProfileName() {
    return !SignalStore.registrationValues().isRegistrationComplete() && Recipient.self().getProfileName() == ProfileName.EMPTY;
  }

  private Intent getCreatePassphraseIntent() {
    return getRoutedIntent(PassphraseCreateActivity.class, getIntent());
  }

  private Intent getPromptPassphraseIntent() {
    return getRoutedIntent(PassphrasePromptActivity.class, getIntent());
  }

  private Intent getUiBlockingUpgradeIntent() {
    return getRoutedIntent(ApplicationMigrationActivity.class,
                           TextSecurePreferences.hasPromptedPushRegistration(this)
                               ? getConversationListIntent()
                               : getPushRegistrationIntent());
  }

  private Intent getPushRegistrationIntent() {
    return RegistrationNavigationActivity.newIntentForNewRegistration(this);
  }

  private Intent getCreateKbsPinIntent() {

    final Intent intent;
    if (userMustSetProfileName()) {
      intent = getCreateProfileNameIntent();
    } else {
      intent = getIntent();
    }

    return getRoutedIntent(CreateKbsPinActivity.class, intent);
  }

  private Intent getCreateProfileNameIntent() {
    return getRoutedIntent(EditProfileActivity.class, getIntent());
  }

  private Intent getRoutedIntent(Class<?> destination, @Nullable Intent nextIntent) {
    final Intent intent = new Intent(this, destination);
    if (nextIntent != null)   intent.putExtra("next_intent", nextIntent);
    return intent;
  }

  private Intent getConversationListIntent() {
    // TODO [greyson] Navigation
    return new Intent(this, MainActivity.class);
  }

  private void initializeClearKeyReceiver() {
    this.clearKeyReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive() for clear key event");
        onMasterSecretCleared();
      }
    };

    IntentFilter filter = new IntentFilter(KeyCachingService.CLEAR_KEY_EVENT);
    registerReceiver(clearKeyReceiver, filter, KeyCachingService.KEY_PERMISSION, null);
  }

  private void removeClearKeyReceiver(Context context) {
    if (clearKeyReceiver != null) {
      context.unregisterReceiver(clearKeyReceiver);
      clearKeyReceiver = null;
    }
  }
}
