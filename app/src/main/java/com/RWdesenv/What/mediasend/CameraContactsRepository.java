package com.RWdesenv.What.mediasend;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.RWdesenv.What.contacts.ContactRepository;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.database.GroupDatabase;
import com.RWdesenv.What.database.RecipientDatabase;
import com.RWdesenv.What.database.ThreadDatabase;
import com.RWdesenv.What.database.model.ThreadRecord;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientId;
import com.RWdesenv.What.util.concurrent.SignalExecutors;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles retrieving the data to be shown in {@link CameraContactSelectionFragment}.
 */
class CameraContactsRepository {

  private static final int RECENT_MAX = 25;

  private final Context           context;
  private final ThreadDatabase    threadDatabase;
  private final GroupDatabase     groupDatabase;
  private final RecipientDatabase recipientDatabase;
  private final ContactRepository contactRepository;

  CameraContactsRepository(@NonNull Context context) {
    this.context           = context.getApplicationContext();
    this.threadDatabase    = DatabaseFactory.getThreadDatabase(context);
    this.groupDatabase     = DatabaseFactory.getGroupDatabase(context);
    this.recipientDatabase = DatabaseFactory.getRecipientDatabase(context);
    this.contactRepository = new ContactRepository(context);
  }

  void getCameraContacts(@NonNull Callback<CameraContacts> callback) {
    getCameraContacts("", callback);
  }

  void getCameraContacts(@NonNull String query, @NonNull Callback<CameraContacts> callback) {
    SignalExecutors.BOUNDED.execute(() -> {
      List<Recipient> recents  = getRecents(query);
      List<Recipient> contacts = getContacts(query);
      List<Recipient> groups   = getGroups(query);

      callback.onComplete(new CameraContacts(recents, contacts, groups));
    });
  }


  @WorkerThread
  private @NonNull List<Recipient> getRecents(@NonNull String query) {
    if (!TextUtils.isEmpty(query)) {
      return Collections.emptyList();
    }

    List<Recipient> recipients = new ArrayList<>(RECENT_MAX);

    try (ThreadDatabase.Reader threadReader = threadDatabase.readerFor(threadDatabase.getRecentPushConversationList(RECENT_MAX, false))) {
      ThreadRecord threadRecord;
      while ((threadRecord = threadReader.getNext()) != null) {
        recipients.add(threadRecord.getRecipient().resolve());
      }
    }

    return recipients;
  }

  @WorkerThread
  private @NonNull List<Recipient> getContacts(@NonNull String query) {
    List<Recipient> recipients = new ArrayList<>();

    try (Cursor cursor = contactRepository.querySignalContacts(query)) {
      while (cursor.moveToNext()) {
        RecipientId id        = RecipientId.from(cursor.getLong(cursor.getColumnIndexOrThrow(ContactRepository.ID_COLUMN)));
        Recipient   recipient = Recipient.resolved(id);
        recipients.add(recipient);
      }
    }

    return recipients;
  }

  @WorkerThread
  private @NonNull List<Recipient> getGroups(@NonNull String query) {
    if (TextUtils.isEmpty(query)) {
      return Collections.emptyList();
    }

    List<Recipient> recipients = new ArrayList<>();

    try (GroupDatabase.Reader reader = groupDatabase.getGroupsFilteredByTitle(query, false)) {
      GroupDatabase.GroupRecord groupRecord;
      while ((groupRecord = reader.getNext()) != null) {
        RecipientId recipientId = recipientDatabase.getOrInsertFromGroupId(groupRecord.getId());
        recipients.add(Recipient.resolved(recipientId));
      }
    }

    return recipients;
  }

  interface Callback<E> {
    void onComplete(E result);
  }
}
