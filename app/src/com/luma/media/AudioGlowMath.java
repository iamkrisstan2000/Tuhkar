/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Bounded FFT band energy. Android Visualizer FFT uses signed real/imaginary byte pairs. */
final class AudioGlowMath {
    static float clamp(float value){return Float.isNaN(value)?0:Math.max(0,Math.min(1,value));}
    static void bands(byte[] fft,int samplingRateMilliHz,float[] out){
        out[0]=out[1]=out[2]=0;
        if(fft==null||fft.length<4||samplingRateMilliHz<=0)return;
        int[] counts={0,0,0};
        for(int k=1;k<fft.length/2;k++){
            double hz=(double)k*samplingRateMilliHz/(1000.0*fft.length);
            if(hz<20||hz>12000)continue;
            int band=hz<250?0:hz<2000?1:2;
            double real=fft[2*k],imag=fft[2*k+1];
            out[band]+=(float)(Math.sqrt(real*real+imag*imag)/128.0);counts[band]++;
        }
        for(int i=0;i<3;i++)out[i]=counts[i]==0?0:clamp(out[i]*5f/counts[i]);
    }
}
