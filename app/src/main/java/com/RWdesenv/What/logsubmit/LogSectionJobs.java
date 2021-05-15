package com.RWdesenv.What.logsubmit;

import android.content.Context;

import androidx.annotation.NonNull;

import com.RWdesenv.What.dependencies.ApplicationDependencies;

import java.util.List;

public class LogSectionJobs implements LogSection {

  @Override
  public @NonNull String getTitle() {
    return "JOBS";
  }

  @Override
  public @NonNull CharSequence getContent(@NonNull Context context) {
    return ApplicationDependencies.getJobManager().getDebugInfo();
  }
}
