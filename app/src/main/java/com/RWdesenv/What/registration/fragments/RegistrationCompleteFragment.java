package com.RWdesenv.What.registration.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.ActivityNavigator;

import com.RWdesenv.What.MainActivity;
import com.RWdesenv.What.R;
import com.RWdesenv.What.lock.v2.CreateKbsPinActivity;
import com.RWdesenv.What.lock.v2.PinUtil;
import com.RWdesenv.What.profiles.edit.EditProfileActivity;
import com.RWdesenv.What.util.CensorshipUtil;
import com.RWdesenv.What.util.FeatureFlags;

public final class RegistrationCompleteFragment extends BaseRegistrationFragment {

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_registration_blank, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    FragmentActivity activity = requireActivity();


    if (!isReregister()) {
      final Intent main    = new Intent(activity, MainActivity.class);
      final Intent profile = new Intent(activity, EditProfileActivity.class);

      profile.putExtra(EditProfileActivity.SHOW_TOOLBAR, false);

      if (PinUtil.shouldShowPinCreationDuringRegistration(requireContext())) {
        Intent kbs = CreateKbsPinActivity.getIntentForPinCreate(requireContext());
        activity.startActivity(chainIntents(chainIntents(profile, kbs), main));
      } else {
        activity.startActivity(chainIntents(profile, main));
      }
    }

    activity.finish();
    ActivityNavigator.applyPopAnimationsToPendingTransition(activity);
  }

  private static Intent chainIntents(@NonNull Intent sourceIntent, @Nullable Intent nextIntent) {
    if (nextIntent != null) sourceIntent.putExtra("next_intent", nextIntent);
    return sourceIntent;
  }
}
