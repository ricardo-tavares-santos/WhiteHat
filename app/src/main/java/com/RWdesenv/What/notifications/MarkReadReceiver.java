package com.RWdesenv.What.notifications;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import com.RWdesenv.What.ApplicationContext;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.database.MessagingDatabase.ExpirationInfo;
import com.RWdesenv.What.database.MessagingDatabase.MarkedMessageInfo;
import com.RWdesenv.What.database.MessagingDatabase.SyncMessageId;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobs.MultiDeviceReadUpdateJob;
import com.RWdesenv.What.jobs.SendReadReceiptJob;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.recipients.RecipientId;
import com.RWdesenv.What.service.ExpiringMessageManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MarkReadReceiver extends BroadcastReceiver {

  private static final String TAG                   = MarkReadReceiver.class.getSimpleName();
  public static final  String CLEAR_ACTION          = "com.RWdesenv.What.notifications.CLEAR";
  public static final  String THREAD_IDS_EXTRA      = "thread_ids";
  public static final  String NOTIFICATION_ID_EXTRA = "notification_id";

  @SuppressLint("StaticFieldLeak")
  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!CLEAR_ACTION.equals(intent.getAction()))
      return;

    final long[] threadIds = intent.getLongArrayExtra(THREAD_IDS_EXTRA);

    if (threadIds != null) {
      NotificationManagerCompat.from(context).cancel(intent.getIntExtra(NOTIFICATION_ID_EXTRA, -1));

      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          List<MarkedMessageInfo> messageIdsCollection = new LinkedList<>();

          for (long threadId : threadIds) {
            Log.i(TAG, "Marking as read: " + threadId);
            List<MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context).setRead(threadId, true);
            messageIdsCollection.addAll(messageIds);
          }

          process(context, messageIdsCollection);

          MessageNotifier.updateNotification(context);

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  public static void process(@NonNull Context context, @NonNull List<MarkedMessageInfo> markedReadMessages) {
    if (markedReadMessages.isEmpty()) return;

    List<SyncMessageId> syncMessageIds = new LinkedList<>();

    for (MarkedMessageInfo messageInfo : markedReadMessages) {
      scheduleDeletion(context, messageInfo.getExpirationInfo());
      syncMessageIds.add(messageInfo.getSyncMessageId());
    }

    ApplicationDependencies.getJobManager().add(new MultiDeviceReadUpdateJob(syncMessageIds));

    Map<Long, List<MarkedMessageInfo>> threadToInfo = Stream.of(markedReadMessages)
                                                            .collect(Collectors.groupingBy(MarkedMessageInfo::getThreadId));

    Stream.of(threadToInfo).forEach(threadToInfoEntry -> {
      Map<RecipientId, List<SyncMessageId>> idMapForThread = Stream.of(threadToInfoEntry.getValue())
                                                                   .map(MarkedMessageInfo::getSyncMessageId)
                                                                   .collect(Collectors.groupingBy(SyncMessageId::getRecipientId));

      Stream.of(idMapForThread).forEach(entry -> {
        List<Long> timestamps = Stream.of(entry.getValue()).map(SyncMessageId::getTimetamp).toList();

        ApplicationDependencies.getJobManager().add(new SendReadReceiptJob(threadToInfoEntry.getKey(), entry.getKey(), timestamps));
      });
    });
  }

  private static void scheduleDeletion(Context context, ExpirationInfo expirationInfo) {
    if (expirationInfo.getExpiresIn() > 0 && expirationInfo.getExpireStarted() <= 0) {
      ExpiringMessageManager expirationManager = ApplicationContext.getInstance(context).getExpiringMessageManager();

      if (expirationInfo.isMms()) DatabaseFactory.getMmsDatabase(context).markExpireStarted(expirationInfo.getId());
      else                        DatabaseFactory.getSmsDatabase(context).markExpireStarted(expirationInfo.getId());

      expirationManager.scheduleDeletion(expirationInfo.getId(), expirationInfo.isMms(), expirationInfo.getExpiresIn());
    }
  }
}
