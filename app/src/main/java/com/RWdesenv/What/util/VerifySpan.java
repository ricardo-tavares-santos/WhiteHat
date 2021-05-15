package com.RWdesenv.What.util;

import android.content.Context;
import android.content.Intent;
import androidx.annotation.NonNull;
import android.text.style.ClickableSpan;
import android.view.View;

import com.RWdesenv.What.VerifyIdentityActivity;
import com.RWdesenv.What.crypto.IdentityKeyParcelable;
import com.RWdesenv.What.database.documents.IdentityKeyMismatch;
import com.RWdesenv.What.recipients.RecipientId;
import org.whispersystems.libsignal.IdentityKey;

public class VerifySpan extends ClickableSpan {

  private final Context     context;
  private final RecipientId recipientId;
  private final IdentityKey identityKey;

  public VerifySpan(@NonNull Context context, @NonNull IdentityKeyMismatch mismatch) {
    this.context     = context;
    this.recipientId = mismatch.getRecipientId(context);
    this.identityKey = mismatch.getIdentityKey();
  }

  public VerifySpan(@NonNull Context context, @NonNull RecipientId recipientId, @NonNull IdentityKey identityKey) {
    this.context     = context;
    this.recipientId = recipientId;
    this.identityKey = identityKey;
  }

  @Override
  public void onClick(@NonNull View widget) {
    Intent intent = new Intent(context, VerifyIdentityActivity.class);
    intent.putExtra(VerifyIdentityActivity.RECIPIENT_EXTRA, recipientId);
    intent.putExtra(VerifyIdentityActivity.IDENTITY_EXTRA, new IdentityKeyParcelable(identityKey));
    intent.putExtra(VerifyIdentityActivity.VERIFIED_EXTRA, false);
    context.startActivity(intent);
  }
}
