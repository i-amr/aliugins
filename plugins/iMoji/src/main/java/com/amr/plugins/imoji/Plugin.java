package com.amr.plugins.imoji;

import android.content.Context;
import android.content.res.Resources;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.patcher.PreHook;
import com.discord.utilities.images.MGImages;

@AliucordPlugin
public class Plugin extends com.aliucord.entities.Plugin {

  @Override
  public void start(Context ctx) throws Throwable {

    final Resources appResources = Utils.getAppContext().getResources();
    final String baseUrl = "https://raw.githubusercontent.com/i-amr/Apple-Emojis/main/72/";
    final String prefix = "res:///";

    patcher.patch(
        MGImages.class.getMethod(
            "getImageRequest",
            String.class,
            int.class,
            int.class,
            boolean.class), // Ljava/lang/String;IIZ
        new PreHook(
            param -> {
              Object o = param.args[0];

              if (o == null || !(o instanceof String)) return;

              String s = (String) o;

              if (!s.startsWith(prefix)) return;
              s = s.substring(prefix.length());

              try {
                final int resId = Integer.parseInt(s);
                final String emojiName = appResources.getResourceEntryName(resId);

                param.args[0] = baseUrl + emojiName.replace('_', '-') + ".png";
              } catch (NumberFormatException e) {
              } catch (Resources.NotFoundException e) {
              }
            }));
  }

  @Override
  public void stop(Context ctx) {
    patcher.unpatchAll();
  }
}
