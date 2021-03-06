package com.RWdesenv.What.webrtc.audio;


import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.RWdesenv.What.R;
import com.RWdesenv.What.util.ServiceUtil;

public class SignalAudioManager {

  @SuppressWarnings("unused")
  private static final String TAG = SignalAudioManager.class.getSimpleName();

  private final Context        context;
  private final IncomingRinger incomingRinger;
  private final OutgoingRinger outgoingRinger;

  private final SoundPool soundPool;
  private final int       connectedSoundId;
  private final int       disconnectedSoundId;

  public SignalAudioManager(@NonNull Context context) {
    this.context             = context.getApplicationContext();
    this.incomingRinger      = new IncomingRinger(context);
    this.outgoingRinger      = new OutgoingRinger(context);
    this.soundPool           = new SoundPool(1, AudioManager.STREAM_VOICE_CALL, 0);

    this.connectedSoundId    = this.soundPool.load(context, R.raw.webrtc_completed, 1);
    this.disconnectedSoundId = this.soundPool.load(context, R.raw.webrtc_disconnected, 1);
  }

  public void initializeAudioForCall() {
    AudioManager audioManager = ServiceUtil.getAudioManager(context);
    audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE);
  }

  public void startIncomingRinger(@Nullable Uri ringtoneUri, boolean vibrate) {
    AudioManager audioManager = ServiceUtil.getAudioManager(context);
    boolean      speaker      = !audioManager.isWiredHeadsetOn() && !audioManager.isBluetoothScoOn();

    audioManager.setMode(AudioManager.MODE_RINGTONE);
    audioManager.setMicrophoneMute(false);
    audioManager.setSpeakerphoneOn(speaker);

    incomingRinger.start(ringtoneUri, vibrate);
  }

  public void startOutgoingRinger(OutgoingRinger.Type type) {
    AudioManager audioManager = ServiceUtil.getAudioManager(context);
    audioManager.setMicrophoneMute(false);

    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

    outgoingRinger.start(type);
  }

  public void silenceIncomingRinger() {
    incomingRinger.stop();
  }

  public void startCommunication(boolean preserveSpeakerphone) {
    AudioManager audioManager = ServiceUtil.getAudioManager(context);

    incomingRinger.stop();
    outgoingRinger.stop();

    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);

    if (!preserveSpeakerphone) {
      audioManager.setSpeakerphoneOn(false);
    }

    soundPool.play(connectedSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
  }

  public void stop(boolean playDisconnected) {
    AudioManager audioManager = ServiceUtil.getAudioManager(context);

    incomingRinger.stop();
    outgoingRinger.stop();

    if (playDisconnected) {
      soundPool.play(disconnectedSoundId, 1.0f, 1.0f, 0, 0, 1.0f);
    }

    if (audioManager.isBluetoothScoOn()) {
      audioManager.setBluetoothScoOn(false);
      audioManager.stopBluetoothSco();
    }

    audioManager.setSpeakerphoneOn(false);
    audioManager.setMicrophoneMute(false);
    audioManager.setMode(AudioManager.MODE_NORMAL);
    audioManager.abandonAudioFocus(null);
  }
}
