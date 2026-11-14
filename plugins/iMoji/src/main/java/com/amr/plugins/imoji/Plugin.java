package com.amr.plugins.imoji;

import android.content.Context;
import android.content.res.Resources;
import com.aliucord.Constants;
import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.patcher.PreHook;
import com.discord.utilities.images.MGImages;
import java.io.File;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@AliucordPlugin
public class Plugin extends com.aliucord.entities.Plugin {
  private static final String CACHE_DIR = "emojis";
  private final Set<String> downloading = ConcurrentHashMap.newKeySet();

  @Override
  public void start(Context ctx) throws Throwable {
    final Resources appResources = Utils.getAppContext().getResources();
    final String baseUrl = "https://raw.githubusercontent.com/i-amr/Apple-Emojis/main/72/";
    final String prefix = "res:///";

    final File cacheDir = new File(Constants.BASE_PATH, CACHE_DIR);
    if (!cacheDir.exists()) {
      cacheDir.mkdirs();
    }

    patcher.patch(
        MGImages.class.getMethod(
            "getImageRequest",
            String.class,
            int.class,
            int.class,
            boolean.class),
        new PreHook(
            param -> {
              Object o = param.args[0];
              if (!(o instanceof String)) return;

              String s = (String) o;
              if (!s.startsWith(prefix)) return;

              try {
                final int resId = Integer.parseInt(s.substring(prefix.length()));
                final String emojiName = appResources.getResourceEntryName(resId);
                if (!emojiName.startsWith("emoji_")) return;

                final String fileName = emojiName.replace('_', '-') + ".png";
                final File cachedFile = new File(cacheDir, fileName);

                if (cachedFile.exists() && cachedFile.length() > 0) {
                  param.args[0] = "file:///" + cachedFile.getAbsolutePath();
                } else {
                  final String remoteUrl = baseUrl + fileName;
                  param.args[0] = remoteUrl;
                  cacheAsync(remoteUrl, cachedFile, fileName);
                }
              } catch (NumberFormatException | Resources.NotFoundException ignored) {
              }
            }));
  }

  private void cacheAsync(String url, File targetFile, String key) {
    if (!downloading.add(key)) return;

    Utils.threadPool.execute(
        () -> {
          File tempFile = new File(targetFile.getParentFile(), key + ".tmp");
          try {
            Http.simpleDownload(url, tempFile);
            if (tempFile.exists() && tempFile.length() > 0) {
              tempFile.renameTo(targetFile);
            }
          } catch (Throwable ignored) {
            if (tempFile.exists()) tempFile.delete();
          } finally {
            downloading.remove(key);
          }
        });
  }

  @Override
  public void stop(Context ctx) {
    patcher.unpatchAll();
  }
}
