package com.RWdesenv.What.contacts;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.RWdesenv.What.R;
import com.RWdesenv.What.components.AvatarImageView;
import com.RWdesenv.What.components.FromTextView;
import com.RWdesenv.What.groups.GroupId;
import com.RWdesenv.What.mms.GlideRequests;
import com.RWdesenv.What.recipients.LiveRecipient;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientForeverObserver;
import com.RWdesenv.What.recipients.RecipientId;
import com.RWdesenv.What.util.ViewUtil;
import org.whispersystems.libsignal.util.guava.Optional;

public class ContactSelectionListItem extends LinearLayout implements RecipientForeverObserver {

  @SuppressWarnings("unused")
  private static final String TAG = ContactSelectionListItem.class.getSimpleName();

  private AvatarImageView contactPhotoImage;
  private TextView        numberView;
  private FromTextView    nameView;
  private TextView        labelView;
  private CheckBox        checkBox;

  private String        number;
  private int           contactType;
  private LiveRecipient recipient;
  private GlideRequests glideRequests;

  public ContactSelectionListItem(Context context) {
    super(context);
  }

  public ContactSelectionListItem(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    this.contactPhotoImage = findViewById(R.id.contact_photo_image);
    this.numberView        = findViewById(R.id.number);
    this.labelView         = findViewById(R.id.label);
    this.nameView          = findViewById(R.id.name);
    this.checkBox          = findViewById(R.id.check_box);

    ViewUtil.setTextViewGravityStart(this.nameView, getContext());
  }

  public void set(@NonNull GlideRequests glideRequests,
                  @Nullable RecipientId recipientId,
                  int type,
                  String name,
                  String number,
                  String label,
                  int color,
                  boolean multiSelect)
  {
    this.glideRequests = glideRequests;
    this.number        = number;
    this.contactType   = type;

    if (type == ContactRepository.NEW_PHONE_TYPE || type == ContactRepository.NEW_USERNAME_TYPE) {
      this.recipient = null;
      this.contactPhotoImage.setAvatar(glideRequests, null, false);
    } else if (recipientId != null) {
      this.recipient = Recipient.live(recipientId);
      this.recipient.observeForever(this);
      name = this.recipient.get().getDisplayName(getContext());
    }

    Recipient recipientSnapshot = recipient != null ? recipient.get() : null;

    this.nameView.setTextColor(color);
    this.numberView.setTextColor(color);
    this.contactPhotoImage.setAvatar(glideRequests, recipientSnapshot, false);

    setText(recipientSnapshot, type, name, number, label);

    if (multiSelect) this.checkBox.setVisibility(View.VISIBLE);
    else             this.checkBox.setVisibility(View.GONE);
  }

  public void setChecked(boolean selected) {
    this.checkBox.setChecked(selected);
  }

  public void unbind(GlideRequests glideRequests) {
    if (recipient != null) {
      recipient.removeForeverObserver(this);
      recipient = null;
    }
  }

  @SuppressLint("SetTextI18n")
  private void setText(@Nullable Recipient recipient, int type, String name, String number, String label) {
    if (number == null || number.isEmpty() || GroupId.isEncodedGroup(number)) {
      this.nameView.setEnabled(false);
      this.numberView.setText("");
      this.labelView.setVisibility(View.GONE);
    } else if (type == ContactRepository.PUSH_TYPE) {
      this.numberView.setText(number);
      this.nameView.setEnabled(true);
      this.labelView.setVisibility(View.GONE);
    } else if (type == ContactRepository.NEW_USERNAME_TYPE) {
      this.numberView.setText("@" + number);
      this.nameView.setEnabled(true);
      this.labelView.setText(label);
      this.labelView.setVisibility(View.VISIBLE);
    } else {
      this.numberView.setText(number);
      this.nameView.setEnabled(true);
      this.labelView.setText(label != null && !label.equals("null") ? label : "");
      this.labelView.setVisibility(View.VISIBLE);
    }

    if (recipient != null) {
      this.nameView.setText(recipient);
    } else {
      this.nameView.setText(name);
    }
  }

  public String getNumber() {
    return number;
  }

  public boolean isUsernameType() {
    return contactType == ContactRepository.NEW_USERNAME_TYPE;
  }

  public Optional<RecipientId> getRecipientId() {
    return recipient != null ? Optional.of(recipient.getId()) : Optional.absent();
  }

  @Override
  public void onRecipientChanged(@NonNull Recipient recipient) {
    contactPhotoImage.setAvatar(glideRequests, recipient, false);
    nameView.setText(recipient);
  }
}
