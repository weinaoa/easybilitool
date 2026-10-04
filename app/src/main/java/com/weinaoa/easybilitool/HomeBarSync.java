/*
Copyright (c) 2026 weinaoa
EasyBiliTool is licensed under Mulan PubL v2.
You can use this software according to the terms and conditions of the Mulan PubL v2.
You may obtain a copy of Mulan PubL v2 at:
    http://license.coscl.org.cn/MulanPubL-2.0
THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
See the Mulan PubL v2 for more details.
*/

package com.weinaoa.easybilitool;

/** Native AppBar collapse progress and matching bottom-bar opacity. */
final class HomeBarSync {
    private HomeBarSync() {}
    static float fraction(int offset, int scrollRange) {
        return scrollRange <= 0 ? 0 : (float)Math.max(0, Math.min(1, -(double)offset / scrollRange));
    }
    static float opacity(float fraction) { return 1 - Math.max(0, Math.min(1, fraction)); }
}
