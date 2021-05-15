package com.RWdesenv.What.jobs;

import android.content.Context;
import android.media.MediaDataSource;

import androidx.annotation.NonNull;

import org.greenrobot.eventbus.EventBus;
import com.RWdesenv.What.R;
import com.RWdesenv.What.attachments.Attachment;
import com.RWdesenv.What.attachments.AttachmentId;
import com.RWdesenv.What.attachments.DatabaseAttachment;
import com.RWdesenv.What.database.AttachmentDatabase;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.events.PartProgressEvent;
import com.RWdesenv.What.jobmanager.Data;
import com.RWdesenv.What.jobmanager.Job;
import com.RWdesenv.What.jobmanager.impl.NetworkConstraint;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.mms.DecryptableStreamUriLoader;
import com.RWdesenv.What.mms.MediaConstraints;
import com.RWdesenv.What.mms.MediaStream;
import com.RWdesenv.What.mms.MmsException;
import com.RWdesenv.What.service.GenericForegroundService;
import com.RWdesenv.What.service.NotificationController;
import com.RWdesenv.What.transport.UndeliverableMessageException;
import com.RWdesenv.What.util.BitmapDecodingException;
import com.RWdesenv.What.util.BitmapUtil;
import com.RWdesenv.What.util.MediaUtil;
import com.RWdesenv.What.util.MemoryFileDescriptor.MemoryFileException;
import com.RWdesenv.What.video.InMemoryTranscoder;
import com.RWdesenv.What.video.VideoSizeException;
import com.RWdesenv.What.video.VideoSourceException;
import com.RWdesenv.What.video.videoconverter.EncodingException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public final class AttachmentCompressionJob extends BaseJob {

  public static final String KEY = "AttachmentCompressionJob";

  @SuppressWarnings("unused")
  private static final String TAG = Log.tag(AttachmentCompressionJob.class);

  private static final String KEY_ROW_ID              = "row_id";
  private static final String KEY_UNIQUE_ID           = "unique_id";
  private static final String KEY_MMS                 = "mms";
  private static final String KEY_MMS_SUBSCRIPTION_ID = "mms_subscription_id";

  private final AttachmentId attachmentId;
  private final boolean      mms;
  private final int          mmsSubscriptionId;

  public static AttachmentCompressionJob fromAttachment(@NonNull DatabaseAttachment databaseAttachment,
                                                        boolean mms,
                                                        int mmsSubscriptionId)
  {
    return new AttachmentCompressionJob(databaseAttachment.getAttachmentId(),
                                        MediaUtil.isVideo(databaseAttachment) && MediaConstraints.isVideoTranscodeAvailable(),
                                        mms,
                                        mmsSubscriptionId);
  }

  private AttachmentCompressionJob(@NonNull AttachmentId attachmentId,
                                   boolean isVideoTranscode,
                                   boolean mms,
                                   int mmsSubscriptionId)
  {
    this(new Parameters.Builder()
                       .addConstraint(NetworkConstraint.KEY)
                       .setLifespan(TimeUnit.DAYS.toMillis(1))
                       .setMaxAttempts(Parameters.UNLIMITED)
                       .setQueue(isVideoTranscode ? "VIDEO_TRANSCODE" : "GENERIC_TRANSCODE")
                       .build(),
         attachmentId,
         mms,
         mmsSubscriptionId);
  }

  private AttachmentCompressionJob(@NonNull Parameters parameters,
                                   @NonNull AttachmentId attachmentId,
                                   boolean mms,
                                   int mmsSubscriptionId)
  {
    super(parameters);
    this.attachmentId      = attachmentId;
    this.mms               = mms;
    this.mmsSubscriptionId = mmsSubscriptionId;
  }

  @Override
  public @NonNull Data serialize() {
    return new Data.Builder().putLong(KEY_ROW_ID, attachmentId.getRowId())
                             .putLong(KEY_UNIQUE_ID, attachmentId.getUniqueId())
                             .putBoolean(KEY_MMS, mms)
                             .putInt(KEY_MMS_SUBSCRIPTION_ID, mmsSubscriptionId)
                             .build();
  }

  @Override
  public @NonNull String getFactoryKey() {
    return KEY;
  }

  @Override
  public void onRun() throws Exception {
    Log.d(TAG, "Running for: " + attachmentId);

    AttachmentDatabase database           = DatabaseFactory.getAttachmentDatabase(context);
    DatabaseAttachment databaseAttachment = database.getAttachment(attachmentId);

    if (databaseAttachment == null) {
      throw new UndeliverableMessageException("Cannot find the specified attachment.");
    }

    if (databaseAttachment.getTransformProperties().shouldSkipTransform()) {
      Log.i(TAG, "Skipping at the direction of the TransformProperties.");
      return;
    }

    MediaConstraints mediaConstraints = mms ? MediaConstraints.getMmsMediaConstraints(mmsSubscriptionId)
                                            : MediaConstraints.getPushMediaConstraints();

    scaleAndStripExif(database, mediaConstraints, databaseAttachment);
  }

  @Override
  public void onFailure() { }

  @Override
  protected boolean onShouldRetry(@NonNull Exception exception) {
    return exception instanceof IOException;
  }

  private void scaleAndStripExif(@NonNull AttachmentDatabase attachmentDatabase,
                                 @NonNull MediaConstraints constraints,
                                 @NonNull DatabaseAttachment attachment)
      throws UndeliverableMessageException
  {
    try {
      if (MediaUtil.isVideo(attachment)) {
        attachment = transcodeVideoIfNeededToDatabase(context, attachmentDatabase, attachment, constraints, EventBus.getDefault(), this::isCanceled);
        if (!constraints.isSatisfied(context, attachment)) {
          throw new UndeliverableMessageException("Size constraints could not be met on video!");
        }
      } else if (constraints.isSatisfied(context, attachment)) {
        if (MediaUtil.isJpeg(attachment)) {
          MediaStream stripped = getResizedMedia(context, attachment, constraints);
          attachmentDatabase.updateAttachmentData(attachment, stripped, false);
          attachmentDatabase.markAttachmentAsTransformed(attachmentId);
        }
      } else if (constraints.canResize(attachment)) {
        MediaStream resized = getResizedMedia(context, attachment, constraints);
        attachmentDatabase.updateAttachmentData(attachment, resized, false);
        attachmentDatabase.markAttachmentAsTransformed(attachmentId);
      } else {
        throw new UndeliverableMessageException("Size constraints could not be met!");
      }
    } catch (IOException | MmsException e) {
      throw new UndeliverableMessageException(e);
    }
  }

  private static @NonNull DatabaseAttachment transcodeVideoIfNeededToDatabase(@NonNull Context context,
                                                                              @NonNull AttachmentDatabase attachmentDatabase,
                                                                              @NonNull DatabaseAttachment attachment,
                                                                              @NonNull MediaConstraints constraints,
                                                                              @NonNull EventBus eventBus,
                                                                              @NonNull InMemoryTranscoder.CancelationSignal cancelationSignal)
      throws UndeliverableMessageException
  {
    AttachmentDatabase.TransformProperties transformProperties = attachment.getTransformProperties();

    boolean allowSkipOnFailure = false;

    if (!MediaConstraints.isVideoTranscodeAvailable()) {
      if (transformProperties.isVideoEdited()) {
        throw new UndeliverableMessageException("Video edited, but transcode is not available");
      }
      return attachment;
    }

    try (NotificationController notification = GenericForegroundService.startForegroundTask(context, context.getString(R.string.AttachmentUploadJob_compressing_video_start))) {

      notification.setIndeterminateProgress();

      try (MediaDataSource dataSource = attachmentDatabase.mediaDataSourceFor(attachment.getAttachmentId())) {

        if (dataSource == null) {
          throw new UndeliverableMessageException("Cannot get media data source for attachment.");
        }

        allowSkipOnFailure = !transformProperties.isVideoEdited();
        InMemoryTranscoder.Options options = null;
        if (transformProperties.isVideoTrim()) {
          options = new InMemoryTranscoder.Options(transformProperties.getVideoTrimStartTimeUs(), transformProperties.getVideoTrimEndTimeUs());
        }

        try (InMemoryTranscoder transcoder = new InMemoryTranscoder(context, dataSource, options, constraints.getCompressedVideoMaxSize(context))) {
          if (transcoder.isTranscodeRequired()) {
            MediaStream mediaStream = transcoder.transcode(percent -> {
              notification.setProgress(100, percent);
              eventBus.postSticky(new PartProgressEvent(attachment,
                                                        PartProgressEvent.Type.COMPRESSION,
                                                        100,
                                                        percent));
            }, cancelationSignal);

            attachmentDatabase.updateAttachmentData(attachment, mediaStream, transformProperties.isVideoEdited());
            attachmentDatabase.markAttachmentAsTransformed(attachment.getAttachmentId());
            DatabaseAttachment updatedAttachment = attachmentDatabase.getAttachment(attachment.getAttachmentId());
            if (updatedAttachment == null) {
              throw new AssertionError();
            }
            return updatedAttachment;
          }
        }
      }
    } catch (VideoSourceException | EncodingException | MemoryFileException e) {
      if (attachment.getSize() > constraints.getVideoMaxSize(context)) {
        throw new UndeliverableMessageException("Duration not found, attachment too large to skip transcode", e);
      } else {
        if (allowSkipOnFailure) {
          Log.w(TAG, "Problem with video source, but video small enough to skip transcode", e);
        } else {
          throw new UndeliverableMessageException("Failed to transcode and cannot skip due to editing", e);
        }
      }
    } catch (IOException | MmsException | VideoSizeException e) {
      throw new UndeliverableMessageException("Failed to transcode", e);
    }
    return attachment;
  }

  private static MediaStream getResizedMedia(@NonNull Context context,
                                             @NonNull Attachment attachment,
                                             @NonNull MediaConstraints constraints)
      throws IOException
  {
    if (!constraints.canResize(attachment)) {
      throw new UnsupportedOperationException("Cannot resize this content type");
    }

    try {
      BitmapUtil.ScaleResult scaleResult = BitmapUtil.createScaledBytes(context,
                                                                        new DecryptableStreamUriLoader.DecryptableUri(attachment.getDataUri()),
                                                                        constraints);

      return new MediaStream(new ByteArrayInputStream(scaleResult.getBitmap()),
                             MediaUtil.IMAGE_JPEG,
                             scaleResult.getWidth(),
                             scaleResult.getHeight());
    } catch (BitmapDecodingException e) {
      throw new IOException(e);
    }
  }

  public static final class Factory implements Job.Factory<AttachmentCompressionJob> {
    @Override
    public @NonNull AttachmentCompressionJob create(@NonNull Parameters parameters, @NonNull Data data) {
      return new AttachmentCompressionJob(parameters,
                                          new AttachmentId(data.getLong(KEY_ROW_ID), data.getLong(KEY_UNIQUE_ID)),
                                          data.getBoolean(KEY_MMS),
                                          data.getInt(KEY_MMS_SUBSCRIPTION_ID));
    }
  }
}
