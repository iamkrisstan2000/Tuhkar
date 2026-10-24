/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

public final class PaletteToneTest {
    private static int assertions;
    private static void require(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);}
    private static double channel(int c){double value=c/255.0;return value<=.04045?value/12.92:Math.pow((value+.055)/1.055,2.4);}
    private static double luminance(int c){return .2126*channel((c>>16)&255)+.7152*channel((c>>8)&255)+.0722*channel(c&255);}
    private static double contrast(int a,int b){double x=luminance(a),y=luminance(b);return (Math.max(x,y)+.05)/(Math.min(x,y)+.05);}
    public static void main(String[] args){
        int accents=0;double minimumText=100,minimumMuted=100;
        for(int r=0;r<=255;r+=17)for(int g=0;g<=255;g+=17)for(int b=0;b<=255;b+=17){
            int accent=0xFF000000|(r<<16)|(g<<8)|b;accents++;
            for(boolean dark:new boolean[]{false,true}){
                SurfacePalette plain=SurfacePalette.of(dark,false).tinted(accent,dark),oled=SurfacePalette.of(dark,true).tinted(accent,dark);
                require(plain.card==oled.card&&plain.inner==oled.inner&&plain.text==oled.text&&plain.muted==oled.muted&&plain.line==oled.line,"OLED keeps the same accent-toned UI");
                require(oled.page==(dark?0xFF000000:plain.page),"OLED changes only the dark page");
                require((plain.card>>>24)==255&&(plain.inner>>>24)==255,"Tonal surfaces are opaque");
                require(plain.page==PageGradient.top(accent,dark,false),"Page follows the stable selected accent");
                double text=contrast(plain.text,plain.card),muted=contrast(plain.muted,plain.card);minimumText=Math.min(minimumText,text);minimumMuted=Math.min(minimumMuted,muted);
                require(text>=7,"Primary text contrast is at least 7:1");require(muted>=4.5,"Description contrast is at least 4.5:1");
                require(contrast(plain.text,plain.inner)>=7&&contrast(plain.muted,plain.inner)>=4.5,"Inner-surface text stays readable");
                double lum=luminance(accent);int ink=lum>.179?0xFF000000:0xFFFFFFFF;require(contrast(accent,ink)>=4.5,"Solid accent buttons use readable foregrounds");
            }
        }
        SurfacePalette red=SurfacePalette.of(false,false).tinted(0xFFFF0000,false),blue=SurfacePalette.of(false,false).tinted(0xFF0000FF,false);
        require(((red.card>>16)&255)>(red.card&255),"Red accent makes red-toned cards");require((blue.card&255)>((blue.card>>16)&255),"Blue accent makes blue-toned cards");
        require(SurfacePalette.mix(0xFF000000,0xFFFFFFFF,0)==0xFF000000&&SurfacePalette.mix(0xFF000000,0xFFFFFFFF,1)==0xFFFFFFFF,"Blend endpoints");
        System.out.printf(java.util.Locale.ROOT,"PASS: %,d palette assertions across %,d accents and both themes; minimum primary text %.2f:1, descriptions %.2f:1; OLED page-only behavior and accent hue propagation.%n",assertions,accents,minimumText,minimumMuted);
    }
}
