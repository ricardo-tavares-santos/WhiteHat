package com.RWdesenv.What.mms;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.RWdesenv.What.attachments.Attachment;
import com.RWdesenv.What.contactshare.Contact;
import com.RWdesenv.What.database.ThreadDatabase;
import com.RWdesenv.What.linkpreview.LinkPreview;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.util.Base64;
import org.whispersystems.signalservice.internal.push.SignalServiceProtos.GroupContext;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class OutgoingGroupMediaMessage extends OutgoingSecureMediaMessage {

  private final GroupContext group;

  public OutgoingGroupMediaMessage(@NonNull Recipient recipient,
                                   @NonNull String encodedGroupContext,
                                   @NonNull List<Attachment> avatar,
                                   long sentTimeMillis,
                                   long expiresIn,
                                   boolean viewOnce,
                                   @Nullable QuoteModel quote,
                                   @NonNull List<Contact> contacts,
                                   @NonNull List<LinkPreview> previews)
      throws IOException
  {
    super(recipient, encodedGroupContext, avatar, sentTimeMillis,
          ThreadDatabase.DistributionTypes.CONVERSATION, expiresIn, viewOnce, quote, contacts, previews);

    this.group = GroupContext.parseFrom(Base64.decode(encodedGroupContext));
  }

  public OutgoingGroupMediaMessage(@NonNull Recipient recipient,
                                   @NonNull GroupContext group,
                                   @Nullable final Attachment avatar,
                                   long sentTimeMillis,
                                   long expireIn,
                                   boolean viewOnce,
                                   @Nullable QuoteModel quote,
                                   @NonNull List<Contact> contacts,
                                   @NonNull List<LinkPreview> previews)
  {
    super(recipient, Base64.encodeBytes(group.toByteArray()),
          new LinkedList<Attachment>() {{if (avatar != null) add(avatar);}},
          System.currentTimeMillis(),
          ThreadDatabase.DistributionTypes.CONVERSATION, expireIn, viewOnce, quote, contacts, previews);

    this.group = group;
  }

  @Override
  public boolean isGroup() {
    return true;
  }

  public boolean isGroupUpdate() {
    return group.getType().getNumber() == GroupContext.Type.UPDATE_VALUE;
  }

  public boolean isGroupQuit() {
    return group.getType().getNumber() == GroupContext.Type.QUIT_VALUE;
  }

  public GroupContext getGroupContext() {
    return group;
  }
}
