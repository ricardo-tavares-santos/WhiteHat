package com.RWdesenv.What.util;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;

import com.RWdesenv.What.database.model.MediaMmsMessageRecord;
import com.RWdesenv.What.database.model.MessageRecord;
import com.RWdesenv.What.database.model.MmsMessageRecord;
import com.RWdesenv.What.mms.Slide;

public final class MessageRecordUtil {

  private MessageRecordUtil() {
  }

  public static boolean isMediaMessage(@NonNull MessageRecord messageRecord) {
    return messageRecord.isMms()                                    &&
        !messageRecord.isMmsNotification()                          &&
        ((MediaMmsMessageRecord)messageRecord).containsMediaSlide() &&
        ((MediaMmsMessageRecord)messageRecord).getSlideDeck().getStickerSlide() == null;
  }

  public static boolean hasSticker(@NonNull MessageRecord messageRecord) {
    return messageRecord.isMms() && ((MmsMessageRecord)messageRecord).getSlideDeck().getStickerSlide() != null;
  }

  public static boolean hasSharedContact(@NonNull MessageRecord messageRecord) {
    return messageRecord.isMms() && !((MmsMessageRecord)messageRecord).getSharedContacts().isEmpty();
  }

  public static boolean hasLocation(@NonNull MessageRecord messageRecord) {
    return messageRecord.isMms() && Stream.of(((MmsMessageRecord) messageRecord).getSlideDeck().getSlides())
                                          .anyMatch(Slide::hasLocation);
  }
}
