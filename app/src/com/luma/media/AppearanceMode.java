/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Stored app appearance; System is the safe default for old/unknown preferences. */
final class AppearanceMode {
    static String normalize(String value){return "light".equals(value)||"dark".equals(value)?value:"system";}
    static boolean dark(String value,boolean systemDark){String mode=normalize(value);return "dark".equals(mode)||("system".equals(mode)&&systemDark);}
}
