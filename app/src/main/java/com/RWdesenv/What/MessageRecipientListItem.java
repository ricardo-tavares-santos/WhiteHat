/*
 * Copyright (C) 2014 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.RWdesenv.What;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.RWdesenv.What.MessageDetailsRecipientAdapter.RecipientDeliveryStatus;
import com.RWdesenv.What.components.AvatarImageView;
import com.RWdesenv.What.components.DeliveryStatusView;
import com.RWdesenv.What.components.FromTextView;
import com.RWdesenv.What.database.documents.IdentityKeyMismatch;
import com.RWdesenv.What.database.documents.NetworkFailure;
import com.RWdesenv.What.database.model.MessageRecord;
import com.RWdesenv.What.logging.Log;
import com.RWdesenv.What.mms.GlideRequests;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientForeverObserver;
import com.RWdesenv.What.util.TextSecurePreferences;

/**
 * A simple view to show the recipients of a message
 *
 * @author Jake McGinty
 */
public class MessageRecipientListItem extends RelativeLayout
    implements RecipientForeverObserver
{
  @SuppressWarnings("unused")
  private final static String TAG = MessageRecipientListItem.class.getSimpleName();

  private RecipientDeliveryStatus member;
  private GlideRequests           glideRequests;
  private FromTextView            fromView;
  private TextView                errorDescription;
  private TextView                actionDescription;
  private Button                  conflictButton;
  private AvatarImageView         contactPhotoImage;
  private ImageView               unidentifiedDeliveryIcon;
  private DeliveryStatusView      deliveryStatusView;

  public MessageRecipientListItem(Context context) {
    super(context);
  }

  public MessageRecipientListItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    this.fromView                 = findViewById(R.id.from);
    this.errorDescription         = findViewById(R.id.error_description);
    this.actionDescription        = findViewById(R.id.action_description);
    this.contactPhotoImage        = findViewById(R.id.contact_photo_image);
    this.conflictButton           = findViewById(R.id.conflict_button);
    this.unidentifiedDeliveryIcon = findViewById(R.id.ud_indicator);
    this.deliveryStatusView       = findViewById(R.id.delivery_status);
  }

  public void set(final GlideRequests glideRequests,
                  final MessageRecord record,
                  final RecipientDeliveryStatus member,
                  final boolean isPushGroup)
  {
    if (this.member != null) this.member.getRecipient().live().removeForeverObserver(this);

    this.glideRequests = glideRequests;
    this.member        = member;

    member.getRecipient().live().observeForever(this);
    fromView.setText(member.getRecipient());
    contactPhotoImage.setAvatar(glideRequests, member.getRecipient(), false);
    setIssueIndicators(record, isPushGroup);
    unidentifiedDeliveryIcon.setVisibility(TextSecurePreferences.isShowUnidentifiedDeliveryIndicatorsEnabled(getContext()) && member.isUnidentified() ? VISIBLE : GONE);
  }

  private void setIssueIndicators(final MessageRecord record,
                                  final boolean isPushGroup)
  {
    final NetworkFailure      networkFailure = getNetworkFailure(record);
    final IdentityKeyMismatch keyMismatch    = networkFailure == null ? getKeyMismatch(record) : null;

    String errorText = "";

    if (keyMismatch != null) {
      conflictButton.setVisibility(View.VISIBLE);

      errorText = getContext().getString(R.string.MessageDetailsRecipient_new_safety_number);
      conflictButton.setOnClickListener(v -> new ConfirmIdentityDialog(getContext(), record, keyMismatch).show());
    } else if ((networkFailure != null && !record.isPending()) || (!isPushGroup && record.isFailed())) {
      conflictButton.setVisibility(View.GONE);
      errorText = getContext().getString(R.string.MessageDetailsRecipient_failed_to_send);
    } else {
      if (record.isOutgoing()) {
        if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.PENDING || member.getDeliveryStatus() == RecipientDeliveryStatus.Status.UNKNOWN) {
          deliveryStatusView.setVisibility(View.GONE);
        } else if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.READ) {
          deliveryStatusView.setRead();
          deliveryStatusView.setVisibility(View.VISIBLE);
        } else if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.DELIVERED) {
          deliveryStatusView.setDelivered();
          deliveryStatusView.setVisibility(View.VISIBLE);
        } else if (member.getDeliveryStatus() == RecipientDeliveryStatus.Status.SENT) {
          deliveryStatusView.setSent();
          deliveryStatusView.setVisibility(View.VISIBLE);
        }
      } else {
        deliveryStatusView.setVisibility(View.GONE);
      }

      conflictButton.setVisibility(View.GONE);
    }

    errorDescription.setText(errorText);
    errorDescription.setVisibility(TextUtils.isEmpty(errorText) ? View.GONE : View.VISIBLE);
  }

  private NetworkFailure getNetworkFailure(final MessageRecord record) {
    if (record.hasNetworkFailures()) {
      for (final NetworkFailure failure : record.getNetworkFailures()) {
        if (failure.getRecipientId(getContext()).equals(member.getRecipient().getId())) {
          return failure;
        }
      }
    }
    return null;
  }

  private IdentityKeyMismatch getKeyMismatch(final MessageRecord record) {
    if (record.isIdentityMismatchFailure()) {
      for (final IdentityKeyMismatch mismatch : record.getIdentityKeyMismatches()) {
        if (mismatch.getRecipientId(getContext()).equals(member.getRecipient().getId())) {
          return mismatch;
        }
      }
    }
    return null;
  }

  public void unbind() {
    if (this.member != null && this.member.getRecipient() != null) this.member.getRecipient().live().removeForeverObserver(this);
  }

  @Override
  public void onRecipientChanged(@NonNull Recipient recipient) {
    if (this.member != null && this.member.getRecipient().equals(recipient)) {
      Log.d(TAG, "onRecipientChanged -- valid");
      fromView.setText(recipient);
      contactPhotoImage.setAvatar(glideRequests, recipient, false);
    } else {
      Log.d(TAG, "onRecipientChanged -- invalid");
    }
  }
}
