/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Small bounded meniscus; liquid always starts empty and ends completely full. */
final class LiquidFlowMath {
    static float edge(float progress,float y,float height,float width,float density,float phase){
        float p=Float.isNaN(progress)?0:Math.max(0,Math.min(1,progress));
        if(p==0||p==1)return Math.max(0,width)*p;
        float amplitude=(float)Math.sin(Math.PI*p)*Math.max(0,density)*3;
        float bend=(float)(Math.sin(y/Math.max(1,height)*Math.PI*2-phase)*.7+Math.sin(y/Math.max(1,height)*Math.PI*4+phase)*.3);
        return Math.max(0,Math.min(Math.max(0,width),width*p+amplitude*bend));
    }
}
