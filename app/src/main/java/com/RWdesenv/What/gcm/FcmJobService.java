package com.RWdesenv.What.gcm;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.RWdesenv.What.ApplicationContext;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.notifications.MessageNotifier;
import com.RWdesenv.What.util.ServiceUtil;
import com.RWdesenv.What.util.TextSecurePreferences;
import com.RWdesenv.What.util.concurrent.SignalExecutors;

/**
 * Pulls down messages. Used when we fail to pull down messages in {@link FcmService}.
 */
@RequiresApi(26)
public class FcmJobService extends JobService {

  private static final String TAG = FcmJobService.class.getSimpleName();

  private static final int ID = 1337;

  @RequiresApi(26)
  public static void schedule(@NonNull Context context) {
    JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(ID, new ComponentName(context, FcmJobService.class))
                                                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                                                .setBackoffCriteria(0, JobInfo.BACKOFF_POLICY_LINEAR)
                                                .setPersisted(true);

    ServiceUtil.getJobScheduler(context).schedule(jobInfoBuilder.build());
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    Log.d(TAG, "onStartJob()");

    if (MessageRetriever.shouldIgnoreFetch(this)) {
      Log.i(TAG, "App is foregrounded. No need to run.");
      return false;
    }

    SignalExecutors.UNBOUNDED.execute(() -> {
      Context          context   = getApplicationContext();
      MessageRetriever retriever = ApplicationDependencies.getMessageRetriever();
      boolean          success   = retriever.retrieveMessages(context, new RestStrategy(), new RestStrategy());

      if (success) {
        Log.i(TAG, "Successfully retrieved messages.");
        jobFinished(params, false);
      } else {
        Log.w(TAG, "Failed to retrieve messages. Scheduling a retry.");
        jobFinished(params, true);
      }
    });

    return true;
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    Log.d(TAG, "onStopJob()");
    return TextSecurePreferences.getNeedsMessagePull(getApplicationContext());
  }
}
