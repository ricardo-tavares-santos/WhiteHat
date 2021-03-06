package com.RWdesenv.What;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;

import com.RWdesenv.What.database.model.MessageRecord;
import com.RWdesenv.What.mms.GlideRequests;
import com.RWdesenv.What.recipients.Recipient;
import com.RWdesenv.What.recipients.RecipientId;
import com.RWdesenv.What.util.Conversions;
import com.RWdesenv.What.util.adapter.StableIdGenerator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

class MessageDetailsRecipientAdapter extends BaseAdapter implements AbsListView.RecyclerListener {

  private final Context                        context;
  private final GlideRequests                  glideRequests;
  private final MessageRecord                  record;
  private final List<RecipientDeliveryStatus>  members;
  private final boolean                        isPushGroup;
  private final StableIdGenerator<RecipientId> idGenerator;

  MessageDetailsRecipientAdapter(@NonNull Context context, @NonNull GlideRequests glideRequests,
                                 @NonNull MessageRecord record, @NonNull List<RecipientDeliveryStatus> members,
                                 boolean isPushGroup)
  {
    this.context       = context;
    this.glideRequests = glideRequests;
    this.record        = record;
    this.isPushGroup   = isPushGroup;
    this.members       = members;
    this.idGenerator   = new StableIdGenerator<>();
  }

  @Override
  public int getCount() {
    return members.size();
  }

  @Override
  public Object getItem(int position) {
    return members.get(position);
  }

  @Override
  public long getItemId(int position) {
    return idGenerator.getId(members.get(position).recipient.getId());
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (convertView == null) {
      convertView = LayoutInflater.from(context).inflate(R.layout.message_recipient_list_item, parent, false);
    }

    RecipientDeliveryStatus member = members.get(position);

    ((MessageRecipientListItem)convertView).set(glideRequests, record, member, isPushGroup);
    return convertView;
  }

  @Override
  public void onMovedToScrapHeap(View view) {
    ((MessageRecipientListItem)view).unbind();
  }


  static class RecipientDeliveryStatus {

    enum Status {
      UNKNOWN, PENDING, SENT, DELIVERED, READ
    }

    private final Recipient recipient;
    private final Status    deliveryStatus;
    private final boolean   isUnidentified;
    private final long      timestamp;

    RecipientDeliveryStatus(Recipient recipient, Status deliveryStatus, boolean isUnidentified, long timestamp) {
      this.recipient      = recipient;
      this.deliveryStatus = deliveryStatus;
      this.isUnidentified = isUnidentified;
      this.timestamp      = timestamp;
    }

    Status getDeliveryStatus() {
      return deliveryStatus;
    }

    boolean isUnidentified() {
      return isUnidentified;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public Recipient getRecipient() {
      return recipient;
    }

  }

}
