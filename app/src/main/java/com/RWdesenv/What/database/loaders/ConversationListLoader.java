package com.RWdesenv.What.database.loaders;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;

import com.RWdesenv.What.contacts.ContactAccessor;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.database.ThreadDatabase;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientId;
import com.RWdesenv.What.util.AbstractCursorLoader;

import java.util.LinkedList;
import java.util.List;

public class ConversationListLoader extends AbstractCursorLoader {

  private final String filter;
  private final boolean archived;

  public ConversationListLoader(Context context, String filter, boolean archived) {
    super(context);
    this.filter   = filter;
    this.archived = archived;
  }

  @Override
  public Cursor getCursor() {
    if      (filter != null && filter.trim().length() != 0) return getFilteredConversationList(filter);
    else if (!archived)                                     return getUnarchivedConversationList();
    else                                                    return getArchivedConversationList();
  }

  private Cursor getUnarchivedConversationList() {
    List<Cursor> cursorList = new LinkedList<>();
    cursorList.add(DatabaseFactory.getThreadDatabase(context).getConversationList());

    int archivedCount = DatabaseFactory.getThreadDatabase(context)
                                       .getArchivedConversationListCount();

    if (archivedCount > 0) {
      MatrixCursor switchToArchiveCursor = new MatrixCursor(new String[] {
          ThreadDatabase.ID, ThreadDatabase.DATE, ThreadDatabase.MESSAGE_COUNT,
          ThreadDatabase.RECIPIENT_ID, ThreadDatabase.SNIPPET, ThreadDatabase.READ, ThreadDatabase.UNREAD_COUNT,
          ThreadDatabase.TYPE, ThreadDatabase.SNIPPET_TYPE, ThreadDatabase.SNIPPET_URI,
          ThreadDatabase.SNIPPET_CONTENT_TYPE, ThreadDatabase.SNIPPET_EXTRAS,
          ThreadDatabase.ARCHIVED, ThreadDatabase.STATUS, ThreadDatabase.DELIVERY_RECEIPT_COUNT,
          ThreadDatabase.EXPIRES_IN, ThreadDatabase.LAST_SEEN, ThreadDatabase.READ_RECEIPT_COUNT}, 1);


      if (cursorList.get(0).getCount() <= 0) {
        switchToArchiveCursor.addRow(new Object[] {-1L, System.currentTimeMillis(), archivedCount,
                                                   "-1", null, 1, 0, ThreadDatabase.DistributionTypes.INBOX_ZERO,
                                                   0, null, null, null, 0, -1, 0, 0, 0, -1});
      }

      switchToArchiveCursor.addRow(new Object[] {-1L, System.currentTimeMillis(), archivedCount,
                                                 "-1", null, 1, 0, ThreadDatabase.DistributionTypes.ARCHIVE,
                                                 0, null, null, null, 0, -1, 0, 0, 0, -1});

      cursorList.add(switchToArchiveCursor);
    }

    return new MergeCursor(cursorList.toArray(new Cursor[0]));
  }

  private Cursor getArchivedConversationList() {
    return DatabaseFactory.getThreadDatabase(context).getArchivedConversationList();
  }

  private Cursor getFilteredConversationList(String filter) {
    List<String>      numbers      = ContactAccessor.getInstance().getNumbersForThreadSearchFilter(context, filter);
    List<RecipientId> recipientIds = new LinkedList<>();

    for (String number : numbers) {
      recipientIds.add(Recipient.external(context, number).getId());
    }

    return DatabaseFactory.getThreadDatabase(context).getFilteredConversationList(recipientIds);
  }
}
