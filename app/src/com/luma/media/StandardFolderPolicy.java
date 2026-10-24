/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import java.util.Locale;

/** Matches standard shared-storage roots, without granting filesystem access. */
final class StandardFolderPolicy {
    private static String directory(int target){return target==0?"music":"movies";}
    static boolean relative(String path,int target){
        if(path==null)return false;
        String clean=path.replace('\\','/').toLowerCase(Locale.ROOT);
        if(clean.startsWith("/")||clean.isEmpty())return false;
        for(String segment:clean.split("/"))if(segment.equals(".")||segment.equals(".."))return false;
        String root=directory(target);
        return clean.equals(root)||clean.startsWith(root+"/");
    }
    static boolean absolute(String path,int target){
        if(path==null)return false;
        String clean=path.replace('\\','/');
        for(String prefix:new String[]{"/storage/emulated/","/storage/self/","/mnt/media_rw/"}){
            if(clean.startsWith(prefix)){
                int end=clean.indexOf('/',prefix.length());
                return end>=0&&relative(clean.substring(end+1),target);
            }
        }
        if(clean.startsWith("/sdcard/"))return relative(clean.substring(8),target);
        if(clean.startsWith("/mnt/sdcard/"))return relative(clean.substring(12),target);
        if(clean.startsWith("/storage/")){
            int end=clean.indexOf('/',9);
            return end>=0&&relative(clean.substring(end+1),target);
        }
        return false;
    }
    static boolean document(String id,int target){
        if(id==null)return false;
        if(id.startsWith("raw:"))return absolute(id.substring(4),target);
        int colon=id.indexOf(':');
        // ExternalStorageProvider IDs are volume:relative/path, already URI-decoded by Android.
        return colon>0&&relative(id.substring(colon+1),target);
    }
}
