/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Stable category identifiers, independent of locale, layout and rotation. */
final class SettingsNavigation {
    static final String[] IDS={"appearance","library","playback","audio","access","about"};
    static final String[] TITLES={"Appearance","Library & artwork","Playback & glow","Audio enhancements","Media access","About & reset"};
    static final String[] SUMMARIES={"App mode, accents and OLED","Media layout and cover downloads","Background video audio and reactive glow","Equalizer, reverb and crossfade","Permissions, folders and setup","Privacy, credits and app reset"};
    static final int[] ICONS={4,18,0,6,15,17};
    static String normalize(String id){for(String candidate:IDS)if(candidate.equals(id))return candidate;return "";}
    static int index(String id){for(int i=0;i<IDS.length;i++)if(IDS[i].equals(id))return i;return -1;}
}
