/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.RWdesenv.What.conversation;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Browser;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.ViewModelProviders;

import com.annimon.stream.Stream;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import com.RWdesenv.What.ApplicationContext;
import com.RWdesenv.What.ExpirationDialog;
import com.RWdesenv.What.GroupCreateActivity;
import com.RWdesenv.What.GroupMembersDialog;
import com.RWdesenv.What.MainActivity;
import com.RWdesenv.What.MuteDialog;
import com.RWdesenv.What.PassphraseRequiredActionBarActivity;
import com.RWdesenv.What.PromptMmsActivity;
import com.RWdesenv.What.R;
import com.RWdesenv.What.RecipientPreferenceActivity;
import com.RWdesenv.What.ShortcutLauncherActivity;
import com.RWdesenv.What.TransportOption;
import com.RWdesenv.What.VerifyIdentityActivity;
import com.RWdesenv.What.attachments.Attachment;
import com.RWdesenv.What.attachments.TombstoneAttachment;
import com.RWdesenv.What.audio.AudioRecorder;
import com.RWdesenv.What.audio.AudioSlidePlayer;
import com.RWdesenv.What.color.MaterialColor;
import com.RWdesenv.What.components.AnimatingToggle;
import com.RWdesenv.What.components.ComposeText;
import com.RWdesenv.What.components.ConversationSearchBottomBar;
import com.RWdesenv.What.components.HidingLinearLayout;
import com.RWdesenv.What.components.InputAwareLayout;
import com.RWdesenv.What.components.InputPanel;
import com.RWdesenv.What.components.KeyboardAwareLinearLayout.OnKeyboardShownListener;
import com.RWdesenv.What.components.SendButton;
import com.RWdesenv.What.components.TooltipPopup;
import com.RWdesenv.What.components.emoji.EmojiKeyboardProvider;
import com.RWdesenv.What.components.emoji.EmojiStrings;
import com.RWdesenv.What.components.emoji.MediaKeyboard;
import com.RWdesenv.What.components.identity.UntrustedSendDialog;
import com.RWdesenv.What.components.identity.UnverifiedBannerView;
import com.RWdesenv.What.components.identity.UnverifiedSendDialog;
import com.RWdesenv.What.components.location.SignalPlace;
import com.RWdesenv.What.components.reminder.ExpiredBuildReminder;
import com.RWdesenv.What.components.reminder.Reminder;
import com.RWdesenv.What.components.reminder.ReminderView;
import com.RWdesenv.What.components.reminder.ServiceOutageReminder;
import com.RWdesenv.What.components.reminder.UnauthorizedReminder;
import com.RWdesenv.What.contacts.ContactAccessor;
import com.RWdesenv.What.contacts.ContactAccessor.ContactData;
import com.RWdesenv.What.contacts.sync.DirectoryHelper;
import com.RWdesenv.What.contactshare.Contact;
import com.RWdesenv.What.contactshare.ContactShareEditActivity;
import com.RWdesenv.What.contactshare.ContactUtil;
import com.RWdesenv.What.contactshare.SimpleTextWatcher;
import com.RWdesenv.What.conversationlist.model.MessageResult;
import com.RWdesenv.What.crypto.IdentityKeyParcelable;
import com.RWdesenv.What.crypto.SecurityEvent;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.database.DraftDatabase;
import com.RWdesenv.What.database.DraftDatabase.Draft;
import com.RWdesenv.What.database.DraftDatabase.Drafts;
import com.RWdesenv.What.database.GroupDatabase;
import com.RWdesenv.What.database.IdentityDatabase;
import com.RWdesenv.What.database.IdentityDatabase.IdentityRecord;
import com.RWdesenv.What.database.IdentityDatabase.VerifiedStatus;
import com.RWdesenv.What.database.MessagingDatabase.MarkedMessageInfo;
import com.RWdesenv.What.database.MmsSmsColumns.Types;
import com.RWdesenv.What.database.RecipientDatabase;
import com.RWdesenv.What.database.RecipientDatabase.RegisteredState;
import com.RWdesenv.What.database.ThreadDatabase;
import com.RWdesenv.What.database.identity.IdentityRecordList;
import com.RWdesenv.What.database.model.MessageRecord;
import com.RWdesenv.What.database.model.MmsMessageRecord;
import com.RWdesenv.What.database.model.ReactionRecord;
import com.RWdesenv.What.database.model.StickerRecord;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.events.ReminderUpdateEvent;
import com.RWdesenv.What.giph.ui.GiphyActivity;
import com.RWdesenv.What.groups.GroupManager;
import com.RWdesenv.What.insights.InsightsLauncher;
import com.RWdesenv.What.invites.InviteReminderModel;
import com.RWdesenv.What.invites.InviteReminderRepository;
import com.RWdesenv.What.jobs.RetrieveProfileJob;
import com.RWdesenv.What.jobs.ServiceOutageDetectionJob;
import com.RWdesenv.What.linkpreview.LinkPreview;
import com.RWdesenv.What.linkpreview.LinkPreviewRepository;
import com.RWdesenv.What.linkpreview.LinkPreviewViewModel;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.maps.PlacePickerActivity;
import com.RWdesenv.What.mediaoverview.MediaOverviewActivity;
import com.RWdesenv.What.mediasend.Media;
import com.RWdesenv.What.mediasend.MediaSendActivity;
import com.RWdesenv.What.mediasend.MediaSendActivityResult;
import com.RWdesenv.What.messagerequests.MessageRequestViewModel;
import com.RWdesenv.What.messagerequests.MessageRequestsBottomView;
import com.RWdesenv.What.mms.AttachmentManager;
import com.RWdesenv.What.mms.AttachmentManager.MediaType;
import com.RWdesenv.What.mms.AudioSlide;
import com.RWdesenv.What.mms.GifSlide;
import com.RWdesenv.What.mms.GlideApp;
import com.RWdesenv.What.mms.GlideRequests;
import com.RWdesenv.What.mms.ImageSlide;
import com.RWdesenv.What.mms.LocationSlide;
import com.RWdesenv.What.mms.MediaConstraints;
import com.RWdesenv.What.mms.OutgoingExpirationUpdateMessage;
import com.RWdesenv.What.mms.OutgoingMediaMessage;
import com.RWdesenv.What.mms.OutgoingSecureMediaMessage;
import com.RWdesenv.What.mms.QuoteId;
import com.RWdesenv.What.mms.QuoteModel;
import com.RWdesenv.What.mms.Slide;
import com.RWdesenv.What.mms.SlideDeck;
import com.RWdesenv.What.mms.StickerSlide;
import com.RWdesenv.What.mms.VideoSlide;
import com.RWdesenv.What.notifications.MarkReadReceiver;
import com.RWdesenv.What.notifications.MessageNotifier;
import com.RWdesenv.What.notifications.NotificationChannels;
import com.RWdesenv.What.permissions.Permissions;
import com.RWdesenv.What.profiles.GroupShareProfileView;
import com.RWdesenv.What.providers.BlobProvider;
import com.RWdesenv.What.recipients.LiveRecipient;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientExporter;
import com.RWdesenv.What.recipients.RecipientFormattingException;
import com.RWdesenv.What.recipients.RecipientId;
import com.RWdesenv.What.recipients.RecipientUtil;
import com.RWdesenv.What.registration.RegistrationNavigationActivity;
import com.RWdesenv.What.service.KeyCachingService;
import com.RWdesenv.What.sms.MessageSender;
import com.RWdesenv.What.sms.OutgoingEncryptedMessage;
import com.RWdesenv.What.sms.OutgoingEndSessionMessage;
import com.RWdesenv.What.sms.OutgoingTextMessage;
import com.RWdesenv.What.stickers.StickerKeyboardProvider;
import com.RWdesenv.What.stickers.StickerLocator;
import com.RWdesenv.What.stickers.StickerManagementActivity;
import com.RWdesenv.What.stickers.StickerPackInstallEvent;
import com.RWdesenv.What.stickers.StickerSearchRepository;
import com.RWdesenv.What.util.BitmapUtil;
import com.RWdesenv.What.util.CharacterCalculator.CharacterState;
import com.RWdesenv.What.util.CommunicationActions;
import com.RWdesenv.What.util.DrawableUtil;
import com.RWdesenv.What.util.DynamicDarkToolbarTheme;
import com.RWdesenv.What.util.DynamicLanguage;
import com.RWdesenv.What.util.DynamicTheme;
import com.RWdesenv.What.util.FeatureFlags;
import com.RWdesenv.What.util.IdentityUtil;
import com.RWdesenv.What.util.MediaUtil;
import com.RWdesenv.What.util.MessageUtil;
import com.RWdesenv.What.util.ServiceUtil;
import com.RWdesenv.What.util.TextSecurePreferences;
import com.RWdesenv.What.util.TextSecurePreferences.MediaKeyboardMode;
import com.RWdesenv.What.util.Util;
import com.RWdesenv.What.util.ViewUtil;
import com.RWdesenv.What.util.concurrent.AssertedSuccessListener;
import com.RWdesenv.What.util.concurrent.ListenableFuture;
import com.RWdesenv.What.util.concurrent.SettableFuture;
import com.RWdesenv.What.util.concurrent.SignalExecutors;
import com.RWdesenv.What.util.concurrent.SimpleTask;
import com.RWdesenv.What.util.views.Stub;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.util.Pair;
import org.whispersystems.libsignal.util.guava.Optional;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static com.RWdesenv.What.TransportOption.Type;
import static com.RWdesenv.What.database.GroupDatabase.GroupRecord;
import static org.whispersystems.libsignal.SessionCipher.SESSION_LOCK;

/**
 * Activity for displaying a message thread, as well as
 * composing/sending a new message into that thread.
 *
 * @author Moxie Marlinspike
 *
 */
