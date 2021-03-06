/*
 * Copyright (C) 2011 Whisper Systems
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
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
import com.RWdesenv.What.util.MediaUtil;

public class ImageSlide extends Slide {

  @SuppressWarnings("unused")
  private static final String TAG = ImageSlide.class.getSimpleName();

  public ImageSlide(@NonNull Context context, @NonNull Attachment attachment) {
    super(context, attachment);
  }

  public ImageSlide(Context context, Uri uri, long size, int width, int height, @Nullable BlurHash blurHash) {
    this(context, uri, size, width, height, null, blurHash);
  }

  public ImageSlide(Context context, Uri uri, long size, int width, int height, @Nullable String caption, @Nullable BlurHash blurHash) {
    super(context, constructAttachmentFromUri(context, uri, MediaUtil.IMAGE_JPEG, size, width, height, true, null, caption, null, blurHash, false, false));
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
  public boolean hasImage() {
    return true;
  }

  @Override
  public boolean hasPlaceholder() {
    return getPlaceholderBlur() != null;
  }

  @NonNull
  @Override
  public String getContentDescription() {
    return context.getString(R.string.Slide_image);
  }
}
