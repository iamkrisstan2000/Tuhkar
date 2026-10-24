# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Real RingPlayButton against small Java Android/hold-button stubs.
Validates event routing only, not real Android dispatch, drawing or timing.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
stubs = {
'android/content/Context.java':'package android.content;public class Context {}',
'android/content/res/Resources.java':'package android.content.res;public class Resources {public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}',
'android/util/DisplayMetrics.java':'package android.util;public class DisplayMetrics {public float density=1;}',
'android/os/Bundle.java':'package android.os;public class Bundle {public float value;public float getFloat(String key){return value;}}',
'android/graphics/Paint.java':'package android.graphics;public class Paint {public static final int ANTI_ALIAS_FLAG=1;public enum Style{STROKE,FILL}public enum Cap{ROUND}public Paint(int flags){}public void setStyle(Style s){}public void setStrokeWidth(float w){}public void setStrokeCap(Cap c){}public void setColor(int c){}public void setAlpha(int a){}}',
'android/graphics/RectF.java':'package android.graphics;public class RectF {float l,r;public void set(float l,float t,float r,float b){this.l=l;this.r=r;}public float width(){return r-l;}}',
'android/graphics/Canvas.java':'package android.graphics;public class Canvas {public void drawArc(RectF r,float start,float end,boolean center,Paint p){}public void drawCircle(float x,float y,float radius,Paint p){}}',
'android/view/MotionEvent.java':'package android.view;public class MotionEvent {public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,ACTION_POINTER_DOWN=5;final int action;final float x,y;public MotionEvent(int a,float x,float y){action=a;this.x=x;this.y=y;}public int getActionMasked(){return action;}public float getX(){return x;}public float getY(){return y;}}',
'android/view/KeyEvent.java':'package android.view;public class KeyEvent {public static final int KEYCODE_DPAD_LEFT=21,KEYCODE_DPAD_RIGHT=22;}',
'android/view/ViewConfiguration.java':'package android.view;import android.content.Context;public class ViewConfiguration {public static ViewConfiguration get(Context c){return new ViewConfiguration();}public int getScaledTouchSlop(){return 8;}}',
'android/view/ViewParent.java':'package android.view;public class ViewParent {public boolean blocked;public void requestDisallowInterceptTouchEvent(boolean b){blocked=b;}}',
'android/view/accessibility/AccessibilityNodeInfo.java':'''package android.view.accessibility;
public class AccessibilityNodeInfo {
 public static final int ACTION_SCROLL_FORWARD=4096,ACTION_SCROLL_BACKWARD=8192;public static final String ACTION_ARGUMENT_PROGRESS_VALUE="progress";
 public static class AccessibilityAction {int id;public AccessibilityAction(int id){this.id=id;}public int getId(){return id;}public static final AccessibilityAction ACTION_SET_PROGRESS=new AccessibilityAction(100),ACTION_SCROLL_FORWARD=new AccessibilityAction(4096),ACTION_SCROLL_BACKWARD=new AccessibilityAction(8192);}
 public static class RangeInfo {public static final int RANGE_TYPE_FLOAT=1;public static RangeInfo obtain(int type,float min,float max,float current){return new RangeInfo();}}
 public void setRangeInfo(RangeInfo r){}public void addAction(AccessibilityAction a){}
}''',
'com/luma/media/HoldPlayButton.java':'''package com.luma.media;
import android.content.Context;import android.content.res.Resources;import android.graphics.Canvas;import android.os.Bundle;import android.view.*;import android.view.accessibility.AccessibilityNodeInfo;
class HoldPlayButton {
 boolean holding;final Runnable tap;final ViewParent parent=new ViewParent();
 HoldPlayButton(Context c,int color,Runnable tap,Runnable stop,Runnable changed){this.tap=tap;}
 Resources getResources(){return new Resources();}int getWidth(){return 64;}int getHeight(){return 64;}ViewParent getParent(){return parent;}boolean isEnabled(){return true;}
 void setPressed(boolean p){}void invalidate(){}void setContentDescription(String label){}void cancelHold(){holding=false;}boolean isHolding(){return holding;}
 protected float holdRingInset(){return 4;}protected void onDraw(Canvas c){}
 public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()==0){cancelHold();holding=true;}else if(e.getActionMasked()==1){if(holding){holding=false;tap.run();}}else if(e.getActionMasked()==3||e.getActionMasked()==5)cancelHold();return true;}
 public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo i){}public boolean performAccessibilityAction(int a,Bundle b){return false;}public boolean onKeyDown(int k,KeyEvent e){return false;}
}''',
'com/luma/media/RingGestureContractTest.java':'''package com.luma.media;
import android.content.Context;import android.view.MotionEvent;import android.view.KeyEvent;import android.os.Bundle;import android.view.accessibility.AccessibilityNodeInfo;
public class RingGestureContractTest {
 static int checks,taps,seeks;static long target;static RingPlayButton button;
 static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
 static void event(int action,float x,float y){button.onTouchEvent(new MotionEvent(action,x,y));}
 static void reset(){button.media("item"+checks);button.progress(50000,100000,false);seeks=0;taps=0;target=-1;}
 public static void main(String[] args){
  button=new RingPlayButton(new Context(),0,0,()->taps++,()->{},()->{},value->{seeks++;target=value;});
  reset();event(0,32,32);check(button.isHolding(),"center starts hold");event(1,32,32);check(taps==1&&seeks==0,"center tap only");
  reset();event(0,62,32);check(!button.isHolding()&&button.parent.blocked,"ring captures without hold");event(1,62,32);check(seeks==1&&target==25000&&taps==0,"quarter ring seeks only");check(!button.parent.blocked,"release parent interception");
  reset();event(0,62,32);event(2,32,62);event(1,32,62);check(target==50000&&taps==0,"drag clockwise to half");
  reset();event(0,62,32);event(3,62,32);event(1,62,32);check(seeks==0&&taps==0&&!button.isHolding(),"cancel prevents seek and play");
  reset();event(0,62,32);event(5,62,32);event(1,62,32);check(seeks==0&&taps==0,"second pointer cancels");
  reset();event(0,32,32);event(2,54,32);event(1,54,32);check(seeks==0&&taps==0&&!button.isHolding(),"drag starting in center cancels rather than seek/stop");
  reset();event(0,62,32);event(2,150,150);event(1,150,150);check(seeks==0&&taps==0,"drag far off cancels");
  reset();event(0,62,32);event(1,32,32);check(seeks==0&&taps==0,"release at center cancels seek");
  reset();event(0,0,0);event(1,0,0);check(seeks==0&&taps==0&&!button.isHolding(),"square corners are neither center nor valid ring");
  reset();button.progress(0,0,true);event(0,62,32);event(1,62,32);check(seeks==0&&taps==0,"live/unknown duration ring inactive");
  reset();event(0,62,32);button.media("new item");event(1,62,32);check(seeks==0&&taps==0,"track change cancels gesture");
  reset();event(0,62,32);button.cancelHold();event(1,62,32);check(seeks==0&&taps==0,"window/lifecycle cancellation also cancels ring");
  reset();Bundle argsBundle=new Bundle();argsBundle.value=750;check(button.performAccessibilityAction(100,argsBundle)&&target==75000,"accessible set progress");
  reset();button.performAccessibilityAction(4096,null);check(target==60000,"accessible forward ten seconds");
  reset();button.performAccessibilityAction(8192,null);check(target==40000,"accessible back ten seconds");
  reset();button.onKeyDown(22,new KeyEvent());check(target==60000,"keyboard right seeks");
  reset();button.onKeyDown(21,new KeyEvent());check(target==40000,"keyboard left seeks");
  check(button.holdRingInset()==12,"stop ring is inside the seek ring");
  System.out.println("PASS: "+checks+" real RingPlayButton routing assertions: center/outer separation, seek, multi-touch/movement/lifecycle cancellation, live mode, keyboard and accessibility seeking.");
  System.out.println("NOTE: Java Android/hold-button stubs, not real touch dispatch, hold timing, pixels or media playback.");
 }
}'''
}
with TemporaryDirectory(prefix='tuhkar-ring-') as temp:
    root=Path(temp);files=[]
    for name,content in stubs.items():
        file=root/name;file.parent.mkdir(parents=True,exist_ok=True);file.write_text(content);files.append(str(file))
    actual=[str(Path('app/src/com/luma/media/'+name+'.java').resolve()) for name in ['RingPlayButton','SeekRingMath']]
    subprocess.run(['javac','-d',str(root/'classes'),*files,*actual],check=True)
    subprocess.run(['java','-cp',str(root/'classes'),'com.luma.media.RingGestureContractTest'],check=True)
