package com.RWdesenv.What.mediapreview;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import com.RWdesenv.What.R;
import com.RWdesenv.What.components.ZoomingImageView;
import com.RWdesenv.What.mms.GlideApp;
import com.RWdesenv.What.mms.GlideRequests;
import com.RWdesenv.What.util.MediaUtil;

public final class ImageMediaPreviewFragment extends MediaPreviewFragment {

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    ZoomingImageView zoomingImageView = (ZoomingImageView) inflater.inflate(R.layout.media_preview_image_fragment, container, false);
    GlideRequests    glideRequests    = GlideApp.with(requireActivity());
    Bundle           arguments        = requireArguments();
    Uri              uri              = arguments.getParcelable(DATA_URI);
    String           contentType      = arguments.getString(DATA_CONTENT_TYPE);

    if (!MediaUtil.isImageType(contentType)) {
      throw new AssertionError("This fragment can only display images");
    }

    //noinspection ConstantConditions
    zoomingImageView.setImageUri(glideRequests, uri, contentType);

    zoomingImageView.setOnClickListener(v -> events.singleTapOnMedia());

    return zoomingImageView;
  }
}
