package com.example.floatingkeys

import android.app.*
import android.content.*
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.IBinder
import android.view.*
import android.widget.TextView
import rikka.shizuku.Shizuku

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private val views = mutableListOf<View>()
    private var keySvc: IKeyService? = null
    private var edit = false
    private var latch = false
    private val held = HashSet<String>()

    private val args by lazy {
        Shizuku.UserServiceArgs(ComponentName(packageName, KeyService::class.java.name))
            .daemon(false).processNameSuffix("keys").version(1)
    }
    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName?, s: IBinder?) { keySvc = IKeyService.Stub.asInterface(s) }
        override fun onServiceDisconnected(n: ComponentName?) { keySvc = null }
    }

    override fun onBind(i: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val ch = NotificationChannel("fk", "Floating Keys", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        startForeground(1, Notification.Builder(this, "fk").setContentTitle("Floating Keys running")
            .setSmallIcon(android.R.drawable.ic_menu_edit).build())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        Shizuku.bindUserService(args, conn)
        build()
    }

    private fun send(name: String, down: Boolean) {
        val code = Keys.all[name] ?: return
        try { keySvc?.key(code, down) } catch (_: Throwable) {}
    }

    private fun build() {
        val prefs = getSharedPreferences("keys", MODE_PRIVATE)
        val chosen = prefs.getStringSet("chosen", emptySet())!!.toList()
        val size = (56 * resources.displayMetrics.density).toInt()

        // control bar: drag mode / hold-latch mode / close
        addControl("✥", 0) { v -> edit = !edit; tint(v, edit) }
        addControl("🔒", 1) { v -> latch = !latch; tint(v, latch); if (!latch) releaseAll() }
        addControl("✕", 2) { stopSelf() }

        chosen.forEachIndexed { i, name ->
            val tv = TextView(this).apply {
                text = name; gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 14f
                background = bg(0xAA222222.toInt())
            }
            val lp = params(size, size,
                prefs.getInt("x_$name", 40 + (i % 6) * (size + 8)),
                prefs.getInt("y_$name", 200 + (i / 6) * (size + 8)))
            var dx = 0f; var dy = 0f; var sx = 0; var sy = 0
            tv.setOnTouchListener { v, e ->
                if (edit) {
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> { dx = e.rawX; dy = e.rawY; sx = lp.x; sy = lp.y }
                        MotionEvent.ACTION_MOVE -> { lp.x = sx + (e.rawX - dx).toInt(); lp.y = sy + (e.rawY - dy).toInt(); wm.updateViewLayout(v, lp) }
                        MotionEvent.ACTION_UP -> prefs.edit().putInt("x_$name", lp.x).putInt("y_$name", lp.y).apply()
                    }
                } else if (latch) {
                    if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                        val on = held.add(name)
                        if (!on) held.remove(name)
                        send(name, on); tint(v, on)
                    }
                } else when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { send(name, true); tint(v, true) }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { send(name, false); tint(v, false) }
                }
                true
            }
            wm.addView(tv, lp); views += tv
        }
    }

    private fun addControl(label: String, idx: Int, onTap: (View) -> Unit) {
        val size = (44 * resources.displayMetrics.density).toInt()
        val tv = TextView(this).apply {
            text = label; gravity = Gravity.CENTER; setTextColor(Color.WHITE); textSize = 16f
            background = bg(0xAA444444.toInt())
            setOnClickListener { onTap(this) }
        }
        wm.addView(tv, params(size, size, 8, 80 + idx * (size + 6))); views += tv
    }

    private fun releaseAll() { held.toList().forEach { send(it, false) }; held.clear()
        views.forEach { if (it is TextView && it.text.length > 0) tint(it, false) } }

    private fun tint(v: View, on: Boolean) { v.background = bg(if (on) 0xCC2E7D32.toInt() else 0xAA222222.toInt()) }
    private fun bg(c: Int) = GradientDrawable().apply { setColor(c); cornerRadius = 20f; setStroke(2, Color.WHITE) }

    private fun params(w: Int, h: Int, x: Int, y: Int) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START; this.x = x; this.y = y }

    override fun onDestroy() {
        held.toList().forEach { send(it, false) }
        views.forEach { try { wm.removeView(it) } catch (_: Throwable) {} }
        try { Shizuku.unbindUserService(args, conn, true) } catch (_: Throwable) {}
        super.onDestroy()
    }
}
