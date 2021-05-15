package com.RWdesenv.What.util;

import androidx.annotation.StyleRes;

import com.RWdesenv.What.R;

public class DynamicIntroTheme extends DynamicTheme {

  protected @StyleRes int getLightThemeStyle() {
    return R.style.TextSecure_LightIntroTheme;
  }

  protected @StyleRes int getDarkThemeStyle() {
    return R.style.TextSecure_DarkIntroTheme;
  }
}
