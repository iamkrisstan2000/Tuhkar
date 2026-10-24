/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
import java.nio.file.*;
import java.io.*;
public final class SafeCacheResetTest {
    static int checks;
    static void check(boolean value,String label){checks++;if(!value)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        Path temp=Files.createTempDirectory("tuhkar-reset-safety-");
        try{
            Path media=Files.createDirectories(temp.resolve("shared-storage/Music"));Path song=media.resolve("precious.mp3");Files.write(song,new byte[]{1,2,3});
            Path videos=Files.createDirectories(temp.resolve("shared-storage/Movies"));Path movie=videos.resolve("precious.mp4");Files.write(movie,new byte[]{4,5,6});
            Path cache=Files.createDirectories(temp.resolve("app/cache"));Path artwork=Files.createDirectories(cache.resolve("artwork-v1/sub"));Files.write(artwork.resolve("thumbnail.jpg"),new byte[]{7});Files.write(cache.resolve("temp.bin"),new byte[]{8});
            check(SafeCacheReset.clear(cache.toFile()),"clear own cache succeeds");check(Files.isDirectory(cache)&&Files.list(cache).count()==0,"cache root retained and emptied");
            check(Files.readAllBytes(song)[0]==1&&Files.readAllBytes(movie)[0]==4,"actual audio/video unchanged");
            Path link=cache.resolve("external-link");Files.createSymbolicLink(link,media);
            check(!SafeCacheReset.clear(cache.toFile()),"symlink escape refused");check(Files.exists(song)&&Files.readAllBytes(song).length==3,"linked music directory never traversed");Files.delete(link);
            Path cycle=cache.resolve("cycle");Files.createSymbolicLink(cycle,cache);check(!SafeCacheReset.clear(cache.toFile()),"root alias/cycle refused");Files.delete(cycle);
            check(SafeCacheReset.clear(cache.toFile()),"repeated reset safe");check(!SafeCacheReset.clear(null),"null root rejected");
            System.out.println("PASS: "+checks+" cache-reset safety checks: only app-cache content deleted, media files unchanged, symlink escapes/cycles refused");
        }finally{try(java.util.stream.Stream<Path> paths=Files.walk(temp)){paths.sorted(java.util.Comparator.reverseOrder()).forEach(path->{try{Files.delete(path);}catch(Exception e){throw new RuntimeException(e);}});}}
    }
}
