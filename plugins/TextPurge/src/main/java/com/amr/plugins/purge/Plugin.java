package com.amr.plugins.purge;

import android.content.Context;

import com.aliucord.Http;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.CommandsAPI;
import com.aliucord.wrappers.ChannelWrapper;
import com.discord.api.channel.Channel;
import com.discord.api.commands.ApplicationCommandType;
import com.discord.models.commands.ApplicationCommandOption;
import com.discord.models.user.User;
import com.discord.stores.StoreStream;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.json.JSONArray;
import org.json.JSONObject;

@AliucordPlugin
public class Plugin extends com.aliucord.entities.Plugin {
  private static final int DEFAULT_LIMIT = 20;

  private final Logger logger = new Logger("TextPurge");
  private final AtomicBoolean isRunning = new AtomicBoolean(false);

  @Override
  public void start(Context context) {
    List<ApplicationCommandOption> options = Arrays.asList(
      Utils.createCommandOption(ApplicationCommandType.USER,    "user",  "The user whose messages you want to delete", null, true, true),
      Utils.createCommandOption(ApplicationCommandType.INTEGER, "limit", "Max number of messages to delete (default: " + DEFAULT_LIMIT + ")", null, false, true),
      Utils.createCommandOption(ApplicationCommandType.BOOLEAN, "stop",  "Set to True to cancel an active purge", null, false, true)
    );

    commands.registerCommand("purge", "Bulk delete messages from a specific user", options,
      ctx -> {
        if (Boolean.TRUE.equals(ctx.getBool("stop"))) {
          isRunning.set(false);
          return new CommandsAPI.CommandResult("Stopping active purge...", null, false);
        }

        if (isRunning.get()) {
          return new CommandsAPI.CommandResult("A purge is already running. Use /purge stop:True to cancel.", null, false);
        }

        final User targetUser = ctx.getUser("user");
        if (targetUser == null) {
          return new CommandsAPI.CommandResult("Invalid user selected.", null, false);
        }

        final Long limitArg = ctx.getLong("limit");
        final int limit = (limitArg != null) ? limitArg.intValue() : DEFAULT_LIMIT;
        if (limit <= 0) {
          return new CommandsAPI.CommandResult("Limit must be greater than 0.", null, false);
        }

        final long channelId = ctx.getChannelId();
        final Channel channel = StoreStream.getChannels().getChannel(channelId);
        // 0 = DM / group DM (no guild)
        final long guildId = (channel != null) ? new ChannelWrapper(channel).getGuildId() : 0L;

        isRunning.set(true);

        Utils.threadPool.execute(() -> {
          try {
            purgeMessages(guildId, channelId, targetUser.getId(), limit);
          } catch (Exception e) {
            logger.error("Purge failed", e);
            Utils.showToast("Purge error: " + e.getMessage());
          } finally {
            isRunning.set(false);
          }
        });

        return new CommandsAPI.CommandResult(
          "Started deleting up to " + limit + " messages from " + targetUser.getUsername() + "...", null, false);
      }
    );
  }

  private void purgeMessages(long guildId, long channelId, long authorId, int limit) throws Exception {
    int deletedCount = 0;
    int offset = 0;
    final String authorIdStr = String.valueOf(authorId);

    while (isRunning.get() && deletedCount < limit) {
      final boolean isGuild = (guildId != 0);
      final Http.QueryBuilder searchUrl = 
          new Http.QueryBuilder((isGuild ? "/guilds/" + guildId : "/channels/" + channelId) + "/messages/search")
              .append("author_id", authorIdStr)
              .append("offset", String.valueOf(of fset));
      if(isGuild) searchUrl.append("channel_id", String.valueOf(channelId));

      Http.Response searchRes = Http.Request
        .newDiscordRequest(searchUrl)
        .execute();

      // 429 = rate limited, 202 = channel not indexed yet; wait and retry.
      if (searchRes.statusCode == 429 || searchRes.statusCode == 202) {
        sleepRetryAfter(searchRes, 5.0);
        continue;
      }

      if (searchRes.statusCode != 200) {
        Utils.showToast("Search failed (HTTP " + searchRes.statusCode + ")");
        break;
      }

      JSONArray messagesArray = new JSONObject(searchRes.text()).optJSONArray("messages");
      if (messagesArray == null || messagesArray.length() == 0) {
        break;
      }

      boolean foundInBatch = false;
      for (int i = 0; i < messagesArray.length(); i++) {
        if (!isRunning.get() || deletedCount >= limit) break;

        JSONArray msgGroup = messagesArray.getJSONArray(i);
        for (int j = 0; j < msgGroup.length(); j++) {
          if (!isRunning.get() || deletedCount >= limit) break;

          JSONObject msg = msgGroup.getJSONObject(j);
          if (msg.optBoolean("hit") && msg.getJSONObject("author").getString("id").equals(authorIdStr)) {
            foundInBatch = true;

            boolean deleted = deleteMessage(msg.getString("channel_id"), msg.getString("id"));
            if (deleted) {
              deletedCount++;
            } else {
              // undeletable message stays in search results, so skip past it.
              offset++;
            }
            // delay to reduce rate-limit hits
            Thread.sleep(1300);
          }
        }
      }

      if (!foundInBatch) {
        break;
      }
    }

    Utils.showToast("Purge complete: deleted " + deletedCount + " messages.");
  }

  private boolean deleteMessage(String channelId, String messageId) throws Exception {
    final String url = "/channels/" + channelId + "/messages/" + messageId;

    while (isRunning.get()) {
      Http.Response res = Http.Request
        .newDiscordRequest(url, "DELETE")
        .execute();

      final int code = res.statusCode;

      if (res.ok()) return true; // 204 on success

      if (code == 429) {
        sleepRetryAfter(res, 3.0);
        continue; // retry the same message
      }

      // 401 / 403 / 404 / anything else: can't delete this one, skip it
      logger.warn("Delete failed for " + messageId + " (HTTP " + code + ")");
      return false;
    }
    return false;
  }

  private void sleepRetryAfter(Http.Response res, double fallbackSeconds) throws InterruptedException {
    double wait = fallbackSeconds;
    try {
      wait = new JSONObject(res.text()).optDouble("retry_after", fallbackSeconds);
    } catch (Exception ignored) { /* not json */ }
    logger.warn("Waiting " + wait + "s (HTTP " + res.statusCode + ")");
    Thread.sleep((long) (wait * 1000) + 500);
  }

  @Override
  public void stop(Context context) {
    isRunning.set(false);
    commands.unregisterAll();
  }
}
