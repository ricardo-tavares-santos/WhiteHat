package com.RWdesenv.What;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.PhoneNumberUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.preference.CheckBoxPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import com.RWdesenv.What.color.MaterialColor;
import com.RWdesenv.What.color.MaterialColors;
import com.RWdesenv.What.components.SwitchPreferenceCompat;
import com.RWdesenv.What.components.ThreadPhotoRailView;
import com.RWdesenv.What.contacts.avatars.ContactPhoto;
import com.RWdesenv.What.contacts.avatars.FallbackContactPhoto;
import com.RWdesenv.What.contacts.avatars.ProfileContactPhoto;
import com.RWdesenv.What.contacts.avatars.ResourceContactPhoto;
import com.RWdesenv.What.conversation.ConversationActivity;
import com.RWdesenv.What.crypto.IdentityKeyParcelable;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.database.IdentityDatabase;
import com.RWdesenv.What.database.IdentityDatabase.IdentityRecord;
import com.RWdesenv.What.database.MediaDatabase;
import com.RWdesenv.What.database.RecipientDatabase;
import com.RWdesenv.What.database.RecipientDatabase.VibrateState;
import com.RWdesenv.What.database.loaders.RecipientMediaLoader;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobs.MultiDeviceContactUpdateJob;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.mediaoverview.MediaOverviewActivity;
import com.RWdesenv.What.mms.GlideApp;
import com.RWdesenv.What.mms.GlideRequests;
import com.RWdesenv.What.notifications.NotificationChannels;
import com.RWdesenv.What.permissions.Permissions;
import com.RWdesenv.What.preferences.CorrectedPreferenceFragment;
import com.RWdesenv.What.preferences.widgets.ColorPickerPreference;
import com.RWdesenv.What.preferences.widgets.ContactPreference;
import com.RWdesenv.What.recipients.LiveRecipient;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientId;
import com.RWdesenv.What.recipients.RecipientUtil;
import com.RWdesenv.What.util.CommunicationActions;
import com.RWdesenv.What.util.Dialogs;
import com.RWdesenv.What.util.DynamicDarkToolbarTheme;
import com.RWdesenv.What.util.DynamicLanguage;
import com.RWdesenv.What.util.DynamicTheme;
import com.RWdesenv.What.util.FeatureFlags;
import com.RWdesenv.What.util.IdentityUtil;
import com.RWdesenv.What.util.ServiceUtil;
import com.RWdesenv.What.util.TextSecurePreferences;
import com.RWdesenv.What.util.ThemeUtil;
import com.RWdesenv.What.util.Util;
import com.RWdesenv.What.util.ViewUtil;
import com.RWdesenv.What.util.concurrent.ListenableFuture;
import com.RWdesenv.What.util.concurrent.SignalExecutors;
import com.RWdesenv.What.util.concurrent.SimpleTask;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.concurrent.ExecutionException;

@SuppressLint("StaticFieldLeak")
public class RecipientPreferenceActivity extends PassphraseRequiredActionBarActivity implements LoaderManager.LoaderCallbacks<Cursor>
{
  private static final String TAG = RecipientPreferenceActivity.class.getSimpleName();

  public static final String RECIPIENT_ID = "recipient";

  private static final String PREFERENCE_MUTED                 = "pref_key_recipient_mute";
  private static final String PREFERENCE_MESSAGE_TONE          = "pref_key_recipient_ringtone";
  private static final String PREFERENCE_CALL_TONE             = "pref_key_recipient_call_ringtone";
  private static final String PREFERENCE_MESSAGE_VIBRATE       = "pref_key_recipient_vibrate";
  private static final String PREFERENCE_CALL_VIBRATE          = "pref_key_recipient_call_vibrate";
  private static final String PREFERENCE_BLOCK                 = "pref_key_recipient_block";
  private static final String PREFERENCE_COLOR                 = "pref_key_recipient_color";
  private static final String PREFERENCE_IDENTITY              = "pref_key_recipient_identity";
  private static final String PREFERENCE_ABOUT                 = "pref_key_number";
  private static final String PREFERENCE_CUSTOM_NOTIFICATIONS  = "pref_key_recipient_custom_notifications";

