/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

public final class RadioDirectoryTest {
    static int checks;
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        check(RadioDirectory.path("","all").equals("/json/stations/topclick/100"),"Worldwide preserves original top stations");
        check(RadioDirectory.path(null,null).equals("/json/stations/topclick/100"),"safe default");
        String ph=RadioDirectory.path("","ph");check(ph.contains("countrycode=PH")&&!ph.contains("&name="),"country filter without search");
        check(ph.contains("hidebroken=true")&&ph.contains("limit=150"),"bounded country directory request");
        String search=RadioDirectory.path(" Rock & Jazz / café ","ph");check(search.contains("countrycode=PH")&&search.contains("name=Rock+%26+Jazz+%2F+caf%C3%A9"),"search safely encoded within selected country");
        check(RadioDirectory.path("news","all").contains("&name=news")&&!RadioDirectory.path("news","all").contains("countrycode="),"Worldwide search remains unfiltered");
        check(RadioDirectory.path("BBC","uk").contains("countrycode=GB"),"UK label mapped to ISO GB for Radio Browser");
        check(RadioDirectory.label("all").equals("Worldwide")&&RadioDirectory.label("ph").equals("PH"),"accessible region labels");
        for(String invalid:new String[]{"", "all", "abc", "a&", "PH&limit=9999", "../", "1x"})check(RadioDirectory.normalize(invalid).equals("all"),"invalid country safely defaults");
        for(char a='a';a<='z';a++)for(char b='a';b<='z';b++){
            String code=""+a+b;String normalized=RadioDirectory.normalize(code);check(normalized.matches("[A-Z]{2}"),"valid two-letter code normalized");
            check(RadioDirectory.path("station",code).contains("countrycode="+normalized),"every region reaches actual request filter");
        }
        System.out.println("PASS: "+checks+" RadioDirectory request assertions: Worldwide default, country filters, scoped UTF-8 searches, ISO UK/GB mapping and bounded safe URL construction.");
    }
}
