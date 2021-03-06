package com.RWdesenv.What.util;

import androidx.annotation.StyleRes;

import com.RWdesenv.What.R;

public class DynamicDarkActionBarTheme extends DynamicTheme {

  protected @StyleRes int getLightThemeStyle() {
    return R.style.TextSecure_LightTheme_Conversation;
  }

  protected @StyleRes int getDarkThemeStyle() {
    return R.style.TextSecure_DarkTheme_Conversation;
  }
}
