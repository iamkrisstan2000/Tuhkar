/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import java.io.File;
import java.io.IOException;

/** Only invoked with Context.getCacheDir(). Never accepts media URIs or storage roots. */
final class SafeCacheReset {
    static boolean clear(File cache){
        if(cache==null)return false;
        try{
            File root=cache.getCanonicalFile();
            if(!root.isDirectory())return true;
            File[] files=root.listFiles();if(files==null)return false;
            boolean ok=true;for(File file:files)ok=removeInside(root,file)&&ok;return ok;
        }catch(IOException|SecurityException e){return false;}
    }
    private static boolean removeInside(File root,File file)throws IOException{
        File canonical=file.getCanonicalFile();
        // Do not traverse symlinks, escape the cache root, or delete the root itself.
        if(!canonical.getPath().startsWith(root.getPath()+File.separator)||!canonical.equals(file.getAbsoluteFile()))return false;
        boolean ok=true;
        if(file.isDirectory()){
            File[] children=file.listFiles();if(children==null)return false;
            for(File child:children)ok=removeInside(root,child)&&ok;
        }
        return file.delete()&&ok;
    }
}
