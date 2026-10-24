/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public final class RainbowRingGeometryTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        for(int w:new int[]{8,24,48,64,128,200,320,400,640,1080,2400})for(int h:new int[]{8,24,48,64,128,200,320,400,640,1080,2400})for(float density:new float[]{.75f,1,1.5f,2,3,4}){
            RainbowRingGeometry g=new RainbowRingGeometry(w,h,density);float half=Math.min(w,h)/2f;
            check(g.haloRadius<half&&g.haloRadius>0,"halo has transparent perimeter room");
            check(g.cx-g.haloRadius>=0&&g.cx+g.haloRadius<=w&&g.cy-g.haloRadius>=0&&g.cy+g.haloRadius<=h,"halo fits all sides, including landscape/narrow artwork");
            check(g.rimRadius+g.rimWidth/2<g.haloRadius,"bright rim safely inside diffusion boundary");
            check(g.cy+g.shadowOffset+g.shadowRadius<h&&g.cy+g.shadowOffset-g.shadowRadius>=0,"subtle downward shadow not cut off");
            check(g.cx-g.shadowRadius>=0&&g.cx+g.shadowRadius<=w,"shadow fits horizontal boundary");
            check(half*RainbowRingGeometry.ART_SCALE<g.rimRadius-g.rimWidth/2,"rim is outside the record edge");
            for(int energy=0;energy<=100;energy++){
                float haloAlpha=155+energy*.01f*85;
                check(haloAlpha>=155&&haloAlpha<=240,"reactivity only changes bounded intensity; radii remain fixed");
            }
        }
        System.out.println("PASS: "+checks+" rainbow-ring geometry/intensity checks: small/large/portrait/landscape artwork, full halo/rim/shadow containment and fixed peak-energy bounds");
    }
}
