package com.example.floatingkeys

import android.os.SystemClock
import android.view.InputDevice
import android.view.InputEvent
import android.view.KeyEvent

/** Runs inside the Shizuku (shell) process, so it is allowed to inject input events. */
class KeyService : IKeyService.Stub() {
    private val manager: Any by lazy {
        val cls = try { Class.forName("android.hardware.input.InputManagerGlobal") }
        catch (e: Throwable) { Class.forName("android.hardware.input.InputManager") }
        cls.getMethod("getInstance").invoke(null)!!
    }
    private val inject by lazy {
        manager.javaClass.getMethod("injectInputEvent", InputEvent::class.java, Int::class.javaPrimitiveType)
    }
    private val setSource by lazy {
        InputEvent::class.java.getMethod("setSource", Int::class.javaPrimitiveType)
    }

    override fun key(keyCode: Int, down: Boolean) {
        val now = SystemClock.uptimeMillis()
        val e = KeyEvent(now, now, if (down) KeyEvent.ACTION_DOWN else KeyEvent.ACTION_UP, keyCode, 0,
            0, -1, 0, 0, InputDevice.SOURCE_KEYBOARD)
        try { setSource.invoke(e, InputDevice.SOURCE_KEYBOARD) } catch (_: Throwable) {}
        inject.invoke(manager, e, 0) // 0 = async
    }

    override fun destroy() { System.exit(0) }
}
