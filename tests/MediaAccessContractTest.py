# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Execute actual permission and setup actions with Java framework doubles, not Android dialogs."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
source=Path('app/src/com/luma/media/MainActivity.java').read_text()
def method(name):
    start=source.index('    '+name)
    pos=source.index('{',start);depth=1;end=pos+1
    while depth:
        if source[end]=='{':depth+=1
        if source[end]=='}':depth-=1
        end+=1
    return source[start:end]
actual='\n'.join(method(name) for name in ['boolean musicAccess()', 'boolean videoAccess()', 'boolean selectedVideoAccess()', 'void requestMedia()', 'void setupGo(', 'void finishSetup(', 'boolean standardFolderOnly(', 'void setupLibrary(', 'ArrayList<String> setupMissingPermissions()', 'void setupPermissionResult()', 'void setupRequestPermissions()', 'void setupReviewContinue()', 'void setupBack()', 'void setupPrimaryAction('])
java='''import java.util.*;
public class MediaAccessContract {
 static class Build {static class VERSION {static int SDK_INT;}}
 static class Manifest {static class permission {static final String POST_NOTIFICATIONS="notifications",RECORD_AUDIO="record",READ_MEDIA_AUDIO="audio",READ_MEDIA_VIDEO="video",READ_EXTERNAL_STORAGE="legacy",READ_MEDIA_VISUAL_USER_SELECTED="selected";}}
 static class Prefs {Map<String,Object> data=new HashMap<>();boolean getBoolean(String key,boolean fallback){Object v=data.get(key);return v==null?fallback:(Boolean)v;}Prefs edit(){return this;}Prefs putBoolean(String k,boolean v){data.put(k,v);return this;}Prefs putInt(String k,int v){data.put(k,v);return this;}Prefs remove(String k){data.remove(k);return this;}void apply(){}}
 interface Action {void click(Object d,int w);}
 class Dialog {Dialog setMessage(String s){return this;}Dialog setPositiveButton(String s,Action a){return this;}Dialog setNegativeButton(String s,Object a){return this;}void show(){dialogs++;}}
 Prefs prefs=new Prefs();Set<String> granted=new HashSet<>(),rationale=new HashSet<>();List<String> requested=new ArrayList<>();List<String> localMusic=new ArrayList<>(),localVideo=new ArrayList<>();int requests;int code,dialogs,opened,renders,locals,onlines,generation,setupStep,tab;boolean setupOpen=true,settingsOpen,expanded;String query="",sourceStatus="";boolean musicFolder,videoFolder;
 boolean allowed(String p){return granted.contains(p);}boolean shouldShowRequestPermissionRationale(String p){return rationale.contains(p);}void requestPermissions(String[] p,int c){requested=Arrays.asList(p);code=c;requests++;}void requestGlowPermission(){prefs.putBoolean("askedGlowPermission",true);requestPermissions(new String[]{"record"},40);}void openAccessSettings(){opened++;}Dialog dialog(String s){return new Dialog();}void render(){renders++;}void loadLocal(){locals++;}void loadOnline(){onlines++;}boolean hasFolders(int t){return t==0?musicFolder:videoFolder;}
''' + actual + '''
 static int checks;
 static void check(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 static MediaAccessContract phone(int api){Build.VERSION.SDK_INT=api;return new MediaAccessContract();}
 public static void main(String[] args){
  MediaAccessContract p=phone(26);check(p.standardFolderOnly(0)&&!p.standardFolderOnly(2),"Music defaults ON and Movies defaults OFF");p.prefs.putBoolean("musicStandardFolderOnly",false).putBoolean("videoStandardFolderOnly",true);check(!p.standardFolderOnly(0)&&p.standardFolderOnly(2),"explicit standard-folder choices preserved");p.requestMedia();check(p.requested.equals(Arrays.asList("legacy"))&&p.code==10,"legacy permission only");check(p.prefs.getBoolean("asked:legacy",false),"asked state saved");
  p=phone(32);p.granted.add("legacy");p.requestMedia();check(p.opened==1&&p.requested.isEmpty(),"manage granted pre-33 access");
  p=phone(33);p.requestMedia();check(p.requested.equals(Arrays.asList("audio","video")),"separate granular permissions");
  p=phone(33);p.granted.add("audio");p.requestMedia();check(p.requested.equals(Arrays.asList("video")),"do not re-request audio");
  p=phone(34);p.granted.add("audio");p.requestMedia();check(p.requested.equals(Arrays.asList("video","selected")),"Android14 selected-video permission paired with video");
  p=phone(35);p.granted.addAll(Arrays.asList("audio","selected"));p.prefs.putBoolean("asked:video",true);p.requestMedia();check(p.requested.equals(Arrays.asList("video","selected")),"expand selected video access");check(!p.videoAccess()&&p.selectedVideoAccess(),"selected is not full");
  p=phone(35);p.granted.addAll(Arrays.asList("audio","video"));p.requestMedia();check(p.opened==1&&p.requested.isEmpty(),"manage full grants");
  p=phone(33);p.prefs.putBoolean("asked:audio",true).putBoolean("asked:video",true);p.requestMedia();check(p.dialogs==1&&p.requested.isEmpty(),"blocked permissions show recovery dialog, not a dead prompt");
  p=phone(33);p.prefs.putBoolean("asked:audio",true).putBoolean("asked:video",true);p.rationale.add("audio");p.requestMedia();check(p.requested.equals(Arrays.asList("audio")),"only request retryable grants");
  for(int api:new int[]{26,32,33,34,35}){p=phone(api);p.setupGo(-5);check(p.setupStep==0&&p.renders==1,"clamp setup start");p.setupGo(99);check(p.setupStep==4&&!(Boolean)p.prefs.data.get("setupComplete"),"persist incomplete setup");check(p.locals==0&&p.onlines==0,"setup navigation has no scans/network load");p.finishSetup(false);check(!p.setupOpen&&(Boolean)p.prefs.data.get("setupComplete")&&!p.prefs.data.containsKey("setupStep"),"finish persisted");check(p.tab==0&&p.locals==1,"normal setup always opens Music, even without grants");}
  p=phone(35);p.granted.add("audio");p.finishSetup(false);check(p.tab==0&&p.locals==1,"music grant selects library");
  p=phone(35);p.granted.add("selected");p.finishSetup(false);check(p.tab==0&&p.locals==1,"selected video grant does not change Music landing");
  
  
  for(int api:new int[]{26,32,33,34,35}){
   p=phone(api);p.setupPrimaryAction();check(p.setupStep==1&&p.requested.isEmpty(),"Welcome Set it Up opens notice without OS prompts");p.setupBack();check(p.setupStep==0&&p.requested.isEmpty(),"notice Back returns Welcome without prompts");
   p.setupLibrary();p.setupPrimaryAction();check(p.setupStep==2&&p.code==50&&p.requests==1,"one explicit Next launches one permission batch");
   check(p.requested.contains("record"),"batch requests playback analysis");check(p.requested.contains(api>=33?"audio":"legacy"),"correct API media read permission");check(p.requested.contains("notifications")== (api>=33),"notifications only API33+");check(p.requested.contains("selected")== (api>=34),"selected-video option only API34+");
   int count=p.requests;p.setupRequestPermissions();check(p.requests==count,"duplicate batch action while waiting ignored");p.setupBack();check(p.setupStep==2&&p.requests==count,"underlying waiting Back does not restart/escape request");
   p.granted.addAll(p.requested);p.setupPermissionResult();check(p.setupStep==4&&p.requests==1,"all grants go straight to Congratulations, no repeated permission screen");p.setupPrimaryAction();check(p.tab==0&&!p.setupOpen&&p.locals==1,"Congratulations Get started opens Music");
  }
  p=phone(35);p.setupGo(1);p.setupPrimaryAction();p.granted.addAll(Arrays.asList("audio","video","selected","notifications"));p.setupPermissionResult();check(p.setupStep==3&&p.setupMissingPermissions().equals(Arrays.asList("record")),"declined recording shows only missing feature in review");p.rationale.add("record");p.setupPrimaryAction();check(p.requested.equals(Arrays.asList("record"))&&p.requests==2,"Retry requests only missing retryable permission");p.granted.add("record");p.setupPermissionResult();check(p.setupStep==4,"successful retry reaches Congratulations");
  p=phone(35);p.setupGo(1);p.setupPrimaryAction();p.setupPermissionResult();check(p.setupStep==3,"denial/cancel reaches review, not same notice");p.setupReviewContinue();check(p.setupStep==4&&p.prefs.getBoolean("askedGlowPermission",false),"Continue anyway reaches Congratulations and suppresses later nags");p.setupPrimaryAction();check(!p.setupOpen&&p.tab==0,"Continue anyway still finishes to Music");
  p=phone(35);p.setupGo(1);p.setupPrimaryAction();p.setupPermissionResult();p.setupPrimaryAction();check(p.setupStep==3&&p.opened==1&&p.requests==1,"blocked Retry explicitly opens App settings, not dead prompt loop");p.granted.addAll(Arrays.asList("audio","video","notifications","record"));p.setupPermissionResult();check(p.setupStep==4,"settings grants rechecked into Congratulations");
  p=phone(35);p.granted.addAll(Arrays.asList("audio","selected","notifications","record"));p.setupGo(1);p.setupPrimaryAction();check(p.setupStep==4&&p.requests==0&&p.opened==0,"existing/selected-video grants skip redundant OS requests");
  p=phone(35);p.setupGo(2);p.setupPrimaryAction();check(p.setupStep==3&&p.requests==0,"waiting page can recover after recreation without launching duplicate prompts");
  p=phone(35);p.finishSetup(true);check(p.requested.isEmpty()&&p.opened==0&&p.tab==1&&p.onlines==1,"Skip everything goes directly to FM with no prompts");check(p.prefs.getBoolean("askedGlowPermission",false)&&p.prefs.getBoolean("askedPlaybackNotifications",false),"Skip suppresses later automatic nags");
  p=phone(35);p.setupGo(3);p.setupBack();check(p.setupStep==0,"review Back returns Welcome");p.setupGo(4);p.setupBack();check(p.tab==0&&!p.setupOpen,"Congratulations Back finishes rather than restarting permission steps");
  System.out.println("PASS: "+checks+" actual media-permission/setup action assertions across API26/32/33/34/35");
  System.out.println("NOTE: Java framework doubles; not Android permission dialog, folder picker, lifecycle or device UI validation.");
 }
}'''
with TemporaryDirectory(prefix='tuhkar-access-') as temp:
    root=Path(temp);file=root/'MediaAccessContract.java';file.write_text(java)
    subprocess.run(['javac','-encoding','UTF-8','-d',str(root),str(file)],check=True)
    subprocess.run(['java','-cp',str(root),'MediaAccessContract'],check=True)
