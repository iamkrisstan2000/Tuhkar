/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public class NavSwipeMathTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){
        check(NavSwipeMath.direction(-80,8,48)==1);check(NavSwipeMath.direction(80,8,48)==-1);
        check(NavSwipeMath.direction(-20,0,48)==0);check(NavSwipeMath.direction(60,70,48)==0);
        check(NavSwipeMath.direction(0,100,48)==0);check(NavSwipeMath.direction(0,0,48)==0);
        for(int i=0;i<5;i++){check(NavSwipeMath.target(i,1)==Math.min(4,i+1));check(NavSwipeMath.target(i,-1)==Math.max(0,i-1));check(NavSwipeMath.target(i,0)==i);}
        check(NavSwipeMath.horizontal(20,2,8));check(!NavSwipeMath.horizontal(4,0,8));
        System.out.println("PASS: bottom navigation direction, intent threshold and bounded five-section ordering");
    }
}
