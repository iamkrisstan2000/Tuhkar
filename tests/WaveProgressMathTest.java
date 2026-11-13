/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public class WaveProgressMathTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        check(WaveProgressMath.fraction(0,0,100)==0);check(WaveProgressMath.fraction(50,100,0)==.5f);
        check(WaveProgressMath.fraction(50,100,25)==.75f);check(WaveProgressMath.fraction(95,100,50)==1);
        check(WaveProgressMath.fraction(-50,100,-10)==0);check(WaveProgressMath.fraction(Long.MAX_VALUE,100,Long.MAX_VALUE)==1);
        check(WaveProgressMath.fraction(0,100,0)==0);check(WaveProgressMath.fraction(100,100,0)==1);
        System.out.println("PASS: wave progress interpolation, pause/static values, live/unknown duration, bounds and overflow safety");
    }
}
