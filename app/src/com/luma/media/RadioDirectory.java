/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

import java.net.URLEncoder;
import java.util.Locale;

/** Pure Radio Browser request policy, independent of TV region and playback. */
final class RadioDirectory {
    static String normalize(String region){
        if(region==null)return "all";
        String value=region.trim().toUpperCase(Locale.ROOT);
        if("UK".equals(value))value="GB";
        return value.matches("[A-Z]{2}")?value:"all";
    }
    static String label(String region){String value=normalize(region);return "all".equals(value)?"Worldwide":value;}
    static String path(String query,String region)throws java.io.UnsupportedEncodingException{
        String q=query==null?"":query.trim(),country=normalize(region);
        if(q.isEmpty()&&"all".equals(country))return "/json/stations/topclick/100";
        String path="/json/stations/search?limit=150&hidebroken=true&order=clickcount&reverse=true";
        if(!"all".equals(country))path+="&countrycode="+country;
        if(!q.isEmpty())path+="&name="+URLEncoder.encode(q,"UTF-8");
        return path;
    }
}
