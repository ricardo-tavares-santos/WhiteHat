package com.RWdesenv.What.jobs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import com.RWdesenv.What.ApplicationContext;
import com.RWdesenv.What.attachments.Attachment;
import com.RWdesenv.What.crypto.UnidentifiedAccessUtil;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.database.GroupDatabase;
import com.RWdesenv.What.database.GroupReceiptDatabase.GroupReceiptInfo;
import com.RWdesenv.What.database.MmsDatabase;
import com.RWdesenv.What.database.NoSuchMessageException;
import com.RWdesenv.What.database.documents.IdentityKeyMismatch;
import com.RWdesenv.What.database.documents.NetworkFailure;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.groups.GroupId;
import com.RWdesenv.What.jobmanager.Data;
import com.RWdesenv.What.jobmanager.Job;
import com.RWdesenv.What.jobmanager.JobManager;
import com.RWdesenv.What.jobmanager.impl.NetworkConstraint;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.mms.MmsException;
import com.RWdesenv.What.mms.OutgoingGroupMediaMessage;
import com.RWdesenv.What.mms.OutgoingMediaMessage;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientId;
import com.RWdesenv.What.recipients.RecipientUtil;
import com.RWdesenv.What.transport.RetryLaterException;
import com.RWdesenv.What.transport.UndeliverableMessageException;
import org.whispersystems.libsignal.util.guava.Optional;
import org.whispersystems.signalservice.api.SignalServiceMessageSender;
import org.whispersystems.signalservice.api.crypto.UnidentifiedAccessPair;
import org.whispersystems.signalservice.api.crypto.UntrustedIdentityException;
import org.whispersystems.signalservice.api.messages.SendMessageResult;
import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage.Preview;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage.Quote;
import org.whispersystems.signalservice.api.messages.SignalServiceGroup;
import org.whispersystems.signalservice.api.messages.shared.SharedContact;
import org.whispersystems.signalservice.api.push.SignalServiceAddress;
import org.whispersystems.signalservice.api.util.UuidUtil;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.GroupContext;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PushGroupSendJob extends PushSendJob {

  public static final String KEY = "PushGroupSendJob";

  private static final String TAG = PushGroupSendJob.class.getSimpleName();

  private static final String KEY_MESSAGE_ID       = "message_id";
  private static final String KEY_FILTER_RECIPIENT = "filter_recipient";

  private long        messageId;
  private RecipientId filterRecipient;

  public PushGroupSendJob(long messageId, @NonNull RecipientId destination, @Nullable RecipientId filterRecipient) {
    this(new Job.Parameters.Builder()
                           .setQueue(destination.toQueueKey())
                           .addConstraint(NetworkConstraint.KEY)
                           .setLifespan(TimeUnit.DAYS.toMillis(1))
                           .setMaxAttempts(Parameters.UNLIMITED)
                           .build(),
         messageId, filterRecipient);

  }

  private PushGroupSendJob(@NonNull Job.Parameters parameters, long messageId, @Nullable RecipientId filterRecipient) {
    super(parameters);

    this.messageId       = messageId;
    this.filterRecipient = filterRecipient;
  }

  @WorkerThread
  public static void enqueue(@NonNull Context context,
                             @NonNull JobManager jobManager,
                             long messageId,
                             @NonNull RecipientId destination,
                             @Nullable RecipientId filterAddress)
  {
    try {
      Recipient group = Recipient.resolved(destination);
      if (!group.isPushGroup()) {
        throw new AssertionError("Not a group!");
      }

      if (!DatabaseFactory.getGroupDatabase(context).isActive(group.requireGroupId())) {
        throw new MmsException("Inactive group!");
      }

      MmsDatabase          database                    = DatabaseFactory.getMmsDatabase(context);
      OutgoingMediaMessage message                     = database.getOutgoingMessage(messageId);
      JobManager.Chain     compressAndUploadAttachment = createCompressingAndUploadAttachmentsChain(jobManager, message);

      compressAndUploadAttachment.then(new PushGroupSendJob(messageId, destination, filterAddress))
                                 .enqueue();

    } catch (NoSuchMessageException | MmsException e) {
      Log.w(TAG, "Failed to enqueue message.", e);
      DatabaseFactory.getMmsDatabase(context).markAsSentFailed(messageId);
      notifyMediaMessageDeliveryFailed(context, messageId);
    }
  }

  @Override
  public @NonNull Data serialize() {
    return new Data.Builder().putLong(KEY_MESSAGE_ID, messageId)
                             .putString(KEY_FILTER_RECIPIENT, filterRecipient != null ? filterRecipient.serialize() : null)
                             .build();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onAdded() {
    DatabaseFactory.getMmsDatabase(context).markAsSending(messageId);
  }

  @Override
  public void onPushSend()
      throws IOException, MmsException, NoSuchMessageException,  RetryLaterException
  {
    MmsDatabase               database                   = DatabaseFactory.getMmsDatabase(context);
    OutgoingMediaMessage      message                    = database.getOutgoingMessage(messageId);
    List<NetworkFailure>      existingNetworkFailures    = message.getNetworkFailures();
    List<IdentityKeyMismatch> existingIdentityMismatches = message.getIdentityKeyMismatches();

    if (database.isSent(messageId)) {
      log(TAG, "Message " + messageId + " was already sent. Ignoring.");
      return;
    }

    if (!message.getRecipient().isPushGroup()) {
      throw new MmsException("Message recipient isn't a group!");
    }

    try {
      log(TAG, "Sending message: " + messageId);

      if (!message.getRecipient().resolve().isProfileSharing() && !database.isGroupQuitMessage(messageId)) {
        RecipientUtil.shareProfileIfFirstSecureMessage(context, message.getRecipient());
      }

      List<RecipientId> target;

      Recipient groupRecipient = message.getRecipient().fresh();

      if      (filterRecipient != null)            target = Collections.singletonList(Recipient.resolved(filterRecipient).getId());
      else if (!existingNetworkFailures.isEmpty()) target = Stream.of(existingNetworkFailures).map(nf -> nf.getRecipientId(context)).toList();
      else                                         target = getGroupMessageRecipients(groupRecipient.requireGroupId(), messageId);

      List<SendMessageResult>   results                  = deliver(message, groupRecipient, target);
      List<NetworkFailure>      networkFailures          = Stream.of(results).filter(SendMessageResult::isNetworkFailure).map(result -> new NetworkFailure(Recipient.externalPush(context, result.getAddress()).getId())).toList();
      List<IdentityKeyMismatch> identityMismatches       = Stream.of(results).filter(result -> result.getIdentityFailure() != null).map(result -> new IdentityKeyMismatch(Recipient.externalPush(context, result.getAddress()).getId(), result.getIdentityFailure().getIdentityKey())).toList();
      Set<RecipientId>          successIds               = Stream.of(results).filter(result -> result.getSuccess() != null).map(SendMessageResult::getAddress).map(a -> Recipient.externalPush(context, a).getId()).collect(Collectors.toSet());
      List<NetworkFailure>      resolvedNetworkFailures  = Stream.of(existingNetworkFailures).filter(failure -> successIds.contains(failure.getRecipientId(context))).toList();
      List<IdentityKeyMismatch> resolvedIdentityFailures = Stream.of(existingIdentityMismatches).filter(failure -> successIds.contains(failure.getRecipientId(context))).toList();
      List<SendMessageResult>   successes                = Stream.of(results).filter(result -> result.getSuccess() != null).toList();

      for (NetworkFailure resolvedFailure : resolvedNetworkFailures) {
        database.removeFailure(messageId, resolvedFailure);
        existingNetworkFailures.remove(resolvedFailure);
      }

      for (IdentityKeyMismatch resolvedIdentity : resolvedIdentityFailures) {
        database.removeMismatchedIdentity(messageId, resolvedIdentity.getRecipientId(context), resolvedIdentity.getIdentityKey());
        existingIdentityMismatches.remove(resolvedIdentity);
      }

      if (!networkFailures.isEmpty()) {
        database.addFailures(messageId, networkFailures);
      }

      for (IdentityKeyMismatch mismatch : identityMismatches) {
        database.addMismatchedIdentity(messageId, mismatch.getRecipientId(context), mismatch.getIdentityKey());
      }

      for (SendMessageResult success : successes) {
        DatabaseFactory.getGroupReceiptDatabase(context).setUnidentified(Recipient.externalPush(context, success.getAddress()).getId(),
                                                                         messageId,
                                                                         success.getSuccess().isUnidentified());
      }

      if (existingNetworkFailures.isEmpty() && networkFailures.isEmpty() && identityMismatches.isEmpty() && existingIdentityMismatches.isEmpty()) {
        database.markAsSent(messageId, true);

        markAttachmentsUploaded(messageId, message.getAttachments());

        if (message.getExpiresIn() > 0 && !message.isExpirationUpdate()) {
          database.markExpireStarted(messageId);
          ApplicationContext.getInstance(context)
                            .getExpiringMessageManager()
                            .scheduleDeletion(messageId, true, message.getExpiresIn());
        }

        if (message.isViewOnce()) {
          DatabaseFactory.getAttachmentDatabase(context).deleteAttachmentFilesForViewOnceMessage(messageId);
        }
      } else if (!networkFailures.isEmpty()) {
        throw new RetryLaterException();
      } else if (!identityMismatches.isEmpty()) {
        database.markAsSentFailed(messageId);
        notifyMediaMessageDeliveryFailed(context, messageId);
      }
    } catch (UntrustedIdentityException | UndeliverableMessageException e) {
      warn(TAG, e);
      database.markAsSentFailed(messageId);
      notifyMediaMessageDeliveryFailed(context, messageId);
    }
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception exception) {
    if (exception instanceof IOException)         return true;
    if (exception instanceof RetryLaterException) return true;
    return false;
  }

  @Override
  public void onFailure() {
    DatabaseFactory.getMmsDatabase(context).markAsSentFailed(messageId);
  }

  private List<SendMessageResult> deliver(OutgoingMediaMessage message, @NonNull Recipient groupRecipient, @NonNull List<RecipientId> destinations)
      throws IOException, UntrustedIdentityException, UndeliverableMessageException {
    rotateSenderCertificateIfNecessary();

    SignalServiceMessageSender                 messageSender      = ApplicationDependencies.getSignalServiceMessageSender();
    GroupId                                    groupId            = groupRecipient.requireGroupId();
    Optional<byte[]>                           profileKey         = getProfileKey(groupRecipient);
    Optional<Quote>                            quote              = getQuoteFor(message);
    Optional<SignalServiceDataMessage.Sticker> sticker            = getStickerFor(message);
    List<SharedContact>                        sharedContacts     = getSharedContactsFor(message);
    List<Preview>                              previews           = getPreviewsFor(message);
    List<SignalServiceAddress>                 addresses          = Stream.of(destinations).map(Recipient::resolved).map(this::getPushAddress).toList();
    List<Attachment>                           attachments        = Stream.of(message.getAttachments()).filterNot(Attachment::isSticker).toList();
    List<SignalServiceAttachment>              attachmentPointers = getAttachmentPointersFor(attachments);
    boolean                                    isRecipientUpdate  = destinations.size() != DatabaseFactory.getGroupReceiptDatabase(context).getGroupReceiptInfo(messageId).size();

    List<Optional<UnidentifiedAccessPair>> unidentifiedAccess = Stream.of(destinations)
                                                                      .map(Recipient::resolved)
                                                                      .map(recipient -> UnidentifiedAccessUtil.getAccessFor(context, recipient))
                                                                      .toList();

    if (message.isGroup()) {
      OutgoingGroupMediaMessage  groupMessage     = (OutgoingGroupMediaMessage) message;
      GroupContext               groupContext     = groupMessage.getGroupContext();
      SignalServiceAttachment    avatar           = attachmentPointers.isEmpty() ? null                         : attachmentPointers.get(0);
      SignalServiceGroup.Type    type             = groupMessage.isGroupQuit()   ? SignalServiceGroup.Type.QUIT : SignalServiceGroup.Type.UPDATE;
      List<SignalServiceAddress> members          = Stream.of(groupContext.getMembersList())
                                                          .map(m -> new SignalServiceAddress(UuidUtil.parseOrNull(m.getUuid()), m.getE164()))
                                                          .toList();
      SignalServiceGroup         group            = new SignalServiceGroup(type, groupId.getDecodedId(), groupContext.getName(), members, avatar);
      SignalServiceDataMessage   groupDataMessage = SignalServiceDataMessage.newBuilder()
                                                                            .withTimestamp(message.getSentTimeMillis())
                                                                            .withExpiration(groupRecipient.getExpireMessages())
                                                                            .asGroupMessage(group)
                                                                            .build();

      return messageSender.sendMessage(addresses, unidentifiedAccess, isRecipientUpdate, groupDataMessage);
    } else {
      SignalServiceGroup       group        = new SignalServiceGroup(groupId.getDecodedId());
      SignalServiceDataMessage groupMessage = SignalServiceDataMessage.newBuilder()
                                                                      .withTimestamp(message.getSentTimeMillis())
                                                                      .asGroupMessage(group)
                                                                      .withAttachments(attachmentPointers)
                                                                      .withBody(message.getBody())
                                                                      .withExpiration((int)(message.getExpiresIn() / 1000))
                                                                      .withViewOnce(message.isViewOnce())
                                                                      .asExpirationUpdate(message.isExpirationUpdate())
                                                                      .withProfileKey(profileKey.orNull())
                                                                      .withQuote(quote.orNull())
                                                                      .withSticker(sticker.orNull())
                                                                      .withSharedContacts(sharedContacts)
                                                                      .withPreviews(previews)
                                                                      .build();

      return messageSender.sendMessage(addresses, unidentifiedAccess, isRecipientUpdate, groupMessage);
    }
  }

  private @NonNull List<RecipientId> getGroupMessageRecipients(@NonNull GroupId groupId, long messageId) {
    List<GroupReceiptInfo> destinations = DatabaseFactory.getGroupReceiptDatabase(context).getGroupReceiptInfo(messageId);
    if (!destinations.isEmpty()) return Stream.of(destinations).map(GroupReceiptInfo::getRecipientId).toList();

    List<Recipient> members = DatabaseFactory.getGroupDatabase(context).getGroupMembers(groupId, GroupDatabase.MemberSet.FULL_MEMBERS_EXCLUDING_SELF);
    return Stream.of(members).map(Recipient::getId).toList();
  }

  public static class Factory implements Job.Factory<PushGroupSendJob> {
    @Override
    public @NonNull PushGroupSendJob create(@NonNull Parameters parameters, @NonNull com.RWdesenv.What.jobmanager.Data data) {
      String      raw    = data.getString(KEY_FILTER_RECIPIENT);
      RecipientId filter = raw != null ? RecipientId.from(raw) : null;

      return new PushGroupSendJob(parameters, data.getLong(KEY_MESSAGE_ID), filter);
    }
  }
}
