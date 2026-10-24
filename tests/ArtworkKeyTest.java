/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Random;
public final class ArtworkKeyTest {
    public static void main(String[] args)throws Exception{
        String[] values={"","abc","content://media/external/audio/media/1|","Cebu · 音楽 · 🎵|https://example.test/art.jpg"};
        int checks=0;Random random=new Random(29);
        for(int n=0;n<1004;n++){
            String value=n<values.length?values[n]:Long.toHexString(random.nextLong())+"|"+random.nextInt();
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder previous=new StringBuilder();
            for(byte b:digest)previous.append(String.format(Locale.ROOT,"%02x",b&255));
            String result=ArtworkKey.of(value);if(!result.equals(previous.toString())||result.length()!=64)throw new AssertionError("Existing artwork cache filenames changed");checks++;
        }
        System.out.println("PASS: "+checks+" artwork-key equivalence checks: exact previous SHA-256 filenames, UTF-8, random inputs; no cache migration or cover loss");
    }
}