  private final DynamicTheme    dynamicTheme    = new DynamicDarkToolbarTheme();
  private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

  private ImageView               avatar;
  private GlideRequests           glideRequests;
  private RecipientId             recipientId;
  private TextView                threadPhotoRailLabel;
  private ThreadPhotoRailView     threadPhotoRailView;
  private CollapsingToolbarLayout toolbarLayout;

  public static @NonNull Intent getLaunchIntent(@NonNull Context context, @NonNull RecipientId id) {
    Intent intent = new Intent(context, RecipientPreferenceActivity.class);
    intent.putExtra(RecipientPreferenceActivity.RECIPIENT_ID, id);

    return intent;
  }

  @Override
  public void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  public void onCreate(Bundle instanceState, boolean ready) {
    setContentView(R.layout.recipient_preference_activity);
    this.glideRequests = GlideApp.with(this);
    this.recipientId   = getIntent().getParcelableExtra(RECIPIENT_ID);

    LiveRecipient recipient = Recipient.live(recipientId);

    initializeToolbar();
    setHeader(recipient.get());
    recipient.observe(this, this::setHeader);

    getSupportLoaderManager().initLoader(0, null, this);
  }

  @Override
  public void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.preference_fragment);
    fragment.onActivityResult(requestCode, resultCode, data);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
      case android.R.id.home:
        onBackPressed();
        return true;
    }

    return false;
  }

  private void initializeToolbar() {
    this.toolbarLayout        = ViewUtil.findById(this, R.id.collapsing_toolbar);
    this.avatar               = ViewUtil.findById(this, R.id.avatar);
    this.threadPhotoRailView  = ViewUtil.findById(this, R.id.recent_photos);
    this.threadPhotoRailLabel = ViewUtil.findById(this, R.id.rail_label);

    this.toolbarLayout.setExpandedTitleColor(ThemeUtil.getThemedColor(this, R.attr.conversation_title_color));
    this.toolbarLayout.setCollapsedTitleTextColor(ThemeUtil.getThemedColor(this, R.attr.conversation_title_color));

    this.threadPhotoRailView.setListener(mediaRecord -> {
      Intent intent = new Intent(RecipientPreferenceActivity.this, MediaPreviewActivity.class);
      intent.putExtra(MediaPreviewActivity.THREAD_ID_EXTRA, mediaRecord.getThreadId());
      intent.putExtra(MediaPreviewActivity.DATE_EXTRA, mediaRecord.getDate());
      intent.putExtra(MediaPreviewActivity.SIZE_EXTRA, mediaRecord.getAttachment().getSize());
      intent.putExtra(MediaPreviewActivity.CAPTION_EXTRA, mediaRecord.getAttachment().getCaption());
      intent.putExtra(MediaPreviewActivity.LEFT_IS_RECENT_EXTRA, ViewCompat.getLayoutDirection(threadPhotoRailView) == ViewCompat.LAYOUT_DIRECTION_LTR);
      intent.setDataAndType(mediaRecord.getAttachment().getDataUri(), mediaRecord.getContentType());
      startActivity(intent);
    });

    SimpleTask.run(
      () -> DatabaseFactory.getThreadDatabase(this).getThreadIdFor(recipientId),
      (threadId) -> {
        if (threadId == null) {
          Log.i(TAG, "No thread id for recipient.");
        } else {
          this.threadPhotoRailLabel.setOnClickListener(v -> startActivity(MediaOverviewActivity.forThread(this, threadId)));
        }
      }
    );

    Toolbar toolbar = ViewUtil.findById(this, R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setLogo(null);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
      getWindow().setStatusBarColor(Color.TRANSPARENT);

      ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.recipient_preference_root), (v, insets) -> {
        ViewUtil.setTopMargin(toolbar, insets.getSystemWindowInsetTop());
        return insets;
      });
    }
  }

  private void setHeader(@NonNull Recipient recipient) {
    ContactPhoto         contactPhoto  = recipient.isLocalNumber() ? new ProfileContactPhoto(recipient, recipient.getProfileAvatar())
                                                                   : recipient.getContactPhoto();
    FallbackContactPhoto fallbackPhoto = recipient.isLocalNumber() ? new ResourceContactPhoto(R.drawable.ic_profile_outline_40, R.drawable.ic_profile_outline_20, R.drawable.ic_person_large)
                                                                   : recipient.getFallbackContactPhoto();

    glideRequests.load(contactPhoto)
                 .fallback(fallbackPhoto.asCallCard(this))
                 .error(fallbackPhoto.asCallCard(this))
                 .diskCacheStrategy(DiskCacheStrategy.ALL)
                 .into(this.avatar);

    if (contactPhoto == null) this.avatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
    else                      this.avatar.setScaleType(ImageView.ScaleType.CENTER_CROP);

    this.avatar.setBackgroundColor(recipient.getColor().toActionBarColor(this));
    this.toolbarLayout.setTitle(recipient.toShortString(this));
    this.toolbarLayout.setContentScrimColor(recipient.getColor().toActionBarColor(this));
    if (recipient.getUuid().isPresent()) {
      toolbarLayout.setOnLongClickListener(v -> {
        Util.copyToClipboard(this, recipient.getUuid().get().toString());
        ServiceUtil.getVibrator(this).vibrate(200);
        return true;
      });
    }
  }

  @Override
  public @NonNull Loader<Cursor> onCreateLoader(int id, Bundle args) {
    return new RecipientMediaLoader(this, recipientId, RecipientMediaLoader.MediaType.GALLERY, MediaDatabase.Sorting.Newest);
  }

  @Override
  public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
    if (data != null && data.getCount() > 0) {
      this.threadPhotoRailLabel.setVisibility(View.VISIBLE);
      this.threadPhotoRailView.setVisibility(View.VISIBLE);
    } else {
      this.threadPhotoRailLabel.setVisibility(View.GONE);
      this.threadPhotoRailView.setVisibility(View.GONE);
    }

    this.threadPhotoRailView.setCursor(glideRequests, data);

    Bundle bundle = new Bundle();
    bundle.putParcelable(RECIPIENT_ID, recipientId);
    initFragment(R.id.preference_fragment, new RecipientPreferenceFragment(), null, bundle);
  }

  @Override
  public void onLoaderReset(@NonNull Loader<Cursor> loader) {
    this.threadPhotoRailView.setCursor(glideRequests, null);
  }

  public static class RecipientPreferenceFragment extends CorrectedPreferenceFragment {
    private LiveRecipient recipient;
    private boolean       canHaveSafetyNumber;

    @Override
    public void onCreate(Bundle icicle) {
      Log.i(TAG, "onCreate (fragment)");
      super.onCreate(icicle);

      initializeRecipients();

      this.canHaveSafetyNumber = recipient.get().isRegistered() && !recipient.get().isLocalNumber();

      Preference customNotificationsPref  = this.findPreference(PREFERENCE_CUSTOM_NOTIFICATIONS);

      if (NotificationChannels.supported()) {
        ((SwitchPreferenceCompat) customNotificationsPref).setChecked(recipient.get().getNotificationChannel() != null);
        customNotificationsPref.setOnPreferenceChangeListener(new CustomNotificationsChangedListener());

        this.findPreference(PREFERENCE_MESSAGE_TONE).setDependency(PREFERENCE_CUSTOM_NOTIFICATIONS);
        this.findPreference(PREFERENCE_MESSAGE_VIBRATE).setDependency(PREFERENCE_CUSTOM_NOTIFICATIONS);

        if (recipient.get().getNotificationChannel() != null) {
          final Context context = requireContext();
          new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
              RecipientDatabase db = DatabaseFactory.getRecipientDatabase(getContext());
              db.setMessageRingtone(recipient.getId(), NotificationChannels.getMessageRingtone(context, recipient.get()));
              db.setMessageVibrate(recipient.getId(), NotificationChannels.getMessageVibrate(context, recipient.get()) ? VibrateState.ENABLED : VibrateState.DISABLED);
              NotificationChannels.ensureCustomChannelConsistency(context);
              return null;
            }
          }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
      } else {
        customNotificationsPref.setVisible(false);
      }

      this.findPreference(PREFERENCE_MESSAGE_TONE)
          .setOnPreferenceChangeListener(new RingtoneChangeListener(false));
      this.findPreference(PREFERENCE_MESSAGE_TONE)
          .setOnPreferenceClickListener(new RingtoneClickedListener(false));
      this.findPreference(PREFERENCE_CALL_TONE)
          .setOnPreferenceChangeListener(new RingtoneChangeListener(true));
      this.findPreference(PREFERENCE_CALL_TONE)
          .setOnPreferenceClickListener(new RingtoneClickedListener(true));
      this.findPreference(PREFERENCE_MESSAGE_VIBRATE)
          .setOnPreferenceChangeListener(new VibrateChangeListener(false));
      this.findPreference(PREFERENCE_CALL_VIBRATE)
          .setOnPreferenceChangeListener(new VibrateChangeListener(true));
      this.findPreference(PREFERENCE_MUTED)
          .setOnPreferenceClickListener(new MuteClickedListener());
      this.findPreference(PREFERENCE_BLOCK)
          .setOnPreferenceClickListener(new BlockClickedListener());
      this.findPreference(PREFERENCE_COLOR)
          .setOnPreferenceChangeListener(new ColorChangeListener());
      ((ContactPreference)this.findPreference(PREFERENCE_ABOUT))
          .setListener(new AboutNumberClickedListener());
    }

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, String rootKey) {
      Log.i(TAG, "onCreatePreferences...");
      addPreferencesFromResource(R.xml.recipient_preferences);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
      Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    public void onResume() {
      super.onResume();
      setSummaries(recipient.get());
    }

    @Override
    public void onDestroy() {
      super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

        findPreference(PREFERENCE_MESSAGE_TONE).getOnPreferenceChangeListener().onPreferenceChange(findPreference(PREFERENCE_MESSAGE_TONE), uri);
      } else if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
        Uri uri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

        findPreference(PREFERENCE_CALL_TONE).getOnPreferenceChangeListener().onPreferenceChange(findPreference(PREFERENCE_CALL_TONE), uri);
      }
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
      RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
      recyclerView.setItemAnimator(null);
      recyclerView.setLayoutAnimation(null);
      return recyclerView;
    }

    private void initializeRecipients() {
      this.recipient = Recipient.live(getArguments().getParcelable(RECIPIENT_ID));
      this.recipient.observe(this, this::setSummaries);
    }

    private void setSummaries(Recipient recipient) {
      CheckBoxPreference    mutePreference            = (CheckBoxPreference) this.findPreference(PREFERENCE_MUTED);
      Preference            customPreference          = this.findPreference(PREFERENCE_CUSTOM_NOTIFICATIONS);
      Preference            ringtoneMessagePreference = this.findPreference(PREFERENCE_MESSAGE_TONE);
      Preference            ringtoneCallPreference    = this.findPreference(PREFERENCE_CALL_TONE);
      ListPreference        vibrateMessagePreference  = (ListPreference) this.findPreference(PREFERENCE_MESSAGE_VIBRATE);
      ListPreference        vibrateCallPreference     = (ListPreference) this.findPreference(PREFERENCE_CALL_VIBRATE);
      ColorPickerPreference colorPreference           = (ColorPickerPreference) this.findPreference(PREFERENCE_COLOR);
      Preference            blockPreference           = this.findPreference(PREFERENCE_BLOCK);
      Preference            identityPreference        = this.findPreference(PREFERENCE_IDENTITY);
      PreferenceCategory    callCategory              = (PreferenceCategory)this.findPreference("call_settings");
      PreferenceCategory    aboutCategory             = (PreferenceCategory)this.findPreference("about");
      PreferenceCategory    aboutDivider              = (PreferenceCategory)this.findPreference("about_divider");
      ContactPreference     aboutPreference           = (ContactPreference)this.findPreference(PREFERENCE_ABOUT);
      PreferenceCategory    privacyCategory           = (PreferenceCategory) this.findPreference("privacy_settings");
      PreferenceCategory    divider                   = (PreferenceCategory) this.findPreference("divider");

      mutePreference.setChecked(recipient.isMuted());

      ringtoneMessagePreference.setSummary(ringtoneMessagePreference.isEnabled() ? getRingtoneSummary(getContext(), recipient.getMessageRingtone()) : "");
      ringtoneCallPreference.setSummary(getRingtoneSummary(getContext(), recipient.getCallRingtone()));

      Pair<String, Integer> vibrateMessageSummary = getVibrateSummary(getContext(), recipient.getMessageVibrate());
      Pair<String, Integer> vibrateCallSummary    = getVibrateSummary(getContext(), recipient.getCallVibrate());

      vibrateMessagePreference.setSummary(vibrateMessagePreference.isEnabled() ? vibrateMessageSummary.first : "");
      vibrateMessagePreference.setValueIndex(vibrateMessageSummary.second);

      vibrateCallPreference.setSummary(vibrateCallSummary.first);
      vibrateCallPreference.setValueIndex(vibrateCallSummary.second);

      blockPreference.setVisible(RecipientUtil.isBlockable(recipient));
      if (recipient.isBlocked()) blockPreference.setTitle(R.string.RecipientPreferenceActivity_unblock);
      else                       blockPreference.setTitle(R.string.RecipientPreferenceActivity_block);

      if (recipient.isLocalNumber()) {
        mutePreference.setVisible(false);
        customPreference.setVisible(false);
        ringtoneMessagePreference.setVisible(false);
        vibrateMessagePreference.setVisible(false);

        if (identityPreference != null) identityPreference.setVisible(false);
        if (aboutCategory      != null) aboutCategory.setVisible(false);
        if (aboutDivider       != null) aboutDivider.setVisible(false);
        if (privacyCategory    != null) privacyCategory.setVisible(false);
        if (divider            != null) divider.setVisible(false);
        if (callCategory       != null) callCategory.setVisible(false);
      }

      if (recipient.isGroup()) {
        if (colorPreference    != null) colorPreference.setVisible(false);
        if (identityPreference != null) identityPreference.setVisible(false);
        if (callCategory       != null) callCategory.setVisible(false);
        if (aboutCategory      != null) aboutCategory.setVisible(false);
        if (aboutDivider       != null) aboutDivider.setVisible(false);
        if (divider            != null) divider.setVisible(false);
      } else {
        colorPreference.setColors(MaterialColors.CONVERSATION_PALETTE.asConversationColorArray(requireActivity()));
        colorPreference.setColor(recipient.getColor().toActionBarColor(requireActivity()));

        if (FeatureFlags.profileDisplay()) {
          aboutPreference.setTitle(recipient.getDisplayName(requireContext()));
          aboutPreference.setSummary(recipient.resolve().getE164().or(""));
        } else {
          aboutPreference.setTitle(formatRecipient(recipient));
          aboutPreference.setSummary(recipient.getCustomLabel());
        }

        aboutPreference.setSecure(recipient.getRegistered() == RecipientDatabase.RegisteredState.REGISTERED);

        IdentityUtil.getRemoteIdentityKey(getActivity(), recipient).addListener(new ListenableFuture.Listener<Optional<IdentityRecord>>() {
          @Override
          public void onSuccess(Optional<IdentityRecord> result) {
            if (result.isPresent()) {
              if (identityPreference != null) identityPreference.setOnPreferenceClickListener(new IdentityClickedListener(result.get()));
              if (identityPreference != null) identityPreference.setEnabled(true);
            } else if (canHaveSafetyNumber) {
              if (identityPreference != null) identityPreference.setSummary(R.string.RecipientPreferenceActivity_available_once_a_message_has_been_sent_or_received);
              if (identityPreference != null) identityPreference.setEnabled(false);
            } else {
              if (identityPreference != null) getPreferenceScreen().removePreference(identityPreference);
            }
          }

          @Override
          public void onFailure(ExecutionException e) {
            if (identityPreference != null) getPreferenceScreen().removePreference(identityPreference);
          }
        });
      }

      if (recipient.isMmsGroup() && privacyCategory != null) {
        privacyCategory.setVisible(false);
      }
    }

    private @NonNull String formatRecipient(@NonNull Recipient recipient) {
      if      (recipient.getE164().isPresent())  return PhoneNumberUtils.formatNumber(recipient.requireE164());
      else if (recipient.getEmail().isPresent()) return recipient.requireEmail();
      else                                       return "";
    }

    private @NonNull String getRingtoneSummary(@NonNull Context context, @Nullable Uri ringtone) {
      if (ringtone == null) {
        return context.getString(R.string.preferences__default);
      } else if (ringtone.toString().isEmpty()) {
        return context.getString(R.string.preferences__silent);
      } else {
        Ringtone tone = RingtoneManager.getRingtone(getActivity(), ringtone);

        if (tone != null) {
          return tone.getTitle(context);
        }
      }

      return context.getString(R.string.preferences__default);
    }

    private @NonNull Pair<String, Integer> getVibrateSummary(@NonNull Context context, @NonNull VibrateState vibrateState) {
      if (vibrateState == VibrateState.DEFAULT) {
        return new Pair<>(context.getString(R.string.preferences__default), 0);
      } else if (vibrateState == VibrateState.ENABLED) {
        return new Pair<>(context.getString(R.string.RecipientPreferenceActivity_enabled), 1);
      } else {
        return new Pair<>(context.getString(R.string.RecipientPreferenceActivity_disabled), 2);
      }
    }

    private class RingtoneChangeListener implements Preference.OnPreferenceChangeListener {

      private final boolean calls;

      RingtoneChangeListener(boolean calls) {
        this.calls = calls;
      }

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = preference.getContext();

        Uri value = (Uri)newValue;

        Uri defaultValue;

        if (calls) defaultValue = TextSecurePreferences.getCallNotificationRingtone(context);
        else       defaultValue = TextSecurePreferences.getNotificationRingtone(context);

        if (defaultValue.equals(value)) value = null;
        else if (value == null)         value = Uri.EMPTY;


        new AsyncTask<Uri, Void, Void>() {
          @Override
          protected Void doInBackground(Uri... params) {
            if (calls) {
              DatabaseFactory.getRecipientDatabase(context).setCallRingtone(recipient.getId(), params[0]);
            } else {
              DatabaseFactory.getRecipientDatabase(context).setMessageRingtone(recipient.getId(), params[0]);
              NotificationChannels.updateMessageRingtone(context, recipient.get(), params[0]);
            }
            return null;
          }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, value);

        return false;
      }
    }

    private class RingtoneClickedListener implements Preference.OnPreferenceClickListener {

      private final boolean calls;

      RingtoneClickedListener(boolean calls) {
        this.calls = calls;
      }

      @Override
      public boolean onPreferenceClick(Preference preference) {
        Uri current;
        Uri defaultUri;

        if (calls) {
          current    = recipient.get().getCallRingtone();
          defaultUri = TextSecurePreferences.getCallNotificationRingtone(getContext());
        } else  {
          current    = recipient.get().getMessageRingtone();
          defaultUri = TextSecurePreferences.getNotificationRingtone(getContext());
        }

        if      (current == null)              current = Settings.System.DEFAULT_NOTIFICATION_URI;
        else if (current.toString().isEmpty()) current = null;

        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, calls ? RingtoneManager.TYPE_RINGTONE : RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current);

        startActivityForResult(intent, calls ? 2 : 1);

        return true;
      }
    }

    private class VibrateChangeListener implements Preference.OnPreferenceChangeListener {

      private final boolean call;

      VibrateChangeListener(boolean call) {
        this.call = call;
      }

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
              int          value        = Integer.parseInt((String) newValue);
        final VibrateState vibrateState = VibrateState.fromId(value);
        final Context      context      = preference.getContext();

        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            if (call) {
              DatabaseFactory.getRecipientDatabase(context).setCallVibrate(recipient.getId(), vibrateState);
            }
            else {
              DatabaseFactory.getRecipientDatabase(context).setMessageVibrate(recipient.getId(), vibrateState);
              NotificationChannels.updateMessageVibrate(context, recipient.get(), vibrateState);
            }
            return null;
          }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        return false;
      }
    }

    private class ColorChangeListener implements Preference.OnPreferenceChangeListener {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getContext();
        if (context == null) return true;

        final int           value         = (Integer) newValue;
        final MaterialColor selectedColor = MaterialColors.CONVERSATION_PALETTE.getByColor(context, value);
        final MaterialColor currentColor  = recipient.get().getColor();

        if (selectedColor == null) return true;

        if (preference.isEnabled() && !currentColor.equals(selectedColor)) {
          new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
              DatabaseFactory.getRecipientDatabase(context).setColor(recipient.getId(), selectedColor);

              if (recipient.get().resolve().getRegistered() == RecipientDatabase.RegisteredState.REGISTERED) {
                ApplicationDependencies.getJobManager().add(new MultiDeviceContactUpdateJob(recipient.getId()));
              }
              return null;
            }
          }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        return true;
      }
    }

    private class MuteClickedListener implements Preference.OnPreferenceClickListener {

      @Override
      public boolean onPreferenceClick(Preference preference) {
        if (recipient.get().isMuted()) handleUnmute(preference.getContext());
        else                           handleMute(preference.getContext());

        return true;
      }

      private void handleMute(@NonNull Context context) {
        MuteDialog.show(context, until -> setMuted(context, recipient.get(), until));

        setSummaries(recipient.get());
      }

      private void handleUnmute(@NonNull Context context) {
        setMuted(context, recipient.get(), 0);
      }

      private void setMuted(@NonNull final Context context, final Recipient recipient, final long until) {
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            DatabaseFactory.getRecipientDatabase(context)
                           .setMuted(recipient.getId(), until);
            return null;
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }
    }

    private class IdentityClickedListener implements Preference.OnPreferenceClickListener {

      private final IdentityRecord identityKey;

      private IdentityClickedListener(IdentityRecord identityKey) {
        Log.i(TAG, "Identity record: " + identityKey);
        this.identityKey = identityKey;
      }

      @Override
      public boolean onPreferenceClick(Preference preference) {
        Intent verifyIdentityIntent = new Intent(preference.getContext(), VerifyIdentityActivity.class);
        verifyIdentityIntent.putExtra(VerifyIdentityActivity.RECIPIENT_EXTRA, recipient.getId());
        verifyIdentityIntent.putExtra(VerifyIdentityActivity.IDENTITY_EXTRA, new IdentityKeyParcelable(identityKey.getIdentityKey()));
        verifyIdentityIntent.putExtra(VerifyIdentityActivity.VERIFIED_EXTRA, identityKey.getVerifiedStatus() == IdentityDatabase.VerifiedStatus.VERIFIED);
        startActivity(verifyIdentityIntent);

        return true;
      }
    }

    private class BlockClickedListener implements Preference.OnPreferenceClickListener {
      @Override
      public boolean onPreferenceClick(Preference preference) {
        if (recipient.get().isBlocked()) handleUnblock(preference.getContext());
        else                             handleBlock(preference.getContext());

        return true;
      }

      private void handleBlock(@NonNull final Context context) {
        new AsyncTask<Void, Void, Pair<Integer, Integer>>() {

          @Override
          protected Pair<Integer, Integer> doInBackground(Void... voids) {
            int titleRes = R.string.RecipientPreferenceActivity_block_this_contact_question;
            int bodyRes  = R.string.RecipientPreferenceActivity_you_will_no_longer_receive_messages_and_calls_from_this_contact;

            if (recipient.get().isGroup()) {
              bodyRes = R.string.RecipientPreferenceActivity_block_and_leave_group_description;

              if (recipient.get().isGroup() && DatabaseFactory.getGroupDatabase(context).isActive(recipient.get().requireGroupId())) {
                titleRes = R.string.RecipientPreferenceActivity_block_and_leave_group;
              } else {
                titleRes = R.string.RecipientPreferenceActivity_block_group;
              }
            }

            return new Pair<>(titleRes, bodyRes);
          }

          @Override
          protected void onPostExecute(Pair<Integer, Integer> titleAndBody) {
            new AlertDialog.Builder(context)
                           .setTitle(titleAndBody.first)
                           .setMessage(titleAndBody.second)
                           .setCancelable(true)
                           .setNegativeButton(android.R.string.cancel, null)
                           .setPositiveButton(R.string.RecipientPreferenceActivity_block, (dialog, which) -> {
                             setBlocked(context, recipient.get(), true);
                           }).show();
          }
        }.execute();
      }

      private void handleUnblock(@NonNull Context context) {
        int titleRes = R.string.RecipientPreferenceActivity_unblock_this_contact_question;
        int bodyRes  = R.string.RecipientPreferenceActivity_you_will_once_again_be_able_to_receive_messages_and_calls_from_this_contact;

        if (recipient.resolve().isGroup()) {
          titleRes = R.string.RecipientPreferenceActivity_unblock_this_group_question;
          bodyRes  = R.string.RecipientPreferenceActivity_unblock_this_group_description;
        }

        new AlertDialog.Builder(context)
                       .setTitle(titleRes)
                       .setMessage(bodyRes)
                       .setCancelable(true)
                       .setNegativeButton(android.R.string.cancel, null)
                       .setPositiveButton(R.string.RecipientPreferenceActivity_unblock, (dialog, which) -> setBlocked(context, recipient.get(), false)).show();
      }

      private void setBlocked(@NonNull final Context context, final Recipient recipient, final boolean blocked) {
        SignalExecutors.BOUNDED.execute(() -> {
          if (blocked) {
            RecipientUtil.block(context, recipient);
          } else {
            RecipientUtil.unblock(context, recipient);
          }
        });
      }
    }

    private class AboutNumberClickedListener implements ContactPreference.Listener {

      @Override
      public void onMessageClicked() {
        CommunicationActions.startConversation(getContext(), recipient.get(), null);
      }

      @Override
      public void onSecureCallClicked() {
        CommunicationActions.startVoiceCall(getActivity(), recipient.get());
      }

      @Override
      public void onSecureVideoClicked() {
        CommunicationActions.startVideoCall(getActivity(), recipient.get());
      }

      @Override
      public void onInSecureCallClicked() {
        CommunicationActions.startInsecureCall(requireActivity(), recipient.get());
      }
    }

    private class CustomNotificationsChangedListener implements Preference.OnPreferenceChangeListener {

      @Override
      public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = preference.getContext();
        final boolean enabled = (boolean) newValue;

        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            if (enabled) {
              String channel = NotificationChannels.createChannelFor(context, recipient.get());
              DatabaseFactory.getRecipientDatabase(context).setNotificationChannel(recipient.getId(), channel);
            } else {
              NotificationChannels.deleteChannelFor(context, recipient.get());
              DatabaseFactory.getRecipientDatabase(context).setNotificationChannel(recipient.getId(), null);
            }
            return null;
          }
        }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);

        return true;
      }
    }
  }
}
