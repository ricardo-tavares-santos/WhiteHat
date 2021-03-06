package com.RWdesenv.What.groups;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.protobuf.ByteString;

import com.RWdesenv.What.attachments.Attachment;
import com.RWdesenv.What.attachments.UriAttachment;
import com.RWdesenv.What.database.AttachmentDatabase;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.database.GroupDatabase;
import com.RWdesenv.What.database.ThreadDatabase;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.groups.GroupManager.GroupActionResult;
import com.RWdesenv.What.jobs.LeaveGroupJob;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.mms.MmsException;
import com.RWdesenv.What.mms.OutgoingGroupMediaMessage;
import com.RWdesenv.What.profiles.AvatarHelper;
import com.RWdesenv.What.providers.BlobProvider;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientId;
import com.RWdesenv.What.recipients.RecipientUtil;
import com.RWdesenv.What.sms.MessageSender;
import com.RWdesenv.What.util.BitmapUtil;
import com.RWdesenv.What.util.GroupUtil;
import com.RWdesenv.What.util.MediaUtil;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.util.InvalidNumberException;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.GroupContext;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

final class V1GroupManager {

  private static final String TAG = Log.tag(V1GroupManager.class);

  static @NonNull GroupActionResult createGroup(@NonNull Context          context,
                                                @NonNull Set<RecipientId> memberIds,
                                                @Nullable Bitmap          avatar,
                                                @Nullable String          name,
                                                          boolean         mms)
  {
    final byte[]        avatarBytes      = BitmapUtil.toByteArray(avatar);
    final GroupDatabase groupDatabase    = DatabaseFactory.getGroupDatabase(context);
    final SecureRandom  secureRandom     = new SecureRandom();
    final GroupId       groupId          = mms ? GroupId.createMms(secureRandom) : GroupId.createV1(secureRandom);
    final RecipientId   groupRecipientId = DatabaseFactory.getRecipientDatabase(context).getOrInsertFromGroupId(groupId);
    final Recipient     groupRecipient   = Recipient.resolved(groupRecipientId);

    memberIds.add(Recipient.self().getId());

    if (groupId.isV1()) {
      GroupId.V1 groupIdV1 = groupId.requireV1();

      groupDatabase.create(groupIdV1, name, memberIds, null, null);

      try {
        AvatarHelper.setAvatar(context, groupRecipientId, avatarBytes != null ? new ByteArrayInputStream(avatarBytes) : null);
      } catch (IOException e) {
        Log.w(TAG, "Failed to save avatar!", e);
      }
      groupDatabase.onAvatarUpdated(groupIdV1, avatarBytes != null);
      DatabaseFactory.getRecipientDatabase(context).setProfileSharing(groupRecipient.getId(), true);
      return sendGroupUpdate(context, groupIdV1, memberIds, name, avatarBytes);
    } else {
      groupDatabase.create(groupId.requireMms(), memberIds);
      long threadId = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(groupRecipient, ThreadDatabase.DistributionTypes.CONVERSATION);
      return new GroupActionResult(groupRecipient, threadId);
    }
  }

  static GroupActionResult updateGroup(@NonNull  Context          context,
                                       @NonNull  GroupId          groupId,
                                       @NonNull  Set<RecipientId> memberAddresses,
                                       @Nullable Bitmap           avatar,
                                       @Nullable String           name)
      throws InvalidNumberException
  {
    final GroupDatabase groupDatabase    = DatabaseFactory.getGroupDatabase(context);
    final RecipientId   groupRecipientId = DatabaseFactory.getRecipientDatabase(context).getOrInsertFromGroupId(groupId);
    final byte[]        avatarBytes      = BitmapUtil.toByteArray(avatar);

    memberAddresses.add(Recipient.self().getId());
    groupDatabase.updateMembers(groupId, new LinkedList<>(memberAddresses));

    if (groupId.isPush()) {
      GroupId.V1 groupIdV1 = groupId.requireV1();

      groupDatabase.updateTitle(groupIdV1, name);
      groupDatabase.onAvatarUpdated(groupIdV1, avatarBytes != null);

      try {
        AvatarHelper.setAvatar(context, groupRecipientId, avatarBytes != null ? new ByteArrayInputStream(avatarBytes) : null);
      } catch (IOException e) {
        Log.w(TAG, "Failed to save avatar!", e);
      }
      return sendGroupUpdate(context, groupIdV1, memberAddresses, name, avatarBytes);
    } else {
      Recipient   groupRecipient   = Recipient.resolved(groupRecipientId);
      long        threadId         = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(groupRecipient);
      return new GroupActionResult(groupRecipient, threadId);
    }
  }

