package com.RWdesenv.What.jobs;

import android.app.Activity;
import androidx.annotation.NonNull;
import android.telephony.SmsManager;

import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobmanager.Data;
import com.RWdesenv.What.jobmanager.Job;
import com.RWdesenv.What.logging.Log;

import com.RWdesenv.What.ApplicationContext;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.database.NoSuchMessageException;
import com.RWdesenv.What.database.SmsDatabase;
import com.RWdesenv.What.database.model.SmsMessageRecord;
import com.RWdesenv.What.notifications.MessageNotifier;
import com.RWdesenv.What.service.SmsDeliveryListener;

public class SmsSentJob extends BaseJob {

  public static final String KEY = "SmsSentJob";

  private static final String TAG = SmsSentJob.class.getSimpleName();

  private static final String KEY_MESSAGE_ID  = "message_id";
  private static final String KEY_ACTION      = "action";
  private static final String KEY_RESULT      = "result";
  private static final String KEY_RUN_ATTEMPT = "run_attempt";

  private long   messageId;
  private String action;
  private int    result;
  private int    runAttempt;

  public SmsSentJob(long messageId, String action, int result, int runAttempt) {
    this(new Job.Parameters.Builder().build(),
         messageId,
         action,
         result,
         runAttempt);
  }

  private SmsSentJob(@NonNull Job.Parameters parameters, long messageId, String action, int result, int runAttempt) {
    super(parameters);

    this.messageId  = messageId;
    this.action     = action;
    this.result     = result;
    this.runAttempt = runAttempt;
  }

  @Override
  public @NonNull Data serialize() {
    return new Data.Builder().putLong(KEY_MESSAGE_ID, messageId)
                             .putString(KEY_ACTION, action)
                             .putInt(KEY_RESULT, result)
                             .putInt(KEY_RUN_ATTEMPT, runAttempt)
                             .build();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() {
    Log.i(TAG, "Got SMS callback: " + action + " , " + result);

    switch (action) {
      case SmsDeliveryListener.SENT_SMS_ACTION:
        handleSentResult(messageId, result);
        break;
      case SmsDeliveryListener.DELIVERED_SMS_ACTION:
        handleDeliveredResult(messageId, result);
        break;
    }
  }

  @Override
  public boolean onShouldRetry(@NonNull Exception throwable) {
    return false;
  }

  @Override
  public void onFailure() {
  }

  private void handleDeliveredResult(long messageId, int result) {
    DatabaseFactory.getSmsDatabase(context).markStatus(messageId, result);
  }

  private void handleSentResult(long messageId, int result) {
    try {
      SmsDatabase      database = DatabaseFactory.getSmsDatabase(context);
      SmsMessageRecord record   = database.getMessage(messageId);

      switch (result) {
        case Activity.RESULT_OK:
          database.markAsSent(messageId, false);
          break;
        case SmsManager.RESULT_ERROR_NO_SERVICE:
        case SmsManager.RESULT_ERROR_RADIO_OFF:
          Log.w(TAG, "Service connectivity problem, requeuing...");
          ApplicationDependencies.getJobManager().add(new SmsSendJob(context, messageId, record.getIndividualRecipient(), runAttempt + 1));
          break;
        default:
          database.markAsSentFailed(messageId);
          MessageNotifier.notifyMessageDeliveryFailed(context, record.getRecipient(), record.getThreadId());
      }
    } catch (NoSuchMessageException e) {
      Log.w(TAG, e);
    }
  }

  public static final class Factory implements Job.Factory<SmsSentJob> {
    @Override
    public @NonNull SmsSentJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new SmsSentJob(parameters,
                            data.getLong(KEY_MESSAGE_ID),
                            data.getString(KEY_ACTION),
                            data.getInt(KEY_RESULT),
                            data.getInt(KEY_RUN_ATTEMPT));
    }
  }
}
