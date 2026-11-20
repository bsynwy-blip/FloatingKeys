package com.example.floatingkeys

import android.view.KeyEvent as K

object Keys {
    val all: Map<String, Int> = linkedMapOf<String, Int>().apply {
        put("Ctrl", K.KEYCODE_CTRL_LEFT); put("Shift", K.KEYCODE_SHIFT_LEFT); put("Alt", K.KEYCODE_ALT_LEFT)
        put("Space", K.KEYCODE_SPACE); put("Enter", K.KEYCODE_ENTER); put("Esc", K.KEYCODE_ESCAPE)
        put("Tab", K.KEYCODE_TAB); put("Backspace", K.KEYCODE_DEL)
        put("↑", K.KEYCODE_DPAD_UP); put("↓", K.KEYCODE_DPAD_DOWN)
        put("←", K.KEYCODE_DPAD_LEFT); put("→", K.KEYCODE_DPAD_RIGHT)
        for (c in 'A'..'Z') put(c.toString(), K.KEYCODE_A + (c - 'A'))
        for (c in '0'..'9') put(c.toString(), K.KEYCODE_0 + (c - '0'))
        for (i in 1..12) put("F$i", K.KEYCODE_F1 + i - 1)
    }
}
