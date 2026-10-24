/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
import java.util.Random;
public final class DiscPulseTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        check(DiscPulse.target(0,0,0)==0,"silence cannot create a beat");
        check(DiscPulse.target(.5f,0,0)>DiscPulse.target(0,.5f,0)&&DiscPulse.target(0,.5f,0)>DiscPulse.target(0,0,.5f),"bass drives pulse, mid/high contribute less");
        check(DiscPulse.follow(0,1,32)>1-DiscPulse.follow(1,0,32),"attack quicker than release");
        float beat=0;for(int i=0;i<20;i++){float next=DiscPulse.follow(beat,1,16);check(next>=beat&&next<1,"fluid attack without overshoot");beat=next;}check(beat>.99f,"visible responsive peak");
        float peak=beat;beat=DiscPulse.follow(beat,0,32);check(beat<peak&&beat>.75f,"beat falls smoothly, not abruptly off");
        for(int i=0;i<200;i++)beat=DiscPulse.follow(beat,0,16);check(beat==0,"silence settles to zero without fake rhythmic motion");
        check(DiscPulse.contrast(0,.9f)==0,"adaptive contrast cannot invent a beat in silence");
        check(DiscPulse.contrast(.8f,.5f)>DiscPulse.contrast(.8f,.8f)*3,"rises above measured average produce strong contrast without constant loudness saturation");
        float average=0;for(int i=0;i<500;i++)average=DiscPulse.average(average,.8f,32);
        check(Math.abs(average-.8f)<.001&&Math.abs(DiscPulse.contrast(.8f,average)-.2f)<.01,"steady loud input settles instead of staying fully bright");
        check(DiscPulse.haloScale(1,0)<DiscPulse.haloScale(1,1)&&DiscPulse.shadowScale(1,0)<DiscPulse.shadowScale(1,1)&&DiscPulse.rimScale(1,0)<DiscPulse.rimScale(1,1),"ON breathes in spatial extent as well as opacity");
        check(DiscPulse.haloScale(0,0)==DiscPulse.haloScale(0,1)&&DiscPulse.rimScale(0,0)==DiscPulse.rimScale(0,1),"OFF ring and halo remain spatially steady");
        Random rng=new Random(32);
        for(int n=0;n<10000;n++){
            float previous=rng.nextFloat(),target=rng.nextFloat();long dt=rng.nextInt(250);float next=DiscPulse.follow(previous,target,dt);
            check(next>=Math.min(previous,target)&&next<=Math.max(previous,target),"bounded frame envelope");
            float mode=rng.nextFloat(),playing=rng.nextFloat(),energy=rng.nextFloat();
            check(DiscPulse.halo(mode,playing,energy)>=0&&DiscPulse.halo(mode,playing,energy)<=255,"halo alpha bounded");
            check(DiscPulse.rim(mode,playing,energy)>=0&&DiscPulse.rim(mode,playing,energy)<=255,"rim alpha bounded");
            check(DiscPulse.shadow(mode,playing,energy)>=0&&DiscPulse.shadow(mode,playing,energy)<=255,"shadow alpha bounded");
            check(DiscPulse.halo(0,playing,energy)==55&&DiscPulse.rim(0,playing,energy)==85&&DiscPulse.shadow(0,playing,energy)==45,"OFF steady/faint independent of music and play state");
        }
        for(int size=8;size<=800;size+=8){RainbowRingGeometry g=new RainbowRingGeometry(size,size+20,3);for(int i=0;i<=100;i++){
            float amount=i/100f;float outer=g.rimRadius*DiscPulse.rimScale(1,amount)+g.rimWidth*DiscPulse.width(1,amount)/2;
            check(outer<g.haloRadius*DiscPulse.haloScale(1,amount)&&g.haloRadius<=size/2f,"pulsing rim remains within fixed circular diffusion/view bounds");
        }}
        check(DiscPulse.halo(1,1,1)>DiscPulse.halo(0,1,1)*4,"ON halo peak clearly exceeds OFF");
        check(DiscPulse.rim(1,0,1)==0&&DiscPulse.halo(1,0,1)==0&&DiscPulse.shadow(1,0,1)==45,"ON paused/stopped leaves only quiet physical shadow");
        System.out.println("PASS: "+checks+" pulse-envelope/style checks: actual-energy-only bass weighting, frame-rate-independent smooth attack/release, distinct ON/OFF and contained rim peaks");
    }
}
