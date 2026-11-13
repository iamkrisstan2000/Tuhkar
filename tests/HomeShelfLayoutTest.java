/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public final class HomeShelfLayoutTest {
    public static void main(String[] args){
        for(int width:new int[]{240,320,360,411,600,800,1280,1600})for(boolean landscape:new boolean[]{false,true}){
            int tile=HomeShelfLayout.tileWidth(width,landscape);
            if(tile<120||tile>168)throw new AssertionError("Unbounded artwork size");
        }
        if(HomeShelfLayout.tileWidth(360,false)!=138)throw new AssertionError("Portrait peek");
        if(HomeShelfLayout.tileWidth(800,true)!=142)throw new AssertionError("Landscape column");
        System.out.println("PASS: bounded Home shelf sizes across compact, portrait, landscape and tablet widths");
    }
}
