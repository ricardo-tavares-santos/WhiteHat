package com.RWdesenv.What.groups;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientId;
import org.whispersystems.signalservice.api.util.InvalidNumberException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class GroupManager {

  public static @NonNull GroupActionResult createGroup(@NonNull  Context        context,
                                                       @NonNull  Set<Recipient> members,
                                                       @Nullable Bitmap         avatar,
                                                       @Nullable String         name,
                                                                 boolean        mms)
  {
    Set<RecipientId> addresses = getMemberIds(members);

    return V1GroupManager.createGroup(context, addresses, avatar, name, mms);
  }

  public static GroupActionResult updateGroup(@NonNull  Context        context,
                                              @NonNull  GroupId        groupId,
                                              @NonNull  Set<Recipient> members,
                                              @Nullable Bitmap         avatar,
                                              @Nullable String         name)
      throws InvalidNumberException
  {
    Set<RecipientId> addresses = getMemberIds(members);

    return V1GroupManager.updateGroup(context, groupId, addresses, avatar, name);
  }

  private static Set<RecipientId> getMemberIds(Collection<Recipient> recipients) {
    final Set<RecipientId> results = new HashSet<>();
    for (Recipient recipient : recipients) {
      results.add(recipient.getId());
    }

    return results;
  }

  @WorkerThread
  public static boolean leaveGroup(@NonNull Context context, @NonNull Recipient groupRecipient) {
    GroupId groupId = groupRecipient.requireGroupId();
    
    return V1GroupManager.leaveGroup(context, groupId.requireV1(), groupRecipient);
  }

  public static class GroupActionResult {
    private final Recipient groupRecipient;
    private final long      threadId;

    public GroupActionResult(Recipient groupRecipient, long threadId) {
      this.groupRecipient = groupRecipient;
      this.threadId       = threadId;
    }

    public Recipient getGroupRecipient() {
      return groupRecipient;
    }

    public long getThreadId() {
      return threadId;
    }
  }
}
