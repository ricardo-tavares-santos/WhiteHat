package com.RWdesenv.What.mms;

import android.content.Context;
import android.content.res.Resources.Theme;
import android.net.Uri;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.RWdesenv.What.R;
import com.RWdesenv.What.attachments.Attachment;
import com.RWdesenv.What.blurhash.BlurHash;
import com.RWdesenv.What.stickers.StickerLocator;
import com.RWdesenv.What.util.MediaUtil;

public class StickerSlide extends Slide {

  public static final int WIDTH  = 512;
  public static final int HEIGHT = 512;

  public StickerSlide(@NonNull Context context, @NonNull Attachment attachment) {
    super(context, attachment);
  }

  public StickerSlide(Context context, Uri uri, long size, @NonNull StickerLocator stickerLocator) {
    super(context, constructAttachmentFromUri(context, uri, MediaUtil.IMAGE_WEBP, size, WIDTH, HEIGHT, true, null, null, stickerLocator, null, false, false));
  }

  @Override
  public @DrawableRes int getPlaceholderRes(Theme theme) {
    return 0;
  }

  @Override
  public @Nullable Uri getThumbnailUri() {
    return getUri();
  }

  @Override
  public boolean hasSticker() {
    return true;
  }

  @Override
  public @NonNull String getContentDescription() {
    return context.getString(R.string.Slide_sticker);
  }
}
