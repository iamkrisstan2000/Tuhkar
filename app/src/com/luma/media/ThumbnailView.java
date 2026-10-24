/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.widget.ImageView;

/** Clips the entire thumbnail draw, including asynchronous/cached art and backgrounds. */
class ThumbnailView extends ImageView {
    private final Path circle=new Path();
    ThumbnailView(Context context){
        super(context);setScaleType(ScaleType.CENTER_CROP);
        // Do not depend on OEM outline clipping or drawable opacity for the circular mask.
        setClipToOutline(false);
    }
    @Override protected void onSizeChanged(int width,int height,int oldWidth,int oldHeight){
        super.onSizeChanged(width,height,oldWidth,oldHeight);
        circle.reset();
        if(width>0&&height>0)circle.addCircle(width/2f,height/2f,Math.min(width,height)/2f,Path.Direction.CW);
    }
    @Override public void draw(Canvas canvas){
        if(getWidth()<=0||getHeight()<=0)return;
        int save=canvas.save();
        try{canvas.clipPath(circle);super.draw(canvas);}finally{canvas.restoreToCount(save);}
    }
}