# Structural integration checks complement execution above.
assert 'if(setupOpen){renderSetup();return;}' in source
assert 'if(!setupOpen){if(tab==0||tab==2)loadLocal();else loadOnline();}' in source
assert 'MediaAccessPolicy.needsSetup' in source and 'info.lastUpdateTime-info.firstInstallTime>1000' in source
assert 'prefs.getInt("setupFlowVersion",0)==3?' in source
assert 'Run setup again' in source and 'Skip Setup' in source
assert 'if(setupOpen&&setupStep==3)setupPermissionResult();else refreshAccess();' in source
assert 'if(r==10)refreshAccess()' in source
assert 'videoAccess()||selectedVideoAccess()' in source
assert "advancedFoldersOpen" not in source
assert "ACTION_OPEN_DOCUMENT_TREE" not in source
print('PASS: setup gate, existing-install migration, persisted step, skip/replay, permission refresh, partial-video scanning and indexed-library-only integration')

assert 'if(r==50){if(setupOpen&&setupStep==2)setupPermissionResult();return;}' in source
assert 'setupScriptTitle(page,"Welcome")' in source and 'setupScriptTitle(page,"Congratulations!")' in source
welcome=source[source.index('        if(setupStep==0){\n'):source.index('        if(setupStep==4){\n')]
assert welcome.count('setupAction(')==2 and 'Set it Up' in welcome and 'Skip Setup' in welcome
assert 'SETUP · ' not in source
assert 'SetupScript.ttf' in source and Path('app/assets/SetupScript.ttf').read_bytes()[:4]==b'\x00\x01\x00\x00'
assert Path('app/assets/SetupScript-LICENSE.txt').is_file()
print('PASS: single prior notice, one OS permission batch/callback, denial review/retry, welcome-only two choices, cursive bundled headings, logo artwork and migration/recreation recovery')
