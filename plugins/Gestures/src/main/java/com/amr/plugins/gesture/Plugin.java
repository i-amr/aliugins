package com.amr.plugins.gesture;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.lytefast.flexinput.fragment.FlexInputFragment;
import com.lytefast.flexinput.widget.FlexEditText;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import rx.functions.Action1;

@AliucordPlugin
public class Plugin extends com.aliucord.entities.Plugin {

  public void start(Context ctx) throws NoSuchMethodException {
    final ClipboardManager clipboard = ctx.getSystemService(ClipboardManager.class);
    patcher.patch(
        FlexInputFragment.class.getDeclaredMethod("onViewCreated", View.class, Bundle.class),
        new Action1<XC_MethodHook.MethodHookParam>() {
          public void call(MethodHookParam cf) {
            final FlexInputFragment input = (FlexInputFragment) cf.thisObject;
            final FlexEditText textInput = input.getView().findViewById(Utils.getResId("text_input", "id"));

            textInput.setOnTouchListener(
                new OnSwipeListener(ctx) {
                  public void onSwipeLeft() {
                    super.onSwipeLeft();
                    textInput.setText("");
                  }

                  public void onSwipeRight() {
                    super.onSwipeRight();
                    if (clipboard == null || !clipboard.hasPrimaryClip()) { return; }

                    ClipDescription description = clipboard.getPrimaryClipDescription();
                    if (description == null || !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) { return; }

                    ClipData clipData = clipboard.getPrimaryClip();
                    if (clipData != null && clipData.getItemCount() > 0) {
                      CharSequence text = clipData.getItemAt(0).coerceToText(ctx);
                      if (text != null) textInput.append(text.toString());
                    }
                  }
                });
          }
        });
  }

  public void stop(Context ctx) {
    patcher.unpatchAll();
  }
}
