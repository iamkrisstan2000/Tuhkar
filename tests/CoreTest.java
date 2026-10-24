/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.*;

public final class CoreTest {
    static final class Row implements MediaOrder.Entry {
        String name,type;long added,modified;
        Row(String name,String type,long added,long modified){this.name=name;this.type=type;this.added=added;this.modified=modified;}
        public String label(){return name;}public String mediaType(){return type;}public long added(){return added;}public long modified(){return modified;}
    }
    static void require(boolean test,String message){if(!test)throw new AssertionError(message);}
    static SharedPreferences memory(){
        Map<String,Object> map=new HashMap<>();
        Object editor=Proxy.newProxyInstance(CoreTest.class.getClassLoader(),new Class[]{SharedPreferences.Editor.class},(p,m,a)->{if(m.getName().startsWith("put")){map.put((String)a[0],a[1]);return p;}if(m.getName().equals("commit"))return true;return null;});
        return (SharedPreferences)Proxy.newProxyInstance(CoreTest.class.getClassLoader(),new Class[]{SharedPreferences.class},(p,m,a)->{if(m.getName().equals("edit"))return editor;if(m.getName().equals("getAll"))return map;if(m.getName().equals("contains"))return map.containsKey(a[0]);if(m.getName().startsWith("get"))return map.getOrDefault(a[0],a[1]);return null;});
    }
    public static void main(String[] args){
        Row z=new Row("Zulu","mp3",200,100),a=new Row("alpha","flac",100,300),b=new Row("Bravo","mp3",0,0);
        List<Row> rows=new ArrayList<>(Arrays.asList(z,a,b));
        rows.sort(MediaOrder.comparator(3));require(rows.get(0)==a&&rows.get(1)==b,"Alphabetical is case-insensitive");
        rows.sort(MediaOrder.comparator(1));require(rows.get(0)==z&&rows.get(2)==b,"Newest added first, unknown dates last");
        rows.sort(MediaOrder.comparator(2));require(rows.get(0)==a&&rows.get(2)==b,"Newest modified first");
        rows.sort(MediaOrder.comparator(0));require(rows.get(0)==a&&rows.get(1)==b,"Type groups extensions then names");
        NativeEq eq=new NativeEq(memory());eq.frequencies=new float[]{60,120,250,500,1000,2000,4000,8000};eq.min=-15;eq.max=15;
        eq.setPreset(1);require(eq.level(0)==-5&&eq.level(3)==0,"Phone-balanced reference gains");
        eq.frequencies=new float[]{(float)Math.sqrt(250*500)};require(Math.abs(eq.level(0)-(-.5))<.001,"Log-frequency interpolation");
        eq.frequencies=new float[]{60,120,250,500,1000,2000,4000,8000};
        for(int preset=0;preset<6;preset++){eq.setPreset(preset);for(int i=0;i<8;i++)require(eq.level(i)<=0&&eq.level(i)>=-15,"Factory curves have no positive gain");}
        eq.setPreset(1);eq.setCustom(2,4);require(eq.preset()==6&&eq.level(2)==4&&eq.level(0)==-5,"Custom edit preserves other preset bands");
        eq.setCustom(2,99);require(eq.level(2)==15,"Gain clamped to native maximum");eq.setCustom(2,-99);require(eq.level(2)==-15,"Gain clamped to native minimum");
        require(!eq.enabled(),"EQ defaults off");
        SurfacePalette normal=SurfacePalette.of(true,false),oled=SurfacePalette.of(true,true);
        require(oled.page==0xFF000000&&normal.page!=oled.page,"OLED page is black");
        require(normal.card==oled.card&&normal.inner==oled.inner&&normal.line==oled.line&&normal.text==oled.text&&normal.muted==oled.muted,"OLED retains normal UI surfaces and text");
        SurfacePalette light=SurfacePalette.of(false,false),lightOled=SurfacePalette.of(false,true);require(light.page==lightOled.page&&light.card==lightOled.card,"Light palette unchanged by OLED preference");
        System.out.println("PASS: four sorting modes, unknown-date fallback, reference EQ gains, log interpolation, cut-only factory presets, custom-band preservation, range clamping, EQ default disabled; OLED black-page-only palette and normal surface retention.");
    }
}
