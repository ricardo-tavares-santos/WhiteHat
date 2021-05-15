package com.RWdesenv.What.attachments;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.RWdesenv.What.blurhash.BlurHash;
import com.RWdesenv.What.database.AttachmentDatabase;
import com.RWdesenv.What.database.AttachmentDatabase.TransformProperties;
import com.RWdesenv.What.stickers.StickerLocator;

public class UriAttachment extends Attachment {

  private final @NonNull  Uri dataUri;
  private final @Nullable Uri thumbnailUri;

  public UriAttachment(@NonNull Uri uri, @NonNull String contentType, int transferState, long size,
                       @Nullable String fileName, boolean voiceNote, boolean quote, @Nullable String caption,
                       @Nullable StickerLocator stickerLocator, @Nullable BlurHash blurHash, @Nullable TransformProperties transformProperties)
  {
    this(uri, uri, contentType, transferState, size, 0, 0, fileName, null, voiceNote, quote, caption, stickerLocator, blurHash, transformProperties);
  }

  public UriAttachment(@NonNull Uri dataUri, @Nullable Uri thumbnailUri,
                       @NonNull String contentType, int transferState, long size, int width, int height,
                       @Nullable String fileName, @Nullable String fastPreflightId,
                       boolean voiceNote, boolean quote, @Nullable String caption, @Nullable StickerLocator stickerLocator,
                       @Nullable BlurHash blurHash, @Nullable TransformProperties transformProperties)
  {
    super(contentType, transferState, size, fileName, null, null, null, null, fastPreflightId, voiceNote, width, height, quote, caption, stickerLocator, blurHash, transformProperties);
    this.dataUri      = dataUri;
    this.thumbnailUri = thumbnailUri;
  }

  @Override
  @NonNull
  public Uri getDataUri() {
    return dataUri;
  }

  @Override
  @Nullable
  public Uri getThumbnailUri() {
    return thumbnailUri;
  }

  @Override
  public boolean equals(Object other) {
    return other != null && other instanceof UriAttachment && ((UriAttachment) other).dataUri.equals(this.dataUri);
  }

  @Override
  public int hashCode() {
    return dataUri.hashCode();
  }
}
