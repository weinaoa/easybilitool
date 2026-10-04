package com.weinaoa.easybilitool;

/** Native AppBar collapse progress and matching bottom-bar opacity. */
final class HomeBarSync {
    private HomeBarSync() {}
    static float fraction(int offset, int scrollRange) {
        return scrollRange <= 0 ? 0 : (float)Math.max(0, Math.min(1, -(double)offset / scrollRange));
    }
    static float opacity(float fraction) { return 1 - Math.max(0, Math.min(1, fraction)); }
}
