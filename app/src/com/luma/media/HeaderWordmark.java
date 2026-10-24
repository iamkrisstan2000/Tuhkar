/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Local transparent wordmark, left aligned and vertically centered in its header slot. */
final class HeaderWordmark extends View {
    private final Drawable artwork;
    HeaderWordmark(Context context){super(context);artwork=context.getDrawable(R.drawable.tuhkar_wordmark);setContentDescription("TUHKAR — wooden lettering with an anahaw leaf");}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);int w=getWidth()-getPaddingLeft()-getPaddingRight(),h=getHeight()-getPaddingTop()-getPaddingBottom();if(w<=0||h<=0)return;
        float scale=Math.min((float)w/artwork.getIntrinsicWidth(),(float)h/artwork.getIntrinsicHeight());int width=Math.round(artwork.getIntrinsicWidth()*scale),height=Math.round(artwork.getIntrinsicHeight()*scale);int left=getPaddingLeft(),top=getPaddingTop()+(h-height)/2;artwork.setBounds(left,top,left+width,top+height);artwork.draw(canvas);
    }
}