  private static GroupActionResult sendGroupUpdate(@NonNull  Context          context,
                                                   @NonNull  GroupId.V1       groupId,
                                                   @NonNull  Set<RecipientId> members,
                                                   @Nullable String           groupName,
                                                   @Nullable byte[]           avatar)
  {
    Attachment  avatarAttachment = null;
    RecipientId groupRecipientId = DatabaseFactory.getRecipientDatabase(context).getOrInsertFromGroupId(groupId);
    Recipient   groupRecipient   = Recipient.resolved(groupRecipientId);

    List<GroupContext.Member> uuidMembers = new LinkedList<>();
    List<String>              e164Members = new LinkedList<>();

    for (RecipientId member : members) {
      Recipient recipient = Recipient.resolved(member);
      uuidMembers.add(GroupV1MessageProcessor.createMember(RecipientUtil.toSignalServiceAddress(context, recipient)));
    }

    GroupContext.Builder groupContextBuilder = GroupContext.newBuilder()
                                                           .setId(ByteString.copyFrom(groupId.getDecodedId()))
                                                           .setType(GroupContext.Type.UPDATE)
                                                           .addAllMembersE164(e164Members)
                                                           .addAllMembers(uuidMembers);
    if (groupName != null) groupContextBuilder.setName(groupName);
    GroupContext groupContext = groupContextBuilder.build();

    if (avatar != null) {
      Uri avatarUri = BlobProvider.getInstance().forData(avatar).createForSingleUseInMemory();
      avatarAttachment = new UriAttachment(avatarUri, MediaUtil.IMAGE_PNG, AttachmentDatabase.TRANSFER_PROGRESS_DONE, avatar.length, null, false, false, null, null, null, null);
    }

    OutgoingGroupMediaMessage outgoingMessage = new OutgoingGroupMediaMessage(groupRecipient, groupContext, avatarAttachment, System.currentTimeMillis(), 0, false, null, Collections.emptyList(), Collections.emptyList());
    long                      threadId        = MessageSender.send(context, outgoingMessage, -1, false, null);

    return new GroupActionResult(groupRecipient, threadId);
  }

  @WorkerThread
  static boolean leaveGroup(@NonNull Context context, @NonNull GroupId.V1 groupId, @NonNull Recipient groupRecipient) {
    long                                threadId     = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(groupRecipient);
    Optional<OutgoingGroupMediaMessage> leaveMessage = GroupUtil.createGroupLeaveMessage(context, groupRecipient);

    if (threadId != -1 && leaveMessage.isPresent()) {
      try {
        long id = DatabaseFactory.getMmsDatabase(context).insertMessageOutbox(leaveMessage.get(), threadId, false, null);
        DatabaseFactory.getMmsDatabase(context).markAsSent(id, true);
      } catch (MmsException e) {
        Log.w(TAG, "Failed to insert leave message.", e);
      }
      ApplicationDependencies.getJobManager().add(LeaveGroupJob.create(groupRecipient));

      GroupDatabase groupDatabase = DatabaseFactory.getGroupDatabase(context);
      groupDatabase.setActive(groupId, false);
      groupDatabase.remove(groupId, Recipient.self().getId());
      return true;
    } else {
      return false;
    }
  }
}
