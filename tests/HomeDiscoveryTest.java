/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;
import java.util.*;
public class HomeDiscoveryTest {
    static void check(boolean value){if(!value)throw new AssertionError();}
    public static void main(String[] args){int checks=0;List<String> source=Arrays.asList("a","b","a","c","d","e","f","g");
        for(int seed=0;seed<100;seed++){ArrayList<String> picks=HomeDiscovery.sample(source,6,s->s,new Random(seed));check(picks.size()==6&&new HashSet<>(picks).size()==6);checks++;}
        check(HomeDiscovery.sample(source,20,s->s,new Random()).size()==7);check(HomeDiscovery.sample(source,0,s->s,new Random()).isEmpty());check(source.size()==8);
        check(HomeDiscovery.sample(Collections.<String>emptyList(),6,s->s,new Random()).isEmpty());
        check(HomeDiscovery.recent(Arrays.asList(0L,2L,10L,1L),2,v->v).equals(Arrays.asList(10L,2L)));
        check(HomeDiscovery.SONGS==6&&HomeDiscovery.FM==2&&HomeDiscovery.VIDEOS==1&&HomeDiscovery.TV==1);
        check(HomeDiscovery.greeting(8).equals("Good morning"));check(HomeDiscovery.greeting(14).equals("Good afternoon"));check(HomeDiscovery.greeting(21).equals("Good evening"));
        Set<String> names=new HashSet<>();Random random=new Random(2);for(int i=0;i<200;i++)names.add(HomeDiscovery.callsign(random));check(names.equals(new HashSet<>(Arrays.asList("Senpai","Master","Adventurer","Listener","Supreme Listener","Audiophile","Boss","Explorer","Dev","Partner","Captain","NPC"))));
        check(HomeDiscovery.updatesPath("uk").contains("countrycode=GB"));check(!HomeDiscovery.updatesPath("all").contains("countrycode"));check(HomeDiscovery.updatesPath("PH&evil=true").equals(HomeDiscovery.updatesPath("all")));check(HomeDiscovery.updatesPath("ph").contains("order=changetimestamp&reverse=true"));
        System.out.println("PASS: Home discovery quotas, unique randomized sampling, honest shortfalls, date-added ordering, greeting/callsign variety and scoped FM change query ("+checks+" seeded sample checks)");
    }
}
