package com.RWdesenv.What.service;


import android.content.Context;
import android.content.Intent;

import com.RWdesenv.What.ApplicationContext;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobs.RotateSignedPreKeyJob;
import com.RWdesenv.What.util.TextSecurePreferences;

import java.util.concurrent.TimeUnit;

public class RotateSignedPreKeyListener extends PersistentAlarmManagerListener {

  private static final long INTERVAL = TimeUnit.DAYS.toMillis(2);

  @Override
  protected long getNextScheduledExecutionTime(Context context) {
    return TextSecurePreferences.getSignedPreKeyRotationTime(context);
  }

  @Override
  protected long onAlarm(Context context, long scheduledTime) {
    if (scheduledTime != 0 && TextSecurePreferences.isPushRegistered(context)) {
      ApplicationDependencies.getJobManager().add(new RotateSignedPreKeyJob());
    }

    long nextTime = System.currentTimeMillis() + INTERVAL;
    TextSecurePreferences.setSignedPreKeyRotationTime(context, nextTime);

    return nextTime;
  }

  public static void schedule(Context context) {
    new RotateSignedPreKeyListener().onReceive(context, new Intent());
  }
}
