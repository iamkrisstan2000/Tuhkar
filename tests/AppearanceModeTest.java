/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public class AppearanceModeTest {
    public static void main(String[] args){int checks=0;for(String mode:new String[]{null,"","system","light","dark","old","DARK"})for(boolean system:new boolean[]{false,true}){
        boolean expected="dark".equals(mode)||(!"light".equals(mode)&&!"dark".equals(mode)&&system);
        if(AppearanceMode.dark(mode,system)!=expected)throw new AssertionError(mode);checks++;
    }System.out.println("PASS: "+checks+" actual appearance overrides and System/default/invalid preference combinations");}
}
