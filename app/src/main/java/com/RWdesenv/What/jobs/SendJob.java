package com.RWdesenv.What.jobs;

import androidx.annotation.NonNull;

import com.RWdesenv.What.BuildConfig;
import com.RWdesenv.What.TextSecureExpiredException;
import com.RWdesenv.What.attachments.Attachment;
import com.RWdesenv.What.database.AttachmentDatabase;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.jobmanager.Job;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.mms.MediaConstraints;
import com.RWdesenv.What.transport.UndeliverableMessageException;
import com.RWdesenv.What.util.Util;

import java.util.List;

public abstract class SendJob extends BaseJob {

  @SuppressWarnings("unused")
  private final static String TAG = SendJob.class.getSimpleName();

  public SendJob(Job.Parameters parameters) {
    super(parameters);
  }

  @Override
  public final void onRun() throws Exception {
    if (Util.getDaysTillBuildExpiry() <= 0) {
      throw new TextSecureExpiredException(String.format("TextSecure expired (build %d, now %d)",
                                                         BuildConfig.BUILD_TIMESTAMP,
                                                         System.currentTimeMillis()));
    }

    Log.i(TAG, "Starting message send attempt");
    onSend();
    Log.i(TAG, "Message send completed");
  }

  protected abstract void onSend() throws Exception;

  protected void markAttachmentsUploaded(long messageId, @NonNull List<Attachment> attachments) {
    AttachmentDatabase database = DatabaseFactory.getAttachmentDatabase(context);

    for (Attachment attachment : attachments) {
      database.markAttachmentUploaded(messageId, attachment);
    }
  }
}
