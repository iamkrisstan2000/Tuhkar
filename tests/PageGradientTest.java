/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

public final class PageGradientTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    static double channel(int c){double v=c/255.;return v<=.04045?v/12.92:Math.pow((v+.055)/1.055,2.4);}
    static double luminance(int c){return .2126*channel((c>>16)&255)+.7152*channel((c>>8)&255)+.0722*channel(c&255);}
    static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    public static void main(String[] args){
        for(int r=0;r<=255;r+=17)for(int g=0;g<=255;g+=17)for(int b=0;b<=255;b+=17){
            int accent=0xFF000000|(r<<16)|(g<<8)|b;
            for(boolean dark:new boolean[]{false,true}){
                int top=PageGradient.top(accent,dark,false),bottom=PageGradient.bottom(accent,dark,false);SurfacePalette palette=SurfacePalette.of(dark,false).tinted(accent,dark);
                check(palette.page==top,"palette page equals accent gradient top");
                check(luminance(bottom)<luminance(top),"bottom darker than top in both themes");
                check((top>>>24)==255&&(bottom>>>24)==255,"opaque stable gradient endpoints");
                check(dark?luminance(bottom)<.004:luminance(bottom)>.55,"dark bottom near black / light mode remains light");
                for(int step=0;step<=10;step++){
                    int color=SurfacePalette.mix(top,bottom,step/10f);
                    check(contrast(color,palette.text)>=7,"page title contrast at least 7:1");check(contrast(color,palette.muted)>=4.5,"page description contrast at least 4.5:1");
                }
                int oledTop=PageGradient.top(accent,dark,true),oledBottom=PageGradient.bottom(accent,dark,true);
                check(dark?oledTop==0xFF000000&&oledBottom==0xFF000000:oledTop==top&&oledBottom==bottom,"OLED is pure black only in dark mode");
            }
        }
        int red=PageGradient.top(0xFFFF0000,true,false),blue=PageGradient.top(0xFF0000FF,true,false);
        check(((red>>16)&255)>(red&255)&&(blue&255)>((blue>>16)&255),"page shade actually changes red/blue, not fixed green");
        int stable=AccentStyle.base("rgb",0);check(PageGradient.top(stable,true,false)==PageGradient.top(AccentStyle.base("rgb",123),true,false),"RGB page uses stable base, not wave or supplied device accent");
        System.out.println("PASS: "+checks+" page-gradient checks: accent-dependent endpoints, darker bottom, light theme preservation, OLED black and text contrast across 4096 accents");
    }
}
