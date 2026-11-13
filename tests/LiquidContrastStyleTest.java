/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public class LiquidContrastStyleTest {
    public static void main(String[] args){int checks=0;for(boolean dark:new boolean[]{false,true})for(String style:AccentStyle.IDS)for(int device:new int[]{0xFFA0F2AE,0xFFD8EBCD,0xFF7DD9FF}){
        int accent=AccentStyle.base(style,device);SurfacePalette p=SurfacePalette.of(dark,false).tinted(accent,dark);
        int edge=LiquidContrastStyle.edge(p.card,accent),fill=LiquidContrastStyle.fill(p.card,accent);
        if(LightSurfaceStyle.contrast(edge,p.card)<2.5||LightSurfaceStyle.contrast(fill,p.card)<1.1||LightSurfaceStyle.contrast(fill,p.text)<7)throw new AssertionError(style+" dark="+dark+" fill contrast="+LightSurfaceStyle.contrast(fill,p.card)+" text="+LightSurfaceStyle.contrast(fill,p.text)+" edge="+LightSurfaceStyle.contrast(edge,p.card));checks++;
    }System.out.println("PASS: "+checks+" actual liquid contrast checks across ten palettes, both app modes and device accents: visible fill/edge and readable title text");}
}
