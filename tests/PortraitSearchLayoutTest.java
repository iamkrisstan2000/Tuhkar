/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public final class PortraitSearchLayoutTest {
 public static void main(String[] args){int checks=0;for(int available=1;available<2000;available+=7)for(boolean results:new boolean[]{false,true}){int height=PortraitSearchLayout.contentHeight(available,520,70,40,48,100,results);if(height<1||height>available||height>(results?520:70))throw new AssertionError("Out of bounds");checks++;}if(PortraitSearchLayout.contentHeight(900,520,70,40,48,100,true)!=520||PortraitSearchLayout.contentHeight(120,520,70,40,48,100,true)!=120||PortraitSearchLayout.contentHeight(900,520,70,40,48,0,false)!=70)throw new AssertionError("IME/cap/field sizing");for(int count=0;count<100;count++){int h=PortraitSearchLayout.contentHeight(900,520,70,40,48,count,true);if(h!=Math.min(520,110+48*count))throw new AssertionError("Content sizing");checks++;}
 if(PortraitSearchLayout.contentHeight(900,520,70,40,48,1,true)>=PortraitSearchLayout.contentHeight(900,520,70,40,48,3,true))throw new AssertionError("Few results must shrink");
 if(PortraitSearchLayout.contentHeight(100,520,70,40,48,100,true)!=100||PortraitSearchLayout.contentHeight(900,520,8,40,48,1,true)!=96||PortraitSearchLayout.contentHeight(900,520,70,40,48,0,false)!=70)throw new AssertionError("IME/hidden field/empty query");
 if(PortraitSearchLayout.contentHeight(900,520,70,40,Integer.MAX_VALUE,Integer.MAX_VALUE,true)!=520)throw new AssertionError("Overflow");
 for(int width=1;width<2500;width+=13){int bounded=PortraitSearchLayout.width(width,720);if(bounded!=Math.min(width,720))throw new AssertionError("Landscape width cap");checks++;}
 System.out.println("PASS: "+checks+" portrait search dropdown sizes, keyboard-constrained height, field-only size and maximum cap");}
}
