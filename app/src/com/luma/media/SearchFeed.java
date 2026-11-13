/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
import java.util.*;
import java.io.*;
import org.json.*;
/** Online search uses selected-region public directories, never microphone/history. */
final class SearchFeed {
    static final class Result {final ArrayList<MainActivity.Item> items=new ArrayList<>();String note="";}
    static String tvCatalogue(MainActivity app,String region)throws Exception {
        File cache=new File(app.getCacheDir(),"tv-"+region.toLowerCase(Locale.ROOT)+".m3u");long age=System.currentTimeMillis()-cache.lastModified();
        if(cache.isFile()&&age>=0&&age<6*60*60*1000L&&cache.length()<=6000000){
            try(InputStream in=new FileInputStream(cache);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n,total=0;while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();total+=n;if(total>6000000)throw new IOException("Catalogue too large");out.write(buffer,0,n);}return out.toString("UTF-8");}
        }
        String data=app.get("https://iptv-org.github.io/iptv/countries/"+region.toLowerCase(Locale.ROOT)+".m3u",6000000);
        if(!Thread.currentThread().isInterrupted())try{File temp=File.createTempFile("tv-search-",".tmp",app.getCacheDir());try{try(FileOutputStream out=new FileOutputStream(temp)){out.write(data.getBytes("UTF-8"));}temp.renameTo(cache);}finally{temp.delete();}}catch(IOException ignored){}
        return data;
    }
    static Result online(MainActivity app,int scope,String query,String fm,String tv){
        Result result=new Result();
        if(SearchPolicy.allows(scope,"radio"))try{
            String json=null;for(String host:new String[]{"https://de1.api.radio-browser.info","https://nl1.api.radio-browser.info","https://at1.api.radio-browser.info"})try{json=app.get(host+RadioDirectory.path(query,fm),2500000);break;}catch(Exception ignored){if(Thread.currentThread().isInterrupted())return result;}
            if(json==null)throw new IOException();JSONArray stations=new JSONArray(json);
            for(int i=0;i<stations.length()&&i<150;i++){JSONObject o=stations.getJSONObject(i);String url=o.optString("url_resolved",o.optString("url"));if(app.validUrl(url))result.items.add(new MainActivity.Item(o.optString("name","Radio"),o.optString("country")+" · "+o.optString("codec"),url,"radio").art(o.optString("favicon")));}
        }catch(Exception e){result.note+="Internet Radio unavailable. ";}
        if(Thread.currentThread().isInterrupted())return result;
        if(SearchPolicy.allows(scope,"tv"))try{
            String data=tvCatalogue(app,tv),name="Channel",logo="";
            try(BufferedReader reader=new BufferedReader(new StringReader(data))){String line;int count=0;while(!Thread.currentThread().isInterrupted()&&(line=reader.readLine())!=null&&count<3000){line=line.trim();if(line.startsWith("#EXTINF:")){int comma=line.lastIndexOf(',');name=comma>=0?line.substring(comma+1).trim():"Channel";java.util.regex.Matcher m=java.util.regex.Pattern.compile("tvg-logo=\"([^\"]*)\"").matcher(line);logo=m.find()?m.group(1):"";}else if(app.validUrl(line)){count++;if(SearchPolicy.matches(query,name,tv))result.items.add(new MainActivity.Item(name,"Public IPTV · "+tv.toUpperCase(Locale.ROOT),line,"tv").art(logo));}}}
        }catch(Exception e){result.note+="Online TV unavailable. ";}
        return result;
    }
}
