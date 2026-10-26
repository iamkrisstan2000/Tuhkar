# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Actual RoundedVideoView against Java graphics/TextureView doubles.
Tests output lifecycle and clipping configuration, not Android video pixels/codecs.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
stubs={
'android/content/Context.java':'package android.content;public class Context {}',
'android/util/DisplayMetrics.java':'package android.util;public class DisplayMetrics {public float density=2;}',
'android/content/res/Resources.java':'package android.content.res;public class Resources {public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}',
'android/graphics/Outline.java':'package android.graphics;public class Outline {public float radius,alpha;public void setRoundRect(int l,int t,int r,int b,float radius){this.radius=radius;}public void setAlpha(float a){alpha=a;}}',
'android/graphics/SurfaceTexture.java':'package android.graphics;public class SurfaceTexture {public boolean released;public void release(){released=true;}}',
'android/view/ViewOutlineProvider.java':'package android.view;import android.graphics.Outline;public abstract class ViewOutlineProvider {public abstract void getOutline(View v,Outline o);}',
'android/view/View.java':'''package android.view;import android.content.res.Resources;public class View {public ViewOutlineProvider outline;public boolean clip;public int invalidations;int width=320,height=180;public Resources getResources(){return new Resources();}public int getWidth(){return width;}public int getHeight(){return height;}public void setOutlineProvider(ViewOutlineProvider p){outline=p;}public void setClipToOutline(boolean on){clip=on;}public void invalidateOutline(){invalidations++;}protected void onSizeChanged(int w,int h,int ow,int oh){width=w;height=h;}}''',
'android/view/Surface.java':'package android.view;import android.graphics.SurfaceTexture;public class Surface {public final SurfaceTexture texture;public boolean released;public Surface(SurfaceTexture t){texture=t;}public void release(){released=true;}}',
'android/view/TextureView.java':'''package android.view;import android.content.Context;import android.graphics.SurfaceTexture;public class TextureView extends View {public boolean opaque=true;public SurfaceTextureListener listener;public SurfaceTexture texture;public void setSurfaceTexture(SurfaceTexture value){if(value.released)throw new AssertionError("released transfer");texture=value;}public TextureView(Context c){}public void setOpaque(boolean b){opaque=b;}public void setSurfaceTextureListener(SurfaceTextureListener l){listener=l;}public interface SurfaceTextureListener {void onSurfaceTextureAvailable(SurfaceTexture t,int w,int h);void onSurfaceTextureSizeChanged(SurfaceTexture t,int w,int h);boolean onSurfaceTextureDestroyed(SurfaceTexture t);void onSurfaceTextureUpdated(SurfaceTexture t);}}''',
'com/luma/media/RoundedVideoContractTest.java':'''package com.luma.media;
import android.content.Context;import android.graphics.*;import android.view.Surface;
public class RoundedVideoContractTest {
 static int checks,calls;static Surface attached;static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 public static void main(String[] args){
  RoundedVideoView view=new RoundedVideoView(new Context());check(!view.opaque&&view.clip,"translucent GPU view with outline clipping");check(view.listener==view,"owns output lifecycle");
  Outline outline=new Outline();view.outline.getOutline(view,outline);check(outline.radius==24&&outline.alpha==1,"12dp at density two and opaque outline");
  check(view.output()==null,"no output before texture ready");
  view.onOutputChanged(()->{calls++;if(view.output()==null&&attached!=null)check(!attached.released,"detach callback before releasing Surface wrapper");attached=view.output();});check(calls==0,"no premature attachment");
  SurfaceTexture texture=new SurfaceTexture();view.onSurfaceTextureAvailable(texture,320,180);check(calls==1&&attached==view.output()&&attached.texture==texture,"attach available texture");Surface first=attached;
  view.onSurfaceTextureSizeChanged(texture,640,360);check(calls==2&&attached==first&&!first.released&&view.invalidations>0,"resize reuses output and refreshes clipping");
  check(view.onSurfaceTextureDestroyed(texture),"TextureView releases owned SurfaceTexture");check(calls==3&&view.output()==null&&attached==null&&first.released,"destroy detaches and releases output wrapper");
  view.onSurfaceTextureAvailable(new SurfaceTexture(),320,180);check(calls==4&&view.output()!=first&&!view.output().released,"recreation uses fresh output");
  final int[] immediate={0};view.onOutputChanged(()->immediate[0]++);check(immediate[0]==1,"late listener receives existing output");
  view.onSizeChanged(800,450,320,180);check(view.getWidth()==800&&view.getHeight()==450&&view.invalidations>=2,"layout size refreshes outline");
  Surface second=view.output();view.onSurfaceTextureDestroyed(new SurfaceTexture());check(second.released&&view.output()==null,"second output also released");
  RoundedVideoView.RetainedOutput retained=new RoundedVideoView.RetainedOutput();retained.setRetaining(true);
  RoundedVideoView original=new RoundedVideoView(new Context(),retained);final int[] transfers={0};original.onOutputChanged(()->transfers[0]++);
  SurfaceTexture sharedTexture=new SurfaceTexture();original.onSurfaceTextureAvailable(sharedTexture,320,180);Surface shared=original.output();check(transfers[0]==1,"initial retained output callback");
  check(!original.onSurfaceTextureDestroyed(sharedTexture),"retained view prevents framework texture release");check(!shared.released&&!sharedTexture.released&&transfers[0]==1,"no output-null callback or resource release during section change");
  RoundedVideoView replacement=new RoundedVideoView(new Context(),retained);check(replacement.output()==shared&&replacement.texture==sharedTexture,"same Surface and SurfaceTexture transferred, no decoder target replacement");replacement.onOutputChanged(()->transfers[0]++);check(transfers[0]==2,"replacement immediately reports existing target");
  check(!original.onSurfaceTextureDestroyed(sharedTexture)&&retained.owner==replacement&&!retained.detached,"stale destruction cannot reclaim transferred output");replacement.onSurfaceTextureAvailable(sharedTexture,320,180);check(replacement.output()==shared,"duplicate availability never replaces retained wrapper");
  for(int i=0;i<100;i++){check(!replacement.onSurfaceTextureDestroyed(sharedTexture),"section detach retains texture");check(!shared.released&&!sharedTexture.released,"live target never released");replacement=new RoundedVideoView(new Context(),retained);check(replacement.output()==shared&&replacement.texture==sharedTexture,"repeated section change reuses exact producer");}
  replacement.onSurfaceTextureSizeChanged(sharedTexture,640,360);check(replacement.output()==shared&&!shared.released,"resize keeps retained target");retained.setRetaining(false);check(!shared.released&&!sharedTexture.released,"live consumer is not released before its detach callback");
  check(replacement.onSurfaceTextureDestroyed(sharedTexture),"Settings/background/fullscreen stop retention and return ownership to framework");check(shared.released&&retained.output==null&&retained.texture==null&&retained.owner==null,"normal detach clears owner and releases Surface");sharedTexture.release();check(sharedTexture.released,"framework releases non-retained texture");
  retained.setRetaining(true);RoundedVideoView orphan=new RoundedVideoView(new Context(),retained);SurfaceTexture orphanTexture=new SurfaceTexture();orphan.onSurfaceTextureAvailable(orphanTexture,320,180);Surface orphanOutput=orphan.output();check(!orphan.onSurfaceTextureDestroyed(orphanTexture),"detached transfer is held temporarily");retained.setRetaining(false);check(orphanOutput.released&&orphanTexture.released&&retained.output==null,"abandoned transfer released on Settings/background/destroy");retained.setRetaining(false);check(retained.owner==null&&retained.texture==null,"repeated cleanup safe");
  System.out.println("PASS: "+checks+" RoundedVideoView contract assertions: 12dp clipping, nonopaque GPU layer, output lifecycle, 100 exact-target transfers, stale-owner safety and normal/abandoned cleanup.");
  System.out.println("NOTE: Java view/graphics doubles, not Android TextureView rendering, decoder output or corner-pixel verification.");
 }
}'''
}
with TemporaryDirectory(prefix='tuhkar-rounded-video-') as temp:
    root=Path(temp);files=[]
    for name,content in stubs.items():
        file=root/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(content);files.append(str(file))
    actual=str(Path('app/src/com/luma/media/RoundedVideoView.java').resolve())
    subprocess.run(['javac','-d',str(root/'classes'),*files,actual],check=True)
    subprocess.run(['java','-cp',str(root/'classes'),'com.luma.media.RoundedVideoContractTest'],check=True)
