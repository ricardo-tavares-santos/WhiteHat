package com.RWdesenv.What.conversationlist;

import android.app.Application;
import android.database.ContentObserver;
import android.os.Handler;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.RWdesenv.What.conversationlist.model.SearchResult;
import com.RWdesenv.What.database.DatabaseContentProviders;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.megaphone.Megaphone;
import com.RWdesenv.What.megaphone.MegaphoneRepository;
import com.RWdesenv.What.megaphone.Megaphones;
import com.RWdesenv.What.search.SearchRepository;
import com.RWdesenv.What.util.Debouncer;
import com.RWdesenv.What.util.Util;

class ConversationListViewModel extends ViewModel {

  private final Application                   application;
  private final MutableLiveData<Megaphone>    megaphone;
  private final MutableLiveData<SearchResult> searchResult;
  private final SearchRepository              searchRepository;
  private final MegaphoneRepository           megaphoneRepository;
  private final Debouncer                     debouncer;
  private final ContentObserver               observer;

  private String lastQuery;

  private ConversationListViewModel(@NonNull Application application, @NonNull SearchRepository searchRepository) {
    this.application         = application;
    this.megaphone           = new MutableLiveData<>();
    this.searchResult        = new MutableLiveData<>();
    this.searchRepository    = searchRepository;
    this.megaphoneRepository = ApplicationDependencies.getMegaphoneRepository();
    this.debouncer           = new Debouncer(300);
    this.observer            = new ContentObserver(new Handler()) {
      @Override
      public void onChange(boolean selfChange) {
        if (!TextUtils.isEmpty(getLastQuery())) {
          searchRepository.query(getLastQuery(), searchResult::postValue);
        }
      }
    };

    application.getContentResolver().registerContentObserver(DatabaseContentProviders.ConversationList.CONTENT_URI, true, observer);
  }

  @NonNull LiveData<SearchResult> getSearchResult() {
    return searchResult;
  }

  @NonNull LiveData<Megaphone> getMegaphone() {
    return megaphone;
  }

  void onVisible() {
    megaphoneRepository.getNextMegaphone(megaphone::postValue);
  }

  void onMegaphoneCompleted(@NonNull Megaphones.Event event) {
    megaphone.postValue(null);
    megaphoneRepository.markFinished(event);
  }

  void onMegaphoneSnoozed(@NonNull Megaphones.Event event) {
    megaphoneRepository.markSeen(event);
    megaphone.postValue(null);
  }

  void onMegaphoneVisible(@NonNull Megaphone visible) {
    megaphoneRepository.markVisible(visible.getEvent());
  }

  void updateQuery(String query) {
    lastQuery = query;
    debouncer.publish(() -> searchRepository.query(query, result -> {
      Util.runOnMain(() -> {
        if (query.equals(lastQuery)) {
          searchResult.setValue(result);
        }
      });
    }));
  }

  private @NonNull String getLastQuery() {
    return lastQuery == null ? "" : lastQuery;
  }

  @Override
  protected void onCleared() {
    debouncer.clear();
    application.getContentResolver().unregisterContentObserver(observer);
  }

  public static class Factory extends ViewModelProvider.NewInstanceFactory {
    @Override
    public @NonNull<T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      //noinspection ConstantConditions
      return modelClass.cast(new ConversationListViewModel(ApplicationDependencies.getApplication(), new SearchRepository()));
    }
  }
}
