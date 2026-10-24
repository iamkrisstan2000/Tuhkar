/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Readable accent-toned surfaces. OLED affects only the dark page background. */
final class SurfacePalette {
    final int page,card,inner,text,muted,line;
    private SurfacePalette(int page,int card,int inner,int text,int muted,int line){this.page=page;this.card=card;this.inner=inner;this.text=text;this.muted=muted;this.line=line;}
    static SurfacePalette of(boolean dark,boolean oled){
        if(dark)return new SurfacePalette(oled?0xFF000000:0xFF101713,0xFF1D2822,0xFF101713,0xFFE5EEE6,0xFFA3B4A8,0xFF334238);
        return new SurfacePalette(0xFFF5F8F4,0xFFFFFFFF,0xFFF5F8F4,0xFF15271E,0xFF5D7064,0xFFDFE8DF);
    }
    SurfacePalette tinted(int accent,boolean dark){
        int base=dark?0xFF14161A:0xFFFFFFFF;
        int card=mix(base,accent,dark?.22f:.18f),inner=mix(base,accent,dark?.11f:.08f);
        int text=dark?0xFFF2F4F2:0xFF151817;
        int muted=mix(text,card,.28f),line=mix(card,dark?0xFFFFFFFF:0xFF000000,dark?.16f:.18f);
        return new SurfacePalette(PageGradient.top(accent,dark,dark&&page==0xFF000000),card,inner,text,muted,line);
    }
    static int mix(int from,int to,float amount){
        float a=Math.max(0,Math.min(1,amount));int r=Math.round(((from>>16)&255)*(1-a)+((to>>16)&255)*a);int g=Math.round(((from>>8)&255)*(1-a)+((to>>8)&255)*a);int b=Math.round((from&255)*(1-a)+(to&255)*a);return 0xFF000000|(r<<16)|(g<<8)|b;
    }
}
