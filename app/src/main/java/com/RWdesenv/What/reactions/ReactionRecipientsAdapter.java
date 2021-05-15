package com.RWdesenv.What.reactions;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.RWdesenv.What.R;
import com.RWdesenv.What.components.AvatarImageView;
import com.RWdesenv.What.mms.GlideApp;
import com.RWdesenv.What.reactions.ReactionsLoader.Reaction;
import com.RWdesenv.What.util.AvatarUtil;

import java.util.Collections;
import java.util.List;

final class ReactionRecipientsAdapter extends RecyclerView.Adapter<ReactionRecipientsAdapter.ViewHolder> {

  private List<Reaction> data = Collections.emptyList();

  public void updateData(List<Reaction> newData) {
    data = newData;
    notifyDataSetChanged();
  }

  @Override
  public @NonNull ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new ViewHolder(LayoutInflater.from(parent.getContext())
                                        .inflate(R.layout.reactions_bottom_sheet_dialog_fragment_recipient_item,
                                                 parent,
                                                 false));
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.bind(data.get(position));
  }

  @Override
  public int getItemCount() {
    return data.size();
  }

  final class ViewHolder extends RecyclerView.ViewHolder {

    private final AvatarImageView avatar;
    private final TextView        recipient;
    private final TextView        emoji;

    public ViewHolder(@NonNull View itemView) {
      super(itemView);

      avatar    = itemView.findViewById(R.id.reactions_bottom_view_recipient_avatar);
      recipient = itemView.findViewById(R.id.reactions_bottom_view_recipient_name);
      emoji     = itemView.findViewById(R.id.reactions_bottom_view_recipient_emoji);
    }

    void bind(@NonNull Reaction reaction) {
      this.emoji.setText(reaction.getEmoji());

      if (reaction.getSender().isLocalNumber()) {
        this.recipient.setText(R.string.ReactionsRecipientAdapter_you);
        this.avatar.setAvatar(GlideApp.with(avatar), null, false);
        AvatarUtil.loadIconIntoImageView(reaction.getSender(), avatar);
      } else {
        this.recipient.setText(reaction.getSender().getDisplayName(itemView.getContext()));
        this.avatar.setAvatar(GlideApp.with(avatar), reaction.getSender(), false);
      }
    }
  }

}
