/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public class LiquidFlowMathTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[] args){
        int checks=0;
        for(int w:new int[]{0,48,200,640})for(float p:new float[]{0,.001f,.25f,.5f,.999f,1})for(int y=0;y<=72;y+=4)for(int phase=0;phase<6;phase++){
            float x=LiquidFlowMath.edge(p,y,72,w,2,phase);check(x>=0&&x<=w);if(p==0)check(x==0);if(p==1)check(x==w);check(Math.abs(x-w*p)<=6.001f);checks++;
        }
        check(LiquidFlowMath.edge(Float.NaN,0,0,200,1,0)==0);
        System.out.println("PASS: "+checks+" liquid geometry checks: bounded minimal meniscus, exact empty/full endpoints, responsive sizes and animation phases");
    }
}