@SuppressLint("StaticFieldLeak")
public class ConversationActivity extends PassphraseRequiredActionBarActivity
    implements ConversationFragment.ConversationFragmentListener,
               AttachmentManager.AttachmentListener,
               OnKeyboardShownListener,
               InputPanel.Listener,
               InputPanel.MediaListener,
               ComposeText.CursorPositionChangedListener,
               ConversationSearchBottomBar.EventListener,
               StickerKeyboardProvider.StickerEventListener,
               AttachmentKeyboard.Callback
{

  private static final int SHORTCUT_ICON_SIZE = Build.VERSION.SDK_INT >= 26 ? ViewUtil.dpToPx(72) : ViewUtil.dpToPx(48 + 16 * 2);

  private static final String TAG = ConversationActivity.class.getSimpleName();

  public static final String RECIPIENT_EXTRA         = "recipient_id";
  public static final String THREAD_ID_EXTRA         = "thread_id";
  public static final String IS_ARCHIVED_EXTRA       = "is_archived";
  public static final String TEXT_EXTRA              = "draft_text";
  public static final String MEDIA_EXTRA             = "media_list";
  public static final String STICKER_EXTRA           = "sticker_extra";
  public static final String DISTRIBUTION_TYPE_EXTRA = "distribution_type";
  public static final String LAST_SEEN_EXTRA         = "last_seen";
  public static final String STARTING_POSITION_EXTRA = "starting_position";

  private static final int PICK_GALLERY        = 1;
  private static final int PICK_DOCUMENT       = 2;
  private static final int PICK_AUDIO          = 3;
  private static final int PICK_CONTACT        = 4;
  private static final int GET_CONTACT_DETAILS = 5;
  private static final int GROUP_EDIT          = 6;
  private static final int TAKE_PHOTO          = 7;
  private static final int ADD_CONTACT         = 8;
  private static final int PICK_LOCATION       = 9;
  private static final int PICK_GIF            = 10;
  private static final int SMS_DEFAULT         = 11;
  private static final int MEDIA_SENDER        = 12;

  private   GlideRequests              glideRequests;
  protected ComposeText                composeText;
  private   AnimatingToggle            buttonToggle;
  private   SendButton                 sendButton;
  private   ImageButton                attachButton;
  protected ConversationTitleView      titleView;
  private   TextView                   charactersLeft;
  private   ConversationFragment       fragment;
  private   Button                     unblockButton;
  private   Button                     makeDefaultSmsButton;
  private   Button                     registerButton;
  private   InputAwareLayout           container;
  private   View                       composePanel;
  protected Stub<ReminderView>         reminderView;
  private   Stub<UnverifiedBannerView> unverifiedBannerView;
  private   Stub<GroupShareProfileView> groupShareProfileView;
  private   TypingStatusTextWatcher     typingTextWatcher;
  private   ConversationSearchBottomBar searchNav;
  private   MenuItem                    searchViewItem;
  private   MessageRequestsBottomView   messageRequestBottomView;
  private   ConversationReactionOverlay reactionOverlay;

  private   AttachmentManager        attachmentManager;
  private   AudioRecorder            audioRecorder;
  private   BroadcastReceiver        securityUpdateReceiver;
  private   Stub<MediaKeyboard>      emojiDrawerStub;
  private   Stub<AttachmentKeyboard> attachmentKeyboardStub;
  protected HidingLinearLayout       quickAttachmentToggle;
  protected HidingLinearLayout       inlineAttachmentToggle;
  private   InputPanel               inputPanel;
  private   View                     panelParent;

  private LinkPreviewViewModel         linkPreviewViewModel;
  private ConversationSearchViewModel  searchViewModel;
  private ConversationStickerViewModel stickerViewModel;
  private ConversationViewModel        viewModel;
  private InviteReminderModel          inviteReminderModel;

  private LiveRecipient recipient;
  private long          threadId;
  private int           distributionType;
  private boolean       archived;
  private boolean       isSecureText;
  private boolean       isDefaultSms                  = true;
  private boolean       isMmsEnabled                  = true;
  private boolean       isSecurityInitialized         = false;
  private boolean       shouldDisplayMessageRequestUi = true;

  private final IdentityRecordList identityRecords = new IdentityRecordList();
  private final DynamicTheme       dynamicTheme    = new DynamicDarkToolbarTheme();
  private final DynamicLanguage    dynamicLanguage = new DynamicLanguage();

  public static @NonNull Intent buildIntent(@NonNull Context context,
                                            @NonNull RecipientId recipientId,
                                            long threadId,
                                            int distributionType,
                                            long lastSeen,
                                            int startingPosition)
  {
    Intent intent = new Intent(context, ConversationActivity.class);
    intent.putExtra(ConversationActivity.RECIPIENT_EXTRA, recipientId);
    intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
    intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, distributionType);
    intent.putExtra(ConversationActivity.LAST_SEEN_EXTRA, lastSeen);
    intent.putExtra(ConversationActivity.STARTING_POSITION_EXTRA, startingPosition);

    return intent;
  }

  @Override
  protected void onPreCreate() {
    dynamicTheme.onCreate(this);
    dynamicLanguage.onCreate(this);
  }

  @Override
  protected void onCreate(Bundle state, boolean ready) {
    RecipientId recipientId = getIntent().getParcelableExtra(RECIPIENT_EXTRA);

    if (recipientId == null) {
      Log.w(TAG, "[onCreate] Missing recipientId!");
      // TODO [greyson] Navigation
      startActivity(new Intent(this, MainActivity.class));
      finish();
      return;
    }


    setContentView(R.layout.conversation_activity);

    TypedArray typedArray = obtainStyledAttributes(new int[] {R.attr.conversation_background});
    int color = typedArray.getColor(0, Color.WHITE);
    typedArray.recycle();

    getWindow().getDecorView().setBackgroundColor(color);

    fragment = initFragment(R.id.fragment_content, new ConversationFragment(), dynamicLanguage.getCurrentLocale());

    initializeReceivers();
    initializeActionBar();
    initializeViews();
    initializeResources();
    initializeLinkPreviewObserver();
    initializeSearchObserver();
    initializeStickerObserver();
    initializeViewModel();
    initializeSecurity(recipient.get().isRegistered(), isDefaultSms).addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        initializeProfiles();
        initializeDraft().addListener(new AssertedSuccessListener<Boolean>() {
          @Override
          public void onSuccess(Boolean loadedDraft) {
            if (loadedDraft != null && loadedDraft) {
              Log.i(TAG, "Finished loading draft");
              Util.runOnMain(() -> {
                if (fragment != null && fragment.isResumed()) {
                  fragment.moveToLastSeen();
                } else {
                  Log.w(TAG, "Wanted to move to the last seen position, but the fragment was in an invalid state");
                }
              });
            }

            if (TextSecurePreferences.isTypingIndicatorsEnabled(ConversationActivity.this)) {
              composeText.addTextChangedListener(typingTextWatcher);
            }
            composeText.setSelection(composeText.length(), composeText.length());
          }
        });
      }
    });
    initializeInsightObserver();
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Log.i(TAG, "onNewIntent()");
    
    if (isFinishing()) {
      Log.w(TAG, "Activity is finishing...");
      return;
    }

    if (!Util.isEmpty(composeText) || attachmentManager.isAttachmentPresent() || inputPanel.getQuote().isPresent()) {
      saveDraft();
      attachmentManager.clear(glideRequests, false);
      inputPanel.clearQuote();
      silentlySetComposeText("");
    }

    RecipientId recipientId = intent.getParcelableExtra(RECIPIENT_EXTRA);

    if (recipientId == null) {
      Log.w(TAG, "[onNewIntent] Missing recipientId!");
      // TODO [greyson] Navigation
      startActivity(new Intent(this, MainActivity.class));
      finish();
      return;
    }

    setIntent(intent);
    initializeResources();
    initializeSecurity(recipient.get().isRegistered(), isDefaultSms).addListener(new AssertedSuccessListener<Boolean>() {
      @Override
      public void onSuccess(Boolean result) {
        initializeDraft();
      }
    });

    if (fragment != null) {
      fragment.onNewIntent();
    }

    searchNav.setVisibility(View.GONE);
  }

  @Override
  protected void onResume() {
    super.onResume();
    dynamicTheme.onResume(this);
    dynamicLanguage.onResume(this);

    EventBus.getDefault().register(this);
    initializeEnabledCheck();
    initializeMmsEnabledCheck();
    initializeIdentityRecords();
    composeText.setTransport(sendButton.getSelectedTransport());

    Recipient recipientSnapshot = recipient.get();

    titleView.setTitle(glideRequests, recipientSnapshot);
    setActionBarColor(recipientSnapshot.getColor());
    setBlockedUserState(recipientSnapshot, isSecureText, isDefaultSms);
    setGroupShareProfileReminder(recipientSnapshot);
    calculateCharactersRemaining();

    MessageNotifier.setVisibleThread(threadId);
    markThreadAsRead();
  }

  @Override
  protected void onPause() {
    super.onPause();
    MessageNotifier.setVisibleThread(-1L);
    if (isFinishing()) overridePendingTransition(R.anim.fade_scale_in, R.anim.slide_to_end);
    inputPanel.onPause();

    fragment.setLastSeen(System.currentTimeMillis());
    markLastSeen();
    AudioSlidePlayer.stopAll();
    EventBus.getDefault().unregister(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    EventBus.getDefault().unregister(this);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    Log.i(TAG, "onConfigurationChanged(" + newConfig.orientation + ")");
    super.onConfigurationChanged(newConfig);
    composeText.setTransport(sendButton.getSelectedTransport());

    if (emojiDrawerStub.resolved() && container.getCurrentInput() == emojiDrawerStub.get()) {
      container.hideAttachedInput(true);
    }

    if (reactionOverlay != null && reactionOverlay.isShowing()) {
      reactionOverlay.hide();
    }
  }

  @Override
  protected void onDestroy() {
    saveDraft();
    if (securityUpdateReceiver != null)  unregisterReceiver(securityUpdateReceiver);
    super.onDestroy();
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent ev) {
    return reactionOverlay.applyTouchEvent(ev) || super.dispatchTouchEvent(ev);
  }

  @Override
  public void onActivityResult(final int reqCode, int resultCode, Intent data) {
    Log.i(TAG, "onActivityResult called: " + reqCode + ", " + resultCode + " , " + data);
    super.onActivityResult(reqCode, resultCode, data);

    if ((data == null && reqCode != TAKE_PHOTO && reqCode != SMS_DEFAULT) ||
        (resultCode != RESULT_OK && reqCode != SMS_DEFAULT))
    {
      updateLinkPreviewState();
      return;
    }

    switch (reqCode) {
    case PICK_DOCUMENT:
      setMedia(data.getData(), MediaType.DOCUMENT);
      break;
    case PICK_AUDIO:
      setMedia(data.getData(), MediaType.AUDIO);
      break;
    case PICK_CONTACT:
      if (isSecureText && !isSmsForced()) {
        openContactShareEditor(data.getData());
      } else {
        addAttachmentContactInfo(data.getData());
      }
      break;
    case GET_CONTACT_DETAILS:
      sendSharedContact(data.getParcelableArrayListExtra(ContactShareEditActivity.KEY_CONTACTS));
      break;
    case GROUP_EDIT:
      Recipient recipientSnapshot = recipient.get();

      onRecipientChanged(recipientSnapshot);
      titleView.setTitle(glideRequests, recipientSnapshot);
      NotificationChannels.updateContactChannelName(this, recipientSnapshot);
      setBlockedUserState(recipientSnapshot, isSecureText, isDefaultSms);
      supportInvalidateOptionsMenu();
      break;
    case TAKE_PHOTO:
      handleImageFromDeviceCameraApp();
      break;
    case ADD_CONTACT:
      onRecipientChanged(recipient.get());
      fragment.reloadList();
      break;
    case PICK_LOCATION:
      SignalPlace place = new SignalPlace(PlacePickerActivity.addressFromData(data));
      attachmentManager.setLocation(place, getCurrentMediaConstraints());
      break;
    case PICK_GIF:
      setMedia(data.getData(),
               MediaType.GIF,
               data.getIntExtra(GiphyActivity.EXTRA_WIDTH, 0),
               data.getIntExtra(GiphyActivity.EXTRA_HEIGHT, 0));
      break;
    case SMS_DEFAULT:
      initializeSecurity(isSecureText, isDefaultSms);
      break;
    case MEDIA_SENDER:
      MediaSendActivityResult result = data.getParcelableExtra(MediaSendActivity.EXTRA_RESULT);
      sendButton.setTransport(result.getTransport());

      if (result.isPushPreUpload()) {
        sendMediaMessage(result);
        return;
      }

      long       expiresIn      = recipient.get().getExpireMessages() * 1000L;
      int        subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
      boolean    initiating     = threadId == -1;
      QuoteModel quote          = result.isViewOnce() ? null : inputPanel.getQuote().orNull();
      SlideDeck  slideDeck      = new SlideDeck();

      for (Media mediaItem : result.getNonUploadedMedia()) {
        if (MediaUtil.isVideoType(mediaItem.getMimeType())) {
          slideDeck.addSlide(new VideoSlide(this, mediaItem.getUri(), 0, mediaItem.getCaption().orNull(), mediaItem.getTransformProperties().orNull()));
        } else if (MediaUtil.isGif(mediaItem.getMimeType())) {
          slideDeck.addSlide(new GifSlide(this, mediaItem.getUri(), 0, mediaItem.getWidth(), mediaItem.getHeight(), mediaItem.getCaption().orNull()));
        } else if (MediaUtil.isImageType(mediaItem.getMimeType())) {
          slideDeck.addSlide(new ImageSlide(this, mediaItem.getUri(), 0, mediaItem.getWidth(), mediaItem.getHeight(), mediaItem.getCaption().orNull(), null));
        } else {
          Log.w(TAG, "Asked to send an unexpected mimeType: '" + mediaItem.getMimeType() + "'. Skipping.");
        }
      }

      final Context context = ConversationActivity.this.getApplicationContext();

      sendMediaMessage(result.getTransport().isSms(),
                       result.getBody(),
                       slideDeck,
                       quote,
                       Collections.emptyList(),
                       Collections.emptyList(),
                       expiresIn,
                       result.isViewOnce(),
                       subscriptionId,
                       initiating,
                       true).addListener(new AssertedSuccessListener<Void>() {
        @Override
        public void onSuccess(Void result) {
          AsyncTask.THREAD_POOL_EXECUTOR.execute(() -> {
            Stream.of(slideDeck.getSlides())
                  .map(Slide::getUri)
                  .withoutNulls()
                  .filter(BlobProvider::isAuthority)
                  .forEach(uri -> BlobProvider.getInstance().delete(context, uri));
          });
        }
      });

      break;
    }
  }

  private void handleImageFromDeviceCameraApp() {
    if (attachmentManager.getCaptureUri() == null) {
      Log.w(TAG, "No image available.");
      return;
    }

    try {
      Uri mediaUri = BlobProvider.getInstance()
                                 .forData(getContentResolver().openInputStream(attachmentManager.getCaptureUri()), 0L)
                                 .withMimeType(MediaUtil.IMAGE_JPEG)
                                 .createForSingleSessionOnDisk(this);

      getContentResolver().delete(attachmentManager.getCaptureUri(), null, null);

      setMedia(mediaUri, MediaType.IMAGE);
    } catch (IOException ioe) {
      Log.w(TAG, "Could not handle public image", ioe);
    }
  }

  @Override
  public void startActivity(Intent intent) {
    if (intent.getStringExtra(Browser.EXTRA_APPLICATION_ID) != null) {
      intent.removeExtra(Browser.EXTRA_APPLICATION_ID);
    }

    try {
      super.startActivity(intent);
    } catch (ActivityNotFoundException e) {
      Log.w(TAG, e);
      Toast.makeText(this, R.string.ConversationActivity_there_is_no_app_available_to_handle_this_link_on_your_device, Toast.LENGTH_LONG).show();
    }
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    MenuInflater inflater = this.getMenuInflater();
    menu.clear();

    if (isInMessageRequest()) {
      if (isActiveGroup()) {
        inflater.inflate(R.menu.conversation_message_requests_group, menu);
      }

      inflater.inflate(R.menu.conversation_message_requests, menu);

      if (recipient != null && recipient.get().isMuted()) inflater.inflate(R.menu.conversation_muted, menu);
      else                                                inflater.inflate(R.menu.conversation_unmuted, menu);

      super.onPrepareOptionsMenu(menu);
      return true;
    }

    if (isSecureText) {
      if (recipient.get().getExpireMessages() > 0) {
        inflater.inflate(R.menu.conversation_expiring_on, menu);
        titleView.showExpiring(recipient);
      } else {
        inflater.inflate(R.menu.conversation_expiring_off, menu);
        titleView.clearExpiring();
      }
    }

    if (isSingleConversation()) {
      if (isSecureText) inflater.inflate(R.menu.conversation_callable_secure, menu);
      else              inflater.inflate(R.menu.conversation_callable_insecure, menu);
    } else if (isGroupConversation()) {
      inflater.inflate(R.menu.conversation_group_options, menu);

      if (!isPushGroupConversation()) {
        inflater.inflate(R.menu.conversation_mms_group_options, menu);
        if (distributionType == ThreadDatabase.DistributionTypes.BROADCAST) {
          menu.findItem(R.id.menu_distribution_broadcast).setChecked(true);
        } else {
          menu.findItem(R.id.menu_distribution_conversation).setChecked(true);
        }
      } else if (isActiveGroup()) {
        inflater.inflate(R.menu.conversation_push_group_options, menu);
      }
    }

    inflater.inflate(R.menu.conversation, menu);

    if (isSingleConversation() && isSecureText) {
      inflater.inflate(R.menu.conversation_secure, menu);
    } else if (isSingleConversation()) {
      inflater.inflate(R.menu.conversation_insecure, menu);
    }

    if (recipient != null && recipient.get().isMuted()) inflater.inflate(R.menu.conversation_muted, menu);
    else                                                inflater.inflate(R.menu.conversation_unmuted, menu);

    if (isSingleConversation() && getRecipient().getContactUri() == null) {
      inflater.inflate(R.menu.conversation_add_to_contacts, menu);
    }

    if (recipient != null && recipient.get().isLocalNumber()) {
      if (isSecureText) {
        menu.findItem(R.id.menu_call_secure).setVisible(false);
        menu.findItem(R.id.menu_video_secure).setVisible(false);
      } else {
        menu.findItem(R.id.menu_call_insecure).setVisible(false);
      }

      MenuItem muteItem = menu.findItem(R.id.menu_mute_notifications);

      if (muteItem != null) {
        muteItem.setVisible(false);
      }
    }

    searchViewItem = menu.findItem(R.id.menu_search);

    SearchView                     searchView    = (SearchView) searchViewItem.getActionView();
    SearchView.OnQueryTextListener queryListener = new SearchView.OnQueryTextListener() {
      @Override
      public boolean onQueryTextSubmit(String query) {
        searchViewModel.onQueryUpdated(query, threadId, true);
        searchNav.showLoading();
        fragment.onSearchQueryUpdated(query);
        return true;
      }

      @Override
      public boolean onQueryTextChange(String query) {
        searchViewModel.onQueryUpdated(query, threadId, false);
        searchNav.showLoading();
        fragment.onSearchQueryUpdated(query);
        return true;
      }
    };

    searchViewItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
      @Override
      public boolean onMenuItemActionExpand(MenuItem item) {
        searchView.setOnQueryTextListener(queryListener);
        searchViewModel.onSearchOpened();
        searchNav.setVisibility(View.VISIBLE);
        searchNav.setData(0, 0);
        inputPanel.setVisibility(View.GONE);

        for (int i = 0; i < menu.size(); i++) {
          if (!menu.getItem(i).equals(searchViewItem)) {
            menu.getItem(i).setVisible(false);
          }
        }
        return true;
      }

      @Override
      public boolean onMenuItemActionCollapse(MenuItem item) {
        searchView.setOnQueryTextListener(null);
        searchViewModel.onSearchClosed();
        searchNav.setVisibility(View.GONE);
        inputPanel.setVisibility(View.VISIBLE);
        fragment.onSearchQueryUpdated(null);
        invalidateOptionsMenu();
        return true;
      }
    });

    super.onPrepareOptionsMenu(menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    super.onOptionsItemSelected(item);
    switch (item.getItemId()) {
    case R.id.menu_call_secure:               handleDial(getRecipient(), true);                  return true;
    case R.id.menu_video_secure:              handleVideo(getRecipient());                       return true;
    case R.id.menu_call_insecure:             handleDial(getRecipient(), false);                 return true;
    case R.id.menu_view_media:                handleViewMedia();                                 return true;
    case R.id.menu_add_shortcut:              handleAddShortcut();                               return true;
    case R.id.menu_search:                    handleSearch();                                    return true;
    case R.id.menu_add_to_contacts:           handleAddToContacts();                             return true;
    case R.id.menu_reset_secure_session:      handleResetSecureSession();                        return true;
    case R.id.menu_group_recipients:          handleDisplayGroupRecipients();                    return true;
    case R.id.menu_distribution_broadcast:    handleDistributionBroadcastEnabled(item);          return true;
    case R.id.menu_distribution_conversation: handleDistributionConversationEnabled(item);       return true;
    case R.id.menu_edit_group:                handleEditPushGroup();                             return true;
    case R.id.menu_leave:                     handleLeavePushGroup();                            return true;
    case R.id.menu_invite:                    handleInviteLink();                                return true;
    case R.id.menu_mute_notifications:        handleMuteNotifications();                         return true;
    case R.id.menu_unmute_notifications:      handleUnmuteNotifications();                       return true;
    case R.id.menu_conversation_settings:     handleConversationSettings();                      return true;
    case R.id.menu_expiring_messages_off:
    case R.id.menu_expiring_messages:         handleSelectMessageExpiration();                   return true;
    case android.R.id.home:                   onBackPressed();                                   return true;
    }

    return false;
  }

  @Override
  public void onBackPressed() {
    Log.d(TAG, "onBackPressed()");
    if (reactionOverlay.isShowing())  reactionOverlay.hide();
    else if (container.isInputOpen()) container.hideCurrentInput(composeText);
    else                              super.onBackPressed();
  }

  @Override
  public void onKeyboardShown() {
    inputPanel.onKeyboardShown();
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onEvent(ReminderUpdateEvent event) {
    updateReminders();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
  }

  @Override
  public void onAttachmentMediaClicked(@NonNull Media media) {
    linkPreviewViewModel.onUserCancel();
    startActivityForResult(MediaSendActivity.buildEditorIntent(ConversationActivity.this, Collections.singletonList(media), recipient.get(), composeText.getTextTrimmed(), sendButton.getSelectedTransport()), MEDIA_SENDER);
    container.hideCurrentInput(composeText);
  }

  @Override
  public void onAttachmentSelectorClicked(@NonNull AttachmentKeyboardButton button) {
    switch (button) {
      case GALLERY:
        AttachmentManager.selectGallery(this, MEDIA_SENDER, recipient.get(), composeText.getTextTrimmed(), sendButton.getSelectedTransport());
        break;
      case GIF:
        AttachmentManager.selectGif(this, PICK_GIF, !isSecureText, recipient.get().getColor().toConversationColor(this));
        break;
      case FILE:
        AttachmentManager.selectDocument(this, PICK_DOCUMENT);
        break;
      case CONTACT:
        AttachmentManager.selectContactInfo(this, PICK_CONTACT);
        break;
      case LOCATION:
        AttachmentManager.selectLocation(this, PICK_LOCATION);
        break;
    }

    container.hideCurrentInput(composeText);
  }

  @Override
  public void onAttachmentPermissionsRequested() {
    Permissions.with(this)
               .request(Manifest.permission.READ_EXTERNAL_STORAGE)
               .onAllGranted(() -> viewModel.onAttachmentKeyboardOpen())
               .execute();
  }

//////// Event Handlers

  private void handleSelectMessageExpiration() {
    if (isPushGroupConversation() && !isActiveGroup()) {
      return;
    }

    //noinspection CodeBlock2Expr
    ExpirationDialog.show(this, recipient.get().getExpireMessages(), expirationTime -> {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          DatabaseFactory.getRecipientDatabase(ConversationActivity.this).setExpireMessages(recipient.getId(), expirationTime);
          OutgoingExpirationUpdateMessage outgoingMessage = new OutgoingExpirationUpdateMessage(getRecipient(), System.currentTimeMillis(), expirationTime * 1000L);
          MessageSender.send(ConversationActivity.this, outgoingMessage, threadId, false, null);

          return null;
        }

        @Override
        protected void onPostExecute(Void result) {
          invalidateOptionsMenu();
          if (fragment != null) fragment.setLastSeen(0);
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    });
  }

  private void handleMuteNotifications() {
    MuteDialog.show(this, until -> {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          DatabaseFactory.getRecipientDatabase(ConversationActivity.this)
                         .setMuted(recipient.getId(), until);

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    });
  }

  private void handleConversationSettings() {
    if (isInMessageRequest()) return;

    Intent intent = RecipientPreferenceActivity.getLaunchIntent(this, recipient.getId());
    startActivitySceneTransition(intent, titleView.findViewById(R.id.contact_photo_image), "avatar");
  }

  private void handleUnmuteNotifications() {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        DatabaseFactory.getRecipientDatabase(ConversationActivity.this)
                       .setMuted(recipient.getId(), 0);

        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private void handleUnblock() {
    int titleRes = R.string.ConversationActivity_unblock_this_contact_question;
    int bodyRes  = R.string.ConversationActivity_you_will_once_again_be_able_to_receive_messages_and_calls_from_this_contact;

    if (recipient.get().isGroup()) {
      titleRes = R.string.ConversationActivity_unblock_this_group_question;
      bodyRes  = R.string.ConversationActivity_unblock_this_group_description;
    }

    //noinspection CodeBlock2Expr
    new AlertDialog.Builder(this)
                   .setTitle(titleRes)
                   .setMessage(bodyRes)
                   .setNegativeButton(android.R.string.cancel, null)
                   .setPositiveButton(R.string.ConversationActivity_unblock, (dialog, which) -> {
                     SignalExecutors.BOUNDED.execute(() -> {
                       RecipientUtil.unblock(ConversationActivity.this, recipient.get());
                     });
                   }).show();
  }

  @TargetApi(Build.VERSION_CODES.KITKAT)
  private void handleMakeDefaultSms() {
    Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
    intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
    startActivityForResult(intent, SMS_DEFAULT);
  }

  private void handleRegisterForSignal() {
    startActivity(RegistrationNavigationActivity.newIntentForReRegistration(this));
  }

  private void handleInviteLink() {
    String inviteText = getString(R.string.ConversationActivity_lets_switch_to_signal, getString(R.string.install_url));

    if (isDefaultSms) {
      composeText.appendInvite(inviteText);
    } else {
      Intent intent = new Intent(Intent.ACTION_SENDTO);
      intent.setData(Uri.parse("smsto:" + recipient.get().requireSmsAddress()));
      intent.putExtra("sms_body", inviteText);
      intent.putExtra(Intent.EXTRA_TEXT, inviteText);
      startActivity(intent);
    }
  }

  private void handleResetSecureSession() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(R.string.ConversationActivity_reset_secure_session_question);
    builder.setIconAttribute(R.attr.dialog_alert_icon);
    builder.setCancelable(true);
    builder.setMessage(R.string.ConversationActivity_this_may_help_if_youre_having_encryption_problems);
    builder.setPositiveButton(R.string.ConversationActivity_reset, (dialog, which) -> {
      if (isSingleConversation()) {
        final Context context = getApplicationContext();

        OutgoingEndSessionMessage endSessionMessage =
            new OutgoingEndSessionMessage(new OutgoingTextMessage(getRecipient(), "TERMINATE", 0, -1));

        new AsyncTask<OutgoingEndSessionMessage, Void, Long>() {
          @Override
          protected Long doInBackground(OutgoingEndSessionMessage... messages) {
            return MessageSender.send(context, messages[0], threadId, false, null);
          }

          @Override
          protected void onPostExecute(Long result) {
            sendComplete(result);
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, endSessionMessage);
      }
    });
    builder.setNegativeButton(android.R.string.cancel, null);
    builder.show();
  }

  private void handleViewMedia() {
    startActivity(MediaOverviewActivity.forThread(this, threadId));
  }

  private void handleAddShortcut() {
    Log.i(TAG, "Creating home screen shortcut for recipient " + recipient.get().getId());

    final Context context = getApplicationContext();
    final Recipient recipient = this.recipient.get();

    GlideApp.with(this)
            .asBitmap()
            .load(recipient.getContactPhoto())
            .error(recipient.getFallbackContactPhoto().asDrawable(this, recipient.getColor().toAvatarColor(this), false))
            .into(new CustomTarget<Bitmap>() {
              @Override
              public void onLoadFailed(@Nullable Drawable errorDrawable) {
                if (errorDrawable == null) {
                  throw new AssertionError();
                }

                Log.w(TAG, "Utilizing fallback photo for shortcut for recipient " + recipient.getId());

                SimpleTask.run(() -> DrawableUtil.toBitmap(errorDrawable, SHORTCUT_ICON_SIZE, SHORTCUT_ICON_SIZE),
                               bitmap -> addIconToHomeScreen(context, bitmap, recipient));
              }

              @Override
              public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                SimpleTask.run(() -> BitmapUtil.createScaledBitmap(resource, SHORTCUT_ICON_SIZE, SHORTCUT_ICON_SIZE),
                               bitmap -> addIconToHomeScreen(context, bitmap, recipient));
              }

              @Override
              public void onLoadCleared(@Nullable Drawable placeholder) {
              }
            });

  }

  private static void addIconToHomeScreen(@NonNull Context context,
                                          @NonNull Bitmap bitmap,
                                          @NonNull Recipient recipient)
  {
    IconCompat icon = IconCompat.createWithAdaptiveBitmap(bitmap);
    String     name = recipient.isLocalNumber() ? context.getString(R.string.note_to_self)
                                                  : recipient.getDisplayName(context);

    ShortcutInfoCompat shortcutInfoCompat = new ShortcutInfoCompat.Builder(context, recipient.getId().serialize() + '-' + System.currentTimeMillis())
                                                                  .setShortLabel(name)
                                                                  .setIcon(icon)
                                                                  .setIntent(ShortcutLauncherActivity.createIntent(context, recipient.getId()))
                                                                  .build();

    if (ShortcutManagerCompat.requestPinShortcut(context, shortcutInfoCompat, null)) {
      Toast.makeText(context, context.getString(R.string.ConversationActivity_added_to_home_screen), Toast.LENGTH_LONG).show();
    }

    bitmap.recycle();
  }

  private void handleSearch() {
    searchViewModel.onSearchOpened();
  }

  private void handleLeavePushGroup() {
    if (getRecipient() == null) {
      Toast.makeText(this, getString(R.string.ConversationActivity_invalid_recipient),
                     Toast.LENGTH_LONG).show();
      return;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(getString(R.string.ConversationActivity_leave_group));
    builder.setIconAttribute(R.attr.dialog_info_icon);
    builder.setCancelable(true);
    builder.setMessage(getString(R.string.ConversationActivity_are_you_sure_you_want_to_leave_this_group));
    builder.setPositiveButton(R.string.yes, (dialog, which) ->
      SimpleTask.run(
        getLifecycle(),
        () -> GroupManager.leaveGroup(ConversationActivity.this, getRecipient()),
        (success) -> {
          if (success) {
            initializeEnabledCheck();
          } else {
            Toast.makeText(ConversationActivity.this, R.string.ConversationActivity_error_leaving_group, Toast.LENGTH_LONG).show();
          }
        }));

    builder.setNegativeButton(R.string.no, null);
    builder.show();
  }

  private void handleEditPushGroup() {
    Intent intent = new Intent(ConversationActivity.this, GroupCreateActivity.class);
    intent.putExtra(GroupCreateActivity.GROUP_ID_EXTRA, recipient.get().requireGroupId().toString());
    startActivityForResult(intent, GROUP_EDIT);
  }

  private void handleDistributionBroadcastEnabled(MenuItem item) {
    distributionType = ThreadDatabase.DistributionTypes.BROADCAST;
    item.setChecked(true);

    if (threadId != -1) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          DatabaseFactory.getThreadDatabase(ConversationActivity.this)
                         .setDistributionType(threadId, ThreadDatabase.DistributionTypes.BROADCAST);
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private void handleDistributionConversationEnabled(MenuItem item) {
    distributionType = ThreadDatabase.DistributionTypes.CONVERSATION;
    item.setChecked(true);

    if (threadId != -1) {
      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          DatabaseFactory.getThreadDatabase(ConversationActivity.this)
                         .setDistributionType(threadId, ThreadDatabase.DistributionTypes.CONVERSATION);
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private void handleDial(final Recipient recipient, boolean isSecure) {
    if (recipient == null) return;

    if (isSecure) {
      CommunicationActions.startVoiceCall(this, recipient);
    } else {
      CommunicationActions.startInsecureCall(this, recipient);
    }
  }

  private void handleVideo(final Recipient recipient) {
    if (recipient == null) return;

    CommunicationActions.startVideoCall(this, recipient);
  }

  private void handleDisplayGroupRecipients() {
    new GroupMembersDialog(this, getRecipient(), getLifecycle()).display();
  }

  private void handleAddToContacts() {
    if (recipient.get().isGroup()) return;

    try {
      startActivityForResult(RecipientExporter.export(recipient.get()).asAddContactIntent(), ADD_CONTACT);
    } catch (ActivityNotFoundException e) {
      Log.w(TAG, e);
    }
  }

  private boolean handleDisplayQuickContact() {
    if (isInMessageRequest() || recipient.get().isGroup()) return false;

    if (recipient.get().getContactUri() != null) {
      ContactsContract.QuickContact.showQuickContact(ConversationActivity.this, titleView, recipient.get().getContactUri(), ContactsContract.QuickContact.MODE_LARGE, null);
    } else {
      handleAddToContacts();
    }

    return true;
  }

  private void handleAddAttachment() {
    if (this.isMmsEnabled || isSecureText) {
      viewModel.getRecentMedia().removeObservers(this);

      if (attachmentKeyboardStub.resolved() && container.isInputOpen() && container.getCurrentInput() == attachmentKeyboardStub.get()) {
        container.showSoftkey(composeText);
      } else {
        viewModel.getRecentMedia().observe(this, media -> attachmentKeyboardStub.get().onMediaChanged(media));
        attachmentKeyboardStub.get().setCallback(this);
        container.show(composeText, attachmentKeyboardStub.get());

        viewModel.onAttachmentKeyboardOpen();
      }
    } else {
      handleManualMmsRequired();
    }
  }

  private void handleManualMmsRequired() {
    Toast.makeText(this, R.string.MmsDownloader_error_reading_mms_settings, Toast.LENGTH_LONG).show();

    Bundle extras = getIntent().getExtras();
    Intent intent = new Intent(this, PromptMmsActivity.class);
    if (extras != null) intent.putExtras(extras);
    startActivity(intent);
  }

  private void handleUnverifiedRecipients() {
    List<Recipient>      unverifiedRecipients = identityRecords.getUnverifiedRecipients(this);
    List<IdentityRecord> unverifiedRecords    = identityRecords.getUnverifiedRecords();
    String               message              = IdentityUtil.getUnverifiedSendDialogDescription(this, unverifiedRecipients);

    if (message == null) return;

    //noinspection CodeBlock2Expr
    new UnverifiedSendDialog(this, message, unverifiedRecords, () -> {
      initializeIdentityRecords().addListener(new ListenableFuture.Listener<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
          sendMessage();
        }

        @Override
        public void onFailure(ExecutionException e) {
          throw new AssertionError(e);
        }
      });
    }).show();
  }

  private void handleUntrustedRecipients() {
    List<Recipient>      untrustedRecipients = identityRecords.getUntrustedRecipients(this);
    List<IdentityRecord> untrustedRecords    = identityRecords.getUntrustedRecords();
    String               untrustedMessage    = IdentityUtil.getUntrustedSendDialogDescription(this, untrustedRecipients);

    if (untrustedMessage == null) return;

    //noinspection CodeBlock2Expr
    new UntrustedSendDialog(this, untrustedMessage, untrustedRecords, () -> {
      initializeIdentityRecords().addListener(new ListenableFuture.Listener<Boolean>() {
        @Override
        public void onSuccess(Boolean result) {
          sendMessage();
        }

        @Override
        public void onFailure(ExecutionException e) {
          throw new AssertionError(e);
        }
      });
    }).show();
  }

  private void handleSecurityChange(boolean isSecureText, boolean isDefaultSms) {
    Log.i(TAG, "handleSecurityChange(" + isSecureText + ", " + isDefaultSms + ")");

    this.isSecureText          = isSecureText;
    this.isDefaultSms          = isDefaultSms;
    this.isSecurityInitialized = true;

    boolean isMediaMessage = recipient.get().isMmsGroup() || attachmentManager.isAttachmentPresent();

    sendButton.resetAvailableTransports(isMediaMessage);

    if (!isSecureText && !isPushGroupConversation()) sendButton.disableTransport(Type.TEXTSECURE);
    if (recipient.get().isPushGroup())            sendButton.disableTransport(Type.SMS);

    if (!recipient.get().isPushGroup() && recipient.get().isForceSmsSelection()) {
      sendButton.setDefaultTransport(Type.SMS);
    } else {
      if (isSecureText || isPushGroupConversation()) sendButton.setDefaultTransport(Type.TEXTSECURE);
      else                                           sendButton.setDefaultTransport(Type.SMS);
    }

    calculateCharactersRemaining();
    supportInvalidateOptionsMenu();
    setBlockedUserState(recipient.get(), isSecureText, isDefaultSms);
  }

  ///// Initializers

  private ListenableFuture<Boolean> initializeDraft() {
    final SettableFuture<Boolean> result = new SettableFuture<>();

    final String         draftText      = getIntent().getStringExtra(TEXT_EXTRA);
    final Uri            draftMedia     = getIntent().getData();
    final MediaType      draftMediaType = MediaType.from(getIntent().getType());
    final List<Media>    mediaList      = getIntent().getParcelableArrayListExtra(MEDIA_EXTRA);
    final StickerLocator stickerLocator = getIntent().getParcelableExtra(STICKER_EXTRA);

    if (stickerLocator != null && draftMedia != null) {
      Log.d(TAG, "Handling shared sticker.");
      sendSticker(stickerLocator, draftMedia, 0, true);
      return new SettableFuture<>(false);
    }

    if (!Util.isEmpty(mediaList)) {
      Log.d(TAG, "Handling shared Media.");
      Intent sendIntent = MediaSendActivity.buildEditorIntent(this, mediaList, recipient.get(), draftText, sendButton.getSelectedTransport());
      startActivityForResult(sendIntent, MEDIA_SENDER);
      return new SettableFuture<>(false);
    }

    if (draftText != null) {
      composeText.setText("");
      composeText.append(draftText);
      result.set(true);
    }

    if (draftMedia != null && draftMediaType != null) {
      Log.d(TAG, "Handling shared Data.");
      return setMedia(draftMedia, draftMediaType);
    }

    if (draftText == null && draftMedia == null && draftMediaType == null) {
      return initializeDraftFromDatabase();
    } else {
      updateToggleButtonState();
      result.set(false);
    }

    return result;
  }

  private void initializeEnabledCheck() {
    boolean enabled = !(isPushGroupConversation() && !isActiveGroup());
    inputPanel.setEnabled(enabled);
    sendButton.setEnabled(enabled);
    attachButton.setEnabled(enabled);
  }

  private ListenableFuture<Boolean> initializeDraftFromDatabase() {
    SettableFuture<Boolean> future = new SettableFuture<>();

    new AsyncTask<Void, Void, List<Draft>>() {
      @Override
      protected List<Draft> doInBackground(Void... params) {
        DraftDatabase draftDatabase = DatabaseFactory.getDraftDatabase(ConversationActivity.this);
        List<Draft> results         = draftDatabase.getDrafts(threadId);

        draftDatabase.clearDrafts(threadId);

        return results;
      }

      @Override
      protected void onPostExecute(List<Draft> drafts) {
        if (drafts.isEmpty()) {
          future.set(false);
          updateToggleButtonState();
          return;
        }

        AtomicInteger                      draftsRemaining = new AtomicInteger(drafts.size());
        AtomicBoolean                      success         = new AtomicBoolean(false);
        ListenableFuture.Listener<Boolean> listener        = new AssertedSuccessListener<Boolean>() {
          @Override
          public void onSuccess(Boolean result) {
            success.compareAndSet(false, result);

            if (draftsRemaining.decrementAndGet() <= 0) {
              future.set(success.get());
            }
          }
        };

        for (Draft draft : drafts) {
          try {
            switch (draft.getType()) {
              case Draft.TEXT:
                composeText.setText(draft.getValue());
                listener.onSuccess(true);
                break;
              case Draft.LOCATION:
                attachmentManager.setLocation(SignalPlace.deserialize(draft.getValue()), getCurrentMediaConstraints()).addListener(listener);
                break;
              case Draft.IMAGE:
                setMedia(Uri.parse(draft.getValue()), MediaType.IMAGE).addListener(listener);
                break;
              case Draft.AUDIO:
                setMedia(Uri.parse(draft.getValue()), MediaType.AUDIO).addListener(listener);
                break;
              case Draft.VIDEO:
                setMedia(Uri.parse(draft.getValue()), MediaType.VIDEO).addListener(listener);
                break;
              case Draft.QUOTE:
                SettableFuture<Boolean> quoteResult = new SettableFuture<>();
                new QuoteRestorationTask(draft.getValue(), quoteResult).execute();
                quoteResult.addListener(listener);
                break;
            }
          } catch (IOException e) {
            Log.w(TAG, e);
          }
        }

        updateToggleButtonState();
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

    return future;
  }

  private ListenableFuture<Boolean> initializeSecurity(final boolean currentSecureText,
                                                       final boolean currentIsDefaultSms)
  {
    final SettableFuture<Boolean> future = new SettableFuture<>();

    handleSecurityChange(currentSecureText || isPushGroupConversation(), currentIsDefaultSms);

    new AsyncTask<Recipient, Void, boolean[]>() {
      @Override
      protected boolean[] doInBackground(Recipient... params) {
        Context           context         = ConversationActivity.this;
        Recipient         recipient       = params[0].resolve();
        Log.i(TAG, "Resolving registered state...");
        RegisteredState registeredState;

        if (recipient.isPushGroup()) {
          Log.i(TAG, "Push group recipient...");
          registeredState = RegisteredState.REGISTERED;
        } else {
          Log.i(TAG, "Checking through resolved recipient");
          registeredState = recipient.resolve().getRegistered();
        }

        Log.i(TAG, "Resolved registered state: " + registeredState);
        boolean           signalEnabled   = TextSecurePreferences.isPushRegistered(context);

        if (registeredState == RegisteredState.UNKNOWN) {
          try {
            Log.i(TAG, "Refreshing directory for user: " + recipient.getId().serialize());
            registeredState = DirectoryHelper.refreshDirectoryFor(context, recipient, false);
          } catch (IOException e) {
            Log.w(TAG, e);
          }
        }

        Log.i(TAG, "Returning registered state...");
        return new boolean[] {registeredState == RegisteredState.REGISTERED && signalEnabled,
                              Util.isDefaultSmsProvider(context)};
      }

      @Override
      protected void onPostExecute(boolean[] result) {
        if (result[0] != currentSecureText || result[1] != currentIsDefaultSms) {
          Log.i(TAG, "onPostExecute() handleSecurityChange: " + result[0] + " , " + result[1]);
          handleSecurityChange(result[0], result[1]);
        }
        future.set(true);
        onSecurityUpdated();
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipient.get());

    return future;
  }

  private void onSecurityUpdated() {
    Log.i(TAG, "onSecurityUpdated()");
    updateReminders();
    updateDefaultSubscriptionId(recipient.get().getDefaultSubscriptionId());
  }

  private void initializeInsightObserver() {
    inviteReminderModel = new InviteReminderModel(this, new InviteReminderRepository(this));
    inviteReminderModel.loadReminder(recipient, this::updateReminders);
  }

  protected void updateReminders() {
    Optional<Reminder> inviteReminder = inviteReminderModel.getReminder();

    if (UnauthorizedReminder.isEligible(this)) {
      reminderView.get().showReminder(new UnauthorizedReminder(this));
    } else if (ExpiredBuildReminder.isEligible()) {
      reminderView.get().showReminder(new ExpiredBuildReminder(this));
    } else if (ServiceOutageReminder.isEligible(this)) {
      ApplicationDependencies.getJobManager().add(new ServiceOutageDetectionJob());
      reminderView.get().showReminder(new ServiceOutageReminder(this));
    } else if (TextSecurePreferences.isPushRegistered(this)      &&
               TextSecurePreferences.isShowInviteReminders(this) &&
               !isSecureText                                     &&
               inviteReminder.isPresent()                        &&
               !recipient.get().isGroup()) {
      reminderView.get().setOnActionClickListener(this::handleReminderAction);
      reminderView.get().setOnDismissListener(() -> inviteReminderModel.dismissReminder());
      reminderView.get().showReminder(inviteReminder.get());
    } else if (reminderView.resolved()) {
      reminderView.get().hide();
    }
  }

  private void handleReminderAction(@IdRes int reminderActionId) {
    switch (reminderActionId) {
      case R.id.reminder_action_invite:
        handleInviteLink();
        reminderView.get().requestDismiss();
        break;
      case R.id.reminder_action_view_insights:
        InsightsLauncher.showInsightsDashboard(getSupportFragmentManager());
        break;
      default:
        throw new IllegalArgumentException("Unknown ID: " + reminderActionId);
    }
  }

  private void updateDefaultSubscriptionId(Optional<Integer> defaultSubscriptionId) {
    Log.i(TAG, "updateDefaultSubscriptionId(" + defaultSubscriptionId.orNull() + ")");
    sendButton.setDefaultSubscriptionId(defaultSubscriptionId);
  }

  private void initializeMmsEnabledCheck() {
    new AsyncTask<Void, Void, Boolean>() {
      @Override
      protected Boolean doInBackground(Void... params) {
        return Util.isMmsCapable(ConversationActivity.this);
      }

      @Override
      protected void onPostExecute(Boolean isMmsEnabled) {
        ConversationActivity.this.isMmsEnabled = isMmsEnabled;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  private ListenableFuture<Boolean> initializeIdentityRecords() {
    final SettableFuture<Boolean> future = new SettableFuture<>();

    new AsyncTask<Recipient, Void, Pair<IdentityRecordList, String>>() {
      @Override
      protected @NonNull Pair<IdentityRecordList, String> doInBackground(Recipient... params) {
        IdentityDatabase   identityDatabase   = DatabaseFactory.getIdentityDatabase(ConversationActivity.this);
        IdentityRecordList identityRecordList = new IdentityRecordList();
        List<Recipient>    recipients         = new LinkedList<>();

        if (params[0].isGroup()) {
          recipients.addAll(DatabaseFactory.getGroupDatabase(ConversationActivity.this)
                                           .getGroupMembers(params[0].requireGroupId(), GroupDatabase.MemberSet.FULL_MEMBERS_EXCLUDING_SELF));
        } else {
          recipients.add(params[0]);
        }

        for (Recipient recipient : recipients) {
          Log.i(TAG, "Loading identity for: " + recipient.getId());
          identityRecordList.add(identityDatabase.getIdentity(recipient.getId()));
        }

        String message = null;

        if (identityRecordList.isUnverified()) {
          message = IdentityUtil.getUnverifiedBannerDescription(ConversationActivity.this, identityRecordList.getUnverifiedRecipients(ConversationActivity.this));
        }

        return new Pair<>(identityRecordList, message);
      }

      @Override
      protected void onPostExecute(@NonNull Pair<IdentityRecordList, String> result) {
        Log.i(TAG, "Got identity records: " + result.first().isUnverified());
        identityRecords.replaceWith(result.first());

        if (result.second() != null) {
          Log.d(TAG, "Replacing banner...");
          unverifiedBannerView.get().display(result.second(), result.first().getUnverifiedRecords(),
                                             new UnverifiedClickedListener(),
                                             new UnverifiedDismissedListener());
        } else if (unverifiedBannerView.resolved()) {
          Log.d(TAG, "Clearing banner...");
          unverifiedBannerView.get().hide();
        }

        titleView.setVerified(isSecureText && identityRecords.isVerified());

        future.set(true);
      }

    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, recipient.get());

    return future;
  }

  private void initializeViews() {
    titleView                = findViewById(R.id.conversation_title_view);
    buttonToggle             = ViewUtil.findById(this, R.id.button_toggle);
    sendButton               = ViewUtil.findById(this, R.id.send_button);
    attachButton             = ViewUtil.findById(this, R.id.attach_button);
    composeText              = ViewUtil.findById(this, R.id.embedded_text_editor);
    charactersLeft           = ViewUtil.findById(this, R.id.space_left);
    emojiDrawerStub          = ViewUtil.findStubById(this, R.id.emoji_drawer_stub);
    attachmentKeyboardStub   = ViewUtil.findStubById(this, R.id.attachment_keyboard_stub);
    unblockButton            = ViewUtil.findById(this, R.id.unblock_button);
    makeDefaultSmsButton     = ViewUtil.findById(this, R.id.make_default_sms_button);
    registerButton           = ViewUtil.findById(this, R.id.register_button);
    composePanel             = ViewUtil.findById(this, R.id.bottom_panel);
    container                = ViewUtil.findById(this, R.id.layout_container);
    reminderView             = ViewUtil.findStubById(this, R.id.reminder_stub);
    unverifiedBannerView     = ViewUtil.findStubById(this, R.id.unverified_banner_stub);
    groupShareProfileView    = ViewUtil.findStubById(this, R.id.group_share_profile_view_stub);
    quickAttachmentToggle    = ViewUtil.findById(this, R.id.quick_attachment_toggle);
    inlineAttachmentToggle   = ViewUtil.findById(this, R.id.inline_attachment_container);
    inputPanel               = ViewUtil.findById(this, R.id.bottom_panel);
    panelParent              = ViewUtil.findById(this, R.id.conversation_activity_panel_parent);
    searchNav                = ViewUtil.findById(this, R.id.conversation_search_nav);
    messageRequestBottomView = ViewUtil.findById(this, R.id.conversation_activity_message_request_bottom_bar);
    reactionOverlay          = ViewUtil.findById(this, R.id.conversation_reaction_scrubber);

    ImageButton quickCameraToggle      = ViewUtil.findById(this, R.id.quick_camera_toggle);
    ImageButton inlineAttachmentButton = ViewUtil.findById(this, R.id.inline_attachment_button);

    container.addOnKeyboardShownListener(this);
    inputPanel.setListener(this);
    inputPanel.setMediaListener(this);

    attachmentManager = new AttachmentManager(this, this);
    audioRecorder     = new AudioRecorder(this);
    typingTextWatcher = new TypingStatusTextWatcher();

    SendButtonListener        sendButtonListener        = new SendButtonListener();
    ComposeKeyPressedListener composeKeyPressedListener = new ComposeKeyPressedListener();

    composeText.setOnEditorActionListener(sendButtonListener);
    composeText.setCursorPositionChangedListener(this);
    attachButton.setOnClickListener(new AttachButtonListener());
    attachButton.setOnLongClickListener(new AttachButtonLongClickListener());
    sendButton.setOnClickListener(sendButtonListener);
    sendButton.setEnabled(true);
    sendButton.addOnTransportChangedListener((newTransport, manuallySelected) -> {
      calculateCharactersRemaining();
      updateLinkPreviewState();
      composeText.setTransport(newTransport);

      buttonToggle.getBackground().setColorFilter(newTransport.getBackgroundColor(), PorterDuff.Mode.MULTIPLY);
      buttonToggle.getBackground().invalidateSelf();

      if (manuallySelected) recordTransportPreference(newTransport);
    });

    titleView.setOnClickListener(v -> handleConversationSettings());
    titleView.setOnLongClickListener(v -> handleDisplayQuickContact());
    unblockButton.setOnClickListener(v -> handleUnblock());
    makeDefaultSmsButton.setOnClickListener(v -> handleMakeDefaultSms());
    registerButton.setOnClickListener(v -> handleRegisterForSignal());

    composeText.setOnKeyListener(composeKeyPressedListener);
    composeText.addTextChangedListener(composeKeyPressedListener);
    composeText.setOnEditorActionListener(sendButtonListener);
    composeText.setOnClickListener(composeKeyPressedListener);
    composeText.setOnFocusChangeListener(composeKeyPressedListener);

    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) && Camera.getNumberOfCameras() > 0) {
      quickCameraToggle.setVisibility(View.VISIBLE);
      quickCameraToggle.setOnClickListener(new QuickCameraToggleListener());
    } else {
      quickCameraToggle.setVisibility(View.GONE);
    }

    searchNav.setEventListener(this);

    inlineAttachmentButton.setOnClickListener(v -> handleAddAttachment());

    reactionOverlay.setOnReactionSelectedListener(this::onReactionSelected);
  }

  protected void initializeActionBar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar == null) throw new AssertionError();

    supportActionBar.setDisplayHomeAsUpEnabled(true);
    supportActionBar.setDisplayShowTitleEnabled(false);
  }

  private void initializeResources() {
    if (recipient != null) {
      recipient.removeObservers(this);
    }

    recipient        = Recipient.live(getIntent().getParcelableExtra(RECIPIENT_EXTRA));
    threadId         = getIntent().getLongExtra(THREAD_ID_EXTRA, -1);
    archived         = getIntent().getBooleanExtra(IS_ARCHIVED_EXTRA, false);
    distributionType = getIntent().getIntExtra(DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
    glideRequests    = GlideApp.with(this);

    recipient.observe(this, this::onRecipientChanged);
  }


  private void initializeLinkPreviewObserver() {
    linkPreviewViewModel = ViewModelProviders.of(this, new LinkPreviewViewModel.Factory(new LinkPreviewRepository())).get(LinkPreviewViewModel.class);

    if (!TextSecurePreferences.isLinkPreviewsEnabled(this)) {
      linkPreviewViewModel.onUserCancel();
      return;
    }

    linkPreviewViewModel.getLinkPreviewState().observe(this, previewState -> {
      if (previewState == null) return;

      if (previewState.isLoading()) {
        Log.d(TAG, "Loading link preview.");
        inputPanel.setLinkPreviewLoading();
      } else {
        Log.d(TAG, "Setting link preview: " + previewState.getLinkPreview().isPresent());
        inputPanel.setLinkPreview(glideRequests, previewState.getLinkPreview());
      }

      updateToggleButtonState();
    });
  }

  private void initializeSearchObserver() {
    searchViewModel = ViewModelProviders.of(this).get(ConversationSearchViewModel.class);

    searchViewModel.getSearchResults().observe(this, result -> {
      if (result == null) return;

      if (!result.getResults().isEmpty()) {
        MessageResult messageResult = result.getResults().get(result.getPosition());
        fragment.jumpToMessage(messageResult.messageRecipient.getId(), messageResult.receivedTimestampMs, searchViewModel::onMissingResult);
      }

      searchNav.setData(result.getPosition(), result.getResults().size());
    });
  }

  private void initializeStickerObserver() {
    StickerSearchRepository repository = new StickerSearchRepository(this);

    stickerViewModel = ViewModelProviders.of(this, new ConversationStickerViewModel.Factory(getApplication(), repository))
                                         .get(ConversationStickerViewModel.class);

    stickerViewModel.getStickerResults().observe(this, stickers -> {
      if (stickers == null) return;

      inputPanel.setStickerSuggestions(stickers);
    });

    stickerViewModel.getStickersAvailability().observe(this, stickersAvailable -> {
      if (stickersAvailable == null) return;

      boolean           isSystemEmojiPreferred = TextSecurePreferences.isSystemEmojiPreferred(this);
      MediaKeyboardMode keyboardMode           = TextSecurePreferences.getMediaKeyboardMode(this);
      boolean           stickerIntro           = !TextSecurePreferences.hasSeenStickerIntroTooltip(this);

      if (stickersAvailable) {
        inputPanel.showMediaKeyboardToggle(true);
        inputPanel.setMediaKeyboardToggleMode(isSystemEmojiPreferred || keyboardMode == MediaKeyboardMode.STICKER);
        if (stickerIntro) showStickerIntroductionTooltip();
      }

      if (emojiDrawerStub.resolved()) {
        initializeMediaKeyboardProviders(emojiDrawerStub.get(), stickersAvailable);
      }
    });
  }

  private void initializeViewModel() {
    this.viewModel = ViewModelProviders.of(this, new ConversationViewModel.Factory()).get(ConversationViewModel.class);
  }

  private void showStickerIntroductionTooltip() {
    TextSecurePreferences.setMediaKeyboardMode(this, MediaKeyboardMode.STICKER);
    inputPanel.setMediaKeyboardToggleMode(true);

    TooltipPopup.forTarget(inputPanel.getMediaKeyboardToggleAnchorView())
                .setBackgroundTint(getResources().getColor(R.color.core_ultramarine))
                .setTextColor(getResources().getColor(R.color.core_white))
                .setText(R.string.ConversationActivity_new_say_it_with_stickers)
                .setOnDismissListener(() -> {
                  TextSecurePreferences.setHasSeenStickerIntroTooltip(this, true);
                  EventBus.getDefault().removeStickyEvent(StickerPackInstallEvent.class);
                })
                .show(TooltipPopup.POSITION_ABOVE);
  }


  private void onReactionSelected(MessageRecord messageRecord, String emoji) {
    final Context context = getApplicationContext();

    SignalExecutors.BOUNDED.execute(() -> {
      ReactionRecord oldRecord = Stream.of(messageRecord.getReactions())
                                       .filter(record -> record.getAuthor().equals(Recipient.self().getId()))
                                       .findFirst()
                                       .orElse(null);

      if (oldRecord != null && oldRecord.getEmoji().equals(emoji)) {
        MessageSender.sendReactionRemoval(context, messageRecord.getId(), messageRecord.isMms(), oldRecord);
      } else {
        MessageSender.sendNewReaction(context, messageRecord.getId(), messageRecord.isMms(), emoji);
      }
    });
  }

  @Override
  public void onSearchMoveUpPressed() {
    searchViewModel.onMoveUp();
  }

  @Override
  public void onSearchMoveDownPressed() {
    searchViewModel.onMoveDown();
  }

  private void initializeProfiles() {
    if (!isSecureText) {
      Log.i(TAG, "SMS contact, no profile fetch needed.");
      return;
    }

    ApplicationDependencies.getJobManager().add(new RetrieveProfileJob(recipient.get()));
  }

  private void onRecipientChanged(@NonNull Recipient recipient) {
    Log.i(TAG, "onModified(" + recipient.getId() + ") " + recipient.getRegistered());
    titleView.setTitle(glideRequests, recipient);
    titleView.setVerified(identityRecords.isVerified());
    setBlockedUserState(recipient, isSecureText, isDefaultSms);
    setActionBarColor(recipient.getColor());
    setGroupShareProfileReminder(recipient);
    updateReminders();
    updateDefaultSubscriptionId(recipient.getDefaultSubscriptionId());
    initializeSecurity(isSecureText, isDefaultSms);

    if (searchViewItem == null || !searchViewItem.isActionViewExpanded()) {
      invalidateOptionsMenu();
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onIdentityRecordUpdate(final IdentityRecord event) {
    initializeIdentityRecords();
  }

  @Subscribe(threadMode =  ThreadMode.MAIN, sticky = true)
  public void onStickerPackInstalled(final StickerPackInstallEvent event) {
    if (!TextSecurePreferences.hasSeenStickerIntroTooltip(this)) return;

    EventBus.getDefault().removeStickyEvent(event);

    if (!inputPanel.isStickerMode()) {
      TooltipPopup.forTarget(inputPanel.getMediaKeyboardToggleAnchorView())
                  .setText(R.string.ConversationActivity_sticker_pack_installed)
                  .setIconGlideModel(event.getIconGlideModel())
                  .show(TooltipPopup.POSITION_ABOVE);
    }
  }

  private void initializeReceivers() {
    securityUpdateReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        initializeSecurity(isSecureText, isDefaultSms);
        calculateCharactersRemaining();
      }
    };

    registerReceiver(securityUpdateReceiver,
                     new IntentFilter(SecurityEvent.SECURITY_UPDATE_EVENT),
                     KeyCachingService.KEY_PERMISSION, null);
  }

  //////// Helper Methods

  private ListenableFuture<Boolean> setMedia(@Nullable Uri uri, @NonNull MediaType mediaType) {
    return setMedia(uri, mediaType, 0, 0);
  }

  private ListenableFuture<Boolean> setMedia(@Nullable Uri uri, @NonNull MediaType mediaType, int width, int height) {
    if (uri == null) {
      return new SettableFuture<>(false);
    }

    if (MediaType.VCARD.equals(mediaType) && isSecureText) {
      openContactShareEditor(uri);
      return new SettableFuture<>(false);
    } else if (MediaType.IMAGE.equals(mediaType) || MediaType.GIF.equals(mediaType) || MediaType.VIDEO.equals(mediaType)) {
      Media media = new Media(uri, MediaUtil.getMimeType(this, uri), 0, width, height, 0, 0, Optional.absent(), Optional.absent(), Optional.absent());
      startActivityForResult(MediaSendActivity.buildEditorIntent(ConversationActivity.this, Collections.singletonList(media), recipient.get(), composeText.getTextTrimmed(), sendButton.getSelectedTransport()), MEDIA_SENDER);
      return new SettableFuture<>(false);
    } else {
      return attachmentManager.setMedia(glideRequests, uri, mediaType, getCurrentMediaConstraints(), width, height);
    }
  }

  private void openContactShareEditor(Uri contactUri) {
    Intent intent = ContactShareEditActivity.getIntent(this, Collections.singletonList(contactUri));
    startActivityForResult(intent, GET_CONTACT_DETAILS);
  }

  private void addAttachmentContactInfo(Uri contactUri) {
    ContactAccessor contactDataList = ContactAccessor.getInstance();
    ContactData contactData = contactDataList.getContactData(this, contactUri);

    if      (contactData.numbers.size() == 1) composeText.append(contactData.numbers.get(0).number);
    else if (contactData.numbers.size() > 1)  selectContactInfo(contactData);
  }

  private void sendSharedContact(List<Contact> contacts) {
    int        subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
    long       expiresIn      = recipient.get().getExpireMessages() * 1000L;
    boolean    initiating     = threadId == -1;

    sendMediaMessage(isSmsForced(), "", attachmentManager.buildSlideDeck(), null, contacts, Collections.emptyList(), expiresIn, false, subscriptionId, initiating, false);
  }

  private void selectContactInfo(ContactData contactData) {
    final CharSequence[] numbers     = new CharSequence[contactData.numbers.size()];
    final CharSequence[] numberItems = new CharSequence[contactData.numbers.size()];

    for (int i = 0; i < contactData.numbers.size(); i++) {
      numbers[i]     = contactData.numbers.get(i).number;
      numberItems[i] = contactData.numbers.get(i).type + ": " + contactData.numbers.get(i).number;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setIconAttribute(R.attr.conversation_attach_contact_info);
    builder.setTitle(R.string.ConversationActivity_select_contact_info);

    builder.setItems(numberItems, (dialog, which) -> composeText.append(numbers[which]));
    builder.show();
  }

  private Drafts getDraftsForCurrentState() {
    Drafts drafts = new Drafts();

    if (!Util.isEmpty(composeText)) {
      drafts.add(new Draft(Draft.TEXT, composeText.getTextTrimmed()));
    }

    for (Slide slide : attachmentManager.buildSlideDeck().getSlides()) {
      if      (slide.hasAudio() && slide.getUri() != null)    drafts.add(new Draft(Draft.AUDIO, slide.getUri().toString()));
      else if (slide.hasVideo() && slide.getUri() != null)    drafts.add(new Draft(Draft.VIDEO, slide.getUri().toString()));
      else if (slide.hasLocation())                           drafts.add(new Draft(Draft.LOCATION, ((LocationSlide)slide).getPlace().serialize()));
      else if (slide.hasImage() && slide.getUri() != null)    drafts.add(new Draft(Draft.IMAGE, slide.getUri().toString()));
    }

    Optional<QuoteModel> quote = inputPanel.getQuote();

    if (quote.isPresent()) {
      drafts.add(new Draft(Draft.QUOTE, new QuoteId(quote.get().getId(), quote.get().getAuthor()).serialize()));
    }

    return drafts;
  }

  protected ListenableFuture<Long> saveDraft() {
    final SettableFuture<Long> future = new SettableFuture<>();

    if (this.recipient == null) {
      future.set(threadId);
      return future;
    }

    final Drafts       drafts               = getDraftsForCurrentState();
    final long         thisThreadId         = this.threadId;
    final int          thisDistributionType = this.distributionType;

    new AsyncTask<Long, Void, Long>() {
      @Override
      protected Long doInBackground(Long... params) {
        ThreadDatabase threadDatabase = DatabaseFactory.getThreadDatabase(ConversationActivity.this);
        DraftDatabase  draftDatabase  = DatabaseFactory.getDraftDatabase(ConversationActivity.this);
        long           threadId       = params[0];

        if (drafts.size() > 0) {
          if (threadId == -1) threadId = threadDatabase.getThreadIdFor(getRecipient(), thisDistributionType);

          draftDatabase.insertDrafts(threadId, drafts);
          threadDatabase.updateSnippet(threadId, drafts.getSnippet(ConversationActivity.this),
                                       drafts.getUriSnippet(),
                                       System.currentTimeMillis(), Types.BASE_DRAFT_TYPE, true);
        } else if (threadId > 0) {
          threadDatabase.update(threadId, false);
        }

        return threadId;
      }

      @Override
      protected void onPostExecute(Long result) {
        future.set(result);
      }

    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, thisThreadId);

    return future;
  }

  private void setActionBarColor(MaterialColor color) {
    ActionBar supportActionBar = getSupportActionBar();
    if (supportActionBar == null) throw new AssertionError();
    supportActionBar.setBackgroundDrawable(new ColorDrawable(color.toActionBarColor(this)));
    setStatusBarColor(color.toStatusBarColor(this));
  }

  private void setBlockedUserState(Recipient recipient, boolean isSecureText, boolean isDefaultSms) {
    if (recipient.isBlocked() && !FeatureFlags.messageRequests()) {
      unblockButton.setVisibility(View.VISIBLE);
      composePanel.setVisibility(View.GONE);
      makeDefaultSmsButton.setVisibility(View.GONE);
      registerButton.setVisibility(View.GONE);
    } else if (!isSecureText && isPushGroupConversation()) {
      unblockButton.setVisibility(View.GONE);
      composePanel.setVisibility(View.GONE);
      makeDefaultSmsButton.setVisibility(View.GONE);
      registerButton.setVisibility(View.VISIBLE);
    } else if (!isSecureText && !isDefaultSms) {
      unblockButton.setVisibility(View.GONE);
      composePanel.setVisibility(View.GONE);
      makeDefaultSmsButton.setVisibility(View.VISIBLE);
      registerButton.setVisibility(View.GONE);
    } else {
      composePanel.setVisibility(View.VISIBLE);
      unblockButton.setVisibility(View.GONE);
      makeDefaultSmsButton.setVisibility(View.GONE);
      registerButton.setVisibility(View.GONE);
    }
  }

  private void setGroupShareProfileReminder(@NonNull Recipient recipient) {
    if (FeatureFlags.messageRequests()) {
      return;
    }

    if (recipient.isPushGroup() && !recipient.isProfileSharing()) {
      groupShareProfileView.get().setRecipient(recipient);
      groupShareProfileView.get().setVisibility(View.VISIBLE);
    } else if (groupShareProfileView.resolved()) {
      groupShareProfileView.get().setVisibility(View.GONE);
    }
  }

  private void calculateCharactersRemaining() {
    String          messageBody     = composeText.getTextTrimmed();
    TransportOption transportOption = sendButton.getSelectedTransport();
    CharacterState  characterState  = transportOption.calculateCharacters(messageBody);

    if (characterState.charactersRemaining <= 15 || characterState.messagesSpent > 1) {
      charactersLeft.setText(String.format(dynamicLanguage.getCurrentLocale(),
                                           "%d/%d (%d)",
                                           characterState.charactersRemaining,
                                           characterState.maxTotalMessageSize,
                                           characterState.messagesSpent));
      charactersLeft.setVisibility(View.VISIBLE);
    } else {
      charactersLeft.setVisibility(View.GONE);
    }
  }

  private void initializeMediaKeyboardProviders(@NonNull MediaKeyboard mediaKeyboard, boolean stickersAvailable) {
    boolean isSystemEmojiPreferred   = TextSecurePreferences.isSystemEmojiPreferred(this);

    if (stickersAvailable) {
      if (isSystemEmojiPreferred) {
        mediaKeyboard.setProviders(0, new StickerKeyboardProvider(this, this));
      } else {
        MediaKeyboardMode keyboardMode = TextSecurePreferences.getMediaKeyboardMode(this);
        int               index        = keyboardMode == MediaKeyboardMode.STICKER ? 1 : 0;

        mediaKeyboard.setProviders(index,
                                   new EmojiKeyboardProvider(this, inputPanel),
                                   new StickerKeyboardProvider(this, this));
      }
    } else if (!isSystemEmojiPreferred) {
      mediaKeyboard.setProviders(0, new EmojiKeyboardProvider(this, inputPanel));
    }
  }

  private boolean isInMessageRequest() {
    return messageRequestBottomView.getVisibility() == View.VISIBLE;
  }

  private boolean isSingleConversation() {
    return getRecipient() != null && !getRecipient().isGroup();
  }

  private boolean isActiveGroup() {
    if (!isGroupConversation()) return false;

    Optional<GroupRecord> record = DatabaseFactory.getGroupDatabase(this).getGroup(getRecipient().getId());
    return record.isPresent() && record.get().isActive();
  }

  @SuppressWarnings("SimplifiableIfStatement")
  private boolean isSelfConversation() {
    if (!TextSecurePreferences.isPushRegistered(this)) return false;
    if (recipient.get().isGroup())                     return false;

    return recipient.get().isLocalNumber();
  }

  private boolean isGroupConversation() {
    return getRecipient() != null && getRecipient().isGroup();
  }

  private boolean isPushGroupConversation() {
    return getRecipient() != null && getRecipient().isPushGroup();
  }

  private boolean isSmsForced() {
    return sendButton.isManualSelection() && sendButton.getSelectedTransport().isSms();
  }

  protected Recipient getRecipient() {
    return this.recipient.get();
  }

  protected long getThreadId() {
    return this.threadId;
  }

  private String getMessage() throws InvalidMessageException {
    String rawText = composeText.getTextTrimmed();

    if (rawText.length() < 1 && !attachmentManager.isAttachmentPresent())
      throw new InvalidMessageException(getString(R.string.ConversationActivity_message_is_empty_exclamation));

    return rawText;
  }

  private MediaConstraints getCurrentMediaConstraints() {
    return sendButton.getSelectedTransport().getType() == Type.TEXTSECURE
           ? MediaConstraints.getPushMediaConstraints()
           : MediaConstraints.getMmsMediaConstraints(sendButton.getSelectedTransport().getSimSubscriptionId().or(-1));
  }

  private void markThreadAsRead() {
    new AsyncTask<Long, Void, Void>() {
      @Override
      protected Void doInBackground(Long... params) {
        Context                 context    = ConversationActivity.this;
        List<MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context).setRead(params[0], false);

        MessageNotifier.updateNotification(context);
        MarkReadReceiver.process(context, messageIds);

        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, threadId);
  }

  private void markLastSeen() {
    new AsyncTask<Long, Void, Void>() {
      @Override
      protected Void doInBackground(Long... params) {
        DatabaseFactory.getThreadDatabase(ConversationActivity.this).setLastSeen(params[0]);
        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, threadId);
  }

  protected void sendComplete(long threadId) {
    boolean refreshFragment = (threadId != this.threadId);
    this.threadId = threadId;

    if (fragment == null || !fragment.isVisible() || isFinishing()) {
      return;
    }

    fragment.setLastSeen(0);

    if (refreshFragment) {
      fragment.reload(recipient.get(), threadId);
      MessageNotifier.setVisibleThread(threadId);
    }

    fragment.scrollToBottom();
    attachmentManager.cleanup();

    updateLinkPreviewState();
  }

  private void sendMessage() {
    if (inputPanel.isRecordingInLockedMode()) {
      inputPanel.releaseRecordingLock();
      return;
    }

    try {
      Recipient recipient = getRecipient();

      if (recipient == null) {
        throw new RecipientFormattingException("Badly formatted");
      }

      String          message        = getMessage();
      TransportOption transport      = sendButton.getSelectedTransport();
      boolean         forceSms       = (recipient.isForceSmsSelection() || sendButton.isManualSelection()) && transport.isSms();
      int             subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
      long            expiresIn      = recipient.getExpireMessages() * 1000L;
      boolean         initiating     = threadId == -1;
      boolean         needsSplit     = !transport.isSms() && message.length() > transport.calculateCharacters(message).maxPrimaryMessageSize;
      boolean         isMediaMessage = attachmentManager.isAttachmentPresent() ||
                                       recipient.isGroup()                     ||
                                       recipient.getEmail().isPresent()        ||
                                       inputPanel.getQuote().isPresent()       ||
                                       linkPreviewViewModel.hasLinkPreview()   ||
                                       needsSplit;

      Log.i(TAG, "isManual Selection: " + sendButton.isManualSelection());
      Log.i(TAG, "forceSms: " + forceSms);

      if ((recipient.isMmsGroup() || recipient.getEmail().isPresent()) && !isMmsEnabled) {
        handleManualMmsRequired();
      } else if (!forceSms && identityRecords.isUnverified()) {
        handleUnverifiedRecipients();
      } else if (!forceSms && identityRecords.isUntrusted()) {
        handleUntrustedRecipients();
      } else if (isMediaMessage) {
        sendMediaMessage(forceSms, expiresIn, false, subscriptionId, initiating);
      } else {
        sendTextMessage(forceSms, expiresIn, subscriptionId, initiating);
      }
    } catch (RecipientFormattingException ex) {
      Toast.makeText(ConversationActivity.this,
                     R.string.ConversationActivity_recipient_is_not_a_valid_sms_or_email_address_exclamation,
                     Toast.LENGTH_LONG).show();
      Log.w(TAG, ex);
    } catch (InvalidMessageException ex) {
      Toast.makeText(ConversationActivity.this, R.string.ConversationActivity_message_is_empty_exclamation,
                     Toast.LENGTH_SHORT).show();
      Log.w(TAG, ex);
    }
  }

  private void sendMediaMessage(@NonNull MediaSendActivityResult result) {
    long                 expiresIn     = recipient.get().getExpireMessages() * 1000L;
    QuoteModel           quote         = result.isViewOnce() ? null : inputPanel.getQuote().orNull();
    boolean              initiating    = threadId == -1;
    OutgoingMediaMessage message       = new OutgoingMediaMessage(recipient.get(), new SlideDeck(), result.getBody(), System.currentTimeMillis(), -1, expiresIn, result.isViewOnce(), distributionType, quote, Collections.emptyList(), Collections.emptyList());
    OutgoingMediaMessage secureMessage = new OutgoingSecureMediaMessage(message                                                                                                                                                                                      );

    ApplicationContext.getInstance(this).getTypingStatusSender().onTypingStopped(threadId);

    inputPanel.clearQuote();
    attachmentManager.clear(glideRequests, false);
    silentlySetComposeText("");

    long id = fragment.stageOutgoingMessage(message);

    SimpleTask.run(() -> {
      if (!FeatureFlags.messageRequests() && initiating) {
        DatabaseFactory.getRecipientDatabase(this).setProfileSharing(recipient.getId(), true);
      }

      long resultId = MessageSender.sendPushWithPreUploadedMedia(this, secureMessage, result.getPreUploadResults(), threadId, () -> fragment.releaseOutgoingMessage(id));

      int deleted = DatabaseFactory.getAttachmentDatabase(this).deleteAbandonedPreuploadedAttachments();
      Log.i(TAG, "Deleted " + deleted + " abandoned attachments.");

      return resultId;
    }, this::sendComplete);
  }

  private void sendMediaMessage(final boolean forceSms, final long expiresIn, final boolean viewOnce, final int subscriptionId, final boolean initiating)
      throws InvalidMessageException
  {
    Log.i(TAG, "Sending media message...");
    sendMediaMessage(forceSms, getMessage(), attachmentManager.buildSlideDeck(), inputPanel.getQuote().orNull(), Collections.emptyList(), linkPreviewViewModel.getActiveLinkPreviews(), expiresIn, viewOnce, subscriptionId, initiating, true);
  }

  private ListenableFuture<Void> sendMediaMessage(final boolean forceSms,
                                                  String body,
                                                  SlideDeck slideDeck,
                                                  QuoteModel quote,
                                                  List<Contact> contacts,
                                                  List<LinkPreview> previews,
                                                  final long expiresIn,
                                                  final boolean viewOnce,
                                                  final int subscriptionId,
                                                  final boolean initiating,
                                                  final boolean clearComposeBox)
  {
    if (!isDefaultSms && (!isSecureText || forceSms)) {
      showDefaultSmsPrompt();
      return new SettableFuture<>(null);
    }

    if (isSecureText && !forceSms) {
      MessageUtil.SplitResult splitMessage = MessageUtil.getSplitMessage(this, body, sendButton.getSelectedTransport().calculateCharacters(body).maxPrimaryMessageSize);
      body = splitMessage.getBody();

      if (splitMessage.getTextSlide().isPresent()) {
        slideDeck.addSlide(splitMessage.getTextSlide().get());
      }
    }

    OutgoingMediaMessage outgoingMessageCandidate = new OutgoingMediaMessage(recipient.get(), slideDeck, body, System.currentTimeMillis(), subscriptionId, expiresIn, viewOnce, distributionType, quote, contacts, previews);

    final SettableFuture<Void> future  = new SettableFuture<>();
    final Context              context = getApplicationContext();

    final OutgoingMediaMessage outgoingMessage;

    if (isSecureText && !forceSms) {
      outgoingMessage = new OutgoingSecureMediaMessage(outgoingMessageCandidate);
      ApplicationContext.getInstance(context).getTypingStatusSender().onTypingStopped(threadId);
    } else {
      outgoingMessage = outgoingMessageCandidate;
    }

    Permissions.with(this)
               .request(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS)
               .ifNecessary(!isSecureText || forceSms)
               .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_needs_sms_permission_in_order_to_send_an_sms))
               .onAllGranted(() -> {
                 if (clearComposeBox) {
                   inputPanel.clearQuote();
                   attachmentManager.clear(glideRequests, false);
                   silentlySetComposeText("");
                 }

                 final long id = fragment.stageOutgoingMessage(outgoingMessage);

                 SimpleTask.run(() -> {
                   if (!FeatureFlags.messageRequests() && initiating) {
                     DatabaseFactory.getRecipientDatabase(this).setProfileSharing(recipient.getId(), true);
                   }

                   return MessageSender.send(context, outgoingMessage, threadId, forceSms, () -> fragment.releaseOutgoingMessage(id));
                 }, result -> {
                   sendComplete(result);
                   future.set(null);
                 });
               })
               .onAnyDenied(() -> future.set(null))
               .execute();

    return future;
  }

  private void sendTextMessage(final boolean forceSms, final long expiresIn, final int subscriptionId, final boolean initiating)
      throws InvalidMessageException
  {
    if (!isDefaultSms && (!isSecureText || forceSms)) {
      showDefaultSmsPrompt();
      return;
    }

    final Context context     = getApplicationContext();
    final String  messageBody = getMessage();

    OutgoingTextMessage message;

    if (isSecureText && !forceSms) {
      message = new OutgoingEncryptedMessage(recipient.get(), messageBody, expiresIn);
      ApplicationContext.getInstance(context).getTypingStatusSender().onTypingStopped(threadId);
    } else {
      message = new OutgoingTextMessage(recipient.get(), messageBody, expiresIn, subscriptionId);
    }

    Permissions.with(this)
               .request(Manifest.permission.SEND_SMS)
               .ifNecessary(forceSms || !isSecureText)
               .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_needs_sms_permission_in_order_to_send_an_sms))
               .onAllGranted(() -> {
                 silentlySetComposeText("");
                 final long id = fragment.stageOutgoingMessage(message);

                 new AsyncTask<OutgoingTextMessage, Void, Long>() {
                   @Override
                   protected Long doInBackground(OutgoingTextMessage... messages) {
                     if (!FeatureFlags.messageRequests() && initiating) {
                       DatabaseFactory.getRecipientDatabase(context).setProfileSharing(recipient.getId(), true);
                     }

                     return MessageSender.send(context, messages[0], threadId, forceSms, () -> fragment.releaseOutgoingMessage(id));
                   }

                   @Override
                   protected void onPostExecute(Long result) {
                     sendComplete(result);
                   }
                 }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);

               })
               .execute();
  }

  private void showDefaultSmsPrompt() {
    new AlertDialog.Builder(this)
                   .setMessage(R.string.ConversationActivity_signal_cannot_sent_sms_mms_messages_because_it_is_not_your_default_sms_app)
                   .setNegativeButton(R.string.ConversationActivity_no, (dialog, which) -> dialog.dismiss())
                   .setPositiveButton(R.string.ConversationActivity_yes, (dialog, which) -> handleMakeDefaultSms())
                   .show();
  }

  private void updateToggleButtonState() {
    if (inputPanel.isRecordingInLockedMode()) {
      buttonToggle.display(sendButton);
      quickAttachmentToggle.show();
      inlineAttachmentToggle.hide();
      return;
    }

    if (composeText.getText().length() == 0 && !attachmentManager.isAttachmentPresent()) {
      buttonToggle.display(attachButton);
      quickAttachmentToggle.show();
      inlineAttachmentToggle.hide();
    } else {
      buttonToggle.display(sendButton);
      quickAttachmentToggle.hide();

      if (!attachmentManager.isAttachmentPresent() && !linkPreviewViewModel.hasLinkPreview()) {
        inlineAttachmentToggle.show();
      } else {
        inlineAttachmentToggle.hide();
      }
    }
  }

  private void updateLinkPreviewState() {
    if (TextSecurePreferences.isLinkPreviewsEnabled(this) && !sendButton.getSelectedTransport().isSms() && !attachmentManager.isAttachmentPresent()) {
      linkPreviewViewModel.onEnabled();
      linkPreviewViewModel.onTextChanged(this, composeText.getTextTrimmed(), composeText.getSelectionStart(), composeText.getSelectionEnd());
    } else {
      linkPreviewViewModel.onUserCancel();
    }
  }

  private void recordTransportPreference(TransportOption transportOption) {
    new AsyncTask<Void, Void, Void>() {
      @Override
      protected Void doInBackground(Void... params) {
        RecipientDatabase recipientDatabase = DatabaseFactory.getRecipientDatabase(ConversationActivity.this);

        recipientDatabase.setDefaultSubscriptionId(recipient.getId(), transportOption.getSimSubscriptionId().or(-1));

        if (!recipient.resolve().isPushGroup()) {
          recipientDatabase.setForceSmsSelection(recipient.getId(), recipient.get().getRegistered() == RegisteredState.REGISTERED && transportOption.isSms());
        }

        return null;
      }
    }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
  }

  @Override
  public void onRecorderPermissionRequired() {
    Permissions.with(this)
               .request(Manifest.permission.RECORD_AUDIO)
               .ifNecessary()
               .withRationaleDialog(getString(R.string.ConversationActivity_to_send_audio_messages_allow_signal_access_to_your_microphone), R.drawable.ic_mic_solid_24)
               .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_requires_the_microphone_permission_in_order_to_send_audio_messages))
               .execute();
  }

  @Override
  public void onRecorderStarted() {
    Vibrator vibrator = ServiceUtil.getVibrator(this);
    vibrator.vibrate(20);

    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    audioRecorder.startRecording();
  }

  @Override
  public void onRecorderLocked() {
    updateToggleButtonState();
  }

  @Override
  public void onRecorderFinished() {
    updateToggleButtonState();
    Vibrator vibrator = ServiceUtil.getVibrator(this);
    vibrator.vibrate(20);

    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    ListenableFuture<Pair<Uri, Long>> future = audioRecorder.stopRecording();
    future.addListener(new ListenableFuture.Listener<Pair<Uri, Long>>() {
      @Override
      public void onSuccess(final @NonNull Pair<Uri, Long> result) {
        boolean    forceSms       = sendButton.isManualSelection() && sendButton.getSelectedTransport().isSms();
        boolean    initiating     = threadId == -1;
        int        subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
        long       expiresIn      = recipient.get().getExpireMessages() * 1000L;
        AudioSlide audioSlide     = new AudioSlide(ConversationActivity.this, result.first(), result.second(), MediaUtil.AUDIO_AAC, true);
        SlideDeck  slideDeck      = new SlideDeck();
        slideDeck.addSlide(audioSlide);

        sendMediaMessage(forceSms, "", slideDeck, inputPanel.getQuote().orNull(), Collections.emptyList(), Collections.emptyList(), expiresIn, false, subscriptionId, initiating, true).addListener(new AssertedSuccessListener<Void>() {
          @Override
          public void onSuccess(Void nothing) {
            new AsyncTask<Void, Void, Void>() {
              @Override
              protected Void doInBackground(Void... params) {
                BlobProvider.getInstance().delete(ConversationActivity.this, result.first());
                return null;
              }
            }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
          }
        });
      }

      @Override
      public void onFailure(ExecutionException e) {
        Toast.makeText(ConversationActivity.this, R.string.ConversationActivity_unable_to_record_audio, Toast.LENGTH_LONG).show();
      }
    });
  }

  @Override
  public void onRecorderCanceled() {
    updateToggleButtonState();
    Vibrator vibrator = ServiceUtil.getVibrator(this);
    vibrator.vibrate(50);

    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    ListenableFuture<Pair<Uri, Long>> future = audioRecorder.stopRecording();
    future.addListener(new ListenableFuture.Listener<Pair<Uri, Long>>() {
      @Override
      public void onSuccess(final Pair<Uri, Long> result) {
        new AsyncTask<Void, Void, Void>() {
          @Override
          protected Void doInBackground(Void... params) {
            BlobProvider.getInstance().delete(ConversationActivity.this, result.first());
            return null;
          }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
      }

      @Override
      public void onFailure(ExecutionException e) {}
    });
  }

  @Override
  public void onEmojiToggle() {
    if (!emojiDrawerStub.resolved()) {
      Boolean stickersAvailable = stickerViewModel.getStickersAvailability().getValue();

      initializeMediaKeyboardProviders(emojiDrawerStub.get(), stickersAvailable == null ? false : stickersAvailable);

      inputPanel.setMediaKeyboard(emojiDrawerStub.get());
    }

    if (container.getCurrentInput() == emojiDrawerStub.get()) {
      container.showSoftkey(composeText);
    } else {
      container.show(composeText, emojiDrawerStub.get());
    }
  }

  @Override
  public void onLinkPreviewCanceled() {
    linkPreviewViewModel.onUserCancel();
  }

  @Override
  public void onStickerSuggestionSelected(@NonNull StickerRecord sticker) {
    sendSticker(sticker, true);
  }

  @Override
  public void onMediaSelected(@NonNull Uri uri, String contentType) {
    if (!TextUtils.isEmpty(contentType) && contentType.trim().equals("image/gif")) {
      setMedia(uri, MediaType.GIF);
    } else if (MediaUtil.isImageType(contentType)) {
      setMedia(uri, MediaType.IMAGE);
    } else if (MediaUtil.isVideoType(contentType)) {
      setMedia(uri, MediaType.VIDEO);
    } else if (MediaUtil.isAudioType(contentType)) {
      setMedia(uri, MediaType.AUDIO);
    }
  }

  @Override
  public void onCursorPositionChanged(int start, int end) {
    linkPreviewViewModel.onTextChanged(this, composeText.getTextTrimmed(), start, end);
  }

  @Override
  public void onStickerSelected(@NonNull StickerRecord stickerRecord) {
    sendSticker(stickerRecord, false);
  }

  @Override
  public void onStickerManagementClicked() {
    startActivity(StickerManagementActivity.getIntent(this));
    container.hideAttachedInput(true);
  }

  private void sendSticker(@NonNull StickerRecord stickerRecord, boolean clearCompose) {
    sendSticker(new StickerLocator(stickerRecord.getPackId(), stickerRecord.getPackKey(), stickerRecord.getStickerId()), stickerRecord.getUri(), stickerRecord.getSize(), clearCompose);

    SignalExecutors.BOUNDED.execute(() ->
     DatabaseFactory.getStickerDatabase(getApplicationContext())
                    .updateStickerLastUsedTime(stickerRecord.getRowId(), System.currentTimeMillis())
    );
  }

  private void sendSticker(@NonNull StickerLocator stickerLocator, @NonNull Uri uri, long size, boolean clearCompose) {
    if (sendButton.getSelectedTransport().isSms()) {
      Media  media  = new Media(uri, MediaUtil.IMAGE_WEBP, System.currentTimeMillis(), StickerSlide.WIDTH, StickerSlide.HEIGHT, size, 0, Optional.absent(), Optional.absent(), Optional.absent());
      Intent intent = MediaSendActivity.buildEditorIntent(this, Collections.singletonList(media), recipient.get(), composeText.getTextTrimmed(), sendButton.getSelectedTransport());
      startActivityForResult(intent, MEDIA_SENDER);
      return;
    }

    long            expiresIn      = recipient.get().getExpireMessages() * 1000L;
    int             subscriptionId = sendButton.getSelectedTransport().getSimSubscriptionId().or(-1);
    boolean         initiating     = threadId == -1;
    TransportOption transport      = sendButton.getSelectedTransport();
    SlideDeck       slideDeck      = new SlideDeck();
    Slide           stickerSlide   = new StickerSlide(this, uri, size, stickerLocator);

    slideDeck.addSlide(stickerSlide);

    sendMediaMessage(transport.isSms(), "", slideDeck, null, Collections.emptyList(), Collections.emptyList(), expiresIn, false, subscriptionId, initiating, clearCompose);
  }

  private void silentlySetComposeText(String text) {
    typingTextWatcher.setEnabled(false);
    composeText.setText(text);
    typingTextWatcher.setEnabled(true);
  }

  // Listeners

  private class QuickCameraToggleListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      Permissions.with(ConversationActivity.this)
                 .request(Manifest.permission.CAMERA)
                 .ifNecessary()
                 .withRationaleDialog(getString(R.string.ConversationActivity_to_capture_photos_and_video_allow_signal_access_to_the_camera), R.drawable.ic_camera_solid_24)
                 .withPermanentDenialDialog(getString(R.string.ConversationActivity_signal_needs_the_camera_permission_to_take_photos_or_video))
                 .onAllGranted(() -> {
                   composeText.clearFocus();
                   startActivityForResult(MediaSendActivity.buildCameraIntent(ConversationActivity.this, recipient.get(), sendButton.getSelectedTransport()), MEDIA_SENDER);
                   overridePendingTransition(R.anim.camera_slide_from_bottom, R.anim.stationary);
                 })
                 .onAnyDenied(() -> Toast.makeText(ConversationActivity.this, R.string.ConversationActivity_signal_needs_camera_permissions_to_take_photos_or_video, Toast.LENGTH_LONG).show())
                 .execute();
    }
  }

  private class SendButtonListener implements OnClickListener, TextView.OnEditorActionListener {
    @Override
    public void onClick(View v) {
      sendMessage();
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
      if (actionId == EditorInfo.IME_ACTION_SEND) {
        sendButton.performClick();
        return true;
      }
      return false;
    }
  }

  private class AttachButtonListener implements OnClickListener {
    @Override
    public void onClick(View v) {
      handleAddAttachment();
    }
  }

  private class AttachButtonLongClickListener implements View.OnLongClickListener {
    @Override
    public boolean onLongClick(View v) {
      return sendButton.performLongClick();
    }
  }

  private class ComposeKeyPressedListener implements OnKeyListener, OnClickListener, TextWatcher, OnFocusChangeListener {

    int beforeLength;

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
      if (event.getAction() == KeyEvent.ACTION_DOWN) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
          if (TextSecurePreferences.isEnterSendsEnabled(ConversationActivity.this)) {
            sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
            sendButton.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER));
            return true;
          }
        }
      }
      return false;
    }

    @Override
    public void onClick(View v) {
      container.showSoftkey(composeText);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,int after) {
      beforeLength = composeText.getTextTrimmed().length();
    }

    @Override
    public void afterTextChanged(Editable s) {
      calculateCharactersRemaining();

      if (composeText.getTextTrimmed().length() == 0 || beforeLength == 0) {
        composeText.postDelayed(ConversationActivity.this::updateToggleButtonState, 50);
      }

      stickerViewModel.onInputTextUpdated(s.toString());
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before,int count) {}

    @Override
    public void onFocusChange(View v, boolean hasFocus) {}
  }

  private class TypingStatusTextWatcher extends SimpleTextWatcher {

    private boolean enabled = true;

    @Override
    public void onTextChanged(String text) {
      if (enabled && threadId > 0 && isSecureText && !isSmsForced()) {
        ApplicationContext.getInstance(ConversationActivity.this).getTypingStatusSender().onTypingStarted(threadId);
      }
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  @Override
  public void onMessageRequest(@NonNull MessageRequestViewModel viewModel) {
    messageRequestBottomView.setAcceptOnClickListener(v -> viewModel.onAccept());
    messageRequestBottomView.setDeleteOnClickListener(v -> onMessageRequestDeleteClicked(viewModel));
    messageRequestBottomView.setBlockOnClickListener(v -> onMessageRequestBlockClicked(viewModel));
    messageRequestBottomView.setUnblockOnClickListener(v -> onMessageRequestUnblockClicked(viewModel));

    viewModel.getRecipient().observe(this, this::presentMessageRequestBottomViewTo);
    viewModel.getMessageRequestDisplayState().observe(this, this::presentMessageRequestDisplayState);
    viewModel.getMessageRequestStatus().observe(this, status -> {
      switch (status) {
        case ACCEPTED:
          messageRequestBottomView.setVisibility(View.GONE);
          return;
        case DELETED:
        case BLOCKED:
          finish();
      }
    });
  }

  @Override
  public void handleReaction(@NonNull View maskTarget,
                             @NonNull MessageRecord messageRecord,
                             @NonNull Toolbar.OnMenuItemClickListener toolbarListener,
                             @NonNull ConversationReactionOverlay.OnHideListener onHideListener)
  {
    reactionOverlay.setOnToolbarItemClickedListener(toolbarListener);
    reactionOverlay.setOnHideListener(onHideListener);
    reactionOverlay.show(this, maskTarget, messageRecord, panelParent.getMeasuredHeight());
  }

  @Override
  public void onListVerticalTranslationChanged(float translationY) {
    reactionOverlay.setListVerticalTranslation(translationY);
  }

  @Override
  public void onCursorChanged() {
    if (!reactionOverlay.isShowing()) {
      return;
    }

    SimpleTask.run(() -> {
          //noinspection CodeBlock2Expr
          return DatabaseFactory.getMmsSmsDatabase(this)
                                .checkMessageExists(reactionOverlay.getMessageRecord());
        }, messageExists -> {
          if (!messageExists) {
            reactionOverlay.hide();
          }
        });
  }

  @Override
  public void setThreadId(long threadId) {
    this.threadId = threadId;
  }

  @Override
  public void handleReplyMessage(MessageRecord messageRecord) {
    Recipient author;

    if (messageRecord.isOutgoing()) {
      author = Recipient.self();
    } else {
      author = messageRecord.getIndividualRecipient();
    }

    if (messageRecord.isMms() && !((MmsMessageRecord) messageRecord).getSharedContacts().isEmpty()) {
      Contact   contact     = ((MmsMessageRecord) messageRecord).getSharedContacts().get(0);
      String    displayName = ContactUtil.getDisplayName(contact);
      String    body        = getString(R.string.ConversationActivity_quoted_contact_message, EmojiStrings.BUST_IN_SILHOUETTE, displayName);
      SlideDeck slideDeck   = new SlideDeck();

      if (contact.getAvatarAttachment() != null) {
        slideDeck.addSlide(MediaUtil.getSlideForAttachment(this, contact.getAvatarAttachment()));
      }

      inputPanel.setQuote(GlideApp.with(this),
                          messageRecord.getDateSent(),
                          author,
                          body,
                          slideDeck);

    } else if (messageRecord.isMms() && !((MmsMessageRecord) messageRecord).getLinkPreviews().isEmpty()) {
      LinkPreview linkPreview = ((MmsMessageRecord) messageRecord).getLinkPreviews().get(0);
      SlideDeck   slideDeck   = new SlideDeck();

      if (linkPreview.getThumbnail().isPresent()) {
        slideDeck.addSlide(MediaUtil.getSlideForAttachment(this, linkPreview.getThumbnail().get()));
      }

      inputPanel.setQuote(GlideApp.with(this),
                          messageRecord.getDateSent(),
                          author,
                          messageRecord.getBody(),
                          slideDeck);
    } else {
      SlideDeck slideDeck = messageRecord.isMms() ? ((MmsMessageRecord) messageRecord).getSlideDeck() : new SlideDeck();

      if (messageRecord.isMms() && ((MmsMessageRecord) messageRecord).isViewOnce()) {
        Attachment attachment = new TombstoneAttachment(MediaUtil.VIEW_ONCE, true);
        slideDeck = new SlideDeck();
        slideDeck.addSlide(MediaUtil.getSlideForAttachment(this, attachment));
      }

      inputPanel.setQuote(GlideApp.with(this),
                          messageRecord.getDateSent(),
                          author,
                          messageRecord.getBody(),
                          slideDeck);
    }

    inputPanel.clickOnComposeInput();
  }

  @Override
  public void onMessageActionToolbarOpened() {
    searchViewItem.collapseActionView();
  }

  @Override
  public void onForwardClicked()  {
    inputPanel.clearQuote();
  }

  @Override
  public void onAttachmentChanged() {
    handleSecurityChange(isSecureText, isDefaultSms);
    updateToggleButtonState();
    updateLinkPreviewState();
  }

  private void onMessageRequestDeleteClicked(@NonNull MessageRequestViewModel requestModel) {
    Recipient recipient = requestModel.getRecipient().getValue();
    if (recipient == null) {
      Log.w(TAG, "[onMessageRequestDeleteClicked] No recipient!");
      return;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                                 .setNeutralButton(R.string.ConversationActivity_cancel, (d, w) -> d.dismiss());

    if (recipient.isGroup() && recipient.isBlocked()) {
      builder.setTitle(R.string.ConversationActivity_delete_conversation);
      builder.setMessage(R.string.ConversationActivity_this_conversation_will_be_deleted_from_all_of_your_devices);
      builder.setPositiveButton(R.string.ConversationActivity_delete, (d, w) -> requestModel.onDelete());
    } else if (recipient.isGroup()) {
      builder.setTitle(R.string.ConversationActivity_delete_and_leave_group);
      builder.setMessage(R.string.ConversationActivity_you_will_leave_this_group_and_it_will_be_deleted_from_all_of_your_devices);
      builder.setNegativeButton(R.string.ConversationActivity_delete_and_leave, (d, w) -> requestModel.onDelete());
    } else {
      builder.setTitle(R.string.ConversationActivity_delete_conversation);
      builder.setMessage(R.string.ConversationActivity_this_conversation_will_be_deleted_from_all_of_your_devices);
      builder.setNegativeButton(R.string.ConversationActivity_delete, (d, w) -> requestModel.onDelete());
    }

    builder.show();
  }

  private void onMessageRequestBlockClicked(@NonNull MessageRequestViewModel requestModel) {
    Recipient recipient = requestModel.getRecipient().getValue();
    if (recipient == null) {
      Log.w(TAG, "[onMessageRequestBlockClicked] No recipient!");
      return;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                                 .setNeutralButton(R.string.ConversationActivity_cancel, (d, w) -> d.dismiss())
                                                 .setPositiveButton(R.string.ConversationActivity_block_and_delete, (d, w) -> requestModel.onBlockAndDelete())
                                                 .setNegativeButton(R.string.ConversationActivity_block, (d, w) -> requestModel.onBlock());

    if (recipient.isGroup()) {
      builder.setTitle(getString(R.string.ConversationActivity_block_and_leave_s, recipient.getDisplayName(this)));
      builder.setMessage(R.string.ConversationActivity_you_will_leave_this_group_and_no_longer_receive_messages_or_updates);
    } else {
      builder.setTitle(getString(R.string.ConversationActivity_block_s, recipient.getDisplayName(this)));
      builder.setMessage(R.string.ConversationActivity_blocked_people_will_not_be_able_to_call_you_or_send_you_messages);
    }

    builder.show();
  }

  private void onMessageRequestUnblockClicked(@NonNull MessageRequestViewModel requestModel) {
    Recipient recipient = requestModel.getRecipient().getValue();
    if (recipient == null) {
      Log.w(TAG, "[onMessageRequestUnblockClicked] No recipient!");
      return;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                                                 .setTitle(getString(R.string.ConversationActivity_unblock_s, recipient.getDisplayName(this)))
                                                 .setNeutralButton(R.string.ConversationActivity_cancel, (d, w) -> d.dismiss())
                                                 .setNegativeButton(R.string.ConversationActivity_unblock, (d, w) -> requestModel.onUnblock());

    if (recipient.isGroup()) {
      builder.setMessage(R.string.ConversationActivity_group_members_will_be_able_to_add_you_to_this_group_again);
    } else {
      builder.setMessage(R.string.ConversationActivity_you_will_be_able_to_message_and_call_each_other);
    }

    builder.show();
  }

  private void presentMessageRequestDisplayState(@NonNull MessageRequestViewModel.DisplayState displayState) {
    if (getIntent().hasExtra(TEXT_EXTRA) || getIntent().hasExtra(MEDIA_EXTRA) || getIntent().hasExtra(STICKER_EXTRA) || (isPushGroupConversation() && !isActiveGroup())) {
      Log.d(TAG, "[presentMessageRequestDisplayState] Have extra, so ignoring provided state.");
      messageRequestBottomView.setVisibility(View.GONE);
    } else {
      Log.d(TAG, "[presentMessageRequestDisplayState] " + displayState);
      switch (displayState) {
        case DISPLAY_MESSAGE_REQUEST:
          messageRequestBottomView.setVisibility(View.VISIBLE);
          if (groupShareProfileView.resolved()) {
            groupShareProfileView.get().setVisibility(View.GONE);
          }
          break;
        case DISPLAY_LEGACY:
          if (recipient.get().isGroup()) {
            groupShareProfileView.get().setRecipient(recipient.get());
            groupShareProfileView.get().setVisibility(View.VISIBLE);
          }
          messageRequestBottomView.setVisibility(View.GONE);
          break;
        case DISPLAY_NONE:
          messageRequestBottomView.setVisibility(View.GONE);
          if (groupShareProfileView.resolved()) {
            groupShareProfileView.get().setVisibility(View.GONE);
          }
          break;
      }
    }

    invalidateOptionsMenu();
  }

  private class UnverifiedDismissedListener implements UnverifiedBannerView.DismissListener {
    @Override
    public void onDismissed(final List<IdentityRecord> unverifiedIdentities) {
      final IdentityDatabase identityDatabase = DatabaseFactory.getIdentityDatabase(ConversationActivity.this);

      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          synchronized (SESSION_LOCK) {
            for (IdentityRecord identityRecord : unverifiedIdentities) {
              identityDatabase.setVerified(identityRecord.getRecipientId(),
                                           identityRecord.getIdentityKey(),
                                           VerifiedStatus.DEFAULT);
            }
          }

          return null;
        }

        @Override
        protected void onPostExecute(Void result) {
          initializeIdentityRecords();
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  private class UnverifiedClickedListener implements UnverifiedBannerView.ClickListener {
    @Override
    public void onClicked(final List<IdentityRecord> unverifiedIdentities) {
      Log.i(TAG, "onClicked: " + unverifiedIdentities.size());
      if (unverifiedIdentities.size() == 1) {
        Intent intent = new Intent(ConversationActivity.this, VerifyIdentityActivity.class);
        intent.putExtra(VerifyIdentityActivity.RECIPIENT_EXTRA, unverifiedIdentities.get(0).getRecipientId());
        intent.putExtra(VerifyIdentityActivity.IDENTITY_EXTRA, new IdentityKeyParcelable(unverifiedIdentities.get(0).getIdentityKey()));
        intent.putExtra(VerifyIdentityActivity.VERIFIED_EXTRA, false);

        startActivity(intent);
      } else {
        String[] unverifiedNames = new String[unverifiedIdentities.size()];

        for (int i=0;i<unverifiedIdentities.size();i++) {
          unverifiedNames[i] = Recipient.resolved(unverifiedIdentities.get(i).getRecipientId()).toShortString(ConversationActivity.this);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(ConversationActivity.this);
        builder.setIconAttribute(R.attr.dialog_alert_icon);
        builder.setTitle("No longer verified");
        builder.setItems(unverifiedNames, (dialog, which) -> {
          Intent intent = new Intent(ConversationActivity.this, VerifyIdentityActivity.class);
          intent.putExtra(VerifyIdentityActivity.RECIPIENT_EXTRA, unverifiedIdentities.get(which).getRecipientId());
          intent.putExtra(VerifyIdentityActivity.IDENTITY_EXTRA, new IdentityKeyParcelable(unverifiedIdentities.get(which).getIdentityKey()));
          intent.putExtra(VerifyIdentityActivity.VERIFIED_EXTRA, false);

          startActivity(intent);
        });
        builder.show();
      }
    }
  }

  private class QuoteRestorationTask extends AsyncTask<Void, Void, MessageRecord> {

    private final String                  serialized;
    private final SettableFuture<Boolean> future;

    QuoteRestorationTask(@NonNull String serialized, @NonNull SettableFuture<Boolean> future) {
      this.serialized = serialized;
      this.future     = future;
    }

    @Override
    protected MessageRecord doInBackground(Void... voids) {
      QuoteId quoteId = QuoteId.deserialize(ConversationActivity.this, serialized);

      if (quoteId != null) {
        return DatabaseFactory.getMmsSmsDatabase(getApplicationContext()).getMessageFor(quoteId.getId(), quoteId.getAuthor());
      }

      return null;
    }

    @Override
    protected void onPostExecute(MessageRecord messageRecord) {
      if (messageRecord != null) {
        handleReplyMessage(messageRecord);
        future.set(true);
      } else {
        Log.e(TAG, "Failed to restore a quote from a draft. No matching message record.");
        future.set(false);
      }
    }
  }

  private void presentMessageRequestBottomViewTo(@Nullable Recipient recipient) {
    if (recipient == null) return;

    messageRequestBottomView.setRecipient(recipient);
  }
}
