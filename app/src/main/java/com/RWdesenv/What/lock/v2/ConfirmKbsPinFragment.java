package com.RWdesenv.What.lock.v2;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.util.Preconditions;
import androidx.lifecycle.ViewModelProviders;

import com.airbnb.lottie.LottieAnimationView;
import com.airbnb.lottie.LottieDrawable;

import com.RWdesenv.What.R;
import com.RWdesenv.What.animation.AnimationCompleteListener;
import com.RWdesenv.What.animation.AnimationRepeatListener;
import com.RWdesenv.What.dependencies.ApplicationDependencies;
import com.RWdesenv.What.keyvalue.SignalStore;
import com.RWdesenv.What.megaphone.Megaphones;
import com.RWdesenv.What.util.SpanUtil;

public class ConfirmKbsPinFragment extends BaseKbsPinFragment<ConfirmKbsPinViewModel> {

  private ConfirmKbsPinViewModel    viewModel;

  @Override
  protected void initializeViewStates() {
    ConfirmKbsPinFragmentArgs args = ConfirmKbsPinFragmentArgs.fromBundle(requireArguments());

    if (args.getIsPinChange()) {
      initializeViewStatesForPinChange();
    } else {
      initializeViewStatesForPinCreate();
    }
  }

  @Override
  protected ConfirmKbsPinViewModel initializeViewModel() {
    ConfirmKbsPinFragmentArgs      args       = ConfirmKbsPinFragmentArgs.fromBundle(requireArguments());
    KbsPin                         userEntry  = Preconditions.checkNotNull(args.getUserEntry());
    PinKeyboardType                keyboard   = args.getKeyboard();
    ConfirmKbsPinRepository        repository = new ConfirmKbsPinRepository();
    ConfirmKbsPinViewModel.Factory factory    = new ConfirmKbsPinViewModel.Factory(userEntry, keyboard, repository);

    viewModel = ViewModelProviders.of(this, factory).get(ConfirmKbsPinViewModel.class);

    viewModel.getLabel().observe(getViewLifecycleOwner(), this::updateLabel);
    viewModel.getSaveAnimation().observe(getViewLifecycleOwner(), this::updateSaveAnimation);

    return viewModel;
  }

  private void initializeViewStatesForPinCreate() {
    getTitle().setText(R.string.CreateKbsPinFragment__create_your_pin);
    getDescription().setText(R.string.ConfirmKbsPinFragment__confirm_your_pin);
    getKeyboardToggle().setVisibility(View.INVISIBLE);
    getLabel().setText("");
  }

  private void initializeViewStatesForPinChange() {
    getTitle().setText(R.string.CreateKbsPinFragment__create_a_new_pin);
    getDescription().setText(R.string.ConfirmKbsPinFragment__confirm_your_pin);
    getKeyboardToggle().setVisibility(View.INVISIBLE);
    getLabel().setText("");
  }

  private void updateLabel(@NonNull ConfirmKbsPinViewModel.Label label) {
    switch (label) {
      case EMPTY:
        getLabel().setText("");
        break;
      case CREATING_PIN:
        getLabel().setText(R.string.ConfirmKbsPinFragment__creating_pin);
        getInput().setEnabled(false);
        break;
      case RE_ENTER_PIN:
        getLabel().setText(R.string.ConfirmKbsPinFragment__re_enter_pin);
        break;
      case PIN_DOES_NOT_MATCH:
        getLabel().setText(SpanUtil.color(ContextCompat.getColor(requireContext(), R.color.red),
                           getString(R.string.ConfirmKbsPinFragment__pins_dont_match)));
        getInput().getText().clear();
        break;
    }
  }

