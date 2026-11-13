/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import org.json.*;
import java.io.*;
import java.util.*;

/** Bounded read-only discovery. Results are small; no separate media database or listening history. */
final class HomeFeed {
    static final class Local {
        final ArrayList<MainActivity.Item> recent=new ArrayList<>(),songs=new ArrayList<>(),videos=new ArrayList<>();
        String message="";
    }
    static final class Online {
        final ArrayList<MainActivity.Item> updates=new ArrayList<>(),fm=new ArrayList<>(),tv=new ArrayList<>();
        String fmMessage="",tvMessage="";
    }
    static Local local(Context context,boolean music,boolean video,boolean musicOnly,boolean moviesOnly,Random random){
        Local result=new Local();ArrayList<MainActivity.Item> songs=scan(context,0,music,musicOnly),videos=scan(context,2,video,moviesOnly),all=new ArrayList<>(songs);all.addAll(videos);
        result.recent.addAll(HomeDiscovery.recent(all,8,item->item.dateAdded));result.songs.addAll(HomeDiscovery.sample(songs,HomeDiscovery.SONGS,item->item.url,random));result.videos.addAll(HomeDiscovery.sample(videos,HomeDiscovery.VIDEOS,item->item.url,random));
        result.message=(!music||!video)?"Uses granted media access and your Music/Movies folder filters. Enable missing access in Settings → Media access.":"From Android's date-added metadata · current folder filters apply.";
        return result;
    }
    static ArrayList<MainActivity.Item> scan(Context context,int target,boolean allowed,boolean standardOnly){
        ArrayList<MainActivity.Item> items=new ArrayList<>();if(!allowed)return items;
        Uri collection=target==0?MediaStore.Audio.Media.EXTERNAL_CONTENT_URI:MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String path=Build.VERSION.SDK_INT>=29?MediaStore.MediaColumns.RELATIVE_PATH:MediaStore.MediaColumns.DATA;String[] columns=target==0?new String[]{"_id","_display_name","title","date_added","date_modified",path,"artist","album","album_id","duration"}:new String[]{"_id","_display_name","title","date_added","date_modified",path,"duration"};
        try(Cursor c=context.getContentResolver().query(collection,columns,null,null,"date_added DESC")){
            if(c!=null)while(c.moveToNext()&&items.size()<5000){
                if(Thread.currentThread().isInterrupted())break;
                if(standardOnly&&!(Build.VERSION.SDK_INT>=29?StandardFolderPolicy.relative(c.getString(5),target):StandardFolderPolicy.absolute(c.getString(5),target)))continue;
                String title=c.getString(2);if(title==null||title.isEmpty())title=c.getString(1);
                MainActivity.Item item=new MainActivity.Item(title,target==0?"Song · On your device":"Video · On your device",ContentUris.withAppendedId(collection,c.getLong(0)).toString(),target==0?"music":"video");if(target==0){item.artist=c.getString(6)==null?"":c.getString(6);item.detail=item.artist+" · "+c.getString(7);item.art="content://media/external/audio/albumart/"+c.getLong(8);}item.dateAdded=c.getLong(3)*1000L;item.dateModified=c.getLong(4)*1000L;items.add(item);
            }
        }catch(Exception ignored){}return items;
    }
    static Online online(MainActivity activity,String fmRegion,String tvRegion,List<MainActivity.Item> saved,Random random){
        Online result=new Online();ArrayList<MainActivity.Item> stations=new ArrayList<>(),channels=new ArrayList<>();
        try{
            String json=null;for(String host:new String[]{"https://de1.api.radio-browser.info","https://nl1.api.radio-browser.info","https://at1.api.radio-browser.info"}){try{json=activity.get(host+HomeDiscovery.updatesPath(fmRegion),1500000);break;}catch(Exception ignored){if(Thread.currentThread().isInterrupted())break;}}
            if(json==null)throw new IOException();JSONArray array=new JSONArray(json);
            for(int i=0;i<array.length()&&i<40;i++){JSONObject o=array.getJSONObject(i);String url=o.optString("url_resolved",o.optString("url"));if(!activity.validUrl(url))continue;
                String changed=o.optString("lastchangetime_iso8601",o.optString("lastchangetime",""));if(changed.length()>10)changed=changed.substring(0,10);
                MainActivity.Item item=new MainActivity.Item(o.optString("name","FM station"),o.optString("country")+" · "+o.optString("codec")+(changed.isEmpty()?"":" · directory change "+changed),url,"radio").art(o.optString("favicon"));stations.add(item);
            }
            LinkedHashMap<String,MainActivity.Item> unique=new LinkedHashMap<>();for(MainActivity.Item item:stations)unique.putIfAbsent(item.url,item);result.updates.addAll(new ArrayList<>(unique.values()).subList(0,Math.min(4,unique.size())));
            result.fmMessage="Recent Radio Browser directory changes · "+RadioDirectory.label(fmRegion)+" · not station news or a verified new broadcast.";
        }catch(Exception e){result.fmMessage="FM directory unavailable. Recommendations use saved stations when available.";}
        if(stations.isEmpty())for(MainActivity.Item item:saved)if("radio".equals(item.kind))stations.add(item);
        result.fm.addAll(HomeDiscovery.sample(stations,HomeDiscovery.FM,item->item.url,random));
        try{
            String m3u=activity.get("https://iptv-org.github.io/iptv/countries/"+tvRegion.toLowerCase(Locale.ROOT)+".m3u",6000000),name="TV channel",logo="";
            try(BufferedReader reader=new BufferedReader(new StringReader(m3u))){String line;while((line=reader.readLine())!=null&&channels.size()<3000){line=line.trim();if(line.startsWith("#EXTINF:")){int comma=line.lastIndexOf(',');name=comma>=0?line.substring(comma+1).trim():"TV channel";java.util.regex.Matcher match=java.util.regex.Pattern.compile("tvg-logo=\"([^\"]*)\"").matcher(line);logo=match.find()?match.group(1):"";}else if(activity.validUrl(line))channels.add(new MainActivity.Item(name,"TV · "+tvRegion.toUpperCase(Locale.ROOT)+" · availability varies",line,"tv").art(logo));}}
            result.tvMessage="Public TV directory · "+tvRegion.toUpperCase(Locale.ROOT)+" · use authorized streams.";
        }catch(Exception e){result.tvMessage="TV directory unavailable. Recommendations use saved channels when available.";}
        if(channels.isEmpty())for(MainActivity.Item item:saved)if("tv".equals(item.kind))channels.add(item);
        result.tv.addAll(HomeDiscovery.sample(channels,HomeDiscovery.TV,item->item.url,random));return result;
    }
}
