package com.RWdesenv.What.giph.net;


import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Stream;

import com.RWdesenv.What.logging.Log;


import com.RWdesenv.What.giph.model.GiphyImage;
import com.RWdesenv.What.giph.model.GiphyResponse;
import com.RWdesenv.What.net.ContentProxySelector;
import com.RWdesenv.What.net.UserAgentInterceptor;
import com.RWdesenv.What.util.AsyncLoader;
import com.RWdesenv.What.util.JsonUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class GiphyLoader extends AsyncLoader<List<GiphyImage>> {

  private static final String TAG = GiphyLoader.class.getSimpleName();

  public static int PAGE_SIZE = 100;

  @Nullable private String searchString;

  private final OkHttpClient client;

  protected GiphyLoader(@NonNull Context context, @Nullable String searchString) {
    super(context);
    this.searchString = searchString;
    this.client       = new OkHttpClient.Builder()
                                        .proxySelector(new ContentProxySelector())
                                        .addInterceptor(new UserAgentInterceptor())
                                        .build();
  }

  @Override
  public List<GiphyImage> loadInBackground() {
    return loadPage(0);
  }

  public @NonNull List<GiphyImage> loadPage(int offset) {
    try {
      String url;

      if (TextUtils.isEmpty(searchString)) url = String.format(getTrendingUrl(), offset);
      else                                 url = String.format(getSearchUrl(), offset, Uri.encode(searchString));

      Request  request  = new Request.Builder().url(url).build();
      Response response = client.newCall(request).execute();

      if (!response.isSuccessful()) {
        throw new IOException("Unexpected code " + response);
      }

      GiphyResponse    giphyResponse = JsonUtils.fromJson(response.body().byteStream(), GiphyResponse.class);
      List<GiphyImage> results       = Stream.of(giphyResponse.getData())
                                             .filterNot(g -> TextUtils.isEmpty(g.getGifUrl()))
                                             .filterNot(g -> TextUtils.isEmpty(g.getGifMmsUrl()))
                                             .filterNot(g -> TextUtils.isEmpty(g.getStillUrl()))
                                             .toList();

      if (results == null) return new LinkedList<>();
      else                 return results;

    } catch (IOException e) {
      Log.w(TAG, e);
      return new LinkedList<>();
    }
  }

  protected abstract String getTrendingUrl();
  protected abstract String getSearchUrl();
}