  private void updateSaveAnimation(@NonNull ConfirmKbsPinViewModel.SaveAnimation animation) {
    updateAnimationAndInputVisibility(animation);
    LottieAnimationView lottieProgress = getLottieProgress();

    switch (animation) {
      case NONE:
        lottieProgress.cancelAnimation();
        break;
      case LOADING:
        lottieProgress.setAnimation(R.raw.lottie_kbs_loading);
        lottieProgress.setRepeatMode(LottieDrawable.RESTART);
        lottieProgress.setRepeatCount(LottieDrawable.INFINITE);
        lottieProgress.playAnimation();
        break;
      case SUCCESS:
        startEndAnimationOnNextProgressRepetition(R.raw.lottie_kbs_success, new AnimationCompleteListener() {
          @Override
          public void onAnimationEnd(Animator animation) {
            requireActivity().setResult(Activity.RESULT_OK);
            closeNavGraphBranch();
            SignalStore.registrationValues().setRegistrationComplete();
            SignalStore.pinValues().onPinChange();
          }
        });
        break;
      case FAILURE:
        startEndAnimationOnNextProgressRepetition(R.raw.lottie_kbs_failure, new AnimationCompleteListener() {
          @Override
          public void onAnimationEnd(Animator animation) {
            SignalStore.registrationValues().setRegistrationComplete();
            displayFailedDialog();
          }
        });
        break;
    }
  }

  private void startEndAnimationOnNextProgressRepetition(@RawRes int lottieAnimationId,
                                                         @NonNull AnimationCompleteListener listener)
  {
    LottieAnimationView lottieProgress = getLottieProgress();
    LottieAnimationView lottieEnd      = getLottieEnd();

    lottieEnd.setAnimation(lottieAnimationId);
    lottieEnd.removeAllAnimatorListeners();
    lottieEnd.setRepeatCount(0);
    lottieEnd.addAnimatorListener(listener);

    if (lottieProgress.isAnimating()) {
      lottieProgress.addAnimatorListener(new AnimationRepeatListener(animator ->
          hideProgressAndStartEndAnimation(lottieProgress, lottieEnd)
      ));
    } else {
      hideProgressAndStartEndAnimation(lottieProgress, lottieEnd);
    }
  }

  private void hideProgressAndStartEndAnimation(@NonNull LottieAnimationView lottieProgress,
                                                @NonNull LottieAnimationView lottieEnd)
  {
    viewModel.onLoadingAnimationComplete();
    lottieProgress.setVisibility(View.GONE);
    lottieEnd.setVisibility(View.VISIBLE);
    lottieEnd.playAnimation();
  }

  private void updateAnimationAndInputVisibility(ConfirmKbsPinViewModel.SaveAnimation saveAnimation) {
    if (saveAnimation == ConfirmKbsPinViewModel.SaveAnimation.NONE) {
      getInput().setVisibility(View.VISIBLE);
      getLottieProgress().setVisibility(View.GONE);
    } else {
      getInput().setVisibility(View.GONE);
      getLottieProgress().setVisibility(View.VISIBLE);
    }
  }

  private void displayFailedDialog() {
    new AlertDialog.Builder(requireContext()).setTitle(R.string.ConfirmKbsPinFragment__pin_creation_failed)
                   .setMessage(R.string.ConfirmKbsPinFragment__your_pin_was_not_saved)
                   .setCancelable(false)
                   .setPositiveButton(R.string.ok, (d, w) -> {
                     d.dismiss();
                     markMegaphoneSeenIfNecessary();
                     requireActivity().setResult(Activity.RESULT_CANCELED);
                     closeNavGraphBranch();
                   })
                   .show();
  }

  private void closeNavGraphBranch() {
    Intent activityIntent = requireActivity().getIntent();
    if (activityIntent != null && activityIntent.hasExtra("next_intent")) {
      startActivity(activityIntent.getParcelableExtra("next_intent"));
    }

    requireActivity().finish();
  }

  private void markMegaphoneSeenIfNecessary() {
    ApplicationDependencies.getMegaphoneRepository().markSeen(Megaphones.Event.PINS_FOR_ALL);
  }
}
