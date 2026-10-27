package com.lucas.xgameassistant

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("x_game_assistant", MODE_PRIVATE) }
    private lateinit var statsText: TextView
    private lateinit var tipText: TextView
    private lateinit var screenText: TextView
    private lateinit var captureStatus: TextView
    private lateinit var adviceText: TextView
    private var receiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val value = intent?.getStringExtra(GameScreenCaptureService.EXTRA_TEXT).orEmpty()
            when (intent?.action) {
                GameScreenCaptureService.ACTION_OCR_RESULT -> showRecognizedText(value)
                GameScreenCaptureService.ACTION_STATUS -> {
                    captureStatus.text = value
                    prefs.edit().putString("capture_status", value).apply()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 10, 13)
        window.navigationBarColor = Color.rgb(10, 10, 13)
        render()
    }

    override fun onStart() {
        super.onStart()
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(GameScreenCaptureService.ACTION_OCR_RESULT)
                addAction(GameScreenCaptureService.ACTION_STATUS)
            }
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(screenReceiver, filter)
            }
            receiverRegistered = true
        }
        captureStatus.text = prefs.getString("capture_status", "Status: pembacaan layar belum dimulai.")
        showRecognizedText(prefs.getString("last_screen_text", "").orEmpty())
    }

    override fun onStop() {
        if (receiverRegistered) {
            try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
            receiverRegistered = false
        }
        super.onStop()
    }

    private fun render() {
        val bg = Color.rgb(10, 10, 13)
        val panel = Color.rgb(24, 24, 30)
        val accent = Color.rgb(138, 92, 246)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 26, 22, 22)
            setBackgroundColor(bg)
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 18)
        }

        content.addView(TextView(this).apply {
            text = "X GAME ASSISTANT"
            textSize = 12f
            letterSpacing = 0.18f
            setTextColor(accent)
            typeface = Typeface.DEFAULT_BOLD
        })
        content.addView(TextView(this).apply {
            text = "Main lebih cerdas."
            textSize = 29f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, 8, 0, 8)
        })
        content.addView(bodyText("Asisten game lokal tanpa API cloud. Membaca teks layar dengan izinmu dan memberi saran heuristik untuk 9 Kings."))

        content.addView(sectionTitle("ANALISIS LAYAR", accent))
        captureStatus = panelText(panel).apply { text = "Status: pembacaan layar belum dimulai." }
        content.addView(captureStatus)
        content.addView(actionButton("Mulai baca layar", accent) { requestScreenCapture() })
        content.addView(actionButton("Hentikan pembacaan layar", panel) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startService(Intent(this, GameScreenCaptureService::class.java).setAction(GameScreenCaptureService.ACTION_STOP))
            } else {
                stopService(Intent(this, GameScreenCaptureService::class.java))
            }
            captureStatus.text = "Meminta layanan berhenti..."
        })

        content.addView(sectionTitle("SARAN LOKAL 9 KINGS", accent))
        adviceText = panelText(panel).apply {
            text = "Buka 9 Kings, lalu mulai baca layar. Saran masih heuristik dan belum menggerakkan permainan secara otomatis."
        }
        content.addView(adviceText)

        content.addView(sectionTitle("TEKS YANG TERDETEKSI", accent))
        screenText = panelText(panel).apply { text = "Teks layar akan tampil di sini." }
        content.addView(screenText)

        content.addView(sectionTitle("KONTROL ANDROID", accent))
        content.addView(bodyText("Fondasi gestur Android sudah disiapkan. Kontrol otomatis belum diaktifkan, karena lokasi tombol dan keadaan game harus divalidasi agar bot tidak menekan pilihan yang salah."))
        content.addView(actionButton("Buka pengaturan aksesibilitas", accent) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })

        content.addView(sectionTitle("PROGRES LATIHAN", accent))
        statsText = panelText(panel)
        content.addView(statsText)
        content.addView(actionButton("Catat sesi berhasil", accent) { record(true) })
        content.addView(actionButton("Catat sesi perlu diperbaiki", panel) { record(false) })

        content.addView(sectionTitle("SARAN LATIHAN", accent))
        tipText = panelText(panel)
        content.addView(tipText)

        content.addView(TextView(this).apply {
            text = "Privasi: OCR diproses di perangkat. Tangkapan layar tidak dikirim ke API cloud. Bot belum memainkan 9 Kings secara mandiri; tindakan otomatis perlu validasi langsung di game."
            textSize = 12f
            setTextColor(Color.rgb(155, 155, 168))
            setPadding(0, 20, 0, 12)
            setLineSpacing(3f, 1f)
        })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshStats()
    }

    private fun showRecognizedText(value: String) {
        if (!::screenText.isInitialized || !::adviceText.isInitialized) return
        screenText.text = if (value.isBlank()) "Belum ada teks yang terbaca." else value
        val advice = NineKingsStrategy(prefs).analyze(value)
        adviceText.text = "TAHAP: ${advice.phase}\n\n${advice.recommendation}\n\nAlasan: ${advice.reason}\nKeyakinan awal: ${advice.confidence}%"
        prefs.edit().putString("last_advice", advice.recommendation).apply()
    }

    private fun requestScreenCapture() {
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            @Suppress("DEPRECATION")
            startActivityForResult(manager.createScreenCaptureIntent(), REQUEST_CAPTURE)
        } catch (error: Exception) {
            captureStatus.text = "Tidak dapat meminta izin berbagi layar: ${error.localizedMessage ?: "kesalahan"}"
        }
    }

    @Deprecated("Uses activity result for compatibility with Android 8+")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CAPTURE) return
        if (resultCode != RESULT_OK || data == null) {
            captureStatus.text = "Izin berbagi layar dibatalkan."
            return
        }
        val serviceIntent = Intent(this, GameScreenCaptureService::class.java).apply {
            putExtra(GameScreenCaptureService.EXTRA_RESULT_CODE, resultCode)
            putExtra(GameScreenCaptureService.EXTRA_RESULT_DATA, data)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent)
            else startService(serviceIntent)
            captureStatus.text = "Memulai pembacaan layar..."
        } catch (error: Exception) {
            captureStatus.text = "Layanan gagal dimulai: ${error.localizedMessage ?: "kesalahan"}"
        }
    }

    private fun sectionTitle(value: String, color: Int) = TextView(this).apply {
        text = value
        textSize = 12f
        letterSpacing = 0.12f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(color)
        setPadding(0, 24, 0, 10)
    }

    private fun bodyText(value: String) = TextView(this).apply {
        text = value
        textSize = 14f
        setTextColor(Color.rgb(190, 190, 202))
        setLineSpacing(4f, 1f)
        setPadding(0, 0, 0, 10)
    }

    private fun panelText(panel: Int) = TextView(this).apply {
        textSize = 14f
        setTextColor(Color.WHITE)
        setPadding(16, 16, 16, 16)
        setBackgroundColor(panel)
        setLineSpacing(4f, 1f)
    }

    private fun actionButton(label: String, color: Int, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 14f
        setTextColor(Color.WHITE)
        setBackgroundColor(color)
        setOnClickListener { action() }
        val params = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.topMargin = 8
        layoutParams = params
    }

    private fun record(success: Boolean) {
        val wins = prefs.getInt("wins", 0) + if (success) 1 else 0
        val attempts = prefs.getInt("attempts", 0) + 1
        prefs.edit().putInt("wins", wins).putInt("attempts", attempts).apply()
        refreshStats()
    }

    private fun refreshStats() {
        val wins = prefs.getInt("wins", 0)
        val attempts = prefs.getInt("attempts", 0)
        val rate = if (attempts == 0) 0 else (wins * 100 / attempts)
        statsText.text = "PROGRES LATIHAN\n\n$attempts sesi tercatat     •     $rate% berhasil"
        tipText.text = when {
            attempts == 0 -> "Mulai dengan satu sesi latihan. Catat hasilnya supaya aplikasi punya data awal."
            rate >= 75 -> "Konsistensi bagus. Naikkan target sedikit demi sedikit dan pertahankan teknik yang berhasil."
            rate >= 45 -> "Progres sedang dibangun. Fokus pada satu keterampilan setiap sesi, lalu catat hasilnya."
            else -> "Coba kurangi target jadi lebih kecil dan ulangi latihan dasar. Satu perubahan setiap sesi lebih mudah dievaluasi."
        }
    }

    companion object {
        private const val REQUEST_CAPTURE = 4206
    }
}
