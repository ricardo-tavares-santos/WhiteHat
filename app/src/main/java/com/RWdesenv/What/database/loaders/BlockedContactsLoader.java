package com.RWdesenv.What.database.loaders;

import android.content.Context;
import android.database.Cursor;

import com.RWdesenv.What.database.DatabaseFactory;
import com.RWdesenv.What.util.AbstractCursorLoader;

public class BlockedContactsLoader extends AbstractCursorLoader {

  public BlockedContactsLoader(Context context) {
    super(context);
  }

  @Override
  public Cursor getCursor() {
    return DatabaseFactory.getRecipientDatabase(getContext()).getBlocked();
  }

}
