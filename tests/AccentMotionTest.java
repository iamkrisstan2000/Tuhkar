/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public final class AccentMotionTest {
    static int checks;
    static void check(boolean v,String label){checks++;if(!v)throw new AssertionError(label);}
    public static void main(String[] args){
        check(AccentStyle.IDS.length==7&&AccentStyle.NAMES.length==7,"exactly seven requested styles");
        String[] expected={"Device","RGB","Tuhkar","Matcha","Purple","Magenta","Red"};
        for(int i=0;i<expected.length;i++){
            check(expected[i].equals(AccentStyle.NAMES[i]),"style name/order");int[] ring=AccentStyle.ring(AccentStyle.IDS[i],0xFF9DB8D0);check(ring.length>=5&&ring[0]==ring[ring.length-1],"closed seamless palette");for(int color:ring)check((color>>>24)==255,"opaque native palette color");
        }
        check(AccentStyle.base("device",0xFFA1B2C3)==0xFFA1B2C3,"Device follows supplied system accent");check(AccentStyle.normalize("bad").equals("device"),"legacy/unknown style safely falls back");
        check(AccentStyle.ring("tuhkar",0)[2]==0xFFE5CCA0&&AccentStyle.ring("tuhkar",0)[3]==0xFFAB8056,"Tuhkar includes sandy and brown tones");
        int base=AccentStyle.base("rgb",0);
        for(long t=0;t<112000;t+=37){int a=AccentStyle.wave(t);check(a==AccentStyle.wave(t+56000),"slow wave periodic");check((a>>>24)==255,"wave opaque");check(AccentStyle.base("rgb",0)==base,"RGB card base does not change with wave");}
        for(int frame:new int[]{8,16,32,50,100}){
            float opacity=0;for(int time=0;time<500;time+=frame){float next=DiscMotion.fade(opacity,true,frame);check(next>=opacity&&next<=1,"smooth bounded fade in");opacity=next;}check(opacity==1,"fade in reaches full glow");
            for(int time=0;time<650;time+=frame){float next=DiscMotion.fade(opacity,false,frame);check(next<=opacity&&next>=0,"smooth bounded pause/stop fade");opacity=next;}check(opacity==0,"lighting fully off after pause/stop");
            float slow=DiscMotion.cycle(0,0,frame),strong=DiscMotion.cycle(0,1,frame);check(strong>slow&&slow>0,"audio energy accelerates accent color cycling");
        }
        System.out.println("PASS: "+checks+" accent/wave/fade/cycle checks: seven themes, Device mapping, closed palettes, Tuhkar sand/brown, steady RGB card base, smooth complete shutoff and audio-driven cycle speed");
    }
}
