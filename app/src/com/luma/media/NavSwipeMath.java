/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Navigation-only gesture policy. Section ends do not wrap. */
final class NavSwipeMath {
    static boolean horizontal(float dx,float dy,float slop){return Math.abs(dx)>slop&&Math.abs(dx)>Math.abs(dy)*1.35f;}
    static int direction(float dx,float dy,float threshold){return horizontal(dx,dy,Math.max(1,threshold))?(dx<0?1:-1):0;}
    static int target(int current,int direction){return Math.max(0,Math.min(4,current+Integer.signum(direction)));}
}
