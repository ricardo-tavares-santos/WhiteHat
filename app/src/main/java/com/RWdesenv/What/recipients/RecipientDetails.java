package com.RWdesenv.What.recipients;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.RWdesenv.What.color.MaterialColor;
import com.RWdesenv.What.database.IdentityDatabase.VerifiedStatus;
import com.RWdesenv.What.database.RecipientDatabase.InsightsBannerTier;
import com.RWdesenv.What.database.RecipientDatabase.RecipientSettings;
import com.RWdesenv.What.database.RecipientDatabase.RegisteredState;
import com.RWdesenv.What.database.RecipientDatabase.UnidentifiedAccessMode;
import com.RWdesenv.What.database.RecipientDatabase.VibrateState;
import com.RWdesenv.What.groups.GroupId;
import com.RWdesenv.What.profiles.ProfileName;
import com.RWdesenv.What.util.TextSecurePreferences;
import com.RWdesenv.What.util.Util;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class RecipientDetails {

  final UUID                   uuid;
  final String                 username;
  final String                 e164;
  final String                 email;
  final GroupId                groupId;
  final String                 name;
  final String                 customLabel;
  final Uri                    systemContactPhoto;
  final Uri                    contactUri;
  final Optional<Long>         groupAvatarId;
  final MaterialColor          color;
  final Uri                    messageRingtone;
  final Uri                    callRingtone;
  final long                   mutedUntil;
  final VibrateState           messageVibrateState;
  final VibrateState           callVibrateState;
  final boolean                blocked;
  final int                    expireMessages;
  final List<Recipient>        participants;
  final ProfileName            profileName;
  final Optional<Integer>      defaultSubscriptionId;
  final RegisteredState        registered;
  final byte[]                 profileKey;
  final byte[]                 profileKeyCredential;
  final String                 profileAvatar;
  final boolean                profileSharing;
  final boolean                systemContact;
  final boolean                isLocalNumber;
  final String                 notificationChannel;
  final UnidentifiedAccessMode unidentifiedAccessMode;
  final boolean                forceSmsSelection;
  final Recipient.Capability   uuidCapability;
  final Recipient.Capability   groupsV2Capability;
  final InsightsBannerTier     insightsBannerTier;
  final byte[] storageId;
  final byte[]                 identityKey;
  final VerifiedStatus         identityStatus;

  RecipientDetails(@NonNull Context context,
                   @Nullable String name,
                   @NonNull Optional<Long> groupAvatarId,
                   boolean systemContact,
                   boolean isLocalNumber,
                   @NonNull RecipientSettings settings,
                   @Nullable List<Recipient> participants)
  {
    this.groupAvatarId                   = groupAvatarId;
    this.systemContactPhoto              = Util.uri(settings.getSystemContactPhotoUri());
    this.customLabel                     = settings.getSystemPhoneLabel();
    this.contactUri                      = Util.uri(settings.getSystemContactUri());
    this.uuid                            = settings.getUuid();
    this.username                        = settings.getUsername();
    this.e164                            = settings.getE164();
    this.email                           = settings.getEmail();
    this.groupId                         = settings.getGroupId();
    this.color                           = settings.getColor();
    this.messageRingtone                 = settings.getMessageRingtone();
    this.callRingtone                    = settings.getCallRingtone();
    this.mutedUntil                      = settings.getMuteUntil();
    this.messageVibrateState             = settings.getMessageVibrateState();
    this.callVibrateState                = settings.getCallVibrateState();
    this.blocked                         = settings.isBlocked();
    this.expireMessages                  = settings.getExpireMessages();
    this.participants                    = participants == null ? new LinkedList<>() : participants;
    this.profileName                     = settings.getProfileName();
    this.defaultSubscriptionId           = settings.getDefaultSubscriptionId();
    this.registered                      = settings.getRegistered();
    this.profileKey                      = settings.getProfileKey();
    this.profileKeyCredential            = settings.getProfileKeyCredential();
    this.profileAvatar                   = settings.getProfileAvatar();
    this.profileSharing                  = settings.isProfileSharing();
    this.systemContact                   = systemContact;
    this.isLocalNumber                   = isLocalNumber;
    this.notificationChannel             = settings.getNotificationChannel();
    this.unidentifiedAccessMode          = settings.getUnidentifiedAccessMode();
    this.forceSmsSelection               = settings.isForceSmsSelection();
    this.uuidCapability                  = settings.getUuidCapability();
    this.groupsV2Capability              = settings.getGroupsV2Capability();
    this.insightsBannerTier              = settings.getInsightsBannerTier();
    this.storageId                       = settings.getStorageId();
    this.identityKey                     = settings.getIdentityKey();
    this.identityStatus                  = settings.getIdentityStatus();

    if (name == null) this.name = settings.getSystemDisplayName();
    else              this.name = name;
  }

  /**
   * Only used for {@link Recipient#UNKNOWN}.
   */
  RecipientDetails() {
    this.groupAvatarId          = null;
    this.systemContactPhoto     = null;
    this.customLabel            = null;
    this.contactUri             = null;
    this.uuid                   = null;
    this.username               = null;
    this.e164                   = null;
    this.email                  = null;
    this.groupId                = null;
    this.color                  = null;
    this.messageRingtone        = null;
    this.callRingtone           = null;
    this.mutedUntil             = 0;
    this.messageVibrateState    = VibrateState.DEFAULT;
    this.callVibrateState       = VibrateState.DEFAULT;
    this.blocked                = false;
    this.expireMessages         = 0;
    this.participants           = new LinkedList<>();
    this.profileName            = ProfileName.EMPTY;
    this.insightsBannerTier     = InsightsBannerTier.TIER_TWO;
    this.defaultSubscriptionId  = Optional.absent();
    this.registered             = RegisteredState.UNKNOWN;
    this.profileKey             = null;
    this.profileKeyCredential   = null;
    this.profileAvatar          = null;
    this.profileSharing         = false;
    this.systemContact          = true;
    this.isLocalNumber          = false;
    this.notificationChannel    = null;
    this.unidentifiedAccessMode = UnidentifiedAccessMode.UNKNOWN;
    this.forceSmsSelection      = false;
    this.name                   = null;
    this.uuidCapability         = Recipient.Capability.UNKNOWN;
    this.groupsV2Capability     = Recipient.Capability.UNKNOWN;
    this.storageId              = null;
    this.identityKey            = null;
    this.identityStatus         = VerifiedStatus.DEFAULT;
  }
}
