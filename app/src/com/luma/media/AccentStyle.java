/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Seven curated accents. RGB animates controls/effects; its card base remains fixed. */
final class AccentStyle {
    static final String[] IDS={"device","rgb","tuhkar","matcha","purple","magenta","red"};
    static final String[] NAMES={"Device","RGB","Tuhkar","Matcha","Purple","Magenta","Red"};
    static String normalize(String value){for(String id:IDS)if(id.equals(value))return id;return "device";}
    static int base(String style,int device){switch(normalize(style)){case "rgb":return 0xFFB6C9EA;case "tuhkar":return 0xFFBACF98;case "matcha":return 0xFFCADBB1;case "purple":return 0xFFCBB7F1;case "magenta":return 0xFFF0B0D6;case "red":return 0xFFF1AAA4;default:return device;}}
    static int[] ring(String style,int device){switch(normalize(style)){
        case "rgb":return new int[]{0xFFFF5277,0xFFFF9E42,0xFFFFDF59,0xFF67ED86,0xFF4DE2E0,0xFF699BFF,0xFFB26EFF,0xFFFF5277};
        case "tuhkar":return new int[]{0xFF7FA35F,0xFFCCDBA1,0xFFE5CCA0,0xFFAB8056,0xFF7FA35F};
        case "matcha":return new int[]{0xFF85AD70,0xFFD0DFB6,0xFFECE1BF,0xFFA5C186,0xFF85AD70};
        case "purple":return new int[]{0xFF8E6CC9,0xFFD3BEF5,0xFFF0CFF8,0xFFAE88E0,0xFF8E6CC9};
        case "magenta":return new int[]{0xFFC05A9D,0xFFF6BADF,0xFFE77AB6,0xFFAC569E,0xFFC05A9D};
        case "red":return new int[]{0xFFCC5A60,0xFFFFB7A6,0xFFF27C67,0xFFD76379,0xFFCC5A60};
        default:int light=SurfacePalette.mix(device,0xFFFFFFFF,.36f),deep=SurfacePalette.mix(device,0xFF24312A,.30f);return new int[]{device,light,deep,light,device};
    }}
    static int wave(long millis){int[] colors={0xFFF1B1CB,0xFFF0C59A,0xFFE8D99C,0xFFBBDCA9,0xFFA7DCD9,0xFFB3C7ED,0xFFD5BAED,0xFFF1B1CB};long phase=Math.floorMod(millis,56000L);int segment=(int)(phase/8000);return SurfacePalette.mix(colors[segment],colors[segment+1],(phase%8000)/8000f);}
}
