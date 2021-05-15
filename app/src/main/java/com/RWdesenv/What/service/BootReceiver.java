package com.RWdesenv.What.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.RWdesenv.What.ApplicationContext;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.jobs.PushNotificationReceiveJob;

public class BootReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    ApplicationDependencies.getJobManager().add(new PushNotificationReceiveJob(context));
  }
}
