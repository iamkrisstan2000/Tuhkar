/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Stable accent shading; RGB waves do not continuously repaint the page. */
final class PageGradient {
    static int top(int accent,boolean dark,boolean oled){
        if(dark&&oled)return 0xFF000000;
        return SurfacePalette.mix(dark?0xFF101114:0xFFF0F2F6,accent,dark?.16f:.08f);
    }
    static int bottom(int accent,boolean dark,boolean oled){
        if(dark&&oled)return 0xFF000000;
        return SurfacePalette.mix(dark?0xFF030405:0xFFE6E8EC,accent,dark?.02f:.12f);
    }
}
