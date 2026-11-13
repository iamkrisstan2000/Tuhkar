# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Execute the actual adaptive pane method with small host UI doubles."""
from pathlib import Path
import subprocess,tempfile
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
a=s.index('    void updateLandscapeVideoPane(){');b=s.index('    void updatePlayer(){',a)
method=s[a:b]
java='''public class AdaptivePane {
static class View {static final int VISIBLE=0,GONE=8;int visibility=VISIBLE;void setVisibility(int v){visibility=v;}}
boolean landscape,homeOpen,settingsOpen,setupOpen,expanded,tvMode,visual;int tab;View playerBox=new View();
boolean inlineVideoVisible(){return visual;}
'''+method+'''
public static void main(String[] args){int checks=0;
for(int mask=0;mask<128;mask++)for(int tab=0;tab<4;tab++){
 AdaptivePane p=new AdaptivePane();p.landscape=(mask&1)!=0;p.homeOpen=(mask&2)!=0;p.settingsOpen=(mask&4)!=0;p.setupOpen=(mask&8)!=0;p.expanded=(mask&16)!=0;p.tvMode=(mask&32)!=0;p.visual=(mask&64)!=0;p.tab=tab;
 boolean eligible=p.landscape&&tab>=2&&!p.homeOpen&&!p.settingsOpen&&!p.setupOpen&&!p.expanded&&!p.tvMode;
 p.updateLandscapeVideoPane();int expected=eligible&&!p.visual?View.GONE:View.VISIBLE;
 if(p.playerBox.visibility!=expected)throw new AssertionError("Scope or idle pane visibility");checks++;
}
AdaptivePane p=new AdaptivePane();p.landscape=true;p.tab=3;p.updateLandscapeVideoPane();if(p.playerBox.visibility!=View.GONE)throw new AssertionError("Idle");
p.visual=true;p.updateLandscapeVideoPane();if(p.playerBox.visibility!=View.VISIBLE)throw new AssertionError("Start/loading/paused visual item");
p.visual=false;p.updateLandscapeVideoPane();if(p.playerBox.visibility!=View.GONE)throw new AssertionError("Stop");
p.playerBox=null;p.updateLandscapeVideoPane();
System.out.println("PASS: "+checks+" adaptive Video/TV pane scope cases, idle/start/stop transitions and null guard using actual method");
}}
'''
with tempfile.TemporaryDirectory(prefix='tuhkar-adaptive-') as d:
 p=Path(d)/'AdaptivePane.java';p.write_text(java)
 subprocess.run(['javac',str(p)],check=True)
 subprocess.run(['java','-cp',d,'AdaptivePane'],check=True)
render=s[s.index('    void render(){'):s.index('    void openHome(){')]
assert 'updateLandscapeVideoPane();' in render
assert 'updateLandscapeVideoPane();' in s[s.index('    void updatePlayer(){'):]
assert 'body.addView(content,new LinearLayout.LayoutParams(0,-1,1))' in render
assert 'current!=null&&isVideo' in s[s.index('    boolean inlineVideoVisible()'):].split('\n',1)[0]
print('NOTE: host doubles/source wiring, not on-device measure/layout or video surface validation.')
