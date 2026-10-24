/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** One coordinate system for the circle and skip glyph; independent of Button text layout. */
final class SkipButtonDrawable extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final FluentIcon glyph;
    private final float density;
    private final int circleColor,iconColor;
    private boolean pressed;
    private int alpha=255;
    SkipButtonDrawable(boolean previous,int circleColor,int iconColor,float density){this.circleColor=circleColor;this.iconColor=iconColor;this.density=density;glyph=new FluentIcon(previous?6:7,iconColor);}
    @Override public void draw(Canvas canvas){
        Rect b=getBounds();float cx=b.exactCenterX(),cy=b.exactCenterY();float radius=Math.max(0,Math.min(b.width(),b.height())/2f-8*density);
        paint.setStyle(Paint.Style.FILL);paint.setColor(circleColor);paint.setAlpha(Math.round(Color.alpha(circleColor)*alpha/255f));canvas.drawCircle(cx,cy,radius,paint);
        if(circleColor==Color.BLACK){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density);paint.setColor(iconColor);paint.setAlpha(Math.round(65*alpha/255f));canvas.drawCircle(cx,cy,Math.max(0,radius-density/2),paint);}
        if(pressed){paint.setStyle(Paint.Style.FILL);paint.setColor(iconColor);paint.setAlpha(Math.round(35*alpha/255f));canvas.drawCircle(cx,cy,radius,paint);}
        int half=Math.round(10*density);int save=canvas.save();canvas.translate(cx,cy);glyph.setBounds(-half,-half,half,half);glyph.setAlpha(alpha);glyph.draw(canvas);canvas.restoreToCount(save);
    }
    @Override public boolean isStateful(){return true;}
    @Override protected boolean onStateChange(int[] states){boolean next=false;for(int state:states)if(state==android.R.attr.state_pressed)next=true;if(next!=pressed){pressed=next;invalidateSelf();return true;}return false;}
    @Override public void setAlpha(int alpha){this.alpha=alpha;invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);glyph.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
