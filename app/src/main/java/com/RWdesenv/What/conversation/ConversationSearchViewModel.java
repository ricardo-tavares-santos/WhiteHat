package com.RWdesenv.What.conversation;

import android.app.Application;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;

import com.RWdesenv.What.contacts.ContactAccessor;
import com.RWdesenv.What.contacts.ContactRepository;
import com.RWdesenv.What.conversationlist.model.SearchResult;
import com.RWdesenv.What.database.CursorList;
import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.search.SearchRepository;
import com.RWdesenv.What.conversationlist.model.MessageResult;
import com.RWdesenv.What.util.CloseableLiveData;
import com.RWdesenv.What.util.Debouncer;
import com.RWdesenv.What.util.Util;
import com.RWdesenv.What.util.concurrent.SignalExecutors;

import java.io.Closeable;
import java.util.List;

public class ConversationSearchViewModel extends AndroidViewModel {

  private final SearchRepository              searchRepository;
  private final MutableLiveData<SearchResult> result;
  private final Debouncer                     debouncer;

  private boolean firstSearch;
  private boolean searchOpen;
  private String  activeQuery;
  private long    activeThreadId;

  public ConversationSearchViewModel(@NonNull Application application) {
    super(application);
    result           = new MutableLiveData<>();
    debouncer        = new Debouncer(500);
    searchRepository = new SearchRepository();
  }

  LiveData<SearchResult> getSearchResults() {
    return result;
  }

  void onQueryUpdated(@NonNull String query, long threadId, boolean forced) {
    if (firstSearch && query.length() < 2) {
      result.postValue(new SearchResult(CursorList.emptyList(), 0));
      return;
    }

    if (query.equals(activeQuery) && !forced) {
      return;
    }

    updateQuery(query, threadId);
  }

  void onMissingResult() {
    if (activeQuery != null) {
      updateQuery(activeQuery, activeThreadId);
    }
  }

  void onMoveUp() {
    if (result.getValue() == null) {
      return;
    }

    debouncer.clear();

    List<MessageResult> messages = result.getValue().getResults();
    int                 position = Math.min(result.getValue().getPosition() + 1, messages.size() - 1);

    result.setValue(new SearchResult(messages, position));
  }

  void onMoveDown() {
    if (result.getValue() == null) {
      return;
    }

    debouncer.clear();

    List<MessageResult> messages = result.getValue().getResults();
    int                 position = Math.max(result.getValue().getPosition() - 1, 0);

    result.setValue(new SearchResult(messages, position));
  }


  void onSearchOpened() {
    searchOpen  = true;
    firstSearch = true;
  }

  void onSearchClosed() {
    searchOpen = false;
    debouncer.clear();
  }

  private void updateQuery(@NonNull String query, long threadId) {
    activeQuery    = query;
    activeThreadId = threadId;

    debouncer.publish(() -> {
      firstSearch = false;

      searchRepository.query(query, threadId, messages -> {
        Util.runOnMain(() -> {
          if (searchOpen && query.equals(activeQuery)) {
            result.setValue(new SearchResult(messages, 0));
          }
        });
      });
    });
  }

  static class SearchResult {

    private final List<MessageResult> results;
    private final int                 position;

    SearchResult(@NonNull List<MessageResult> results, int position) {
      this.results  = results;
      this.position = position;
    }

    public List<MessageResult> getResults() {
      return results;
    }

    public int getPosition() {
      return position;
    }
  }
}
