/* Copyright (C) 2026 KnightWin
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.luma.media;

/** Bounded shelf artwork size; landscape shelves live inside the discoveries column. */
final class HomeShelfLayout {
    static int tileWidth(int screenWidth,boolean landscape){float available=landscape?(screenWidth-124)/2f:screenWidth-32;return Math.max(120,Math.min(168,Math.round(available*.42f)));}
}
