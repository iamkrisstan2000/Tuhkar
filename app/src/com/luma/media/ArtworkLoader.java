/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.util.*;
import android.widget.ImageView;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;
import org.json.*;

/** Lazy artwork: bounded workers, sampled bitmaps, 4 MiB memory and 24 MiB disk cache. */
final class ArtworkLoader {
    private final Context context;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final File directory;
    private final SharedPreferences prefs;
    private final LruCache<String,Bitmap> memory=new LruCache<String,Bitmap>(4*1024*1024){protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}};
    private final LruCache<String,String> keys=new LruCache<>(256);
    private final Set<String> pending=new HashSet<>();
    private final Map<String,ArrayList<WeakReference<ImageView>>> waiting=new HashMap<>();
    private final ThreadPoolExecutor pool=new ThreadPoolExecutor(2,2,20,TimeUnit.SECONDS,new ArrayBlockingQueue<Runnable>(48));
    private volatile boolean active=true,closed;
    private long nextSearch;
    ArtworkLoader(Context context,SharedPreferences prefs){this.context=context.getApplicationContext();this.prefs=prefs;directory=new File(context.getCacheDir(),"artwork-v1");directory.mkdirs();pool.allowCoreThreadTimeOut(true);}
    void setActive(boolean active){this.active=active;}
    void bind(ImageView view,MainActivity.Item item,int color){
        String key=key(item.url+"|"+item.art);
        Bitmap cached=memory.get(key);
        if(cached!=null){
            if(!key.equals(view.getTag())||!(view.getDrawable() instanceof BitmapDrawable)||((BitmapDrawable)view.getDrawable()).getBitmap()!=cached){view.setTag(key);view.setImageBitmap(cached);}
            return;
        }
        if(!key.equals(view.getTag())||view.getDrawable()==null){view.setTag(key);view.setImageDrawable(new FluentIcon(item.kind.equals("music")?0:item.kind.equals("radio")?1:item.kind.equals("video")?2:3,color));}
        synchronized(pending){if(closed)return;if(pending.contains(key)){ArrayList<WeakReference<ImageView>> views=waiting.get(key);for(WeakReference<ImageView> ref:views)if(ref.get()==view)return;views.add(new WeakReference<>(view));return;}if(pending.size()>=40)return;pending.add(key);ArrayList<WeakReference<ImageView>> views=new ArrayList<>();views.add(new WeakReference<>(view));waiting.put(key,views);}
        try{pool.execute(()->{
            Bitmap bitmap=null;
            try{
                if(closed)return;
                File file=new File(directory,key+".jpg"),miss=new File(directory,key+".miss");
                if(file.exists()){bitmap=decode(read(new FileInputStream(file),4*1024*1024));file.setLastModified(System.currentTimeMillis());}
                if(bitmap==null){
                    bitmap=local(item);
                    boolean mayDownload=prefs.getBoolean("artDownloads",true)&&active&&!closed;
                    if(bitmap==null&&mayDownload&&(!miss.exists()||System.currentTimeMillis()-miss.lastModified()>24*60*60*1000L)){
                        if(http(item.art))try{bitmap=decode(download(item.art,4*1024*1024));}catch(Exception ignored){}
                        if(bitmap==null&&(item.kind.equals("music")||item.kind.equals("video"))){
                            String term=item.title.replaceAll("(?i)\\.(mp3|m4a|flac|wav|ogg|aac|mp4|mkv|webm|avi)$","").replaceAll("(?i)\\b(1080p|720p|2160p|x264|x265|webrip|bluray)\\b","").replace('_',' ').trim();
                            if(item.kind.equals("music")&&!item.artist.isEmpty()&&!item.artist.equals("<unknown>"))term+=" "+item.artist;
                            if(term.length()>100)term=term.substring(0,100);
                            try{if(term.length()>2&&active){throttle();if(active&&!closed){String entity=item.kind.equals("music")?"song":"movie";JSONObject json=new JSONObject(new String(download("https://itunes.apple.com/search?term="+URLEncoder.encode(term,"UTF-8")+"&entity="+entity+"&limit=1",512*1024),"UTF-8"));JSONArray results=json.optJSONArray("results");if(results!=null&&results.length()>0){String url=results.getJSONObject(0).optString("artworkUrl100","").replace("100x100bb","300x300bb");if(http(url))bitmap=decode(download(url,4*1024*1024));}}}}catch(Exception ignored){}
                        }
                        if(bitmap==null&&(item.kind.equals("radio")||item.kind.equals("tv"))&&active&&!closed){
                            String title=item.title.replaceAll("\\s*\\([^)]*\\)","").trim();
                            try{JSONObject page=new JSONObject(new String(download("https://en.wikipedia.org/api/rest_v1/page/summary/"+URLEncoder.encode(title,"UTF-8").replace("+","%20"),256*1024),"UTF-8"));JSONObject thumb=page.optJSONObject("thumbnail");if(thumb!=null&&http(thumb.optString("source")))bitmap=decode(download(thumb.getString("source"),4*1024*1024));}catch(Exception ignored){}
                        }
                        if(bitmap==null)synchronized(directory){if(!closed)try{miss.createNewFile();miss.setLastModified(System.currentTimeMillis());trim();}catch(Exception ignored){}}
                    }
                    if(bitmap!=null)synchronized(directory){if(!closed){try(FileOutputStream out=new FileOutputStream(file)){bitmap.compress(Bitmap.CompressFormat.JPEG,85,out);}catch(Exception ignored){}trim();}}
                }
                if(bitmap!=null&&!closed)memory.put(key,bitmap);
            }catch(Exception ignored){}finally{final Bitmap ready=bitmap;final ArrayList<WeakReference<ImageView>> targets;synchronized(pending){pending.remove(key);targets=waiting.remove(key);}if(ready!=null&&targets!=null)main.post(()->{if(closed)return;for(WeakReference<ImageView> ref:targets){ImageView target=ref.get();if(target!=null&&key.equals(target.getTag()))target.setImageBitmap(ready);}});}
        });}catch(RejectedExecutionException e){synchronized(pending){pending.remove(key);waiting.remove(key);}}
    }
    private Bitmap local(MainActivity.Item item){
        if(!item.art.isEmpty()&&!http(item.art))try{Bitmap local=decode(read(context.getContentResolver().openInputStream(Uri.parse(item.art)),4*1024*1024));if(local!=null)return local;}catch(Exception ignored){}
        if(!item.url.startsWith("content://"))return null;
        if(item.kind.equals("video")&&Build.VERSION.SDK_INT>=29)try{return context.getContentResolver().loadThumbnail(Uri.parse(item.url),new Size(160,160),null);}catch(Exception ignored){}
        MediaMetadataRetriever r=new MediaMetadataRetriever();try{
            r.setDataSource(context,Uri.parse(item.url));byte[] art=r.getEmbeddedPicture();if(art!=null&&art.length<=4*1024*1024){Bitmap b=decode(art);if(b!=null)return b;}
            if(item.kind.equals("video")&&Build.VERSION.SDK_INT>=27)return r.getScaledFrameAtTime(1000000,MediaMetadataRetriever.OPTION_CLOSEST_SYNC,160,100);
        }catch(Exception ignored){}finally{try{r.release();}catch(Exception ignored){}}
        return null;
    }
    private synchronized void throttle()throws InterruptedException{long now=System.currentTimeMillis(),delay=Math.max(0,nextSearch-now);if(delay>0)Thread.sleep(Math.min(delay,3100));nextSearch=System.currentTimeMillis()+3100;}
    private boolean http(String s){return s!=null&&(s.startsWith("https://")||s.startsWith("http://"));}
    private byte[] download(String url,int max)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setConnectTimeout(6000);c.setReadTimeout(6000);c.setRequestProperty("User-Agent","Tuhkar/1.0.0 (Android)");try{if(c.getResponseCode()!=200)throw new IOException();return read(c.getInputStream(),max);}finally{c.disconnect();}
    }
    private byte[] read(InputStream stream,int max)throws IOException{if(stream==null)throw new IOException();try(InputStream in=stream;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n,count=0;while((n=in.read(b))!=-1){count+=n;if(count>max)throw new IOException("Artwork too large");out.write(b,0,n);}return out.toByteArray();}}
    private Bitmap decode(byte[] bytes){BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);if(o.outWidth<=0||o.outHeight<=0)return null;int sample=1;while(o.outWidth/sample>320||o.outHeight/sample>320)sample*=2;o.inJustDecodeBounds=false;o.inSampleSize=sample;o.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);}
    private String key(String value){String cached=keys.get(value);if(cached!=null)return cached;String result=ArtworkKey.of(value);keys.put(value,result);return result;}
    private synchronized void trim(){if(closed)return;File[] files=directory.listFiles();if(files==null)return;Arrays.sort(files,Comparator.comparingLong(File::lastModified));long total=0;for(File f:files)total+=f.length();int count=files.length;for(File f:files){if(total<=24*1024*1024&&count<=800)break;long length=f.length();if(f.delete()){total-=length;count--;}}}
    void clear(){memory.evictAll();try{pool.execute(()->{File[] files=directory.listFiles();if(files!=null)for(File f:files)f.delete();});}catch(RejectedExecutionException ignored){}}
    boolean resetDiskCache(){close();synchronized(directory){return SafeCacheReset.clear(context.getCacheDir());}}
    void close(){closed=true;active=false;pool.shutdownNow();memory.evictAll();keys.evictAll();main.removeCallbacksAndMessages(null);}
}
