package com.RWdesenv.What.database.model;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.RWdesenv.What.contactshare.Contact;
import com.RWdesenv.What.database.documents.IdentityKeyMismatch;
import com.RWdesenv.What.database.documents.NetworkFailure;
import com.RWdesenv.What.linkpreview.LinkPreview;
import com.RWdesenv.What.mms.Slide;
import com.RWdesenv.What.mms.SlideDeck;
import com.RWdesenv.What.recipients.Recipient;

import java.util.LinkedList;
import java.util.List;

public abstract class MmsMessageRecord extends MessageRecord {

  private final @NonNull  SlideDeck         slideDeck;
  private final @Nullable Quote             quote;
  private final @NonNull  List<Contact>     contacts     = new LinkedList<>();
  private final @NonNull  List<LinkPreview> linkPreviews = new LinkedList<>();

  private final boolean viewOnce;

  MmsMessageRecord(long id, String body, Recipient conversationRecipient,
                   Recipient individualRecipient, int recipientDeviceId, long dateSent,
                   long dateReceived, long threadId, int deliveryStatus, int deliveryReceiptCount,
                   long type, List<IdentityKeyMismatch> mismatches,
                   List<NetworkFailure> networkFailures, int subscriptionId, long expiresIn,
                   long expireStarted, boolean viewOnce,
                   @NonNull SlideDeck slideDeck, int readReceiptCount,
                   @Nullable Quote quote, @NonNull List<Contact> contacts,
                   @NonNull List<LinkPreview> linkPreviews, boolean unidentified,
                   @NonNull List<ReactionRecord> reactions)
  {
    super(id, body, conversationRecipient, individualRecipient, recipientDeviceId, dateSent, dateReceived, threadId, deliveryStatus, deliveryReceiptCount, type, mismatches, networkFailures, subscriptionId, expiresIn, expireStarted, readReceiptCount, unidentified, reactions);

    this.slideDeck = slideDeck;
    this.quote     = quote;
    this.viewOnce  = viewOnce;

    this.contacts.addAll(contacts);
    this.linkPreviews.addAll(linkPreviews);
  }

  @Override
  public boolean isMms() {
    return true;
  }

  @NonNull
  public SlideDeck getSlideDeck() {
    return slideDeck;
  }

  @Override
  public boolean isMediaPending() {
    for (Slide slide : getSlideDeck().getSlides()) {
      if (slide.isInProgress() || slide.isPendingDownload()) {
        return true;
      }
    }

    return false;
  }

  @Override
  public boolean isViewOnce() {
    return viewOnce;
  }

  public boolean containsMediaSlide() {
    return slideDeck.containsMediaSlide();
  }

  public @Nullable Quote getQuote() {
    return quote;
  }

  public @NonNull List<Contact> getSharedContacts() {
    return contacts;
  }

  public @NonNull List<LinkPreview> getLinkPreviews() {
    return linkPreviews;
  }
}
