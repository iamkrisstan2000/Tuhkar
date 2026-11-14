# Copyright (C) 2026 KnightWin
# SPDX-License-Identifier: GPL-3.0-only
from pathlib import Path
import subprocess,tempfile
art=Path('app/src/com/luma/media/ArtworkLoader.java').read_text()
method=art[art.index('    void trimMemory('):art.index('    void clear(){')]
method=method.replace('android.content.ComponentCallbacks2.','Limits.')
java='''public class MemoryTrim {
static class Limits {static final int TRIM_MEMORY_UI_HIDDEN=20;}
static class Cache {int evictions,limit;void evictAll(){evictions++;}void trimToSize(int n){limit=n;}}
Cache memory=new Cache(),keys=new Cache();
'''+method+'''
public static void main(String[] args){MemoryTrim p=new MemoryTrim();p.trimMemory(10);if(p.memory.limit!=2097152||p.memory.evictions!=0)throw new AssertionError("Moderate trim");p.trimMemory(20);if(p.memory.evictions!=1||p.keys.evictions!=1)throw new AssertionError("UI hidden trim");p.trimMemory(80);if(p.memory.evictions!=2)throw new AssertionError("Severe trim");System.out.println("PASS: actual artwork cache trim method halves moderate cache / evicts hidden-severe cache without recycling displayed bitmaps");}}
'''
with tempfile.TemporaryDirectory() as d:
 p=Path(d)/'MemoryTrim.java';p.write_text(java);subprocess.run(['javac',str(p)],check=True);subprocess.run(['java','-cp',d,'MemoryTrim'],check=True)
s=Path('app/src/com/luma/media/MainActivity.java').read_text()
assert 'EditText search;' not in s and 'pendingSearch' not in s
assert 'onTrimMemory(int level)' in s and 'onLowMemory()' in s
assert 'synchronized(pending){pending.clear();waiting.clear();}' in art
assert 'postOnAnimationDelayed(frame,16)' in Path('app/src/com/luma/media/DiscView.java').read_text()
assert 'preview' in Path('PREVIEW-HISTORY.md').read_text().lower()
assert 'versionCode **77**' in Path('CHANGELOG.md').read_text()
assert 'io.purge();' in s and 'io.allowCoreThreadTimeOut(true)' in s
assert 'static int height(' not in Path('app/src/com/luma/media/PortraitSearchLayout.java').read_text()
print('PASS: obsolete fields removed, live search cancellation retained, pressure callbacks wired, artwork shutdown waiters cleared and display-aligned guarded disc scheduling; V2 consolidation history included')
# Exercise the actual cancellation/purge method with real Java executor/futures.
cancel=s[s.index('    void cancelMediaSearchTasks()'):s.index('    int mediaSearchScope()')]
java='''import java.util.concurrent.*;public class CancelQueue {
ThreadPoolExecutor io=new ThreadPoolExecutor(1,1,20,TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>());Future<?> mediaSearchLocalTask,mediaSearchOnlineTask;
'''+cancel+'''
public static void main(String[] args)throws Exception{CancelQueue p=new CancelQueue();CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);try{p.io.submit(()->{started.countDown();try{release.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});if(!started.await(2,TimeUnit.SECONDS))throw new AssertionError("Worker start");p.mediaSearchLocalTask=p.io.submit(()->{});p.mediaSearchOnlineTask=p.io.submit(()->{});if(p.io.getQueue().size()!=2)throw new AssertionError("Queued work");p.cancelMediaSearchTasks();if(!p.mediaSearchLocalTask.isCancelled()||!p.mediaSearchOnlineTask.isCancelled()||!p.io.getQueue().isEmpty())throw new AssertionError("Purge cancelled tasks");release.countDown();if(p.io.submit(()->42).get(2,TimeUnit.SECONDS)!=42)throw new AssertionError("Worker remains usable");}finally{release.countDown();p.io.shutdownNow();}System.out.println("PASS: actual cancel method releases queued cancelled futures and preserves subsequent executor work");}}
'''
with tempfile.TemporaryDirectory() as d:
 p=Path(d)/'CancelQueue.java';p.write_text(java);subprocess.run(['javac',str(p)],check=True);subprocess.run(['java','-cp',d,'CancelQueue'],check=True)
