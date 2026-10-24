# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
"""Executes actual ArtworkLoader bind/cache-registration path; no downloads or Android frames."""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess
source=Path('app/src/com/luma/media/ArtworkLoader.java').read_text()
a=source.index('    void bind(');b=source.index('        try{pool.execute',a);bind=source[a:b]+'    }\n'
a=source.index('    private String key(');b=source.index('    private synchronized void trim',a);key=source[a:b]
stubs={
'android/graphics/Bitmap.java':'package android.graphics;public class Bitmap {}',
'android/graphics/drawable/Drawable.java':'package android.graphics.drawable;public class Drawable {}',
'android/graphics/drawable/BitmapDrawable.java':'package android.graphics.drawable;import android.graphics.Bitmap;public class BitmapDrawable extends Drawable {Bitmap bitmap;public BitmapDrawable(Bitmap b){bitmap=b;}public Bitmap getBitmap(){return bitmap;}}',
'android/widget/ImageView.java':'''package android.widget;import android.graphics.Bitmap;import android.graphics.drawable.*;public class ImageView {Object tag;Drawable drawable;public int placeholders,bitmaps;public Object getTag(){return tag;}public void setTag(Object t){tag=t;}public Drawable getDrawable(){return drawable;}public void setImageDrawable(Drawable d){drawable=d;placeholders++;}public void setImageBitmap(Bitmap b){drawable=new BitmapDrawable(b);bitmaps++;}}''',
'com/luma/media/FluentIcon.java':'package com.luma.media;public class FluentIcon extends android.graphics.drawable.Drawable {FluentIcon(int kind,int color){}}',
'com/luma/media/MainActivity.java':'package com.luma.media;public class MainActivity {static class Item {String url,art="",kind="music";Item(String u){url=u;}}}',
'com/luma/media/ArtworkBindContract.java':'''package com.luma.media;import java.util.*;import java.lang.ref.WeakReference;import android.widget.ImageView;import android.graphics.Bitmap;import android.graphics.drawable.BitmapDrawable;
public class ArtworkBindContract {Map<String,Bitmap> memory=new HashMap<>();Map<String,String> keys=new HashMap<>();Set<String> pending=new HashSet<>();Map<String,ArrayList<WeakReference<ImageView>>> waiting=new HashMap<>();boolean closed;
'''+bind+key+'''
static int checks;static void check(boolean b,String label){checks++;if(!b)throw new AssertionError(label);}
public static void main(String[] args){ArtworkBindContract loader=new ArtworkBindContract();ImageView first=new ImageView(),second=new ImageView();MainActivity.Item a=new MainActivity.Item("content://song/0"),b=new MainActivity.Item("content://song/1");loader.bind(first,a,1);String key=(String)first.getTag();check(first.placeholders==1&&loader.pending.size()==1,"initial placeholder and single job registration");for(int i=0;i<100;i++)loader.bind(first,a,1);check(first.placeholders==1&&loader.waiting.get(key).size()==1,"repeated measuring/rebinding neither resets placeholder nor adds duplicate weak targets");loader.bind(second,a,1);check(loader.waiting.get(key).size()==2,"distinct views still receive the same pending artwork");loader.bind(first,b,1);check(!key.equals(first.getTag())&&first.placeholders==2,"recycled different URL replaces stale art");Bitmap bitmap=new Bitmap();loader.memory.put(key,bitmap);loader.bind(first,a,1);check(first.bitmaps==1&&first.placeholders==2,"cache hit installs bitmap directly without placeholder flash");for(int i=0;i<100;i++)loader.bind(first,a,1);check(first.bitmaps==1&&first.placeholders==2,"unchanged cached bitmap never reassigned on repeated binds");ImageView third=new ImageView();loader.bind(third,a,1);check(third.bitmaps==1&&third.placeholders==0,"new recycled view with cached art bypasses placeholder allocation");a.art="new cover";loader.bind(first,a,1);check(first.placeholders==3&&!key.equals(first.getTag()),"changed cover identity updates art key and view");check(loader.keys.size()==3,"memoized source keys distinguish URL and art changes");
System.out.println("PASS: "+checks+" actual artwork bind/cache-registration checks, including 200 repeat binds: no bitmap reset/placeholder flash, deduplicated waiters and correct recycled/changed-art identity");System.out.println("NOTE: extracted real bind preparation path with doubles; no network/download, actual ImageView layout or device timing validation.");}}
'''
}
assert 'new LruCache<>(256)' in source
assert 'String.format' not in source[source.index('    private String key('):]
assert 'key.equals(target.getTag())' in source
ui=Path('app/src/com/luma/media/MainActivity.java').read_text();adapter=ui[ui.index('    class ItemAdapter'):ui.index('    String extension(')]
assert 'rowSize==null||rowSize.width!=-1||rowSize.height!=cellHeight' in adapter
assert 'imageSize.width!=artSize||imageSize.height!=artSize' in adapter
assert 'savedKeys.contains(item.kind+' in adapter and 'isSaved(item)' not in adapter
assert 'TextUtils.equals(holder.title.getText(),title)' in adapter
assert 'setContentView(root);movingArt.clear();' in ui
with TemporaryDirectory(prefix='tuhkar-art-bind-') as temp:
    root=Path(temp);files=[]
    for name,text in stubs.items():
        p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(str(p))
    subprocess.run(['javac','-encoding','UTF-8','-d',str(root/'classes'),*files,str(Path('app/src/com/luma/media/ArtworkKey.java').resolve())],check=True)
    subprocess.run(['java','-cp',str(root/'classes'),'com.luma.media.ArtworkBindContract'],check=True)
print('PASS: bounded key memoization, bitmap callback identity guard, conditional row/image sizing and text updates, indexed Saved badges and old-view registry clearing')
