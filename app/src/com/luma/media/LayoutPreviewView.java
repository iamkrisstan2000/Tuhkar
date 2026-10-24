/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

/** Static, allocation-free layout sketches; no media scan, bitmaps or animation. */
final class LayoutPreviewView extends View {
    private final Paint paint=new Paint(3);
    private final int mode,accent,muted;
    LayoutPreviewView(Context context,int mode,int accent,int muted){super(context);this.mode=mode;this.accent=accent;this.muted=muted;}
    private void color(int color,int alpha){paint.setColor(color);paint.setAlpha(alpha);paint.setStyle(Paint.Style.FILL);}
    private void cover(Canvas c,float x,float y,float radius){color(accent,150);c.drawCircle(x,y,radius,paint);color(accent,240);c.drawCircle(x,y,radius*.36f,paint);}
    private void line(Canvas c,float x,float y,float width){color(muted,135);c.drawRoundRect(x,y,x+width,y+2,1,1,paint);}
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);if(getWidth()<=0||getHeight()<=0)return;
        float scale=Math.min(getWidth()/160f,getHeight()/92f);int save=c.save();c.translate((getWidth()-160*scale)/2,(getHeight()-92*scale)/2);c.scale(scale,scale);
        if(mode==0){
            color(muted,50);c.drawCircle(25,40,21,paint);c.drawCircle(135,40,21,paint);
            cover(c,80,38,31);color(muted,130);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);c.drawCircle(80,38,22,paint);paint.setStyle(Paint.Style.FILL);
            line(c,60,76,40);for(int i=0;i<3;i++){color(i==1?accent:muted,i==1?255:80);c.drawCircle(72+i*8,86,2,paint);}
        }else if(mode==1){
            for(int row=0;row<6;row++)for(int col=0;col<4;col++){float x=26+col*36,y=7+row*15;cover(c,x,y,4.5f);line(c,x-9,y+6,18);}
        }else if(mode==2){
            for(int row=0;row<3;row++)for(int col=0;col<3;col++){float x=28+col*52,y=11+row*30;cover(c,x,y,9);line(c,x-14,y+12,28);}
        }else{
            for(int row=0;row<5;row++){float y=9+row*18;cover(c,18,y,7);line(c,34,y-3,86-row%2*20);line(c,34,y+3,56);color(muted,85);c.drawCircle(144,y,1.5f,paint);}
        }
        c.restoreToCount(save);
    }
}
