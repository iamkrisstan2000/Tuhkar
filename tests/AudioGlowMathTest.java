/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
import java.util.Random;
public final class AudioGlowMathTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args){
        float[] out=new float[3];byte[] fft=new byte[512];AudioGlowMath.bands(fft,44100000,out);check(out[0]==0&&out[1]==0&&out[2]==0,"silence never invents a beat");
        fft[2]=100;AudioGlowMath.bands(fft,44100000,out);check(out[0]>0&&out[1]==0&&out[2]==0,"86Hz drives low only");
        fft=new byte[512];fft[20]=-100;fft[21]=60;AudioGlowMath.bands(fft,44100000,out);check(out[0]==0&&out[1]>0&&out[2]==0,"861Hz signed complex magnitudes drive mid");
        fft=new byte[512];fft[100]=100;AudioGlowMath.bands(fft,44100000,out);check(out[0]==0&&out[1]==0&&out[2]>0,"4306Hz drives high");
        fft=new byte[512];fft[0]=127;fft[1]=127;AudioGlowMath.bands(fft,44100000,out);check(out[0]==0&&out[1]==0&&out[2]==0,"DC/Nyquist bins excluded");
        AudioGlowMath.bands(null,44100000,out);check(out[0]==0&&out[1]==0&&out[2]==0,"missing capture resets energy");
        check(AudioGlowMath.clamp(Float.NaN)==0&&AudioGlowMath.clamp(Float.POSITIVE_INFINITY)==1,"nonfinite input bounded");
        Random rng=new Random(23);
        for(int n=0;n<10000;n++){
            fft=new byte[512];rng.nextBytes(fft);AudioGlowMath.bands(fft,48000000,out);
            for(float v:out)check(v>=0&&v<=1&&!Float.isNaN(v),"random capture bounded");
        }
        System.out.println("PASS: "+checks+" FFT band checks: real low/mid/high energy, silence/DC exclusion, signed FFT and bounded energy");
    }
}
