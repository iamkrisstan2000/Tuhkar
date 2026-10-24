/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
public final class StandardFolderPolicyTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args){
        for(int target:new int[]{0,2}){
            String dir=target==0?"Music":"Movies",other=target==0?"Movies":"Music";
            for(String suffix:new String[]{"/","/Artist/","/Artist/Album/song.mp3","/video.mp4","/Sub folder/曲.mp3"}){
                String path=dir+suffix;
                check(StandardFolderPolicy.relative(path,target),"relative subfolder");
                check(StandardFolderPolicy.relative(path.toLowerCase(java.util.Locale.ROOT),target),"case-insensitive root");
                for(String prefix:new String[]{"/storage/emulated/0/","/storage/emulated/10/","/storage/self/primary/","/storage/7CCF-FC6C/","/mnt/media_rw/7CCF-FC6C/","/sdcard/","/mnt/sdcard/"})check(StandardFolderPolicy.absolute(prefix+path,target),"internal/SD legacy path "+prefix);
                for(String volume:new String[]{"primary:","7CCF-FC6C:"})check(StandardFolderPolicy.document(volume+path,target),"internal/SD tree document");
                check(StandardFolderPolicy.document("raw:/storage/emulated/0/"+path,target),"raw provider path");
            }
            for(String path:new String[]{other+"/clip.mp4","Download/"+dir+"/file.mp3",dir+"Box/a",dir+"2/",dir+"/../Download/a",dir+"/./a","Android/data/app/"+dir+"/","",null}){
                check(!StandardFolderPolicy.relative(path,target),"reject non-standard and malformed path");
                check(!StandardFolderPolicy.document(path==null?null:"primary:"+path,target),"reject non-standard tree");
                check(!StandardFolderPolicy.absolute(path==null?null:"/storage/emulated/0/"+path,target),"reject non-standard legacy");
            }
            check(!StandardFolderPolicy.document("opaque_document_123",target),"unknown provider ID not falsely accepted");
            check(!StandardFolderPolicy.absolute("/data/user/0/app/"+dir+"/",target),"private storage not shared storage");
        }
        System.out.println("PASS: "+checks+" standard folder checks: Music/Movies, nested folders, internal/SD, relative/legacy/document paths, case handling and nonmatching roots");
    }
}
