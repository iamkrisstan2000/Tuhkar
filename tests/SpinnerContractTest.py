# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Execute the actual spinning thumbnail lifecycle against Java view/animator doubles.
Not an Android animation or rendering test.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
stubs={
'android/content/Context.java':'package android.content;public class Context {}',
'android/view/ViewTreeObserver.java':'''package android.view;import java.util.*;public class ViewTreeObserver {public interface OnPreDrawListener{boolean onPreDraw();}public List<OnPreDrawListener> listeners=new ArrayList<>();public boolean isAlive(){return true;}public void addOnPreDrawListener(OnPreDrawListener l){listeners.add(l);}public void removeOnPreDrawListener(OnPreDrawListener l){listeners.remove(l);}public void draw(){for(OnPreDrawListener l:new ArrayList<>(listeners))l.onPreDraw();}}''',
'android/view/View.java':'''package android.view;import java.util.*;public class View {public static final int VISIBLE=0;boolean attached,temp,ancestorShown=true;int visibility,window,w=44,h=44;float rotation;public final List<Runnable> posted=new ArrayList<>();public final ViewTreeObserver observer=new ViewTreeObserver();public ViewTreeObserver getViewTreeObserver(){return observer;}public void shown(boolean b){ancestorShown=b;}public void drain(){for(Runnable r:new ArrayList<>(posted)){posted.remove(r);r.run();}}public int getWidth(){return w;}public int getHeight(){return h;}protected void onSizeChanged(int w,int h,int ow,int oh){this.w=w;this.h=h;}public boolean isAttachedToWindow(){return attached;}public boolean isShown(){return attached&&ancestorShown&&visibility==0&&window==0;}public int getWindowVisibility(){return window;}public boolean post(Runnable r){posted.add(r);return true;}public boolean removeCallbacks(Runnable r){posted.remove(r);return true;}public float getRotation(){return rotation;}public void setRotation(float value){rotation=value;}protected void onAttachedToWindow(){attached=true;}protected void onDetachedFromWindow(){attached=false;}protected void onWindowVisibilityChanged(int value){window=value;}protected void onVisibilityChanged(View changed,int value){visibility=value;}public void onStartTemporaryDetach(){temp=true;}public void onFinishTemporaryDetach(){temp=false;}}''',
'android/view/animation/LinearInterpolator.java':'package android.view.animation;public class LinearInterpolator {}',
'android/animation/ObjectAnimator.java':'''package android.animation;import android.view.View;import android.view.animation.LinearInterpolator;import java.util.*;public class ObjectAnimator {public static final int INFINITE=-1;public static final Set<ObjectAnimator> active=new HashSet<>();public float from,to;public long duration;public int repeats;public static ObjectAnimator ofFloat(View v,String prop,float from,float to){ObjectAnimator a=new ObjectAnimator();a.from=from;a.to=to;return a;}public void setDuration(long d){duration=d;}public void setRepeatCount(int n){repeats=n;}public void setInterpolator(LinearInterpolator i){}public void start(){active.add(this);}public void cancel(){active.remove(this);}}''',
'com/luma/media/ThumbnailView.java':'package com.luma.media;import android.content.Context;import android.view.View;class ThumbnailView extends View {ThumbnailView(Context c){}}',
'com/luma/media/SpinnerContractTest.java':'''package com.luma.media;
import android.content.Context;import android.animation.ObjectAnimator;
public class SpinnerContractTest {
 static int checks;static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
 static void count(int n,String message){check(ObjectAnimator.active.size()==n,message);}
 public static void main(String[] args){
  SpinningThumbnailView v=new SpinningThumbnailView(new Context());v.bindUrl("a");v.playback(true,true);count(0,"unattached never spins");
  v.onAttachedToWindow();count(1,"selected playing art starts when attached");ObjectAnimator animation=ObjectAnimator.active.iterator().next();check(animation.duration==16000&&animation.repeats==-1,"slow continuous rotation");
  v.playback(true,true);count(1,"no duplicate animator on updates");v.setRotation(127);v.playback(true,false);count(0,"pause stops CPU animation");check(v.getRotation()==127,"pause keeps angle");
  v.playback(true,true);count(1,"resume starts once");animation=ObjectAnimator.active.iterator().next();check(animation.from==127&&animation.to==487,"resume from angle");
  v.onStartTemporaryDetach();count(0,"list recycling temporary detach stops");v.onFinishTemporaryDetach();v.drain();count(1,"reattaching selected playing art resumes");
  v.onWindowVisibilityChanged(8);count(0,"hidden window stops");v.onWindowVisibilityChanged(0);count(1,"visible window resumes");
  v.onVisibilityChanged(v,8);count(0,"hidden view stops");v.onVisibilityChanged(v,0);count(1,"visible view resumes");
  v.playback(false,true);count(0,"unselected never spins even during playback");check(v.getRotation()==0,"unselected resets angle");
  v.playback(true,true);v.bindUrl("b");count(0,"recycled URI stops old animation");check(v.getRotation()==0&&v.mediaUrl.equals("b"),"recycled URI resets art state");
  v.playback(true,true);v.onDetachedFromWindow();count(0,"detached view cancels global animation");v.onAttachedToWindow();count(1,"reattach resumes desired state");
  v.playback(true,false);v.onDetachedFromWindow();v.onAttachedToWindow();count(0,"paused art stays paused across attach");
  SpinningThumbnailView first=new SpinningThumbnailView(new Context());first.bindUrl("first");first.shown(false);first.onAttachedToWindow();first.playback(true,true);count(0,"first measurement view hidden by ancestor waits");check(first.observer.listeners.size()==1,"one-shot pre-draw armed once");first.playback(true,true);check(first.observer.listeners.size()==1,"repeated bind does not duplicate readiness listeners");first.shown(true);first.observer.draw();count(1,"first item starts after parent visibility settles, without another playback event");check(first.observer.listeners.isEmpty(),"pre-draw readiness listener removes itself");
  first.onStartTemporaryDetach();count(0,"measurement scrap stops animator");first.playback(true,true);count(0,"rebind while temporarily detached must not start even if Android isShown reports true");first.onFinishTemporaryDetach();first.observer.draw();count(1,"recycled first item starts on actual pre-draw");first.drain();count(1,"posted callback does not duplicate pre-draw animator");
  first.playback(true,false);count(0,"first item pause stops");first.onSizeChanged(0,0,44,44);first.playback(true,true);count(0,"zero-size selected row cannot start animator");first.onSizeChanged(44,44,0,0);count(1,"first layout size starts selected art");first.onDetachedFromWindow();count(0,"first row detaches cleanly");check(first.observer.listeners.isEmpty()&&first.posted.isEmpty(),"no detached readiness work retained");
  first.shown(false);first.onAttachedToWindow();check(first.observer.listeners.size()==1,"hidden active row awaits readiness");first.playback(true,false);check(first.observer.listeners.isEmpty(),"pause clears pending pre-draw");first.shown(true);first.observer.draw();count(0,"late pre-draw cannot restart paused item");first.onDetachedFromWindow();
  System.out.println("PASS: "+checks+" actual SpinningThumbnailView lifecycle assertions: selected-only rotation, pause/resume, URI reuse, visibility, list temporary detach and permanent detach.");
  System.out.println("NOTE: Java view/animator doubles, not real Android rendering or animation scheduling.");
 }
}'''
}
with TemporaryDirectory(prefix='tuhkar-spinner-') as temp:
    root=Path(temp);files=[]
    for name,content in stubs.items():
        file=root/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(content);files.append(str(file))
    actual=str(Path('app/src/com/luma/media/SpinningThumbnailView.java').resolve())
    subprocess.run(['javac','-d',str(root/'classes'),*files,actual],check=True)
    subprocess.run(['java','-cp',str(root/'classes'),'com.luma.media.SpinnerContractTest'],check=True)
