/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
/** Overlay geometry remains within the available inset/IME-safe area. */
final class PortraitSearchLayout {
    static int width(int available,int maximum){return Math.max(1,Math.min(available,maximum));}
    static int contentHeight(int available,int maximum,int field,int status,int row,int count,boolean results){
        long desired=Math.max(0,field)+(results?(long)Math.max(0,status)+(long)Math.max(0,row)*Math.max(0,count):0);
        return (int)Math.max(1,Math.min(Math.max(1,available),Math.min(Math.max(1,maximum),desired)));
    }
}
