/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Visible light-mode feedback without sacrificing row text contrast. */
final class LightSurfaceStyle {
    static int selectorStroke(int accent,int text){return SurfacePalette.mix(accent,text,.20f);}
    static int selectorFill(int card,int accent,int text,int muted){
        int tone=selectorStroke(accent,text);
        for(int step=15;step>=0;step--){
            int fill=SurfacePalette.mix(card,tone,step*.02f);
            if(contrast(fill,text)>=7&&contrast(fill,muted)>=4.5)return fill;
        }
        return card;
    }
    static int searchFill(int accent){return SurfacePalette.mix(0xFFFFFFFF,accent,.04f);}
    static int searchStroke(int accent,int text){return SurfacePalette.mix(searchFill(accent),selectorStroke(accent,text),.55f);}
    private static double channel(int c){double v=c/255.;return v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4);}
    private static double luminance(int c){return .2126*channel((c>>16)&255)+.7152*channel((c>>8)&255)+.0722*channel(c&255);}
    static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
}
