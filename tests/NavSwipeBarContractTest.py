# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Actual bottom-bar touch policy with framework doubles; not Android dispatch validation."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
stubs={
'android/content/Context.java':'package android.content;public class Context {}',
'android/content/res/Resources.java':'package android.content.res;public class Resources {public android.util.DisplayMetrics getDisplayMetrics(){return new android.util.DisplayMetrics();}}',
'android/util/DisplayMetrics.java':'package android.util;public class DisplayMetrics {public float density=1;}',
'android/view/ViewConfiguration.java':'package android.view;public class ViewConfiguration {public static ViewConfiguration get(android.content.Context c){return new ViewConfiguration();}public int getScaledTouchSlop(){return 8;}}',
'android/view/MotionEvent.java':'''package android.view;public class MotionEvent {public static final int ACTION_DOWN=0,ACTION_UP=1,ACTION_MOVE=2,ACTION_CANCEL=3,ACTION_POINTER_DOWN=5;int a;float x,y;public MotionEvent(int a,float x,float y){this.a=a;this.x=x;this.y=y;}public int getActionMasked(){return a;}public float getX(){return x;}public float getY(){return y;}}''',
'android/widget/HorizontalScrollView.java':'''package android.widget;public class HorizontalScrollView {public HorizontalScrollView(android.content.Context c){}public android.content.res.Resources getResources(){return new android.content.res.Resources();}public void setTooltipText(String s){}public boolean onInterceptTouchEvent(android.view.MotionEvent e){return false;}public boolean onTouchEvent(android.view.MotionEvent e){return false;}}''',
'com/luma/media/NavSwipeBarContractTest.java':'''package com.luma.media;import android.view.MotionEvent;public class NavSwipeBarContractTest {
static int count,total,checks;static void check(boolean b){checks++;if(!b)throw new AssertionError("check "+checks);}static MotionEvent e(int a,float x,float y){return new MotionEvent(a,x,y);}
public static void main(String[] args){NavSwipeBar bar=new NavSwipeBar(new android.content.Context(),d->{count++;total+=d;});
check(!bar.onInterceptTouchEvent(e(0,120,10)));check(!bar.onInterceptTouchEvent(e(2,124,11)));check(!bar.onInterceptTouchEvent(e(1,124,11)));check(count==0);
bar.onInterceptTouchEvent(e(0,120,10));check(bar.onInterceptTouchEvent(e(2,90,12)));bar.onTouchEvent(e(1,30,12));check(count==1&&total==1);
bar.onInterceptTouchEvent(e(0,20,10));check(bar.onInterceptTouchEvent(e(2,50,12)));bar.onTouchEvent(e(1,110,12));check(count==2&&total==0);
bar.onInterceptTouchEvent(e(0,120,10));check(bar.onInterceptTouchEvent(e(2,100,11)));bar.onTouchEvent(e(1,99,11));check(count==2);
bar.onInterceptTouchEvent(e(0,120,10));check(!bar.onInterceptTouchEvent(e(2,118,40)));check(!bar.onInterceptTouchEvent(e(2,20,40)));check(count==2);
bar.onInterceptTouchEvent(e(0,120,10));bar.onInterceptTouchEvent(e(2,90,10));bar.onTouchEvent(e(3,20,10));bar.onTouchEvent(e(1,20,10));check(count==2);
bar.onInterceptTouchEvent(e(0,120,10));bar.onInterceptTouchEvent(e(2,90,10));bar.onTouchEvent(e(5,80,10));bar.onTouchEvent(e(1,20,10));check(count==2);
bar.onTouchEvent(e(0,120,10));bar.onTouchEvent(e(1,20,10));check(count==3&&total==1);
System.out.println("PASS: "+checks+" actual NavSwipeBar tap, drag, threshold, direction, vertical, cancellation and multitouch checks (framework doubles)");}}
'''
}
with TemporaryDirectory() as d:
    root=Path(d)
    for name,text in stubs.items():
        p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
    for name in ['NavSwipeMath','NavSwipeBar']:
        (root/f'com/luma/media/{name}.java').write_text(Path(f'app/src/com/luma/media/{name}.java').read_text())
    subprocess.run(['javac','-d',d]+[str(p) for p in root.rglob('*.java')],check=True)
    subprocess.run(['java','-cp',d,'com.luma.media.NavSwipeBarContractTest'],check=True)
source=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'void addBottomNav(){HorizontalScrollView scroll=new NavSwipeBar' in source
assert 'void addNavRail(LinearLayout host){ScrollView scroll=new ScrollView' in source
assert 'if(sectionPage==null||!android.animation.ValueAnimator.areAnimatorsEnabled())return;' in source
assert 'cancelSectionTransition();sectionPage=null;' in source
assert 'prefs.getBoolean("backgroundVideoAudio",false)' in source
print('NOTE: real device touch dispatch, accessibility, surface reattachment and visual smoothness still need phone testing.')
