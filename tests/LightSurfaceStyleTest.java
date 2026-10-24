/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

public final class LightSurfaceStyleTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        for(int r=0;r<=255;r+=17)for(int g=0;g<=255;g+=17)for(int b=0;b<=255;b+=17){
            int accent=0xFF000000|(r<<16)|(g<<8)|b;SurfacePalette p=SurfacePalette.of(false,false).tinted(accent,false);
            int fill=LightSurfaceStyle.selectorFill(p.card,accent,p.text,p.muted),stroke=LightSurfaceStyle.selectorStroke(accent,p.text);
            check(fill!=p.card,"light hover is distinct from card, not white-on-white");
            check(LightSurfaceStyle.contrast(fill,p.text)>=7,"hover title contrast");check(LightSurfaceStyle.contrast(fill,p.muted)>=4.5,"hover description contrast");
            check(LightSurfaceStyle.contrast(stroke,fill)>1.1,"accent outline differs from fill");
            int search=LightSurfaceStyle.searchFill(accent),border=LightSurfaceStyle.searchStroke(accent,p.text);
            check(LightSurfaceStyle.contrast(search,p.text)>=7&&LightSurfaceStyle.contrast(search,p.muted)>=4.5,"search text and hint readable");
            check(LightSurfaceStyle.contrast(search,border)>=1.15,"search border visible against field fill");
            int page=SurfacePalette.mix(PageGradient.top(accent,false,false),PageGradient.bottom(accent,false,false),.20f);
            check(LightSurfaceStyle.contrast(search,page)>1.05,"near-white field separates from adjusted light page near search height");
            check((fill>>>24)==255&&(search>>>24)==255,"opaque static surfaces");
        }
        int device=0xFFD1E5FF;SurfacePalette p=SurfacePalette.of(false,false).tinted(device,false);
        int fill=LightSurfaceStyle.selectorFill(p.card,device,p.text,p.muted);
        check((fill&255)>((fill>>16)&255),"pale device blue hover remains blue-tinted");
        System.out.println("PASS: "+checks+" light-surface checks across 4096 accents: distinct hover/outline, readable row/search text, subtle field/page separation and pale Device blue tint");
    }
}
