# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Executes the real ThumbnailView against minimal drawing stubs.
Checks geometry/draw ordering only; NOT an Android GPU or pixel-rendering test.
Requires javac/java, no Android SDK or third-party libraries.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess

stubs = {
'android/content/Context.java': 'package android.content; public class Context {}',
'android/graphics/Path.java': '''package android.graphics;
public class Path {
 public enum Direction {CW}
 public float x,y,r; public boolean empty=true;
 public void reset(){empty=true;}
 public void addCircle(float x,float y,float r,Direction d){this.x=x;this.y=y;this.r=r;empty=false;}
}''',
'android/graphics/Canvas.java': '''package android.graphics;
public class Canvas {
 public String order="";public Path clipped;public boolean fail;
 public int save(){order+="save;";return 7;}
 public boolean clipPath(Path p){order+="clip;";clipped=p;return true;}
 public void image(){order+="image;";if(fail)throw new IllegalStateException("image draw error");}
 public void restoreToCount(int n){if(n!=7)throw new AssertionError("save token");order+="restore;";}
}''',
'android/widget/ImageView.java': '''package android.widget;
import android.content.Context;import android.graphics.Canvas;
public class ImageView {
 public enum ScaleType {CENTER_CROP}
 public ScaleType scale;public boolean outline=true;private int w,h;
 public ImageView(Context c){}
 public void setScaleType(ScaleType type){scale=type;}
 public void setClipToOutline(boolean on){outline=on;}
 public int getWidth(){return w;}public int getHeight(){return h;}
 public void layout(int l,int t,int r,int b){int ow=w,oh=h;w=r-l;h=b-t;onSizeChanged(w,h,ow,oh);}
 protected void onSizeChanged(int w,int h,int ow,int oh){}
 public void draw(Canvas c){c.image();}
}''',
'com/luma/media/ThumbnailContractTest.java': '''package com.luma.media;
import android.content.Context;import android.graphics.Canvas;import android.graphics.Path;import android.widget.ImageView;
public class ThumbnailContractTest {
 static int checks;static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 public static void main(String[] args){
  ThumbnailView view=new ThumbnailView(new Context());
  check(view.scale==ImageView.ScaleType.CENTER_CROP,"center crop");check(!view.outline,"does not rely on outline clipping");
  Path previous=null;
  int[][] sizes={{44,44},{62,62},{200,100},{100,200},{49,49},{300,300},{44,44}};
  for(int[] size:sizes){
   view.layout(0,0,size[0],size[1]);
   for(int replacement=0;replacement<3;replacement++){
    Canvas canvas=new Canvas();view.draw(canvas);
    check(canvas.order.equals("save;clip;image;restore;"),"mask every draw, not just first image");
    Path path=canvas.clipped;check(path!=null&&!path.empty,"nonempty circular path");
    check(path.x==size[0]/2f&&path.y==size[1]/2f,"center in resized view");
    check(path.r==Math.min(size[0],size[1])/2f,"circle, never oval");
    if(previous!=null)check(path==previous,"reuse path instead of per-image allocation");previous=path;
   }
  }
  Canvas failed=new Canvas();failed.fail=true;boolean threw=false;
  try{view.draw(failed);}catch(IllegalStateException expected){threw=true;}
  check(threw&&failed.order.equals("save;clip;image;restore;"),"restore Canvas even when image draw fails");
  view.layout(0,0,0,0);Canvas empty=new Canvas();view.draw(empty);check(empty.order.isEmpty(),"skip zero size safely");
  view.layout(0,0,80,80);Canvas resumed=new Canvas();view.draw(resumed);check(resumed.clipped.r==40,"recover after zero-size layout");
  System.out.println("PASS: "+checks+" ThumbnailView draw-contract assertions: repeated draws, square/wide/tall resizing, circular geometry, path reuse and Canvas restoration.");
  System.out.println("NOTE: minimal Java stubs; not Android runtime or GPU/pixel verification.");
 }
}'''
}
with TemporaryDirectory(prefix='tuhkar-thumbnail-') as temp:
    root = Path(temp)
    paths = []
    for name, content in stubs.items():
        file = root / name
        file.parent.mkdir(parents=True, exist_ok=True)
        file.write_text(content)
        paths.append(str(file))
    actual = Path('app/src/com/luma/media/ThumbnailView.java').resolve()
    subprocess.run(['javac', '-d', str(root / 'classes'), *paths, str(actual)], check=True)
    subprocess.run(['java', '-cp', str(root / 'classes'), 'com.luma.media.ThumbnailContractTest'], check=True)
