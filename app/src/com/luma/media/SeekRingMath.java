/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Pure, clockwise seek geometry. Twelve o'clock is zero. */
final class SeekRingMath {
    static float fraction(float x,float y,float width,float height){
        double angle=Math.atan2(y-height/2f,x-width/2f)+Math.PI/2;
        if(angle<0)angle+=Math.PI*2;
        return clamp((float)(angle/(Math.PI*2)));
    }
    static float clamp(float value){return Float.isNaN(value)?0:Math.max(0,Math.min(1,value));}
    static float drag(float previous,float value){
        value=clamp(value);previous=clamp(previous);
        if(previous>.75f&&value<.25f)return 1;
        if(previous<.25f&&value>.75f)return 0;
        return value;
    }
    static boolean ring(float x,float y,float width,float height,float inner,float allowance){
        double distance=Math.hypot(x-width/2f,y-height/2f);
        return distance>=inner&&distance<=Math.min(width,height)/2f+allowance;
    }
    static long position(float fraction,long duration){return duration<=0?0:Math.min(duration,Math.max(0,Math.round((double)clamp(fraction)*duration)));}
}
