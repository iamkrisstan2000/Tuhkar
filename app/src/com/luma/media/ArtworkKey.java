/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Same SHA-256 disk filenames, without 32 formatting/locale operations per cache key. */
final class ArtworkKey {
    private static final char[] HEX="0123456789abcdef".toCharArray();
    static String of(String value){
        try{
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            char[] result=new char[digest.length*2];
            for(int i=0;i<digest.length;i++){int b=digest[i]&255;result[i*2]=HEX[b>>>4];result[i*2+1]=HEX[b&15];}
            return new String(result);
        }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException("SHA-256 required",e);}
    }
}
