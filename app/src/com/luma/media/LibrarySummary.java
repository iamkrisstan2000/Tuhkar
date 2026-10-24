/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Displayed/total counts for the active section, with the status kept inside the card. */
final class LibrarySummary {
    static String format(int shown,int total,int tab,String status){
        shown=Math.max(0,shown);total=Math.max(shown,total);
        String unit=tab==1?(total==1?"station":"stations"):tab==3?(total==1?"channel":"channels"):(total==1?"item":"items");
        String count=shown==total?total+" "+unit:shown+" of "+total+" "+unit;
        String extra=(status==null?"":status).replaceFirst("^\\d+ (items|stations|channels)\\s*(·\\s*)?","").trim();
        if(extra.isEmpty())extra="hold to save or remove";
        return count+" · "+extra;
    }
}
