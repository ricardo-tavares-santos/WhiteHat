package com.RWdesenv.What.profiles;

import android.content.Context;
import android.os.AsyncTask;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.FrameLayout;

import com.RWdesenv.What.R;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientExporter;
import com.RWdesenv.What.util.ViewUtil;

public class UnknownSenderView extends FrameLayout {

  private final @NonNull Recipient recipient;
  private final          long      threadId;

  public UnknownSenderView(@NonNull Context context, @NonNull Recipient recipient, long threadId) {
    super(context);
    this.recipient = recipient;
    this.threadId  = threadId;

    inflate(context, R.layout.unknown_sender_view, this);
    setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

    View block         = ViewUtil.findById(this, R.id.block);
    View add           = ViewUtil.findById(this, R.id.add_to_contacts);
    View profileAccess = ViewUtil.findById(this, R.id.share_profile);

    block.setOnClickListener(v -> handleBlock());
    add.setOnClickListener(v -> handleAdd());
    profileAccess.setOnClickListener(v -> handleProfileAccess());
  }

  private void handleBlock() {
    final Context context = getContext();

    new AlertDialog.Builder(getContext())
        .setIconAttribute(R.attr.dialog_alert_icon)
        .setTitle(getContext().getString(R.string.UnknownSenderView_block_s, recipient.toShortString(context)))
        .setMessage(R.string.UnknownSenderView_blocked_contacts_will_no_longer_be_able_to_send_you_messages_or_call_you)
        .setPositiveButton(R.string.UnknownSenderView_block, (dialog, which) -> {
          new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
              DatabaseFactory.getRecipientDatabase(context).setBlocked(recipient.getId(), true);
              if (threadId != -1) DatabaseFactory.getThreadDatabase(context).setHasSent(threadId, true);
              return null;
            }
          }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }

  private void handleAdd() {
    getContext().startActivity(RecipientExporter.export(recipient).asAddContactIntent());
    if (threadId != -1) DatabaseFactory.getThreadDatabase(getContext()).setHasSent(threadId, true);
  }

  private void handleProfileAccess() {
    final Context context = getContext();

    new AlertDialog.Builder(getContext())
        .setIconAttribute(R.attr.dialog_info_icon)
        .setTitle(getContext().getString(R.string.UnknownSenderView_share_profile_with_s, recipient.toShortString(context)))
        .setMessage(R.string.UnknownSenderView_the_easiest_way_to_share_your_profile_information_is_to_add_the_sender_to_your_contacts)
        .setPositiveButton(R.string.UnknownSenderView_share_profile, (dialog, which) -> {
          new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
              DatabaseFactory.getRecipientDatabase(context).setProfileSharing(recipient.getId(), true);
              if (threadId != -1) DatabaseFactory.getThreadDatabase(context).setHasSent(threadId, true);
              return null;
            }
          }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        })
        .setNegativeButton(android.R.string.cancel, null)
        .show();
  }
}
