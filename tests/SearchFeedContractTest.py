# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Execute actual directory search and combined merge with fake HTTP/JSON, no live/device claim."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess,ast
module=ast.parse(Path('tests/HomeFeedContractTest.py').read_text())
stubs=next(ast.literal_eval(node.value) for node in module.body if isinstance(node,ast.Assign) and any(isinstance(t,ast.Name) and t.id=='stubs' for t in node.targets))
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
merge=s[s.index('    void mergeMediaSearch('):s.index('    void attachLibrarySearchResults(')]
stub=stubs['com/luma/media/MainActivity.java'].replace('public class MainActivity {','import java.util.*;public class MainActivity {int searchScope;ArrayList<Item> searchResults=new ArrayList<>();'+merge)
stub=stub.replace('public boolean failRadio,failTv;', 'private final java.io.File cache=new java.io.File(System.getProperty("java.io.tmpdir"),"tuhkar-fixture-"+System.nanoTime()); java.io.File getCacheDir(){cache.mkdirs();return cache;}public boolean failRadio,failTv;')
stubs={'org/json/JSONObject.java':stubs['org/json/JSONObject.java'],'org/json/JSONArray.java':stubs['org/json/JSONArray.java'],'com/luma/media/MainActivity.java':stub}
stubs['com/luma/media/SearchFeedContract.java']='''package com.luma.media;import java.util.*;import org.json.*;public class SearchFeedContract {
static int checks;static void check(boolean b){checks++;if(!b)throw new AssertionError("Search check "+checks);}static JSONObject station(String url){return new JSONObject().put("name","Named FM").put("url_resolved",url).put("country","PH").put("codec","MP3");}
public static void main(String[] args){
JSONArray.fixture.add(station("https://radio/one"));JSONArray.fixture.add(station("javascript:bad"));
MainActivity a=new MainActivity();SearchFeed.Result r=SearchFeed.online(a,0,"One","ph","ph");check(r.items.size()==2);check(r.items.get(0).kind.equals("radio"));check(r.items.get(1).title.equals("One")&&r.items.get(1).art.equals("https://art/one"));check(a.paths.get(0).contains("countrycode=PH")&&a.paths.get(0).contains("name=One"));check(a.paths.get(1).contains("countries/ph.m3u"));
a=new MainActivity();r=SearchFeed.online(a,1,"One","all","ph");check(a.requests==1&&r.items.size()==1&&r.items.get(0).kind.equals("radio"));check(!a.paths.get(0).contains("countrycode="));
a=new MainActivity();r=SearchFeed.online(a,2,"TWO","ph","ph");check(a.requests==1&&r.items.size()==1&&r.items.get(0).title.equals("Two"));
r=SearchFeed.online(a,2,"One","ph","ph");check(a.requests==1&&r.items.size()==1&&r.items.get(0).title.equals("One"));new java.io.File(a.getCacheDir(),"tv-ph.m3u").setLastModified(0);r=SearchFeed.online(a,2,"Two","ph","ph");check(a.requests==2&&r.items.size()==1);
a=new MainActivity();a.failRadio=a.failTv=true;r=SearchFeed.online(a,0,"One","ph","ph");check(r.items.isEmpty()&&r.note.contains("Radio unavailable")&&r.note.contains("TV unavailable"));
a=new MainActivity();a.searchScope=1;List<MainActivity.Item> local=Arrays.asList(new MainActivity.Item("One Song","","local/1","music"),new MainActivity.Item("One Song duplicate","","local/1","music"),new MainActivity.Item("One video","","local/2","video"),new MainActivity.Item("Other","","local/3","music"));a.mergeMediaSearch(local,"one",false);check(a.searchResults.size()==1&&a.searchResults.get(0).title.equals("One Song"));
a.mergeMediaSearch(Arrays.asList(new MainActivity.Item("Radio alias","","https://radio/one","radio")),"one",true);check(a.searchResults.size()==2);check(a.searchResults.get(0).kind.equals("music"));
a.searchScope=2;a.searchResults.clear();a.mergeMediaSearch(local,"one",false);check(a.searchResults.size()==1&&a.searchResults.get(0).kind.equals("video"));
Thread.currentThread().interrupt();a=new MainActivity();r=SearchFeed.online(a,2,"one","ph","ph");check(r.items.isEmpty()&&a.requests==0);Thread.interrupted();
System.out.println("PASS: "+checks+" actual SearchFeed/merge checks: scope, selected regions, query routing, TV filter/logo, malformed URL exclusion, offline notes, deduplication, local matching, mixed results and cancellation");
}}
'''
with TemporaryDirectory() as d:
 root=Path(d)
 for name,text in stubs.items():
  p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
 for name in ['SearchFeed','SearchPolicy','RadioDirectory']:(root/f'com/luma/media/{name}.java').write_text(Path(f'app/src/com/luma/media/{name}.java').read_text())
 subprocess.run(['javac','-d',d]+[str(p) for p in root.rglob('*.java')],check=True)
 subprocess.run(['java','-cp',d,'com.luma.media.SearchFeedContract'],check=True)
