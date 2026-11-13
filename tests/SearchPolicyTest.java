/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public final class SearchPolicyTest {
 public static void main(String[] args){
  String[] kinds={"music","radio","video","tv"};int checks=0;
  for(int scope=0;scope<3;scope++)for(String kind:kinds){boolean expected=scope==0||(scope==1&&(kind.equals("music")||kind.equals("radio")))||(scope==2&&(kind.equals("video")||kind.equals("tv")));if(SearchPolicy.allows(scope,kind)!=expected)throw new AssertionError("Scope leak");checks++;}
  if(SearchPolicy.IDLE_MS!=30000||!SearchPolicy.matches("  HELLO  ","Hello world","Artist")||!SearchPolicy.matches("artist","Title","Artist")||SearchPolicy.matches("","title","detail")||SearchPolicy.matches("no match","title","detail"))throw new AssertionError("Query or timer policy");
  if(!AccentStyle.normalize("lambo").equals("cutie"))throw new AssertionError("Saved Lambo migration");
  System.out.println("PASS: "+checks+" media search scope cases, case/whitespace/detail matching, empty query handling, 30s idle policy and saved Cutie migration");
 }
}
