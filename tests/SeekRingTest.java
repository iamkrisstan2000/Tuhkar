/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

public final class SeekRingTest {
    private static int checks;
    private static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    private static void near(float actual,float expected,String label){check(Math.abs(actual-expected)<.0001,label);}
    public static void main(String[] args){
        near(SeekRingMath.fraction(32,0,64,64),0,"top/start");near(SeekRingMath.fraction(64,32,64,64),.25f,"right/quarter");near(SeekRingMath.fraction(32,64,64,64),.5f,"bottom/half");near(SeekRingMath.fraction(0,32,64,64),.75f,"left/three quarters");
        check(!SeekRingMath.ring(32,32,64,64,24,8),"center is not seek ring");check(!SeekRingMath.ring(55.9f,32,64,64,24,8),"48dp center boundary");check(SeekRingMath.ring(56,32,64,64,24,8),"outer annulus");check(SeekRingMath.ring(72,32,64,64,24,8),"allowed slop");check(!SeekRingMath.ring(73,32,64,64,24,8),"reject outside slop");
        near(SeekRingMath.drag(.99f,.01f),1,"clamp at end when dragging across top");near(SeekRingMath.drag(.01f,.99f),0,"clamp at start instead of jumping to end");near(SeekRingMath.drag(.3f,.5f),.5f,"normal drag");
        check(SeekRingMath.position(.5f,100000)==50000,"seek midpoint");check(SeekRingMath.position(2,100000)==100000,"clamp seek end");check(SeekRingMath.position(-1,100000)==0,"clamp seek start");check(SeekRingMath.position(.5f,0)==0&&SeekRingMath.position(.5f,-1)==0,"unknown/live duration");check(SeekRingMath.position(Float.NaN,1000)==0,"invalid fraction safe");
        for(int n=0;n<10000;n++){
            float fraction=n/10000f;double angle=fraction*Math.PI*2-Math.PI/2;
            float x=32+(float)Math.cos(angle)*30,y=32+(float)Math.sin(angle)*30;
            near(SeekRingMath.fraction(x,y,64,64),fraction,"clockwise angular round trip");
            long pos=SeekRingMath.position(fraction,2147483647L);check(pos>=0&&pos<=2147483647L,"long duration without overflow");
            float clamped=SeekRingMath.drag(fraction,(n%137)/136f);check(clamped>=0&&clamped<=1,"drag remains normalized");
        }
        System.out.println("PASS: "+checks+" seek-ring assertions: angular mapping, center/ring separation, top-boundary clamping, live/unknown durations and long-file range safety.");
    }
}
