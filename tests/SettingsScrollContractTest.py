# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Actual Settings capture/restore methods with view/layout doubles, not Android runtime layout."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
source=Path('app/src/com/luma/media/MainActivity.java').read_text()
a=source.index('    void rememberSettingsScroll()');b=source.index('    void renderSettings()',a);actual=source[a:b]
stubs={
'android/view/ViewTreeObserver.java':'''package android.view;import java.util.*;public class ViewTreeObserver {public interface OnGlobalLayoutListener{void onGlobalLayout();}public List<OnGlobalLayoutListener> listeners=new ArrayList<>();public boolean isAlive(){return true;}public void addOnGlobalLayoutListener(OnGlobalLayoutListener l){listeners.add(l);}public void removeOnGlobalLayoutListener(OnGlobalLayoutListener l){listeners.remove(l);}public void fire(){for(OnGlobalLayoutListener l:new ArrayList<>(listeners))l.onGlobalLayout();}}''',
'android/view/View.java':'''package android.view;public class View {public Object tag;public int top,height;public boolean attached=true;public Object getTag(){return tag;}public int getTop(){return top;}public int getHeight(){return height;}public boolean isAttachedToWindow(){return attached;}}''',
'android/widget/LinearLayout.java':'''package android.widget;import android.view.*;import java.util.*;public class LinearLayout extends View {public List<View> children=new ArrayList<>();public int getChildCount(){return children.size();}public View getChildAt(int i){return children.get(i);}public View findViewWithTag(Object tag){for(View v:children)if(tag.equals(v.tag))return v;return null;}}''',
'android/widget/ScrollView.java':'''package android.widget;import android.view.*;public class ScrollView extends View {public interface OnScrollChangeListener{void onScrollChange(View v,int x,int y,int oldX,int oldY);}OnScrollChangeListener listener;public int y;ViewTreeObserver observer=new ViewTreeObserver();public int getScrollY(){return y;}public int getPaddingTop(){return 0;}public int getPaddingBottom(){return 0;}public void setOnScrollChangeListener(OnScrollChangeListener l){listener=l;}public ViewTreeObserver getViewTreeObserver(){return observer;}public void scrollTo(int x,int target){int old=y;y=target;if(listener!=null)listener.onScrollChange(this,x,y,0,old);}}''',
'com/luma/media/SettingsScrollContract.java':'''package com.luma.media;import android.view.*;import android.widget.*;
public class SettingsScrollContract {
 ScrollView settingsScroll;LinearLayout settingsPage;String settingsAnchor;int settingsScrollY,settingsAnchorOffset;boolean settingsRestoring;
'''+actual+'''
 static int checks;static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
 static View card(String title,int top,int height){View v=new View();v.tag="settings:"+title;v.top=top;v.height=height;return v;}
 static ScrollView scroll(){ScrollView v=new ScrollView();v.height=800;return v;}
 static LinearLayout page(int accentTop){LinearLayout p=new LinearLayout();p.height=accentTop+1500;p.children.add(card("Media access",1200,600));p.children.add(card("Accent color",accentTop,800));return p;}
 public static void main(String[] args){
  SettingsScrollContract s=new SettingsScrollContract();ScrollView old=scroll();LinearLayout before=page(1900);s.settingsScroll=old;s.settingsPage=before;old.y=2050;s.rememberSettingsScroll();check(s.settingsAnchor.equals("settings:Accent color")&&s.settingsAnchorOffset==150&&s.settingsScrollY==2050,"capture current section plus intra-card offset");
  ScrollView fresh=scroll();LinearLayout after=page(2300);s.restoreSettingsScroll(fresh,after);s.rememberSettingsScroll();check(s.settingsScrollY==2050,"pending restore never overwrites saved location with zero");fresh.getViewTreeObserver().fire();check(fresh.y==2450&&!s.settingsRestoring,"height changes above section preserve the same visible portion");check(fresh.getViewTreeObserver().listeners.isEmpty(),"one-shot layout listener removed");
  fresh.scrollTo(0,1250);check(s.settingsAnchor.equals("settings:Media access")&&s.settingsAnchorOffset==50,"user scrolling updates remembered section");
  for(int i=0;i<20;i++){ScrollView again=scroll();s.restoreSettingsScroll(again,page(2300));again.getViewTreeObserver().fire();check(again.y==1250,"repeated changes do not jump to top or drift");}
  ScrollView obsolete=scroll(),newer=scroll();s.restoreSettingsScroll(obsolete,page(2300));s.restoreSettingsScroll(newer,page(2300));obsolete.getViewTreeObserver().fire();check(s.settingsScroll==newer&&s.settingsRestoring,"stale restore cannot touch newer Settings render");newer.getViewTreeObserver().fire();check(newer.y==1250&&!s.settingsRestoring,"newest restore completes");
  s.settingsAnchor="settings:removed";s.settingsScrollY=99999;ScrollView shortened=scroll();LinearLayout shortPage=page(1900);s.restoreSettingsScroll(shortened,shortPage);shortened.getViewTreeObserver().fire();check(shortened.y==shortPage.height-800,"missing section falls back to clamped old position");
  check(SettingsScrollAnchor.restore(900,100,50,400,2000)==149,"shrunk card retains section, clamps internal offset");check(SettingsScrollAnchor.restore(-100,-1,0,-5,-10)==0,"negative state bounded");check(SettingsScrollAnchor.restore(0,Integer.MAX_VALUE,100,99,500)==500,"overflow-safe upper clamp");
  s.settingsScroll=null;s.settingsPage=null;s.settingsAnchor=null;s.settingsScrollY=0;s.settingsAnchorOffset=0;s.rememberSettingsScroll();check(s.settingsScrollY==0&&s.settingsAnchor==null,"Reset-cleared location stays clear");
  System.out.println("PASS: "+checks+" actual Settings scroll capture/restore checks: section anchoring, changing heights, repeated edits, user scroll, pending/stale layout guards, clamping and reset state");
  System.out.println("NOTE: deterministic Java view/layout doubles; not Android UI traversal, rotation or phone permission-return validation.");
 }
}'''
}
with TemporaryDirectory(prefix='tuhkar-settings-scroll-') as temp:
    root=Path(temp);files=[]
    for name,text in stubs.items():
        p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(str(p))
    subprocess.run(['javac','-encoding','UTF-8','-d',str(root/'classes'),*files,str(Path('app/src/com/luma/media/SettingsScrollAnchor.java').resolve())],check=True)
    subprocess.run(['java','-cp',str(root/'classes'),'com.luma.media.SettingsScrollContract'],check=True)
