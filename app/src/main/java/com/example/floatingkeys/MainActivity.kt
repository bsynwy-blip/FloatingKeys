package com.example.floatingkeys

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.*
import rikka.shizuku.Shizuku

class MainActivity : Activity() {
    private val boxes = LinkedHashMap<String, CheckBox>()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = getSharedPreferences("keys", MODE_PRIVATE)
        val chosen = prefs.getStringSet("chosen", setOf("Ctrl", "Space"))!!

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 48, 32, 32) }
        val grid = GridLayout(this).apply { columnCount = 4 }
        Keys.all.keys.forEach { name ->
            val cb = CheckBox(this).apply { text = name; isChecked = name in chosen }
            boxes[name] = cb; grid.addView(cb)
        }
        root.addView(TextView(this).apply { text = "اختر الأزرار / Choose keys"; textSize = 18f })
        root.addView(Button(this).apply {
            text = "تحديد الكل / Select all"
            setOnClickListener { boxes.values.forEach { it.isChecked = true } }
        })
        root.addView(Button(this).apply {
            text = "مسح الكل / Clear"
            setOnClickListener { boxes.values.forEach { it.isChecked = false } }
        })
        root.addView(ScrollView(this).apply { addView(grid) }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(Button(this).apply { text = "تشغيل / Start"; setOnClickListener { start() } })
        root.addView(Button(this).apply { text = "إيقاف / Stop"; setOnClickListener { stopService(Intent(this@MainActivity, FloatingService::class.java)) } })
        setContentView(root)
    }

    private fun start() {
        val sel = boxes.filter { it.value.isChecked }.keys
        if (sel.isEmpty()) return toast("اختر زر واحد على الأقل")
        getSharedPreferences("keys", MODE_PRIVATE).edit().putStringSet("chosen", sel.toSet()).apply()

        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return toast("فعّل الظهور فوق التطبيقات ثم ارجع")
        }
        if (!Shizuku.pingBinder()) return toast("شغّل Shizuku أولاً")
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            Shizuku.requestPermission(1); return toast("اسمح لـ Shizuku ثم اضغط تشغيل")
        }
        if (android.os.Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 0)
        startForegroundService(Intent(this, FloatingService::class.java))
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
}
