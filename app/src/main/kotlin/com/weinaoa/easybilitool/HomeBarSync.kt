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

package com.weinaoa.easybilitool



/**
 * Native AppBar collapse progress and matching bottom-bar opacity.
 */
class HomeBarSync {
    constructor() {

    }

    companion object {
        @JvmStatic fun fraction(offset: Int, scrollRange: Int): Float {
            return (if ((scrollRange <= 0)) 0F else (JvmNumbers.max(0.0, JvmNumbers.min(1.0, (-(offset).toDouble() / scrollRange)))).toFloat())
        }

        @JvmStatic fun opacity(fraction: Float): Float {
            return (1 - JvmNumbers.max(0F, JvmNumbers.min(1F, fraction)))
        }
    }
}
