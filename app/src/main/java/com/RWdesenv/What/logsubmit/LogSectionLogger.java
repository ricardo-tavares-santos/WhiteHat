package com.RWdesenv.What.logsubmit;

import android.content.Context;

import androidx.annotation.NonNull;

import com.RWdesenv.What.ApplicationContext;

import java.util.concurrent.ExecutionException;

public class LogSectionLogger implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "LOGGER";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    try {
      return ApplicationContext.getInstance(context).getPersistentLogger().getLogs().get();
    } catch (ExecutionException | InterruptedException e) {
      return "Failed to retrieve.";
    }
  }
}
