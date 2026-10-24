/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public final class LibrarySummaryTest {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    public static void main(String[] args){
        check(LibrarySummary.format(1201,1201,0,"1201 items · hold to save").equals("1201 items · hold to save"),"no duplicate total");
        check(LibrarySummary.format(12,1201,0,"1201 items · hold to save").equals("12 of 1201 items · hold to save"),"filtered count with total");
        check(LibrarySummary.format(1,1,2,"").equals("1 item · hold to save or remove"),"singular video");
        check(LibrarySummary.format(0,1,2,"").startsWith("0 of 1 item"),"no matches but total retained");
        check(LibrarySummary.format(5,100,1,"100 stations · search worldwide · Radio Browser").equals("5 of 100 stations · search worldwide · Radio Browser"),"radio provider details retained");
        check(LibrarySummary.format(17,17,3,"17 channels · PH · iptv-org · availability varies").startsWith("17 channels · PH · iptv-org"),"TV details retained");
        check(LibrarySummary.format(1,1,1,null).startsWith("1 station ·"),"singular station and null status");
        check(LibrarySummary.format(1,1,3,null).startsWith("1 channel ·"),"singular channel");
        check(LibrarySummary.format(0,0,0,"Scanning media…").equals("0 items · Scanning media…"),"loading shown inside card");
        check(LibrarySummary.format(0,0,1,"Directory unavailable.").equals("0 stations · Directory unavailable."),"error retained");
        check(LibrarySummary.format(5000,5000,0,"5000 items · hold to save · 5,000-item scan limit").contains("5,000-item scan limit"),"scan warning retained");
        for(int tab=0;tab<4;tab++)for(int total=0;total<=100;total++)for(int shown=0;shown<=total;shown++){
            String header=LibrarySummary.format(shown,total,tab,"");
            check(header.startsWith(shown==total?total+" ":shown+" of "+total+" "),"correct displayed/total prefix");
        }
        System.out.println("PASS: "+checks+" library summary assertions: totals, filtered counts, singular/plural units, provider details, loading/errors and warnings.");
    }
}
