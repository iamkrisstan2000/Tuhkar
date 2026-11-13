# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Real card touch/draw/accessibility logic with deterministic framework doubles."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
stubs={
'android/content/Context.java':'package android.content;public class Context {}',
'android/content/res/Resources.java':'package android.content.res;public class Resources {public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}',
'android/util/DisplayMetrics.java':'package android.util;public class DisplayMetrics {public float density=1;}',
'android/animation/ValueAnimator.java':'package android.animation;public class ValueAnimator {public static boolean enabled=true;public static boolean areAnimatorsEnabled(){return enabled;}}',
'android/os/SystemClock.java':'package android.os;public class SystemClock {public static long now;public static long uptimeMillis(){return now;}}',
'android/os/Bundle.java':'package android.os;public class Bundle {public float value;public float getFloat(String k,float d){return value;}}',
'android/graphics/Color.java':'package android.graphics;public class Color {public static final int TRANSPARENT=0;public static int red(int c){return c>>16&255;}public static int green(int c){return c>>8&255;}public static int blue(int c){return c&255;}public static int argb(int a,int r,int g,int b){return a<<24|r<<16|g<<8|b;}}',
'android/graphics/Paint.java':'package android.graphics;public class Paint {public static final int ANTI_ALIAS_FLAG=1;public enum Style {FILL,STROKE}public int alpha=255;public Paint(int f){}public void setStyle(Style s){}public void setColor(int c){}public void setAlpha(int a){alpha=a;}public void setStrokeWidth(float f){}public void setShader(Shader s){}}',
'android/graphics/Shader.java':'package android.graphics;public class Shader {public enum TileMode {CLAMP}}',
'android/graphics/RadialGradient.java':'package android.graphics;public class RadialGradient extends Shader {public static int last;public RadialGradient(float x,float y,float r,int[] colors,float[] pos,TileMode t){last=colors[0]>>>24;}}',
'android/graphics/Path.java':'package android.graphics;public class Path {public enum Direction {CW}public void reset(){}public void close(){}public void addRoundRect(float a,float b,float c,float d,float e,float f,Direction q){}public void moveTo(float x,float y){}public void lineTo(float x,float y){}}',
'android/graphics/Rect.java':'package android.graphics;public class Rect {public int l,t,r,b;public void set(int l,int t,int r,int b){this.l=l;this.t=t;this.r=r;this.b=b;}public boolean contains(int x,int y){return x>=l&&x<r&&y>=t&&y<b;}}',
'android/graphics/Canvas.java':'package android.graphics;public class Canvas {public int saves,restores,paths,rects;public float end;public int save(){return ++saves;}public void clipPath(Path p){}public void drawRect(float a,float b,float c,float d,Paint p){rects++;end=c;}public void drawPath(Path p,Paint q){paths++;}public void restoreToCount(int c){restores++;}}',
'android/view/MotionEvent.java':'package android.view;public class MotionEvent {public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,ACTION_POINTER_DOWN=5;int a;float x,y;public MotionEvent(int a,float x,float y){this.a=a;this.x=x;this.y=y;}public int getActionMasked(){return a;}public float getX(){return x;}public float getY(){return y;}}',
'android/view/ViewConfiguration.java':'package android.view;public class ViewConfiguration {public static ViewConfiguration get(android.content.Context c){return new ViewConfiguration();}public int getScaledTouchSlop(){return 8;}}',
'android/view/View.java':'''package android.view;public class View {public static final int VISIBLE=0;public int w=200,h=72,x,frames,clicks,visibility;public boolean attached=true,shown=true;public int getWidth(){return w;}public int getHeight(){return h;}public void requestDisallowInterceptTouchEvent(boolean v){}public View getParent(){return this;}public void invalidate(){}public void postInvalidateDelayed(long ms){frames++;}public boolean isAttachedToWindow(){return attached;}public boolean isShown(){return shown;}public int getWindowVisibility(){return visibility;}public boolean performClick(){clicks++;return true;}public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo n){}public boolean performAccessibilityAction(int action,android.os.Bundle b){return false;}}''',
'android/view/accessibility/AccessibilityNodeInfo.java':'''package android.view.accessibility;public class AccessibilityNodeInfo {public static final int ACTION_SCROLL_FORWARD=4096,ACTION_SCROLL_BACKWARD=8192;public static final String ACTION_ARGUMENT_PROGRESS_VALUE="progress";public int actions;public RangeInfo range;public void setRangeInfo(RangeInfo r){range=r;}public void addAction(AccessibilityAction a){actions++;}public static class RangeInfo {public static final int RANGE_TYPE_FLOAT=1;public static RangeInfo obtain(int t,float a,float b,float c){return new RangeInfo();}}public static class AccessibilityAction {public static final AccessibilityAction ACTION_SET_PROGRESS=new AccessibilityAction(1),ACTION_SCROLL_FORWARD=new AccessibilityAction(4096),ACTION_SCROLL_BACKWARD=new AccessibilityAction(8192);int id;public AccessibilityAction(int id){this.id=id;}public int getId(){return id;}}}''',
'android/widget/FrameLayout.java':'''package android.widget;public class FrameLayout extends android.view.View {public FrameLayout(android.content.Context c){}public android.content.res.Resources getResources(){return new android.content.res.Resources();}public void setClickable(boolean b){}public void setFocusable(boolean b){}public void setWillNotDraw(boolean b){}protected void onSizeChanged(int w,int h,int ow,int oh){}protected void dispatchDraw(android.graphics.Canvas c){}public void offsetDescendantRectToMyCoords(android.view.View v,android.graphics.Rect r){r.l+=v.x;r.r+=v.x;}public boolean onInterceptTouchEvent(android.view.MotionEvent e){return false;}public boolean onTouchEvent(android.view.MotionEvent e){return false;}}''',
'com/luma/media/MiniPlayerCardContract.java':'''package com.luma.media;import android.view.*;import android.graphics.*;import android.view.accessibility.*;public class MiniPlayerCardContract {
static int checks,seeks;static float sought;static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}static MotionEvent e(int a,float x,float y){return new MotionEvent(a,x,y);}static Canvas draw(MiniPlayerCard c){Canvas canvas=new Canvas();c.dispatchDraw(canvas);return canvas;}
public static void main(String[] args){MiniPlayerCard c=new MiniPlayerCard(new android.content.Context(),1,2,f->{seeks++;sought=f;});c.onSizeChanged(200,72,0,0);c.media(50,100,false,false,true);Canvas canvas=draw(c);check(canvas.saves==canvas.restores&&canvas.paths==2);check(c.frames==0);
c.onTouchEvent(e(0,20,20));c.onTouchEvent(e(1,20,20));check(c.clicks==1&&seeks==0);
c.onTouchEvent(e(0,20,20));c.onTouchEvent(e(2,100,20));c.onTouchEvent(e(1,150,20));check(seeks==1&&sought==.75f&&c.clicks==1);
c.onTouchEvent(e(0,20,20));c.onTouchEvent(e(2,100,20));c.onTouchEvent(e(3,150,20));check(seeks==1&&c.clicks==1);
c.onTouchEvent(e(0,20,20));c.onTouchEvent(e(2,20,50));c.onTouchEvent(e(1,20,50));check(c.clicks==1&&seeks==1);
View control=new View();control.x=150;control.w=50;c.controls(control);c.onInterceptTouchEvent(e(0,170,20));check(!c.onInterceptTouchEvent(e(2,20,20)));c.controls();c.onInterceptTouchEvent(e(0,20,20));check(c.onInterceptTouchEvent(e(2,120,20)));c.onTouchEvent(e(3,120,20));
c.media(0,0,true,true,true);c.onTouchEvent(e(0,20,20));c.onTouchEvent(e(2,120,20));c.onTouchEvent(e(1,120,20));check(seeks==1);canvas=draw(c);check(canvas.paths==0);int baseline=RadialGradient.last;c.energy(1);draw(c);check(RadialGradient.last>baseline);
c.media(0,0,false,true,true);draw(c);check(RadialGradient.last==5);
c.media(20,100,true,false,true);int frames=c.frames;draw(c);check(c.frames>frames);c.shown=false;frames=c.frames;draw(c);check(c.frames==frames);c.shown=true;android.animation.ValueAnimator.enabled=false;draw(c);check(c.frames==frames);android.animation.ValueAnimator.enabled=true;
AccessibilityNodeInfo info=new AccessibilityNodeInfo();c.onInitializeAccessibilityNodeInfo(info);check(info.actions==3&&info.range!=null);android.os.Bundle args2=new android.os.Bundle();args2.value=.9f;check(c.performAccessibilityAction(1,args2)&&sought==.9f);c.media(0,0,true,true,true);check(!c.performAccessibilityAction(1,args2));
c.media(20,100,false,false,true);c.onTouchEvent(e(0,20,20));c.onTouchEvent(e(2,100,20));c.onTouchEvent(e(5,100,20));int before=seeks;c.onTouchEvent(e(1,180,20));check(seeks==before);
System.out.println("PASS: "+checks+" actual whole-card checks: click/drag/cancel, vertical and button exclusions, FM no-seek/real-energy glow, paused/reduced-motion/hidden drawing, clipping and accessible seeking");}}
'''
}
with TemporaryDirectory() as d:
    root=Path(d)
    for name,text in stubs.items():
        p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
    for name in ['MiniPlayerCard','WaveProgressMath','NavSwipeMath','AudioGlowMath','LiquidFlowMath','LiquidContrastStyle','SurfacePalette','LightSurfaceStyle','PageGradient']:
        (root/f'com/luma/media/{name}.java').write_text(Path(f'app/src/com/luma/media/{name}.java').read_text())
    subprocess.run(['javac','-d',d]+[str(p) for p in root.rglob('*.java')],check=True)
    subprocess.run(['java','-cp',d,'com.luma.media.MiniPlayerCardContract'],check=True)
