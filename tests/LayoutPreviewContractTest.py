# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Executes native preview drawing with Canvas doubles, not pixel/Android UI tests."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
stubs={
'android/content/Context.java':'package android.content;public class Context {}',
'android/graphics/Paint.java':'package android.graphics;public class Paint {public enum Style{FILL,STROKE}public Paint(int flags){}public void setColor(int c){}public void setAlpha(int a){}public void setStyle(Style s){}public void setStrokeWidth(float w){}}',
'android/graphics/Canvas.java':'''package android.graphics;public class Canvas {public int circles,lines,saves,restores;public void valid(float left,float top,float right,float bottom){if(left<0||top<0||right>160||bottom>92||right<left||bottom<top)throw new AssertionError("preview shape outside design bounds");}public int save(){saves++;return saves;}public void translate(float x,float y){if(!Float.isFinite(x)||!Float.isFinite(y))throw new AssertionError();}public void scale(float x,float y){if(!Float.isFinite(x)||x<=0||x!=y)throw new AssertionError();}public void restoreToCount(int n){restores++;}public void drawCircle(float x,float y,float r,Paint p){circles++;valid(x-r,y-r,x+r,y+r);}public void drawRoundRect(float l,float t,float r,float b,float rx,float ry,Paint p){lines++;valid(l,t,r,b);}}''',
'android/view/View.java':'''package android.view;import android.content.Context;import android.graphics.Canvas;public class View {public int w,h;public View(Context c){}public int getWidth(){return w;}public int getHeight(){return h;}protected void onDraw(Canvas c){}}''',
'com/luma/media/LayoutPreviewContract.java':'''package com.luma.media;import android.content.Context;import android.graphics.Canvas;public class LayoutPreviewContract {public static void main(String[] args){int checks=0;int[] circles={8,48,18,15},lines={1,24,9,10};for(int mode=0;mode<4;mode++)for(int width:new int[]{48,96,160,320,640})for(int height:new int[]{48,92,120}){LayoutPreviewView view=new LayoutPreviewView(new Context(),mode,0xff759d70,0xffcccccc);view.w=width;view.h=height;for(int repeat=0;repeat<3;repeat++){Canvas c=new Canvas();view.onDraw(c);if(c.circles!=circles[mode]||c.lines!=lines[mode]||c.saves!=1||c.restores!=1)throw new AssertionError("wrong sketch or unbalanced Canvas");checks++;}}LayoutPreviewView zero=new LayoutPreviewView(new Context(),0,0,0);Canvas c=new Canvas();zero.onDraw(c);if(c.saves!=0||c.circles!=0)throw new AssertionError("zero-size view must skip drawing");System.out.println("PASS: "+(checks+1)+" actual layout-preview draw checks: distinct swipe/24-tile/9-tile/list sketches, bounded shapes, small/wide sizing and Canvas restoration");System.out.println("NOTE: deterministic Canvas doubles; not device appearance, selection touch dispatch or accessibility validation.");}}'''
}
source=Path('app/src/com/luma/media/MainActivity.java').read_text();start=source.index('    void librarySettings(');end=source.index('    String equalizerUiState()',start);ui=source[start:end]
for token in ['new LayoutPreviewView(this,mode,accent,muted)','option.setSelected(selected)','option.setContentDescription','option.setFocusable(true)','putInt("layoutMode",mode)','rowIndex<2','col<2','render();','Previews are illustrative']:
    assert token in ui,token
preview=Path('app/src/com/luma/media/LayoutPreviewView.java').read_text();draw=preview[preview.index('@Override protected void onDraw'):];assert 'new ' not in draw
with TemporaryDirectory(prefix='tuhkar-layout-preview-') as tmp:
    root=Path(tmp);files=[]
    for name,text in stubs.items():
        p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(str(p))
    subprocess.run(['javac','-encoding','UTF-8','-d',str(root/'classes'),*files,str(Path('app/src/com/luma/media/LayoutPreviewView.java').resolve())],check=True)
    subprocess.run(['java','-cp',str(root/'classes'),'com.luma.media.LayoutPreviewContract'],check=True)
print('PASS: four directly selectable preview tiles, selected border/check state, saved layout option, accessible labels and no per-draw object allocations')
