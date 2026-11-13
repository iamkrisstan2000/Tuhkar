/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Distinct progress tone with text-safe fill, including pastel-on-pastel Light mode. */
final class LiquidContrastStyle {
    static boolean light(int base){return LightSurfaceStyle.contrast(base,0xFF000000)>LightSurfaceStyle.contrast(base,0xFFFFFFFF);}
    static int edge(int base,int accent){
        boolean light=light(base);int target=light?0xFF10251B:0xFFF2FFF4;
        int tone=light?SurfacePalette.mix(accent,target,.58f):accent;
        for(int i=0;i<5&&LightSurfaceStyle.contrast(base,tone)<3;i++)tone=SurfacePalette.mix(tone,target,.18f);
        return tone;
    }
    static int fill(int base,int accent){
        boolean light=light(base);float amount=light?.28f:.22f;int text=light?0xFF151817:0xFFF2F4F2,tone=edge(base,accent);
        int fill=SurfacePalette.mix(base,tone,amount);
        while(amount>.04f&&LightSurfaceStyle.contrast(fill,text)<7){amount-=.02f;fill=SurfacePalette.mix(base,tone,amount);}
        return fill;
    }
}
