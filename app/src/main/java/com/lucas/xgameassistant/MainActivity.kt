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
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener { view, insets ->
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars())
                view.setPadding(22, 26 + bars.top, 22, 22 + bars.bottom)
                insets
            }
        } else {
            @Suppress("DEPRECATION")
            root.setOnApplyWindowInsetsListener { view, insets ->
                view.setPadding(22, 26 + insets.systemWindowInsetTop, 22, 22 + insets.systemWindowInsetBottom)
                insets
            }
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
        content.addView(bodyText("Asisten game lokal tanpa API cloud. Pilih profil game, lalu baca teks layar dengan izinmu untuk mendapat saran lokal."))

        content.addView(sectionTitle("PROFIL GAME", accent))
        content.addView(actionButton("Pilih profil 9 Kings", accent) { selectGame(GameStrategyRegistry.NINE_KINGS) })
        content.addView(actionButton("Pilih profil game lain", panel) { selectGame(GameStrategyRegistry.GENERIC) })

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
        val gameId = prefs.getString("selected_game", GameStrategyRegistry.NINE_KINGS) ?: GameStrategyRegistry.NINE_KINGS
        val advice = GameStrategyRegistry.forGame(gameId, prefs).analyze(value)
        adviceText.text = "TAHAP: ${advice.phase}\n\n${advice.recommendation}\n\nAlasan: ${advice.reason}\nKeyakinan awal: ${advice.confidence}%"
        prefs.edit().putString("last_advice", advice.recommendation).apply()
    }

    private fun selectGame(gameId: String) {
        prefs.edit().putString("selected_game", gameId).apply()
        val name = if (gameId == GameStrategyRegistry.NINE_KINGS) "9 Kings" else "game lain"
        captureStatus.text = "Profil aktif: $name"
        showRecognizedText(prefs.getString("last_screen_text", "").orEmpty())
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


// Self-contained local strategy models. Kept in this source file so the project does not
// depend on missing auxiliary Kotlin files.
data class StrategyAdvice(
    val phase: String,
    val recommendation: String,
    val reason: String,
    val confidence: Int
)

interface LocalGameStrategy {
    fun analyze(text: String): StrategyAdvice
}

private class NineKingsLocalStrategy : LocalGameStrategy {
    override fun analyze(rawText: String): StrategyAdvice {
        val text = rawText.lowercase()
        return when {
            text.isBlank() -> StrategyAdvice("Menunggu layar", "Buka 9 Kings dan pastikan teks game terlihat.", "Belum ada teks yang dapat dianalisis.", 10)
            listOf("upgrade", "choose", "select", "pilih").any { it in text } ->
                StrategyAdvice("Pilihan", "Bandingkan efek tiap pilihan dan utamakan sinergi dengan strategi kerajaanmu.", "Teks mungkin menandakan layar pilihan; rekomendasi masih umum.", 35)
            listOf("wave", "enemy", "battle", "musuh", "serangan").any { it in text } ->
                StrategyAdvice("Pertempuran", "Periksa kekuatan pasukan, pertahanan, dan sumber daya sebelum bertindak.", "Terdeteksi istilah pertempuran, tetapi status unit belum dipahami penuh.", 30)
            listOf("gold", "food", "wood", "stone", "resource", "sumber daya").any { it in text } ->
                StrategyAdvice("Ekonomi", "Jaga keseimbangan sumber daya dan hindari menghabiskan semuanya untuk satu peningkatan.", "Terdeteksi istilah sumber daya; nilai dan biaya belum terbaca andal.", 30)
            else -> StrategyAdvice("Analisis dasar", "Amati kondisi kerajaan dan pilih tindakan yang mendukung rencana jangka panjang.", "Belum ada pola teks yang cukup kuat untuk saran spesifik.", 20)
        }
    }
}

private class GenericLocalStrategy : LocalGameStrategy {
    override fun analyze(text: String) = StrategyAdvice(
        "Profil umum",
        "Profil khusus game ini belum dikonfigurasi. Pertimbangkan tujuan, sumber daya, dan risiko yang terlihat.",
        if (text.isBlank()) "Belum ada teks layar." else "Profil umum tidak mengenali mekanik game secara khusus.",
        10
    )
}

object GameStrategyRegistry {
    const val NINE_KINGS = "nine_kings"
    const val GENERIC = "generic"
    fun forGame(gameId: String, prefs: android.content.SharedPreferences): LocalGameStrategy =
        if (gameId == NINE_KINGS) NineKingsLocalStrategy() else GenericLocalStrategy()
}

/**
 * Android 14+ requires a foreground media-projection service. The user must approve
 * each capture session through Android's own screen-sharing consent dialog.
 */
class GameScreenCaptureService : android.app.Service() {
    private var projection: android.media.projection.MediaProjection? = null
    private var display: android.hardware.display.VirtualDisplay? = null
    private var reader: android.media.ImageReader? = null
    private var thread: android.os.HandlerThread? = null
    private var handler: android.os.Handler? = null
    private var busy = false
    private val recognizer by lazy {
        com.google.mlkit.vision.text.TextRecognition.getClient(
            com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
        )
    }

    override fun onBind(intent: android.content.Intent?): android.os.IBinder? = null

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }
        if (android.os.Build.VERSION.SDK_INT >= 29) {\n            startForeground(NOTIFICATION_ID, notification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)\n        } else {\n            startForeground(NOTIFICATION_ID, notification())\n        }
        val code = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val data: android.content.Intent? = if (android.os.Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, android.content.Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (code == 0 || data == null) {
            broadcast(ACTION_STATUS, "Izin berbagi layar tidak tersedia.")
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            val manager = getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE) as android.media.projection.MediaProjectionManager
            projection = manager.getMediaProjection(code, data)
            projection?.registerCallback(object : android.media.projection.MediaProjection.Callback() {
                override fun onStop() {
                    stopCapture()
                    stopSelf()
                }
            }, android.os.Handler(mainLooper))
            beginCapture()
            broadcast(ACTION_STATUS, "Pembacaan layar aktif. OCR berjalan lokal di perangkat.")
        } catch (e: Exception) {
            broadcast(ACTION_STATUS, "Gagal memulai pembacaan layar: ${e.localizedMessage ?: "kesalahan"}")
            stopCapture()
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun notification(): android.app.Notification {
        val channel = "x_game_assistant_capture"
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(android.app.NotificationChannel(channel, "Analisis layar", android.app.NotificationManager.IMPORTANCE_LOW))
        }
        val builder = if (android.os.Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(this, channel)
            else @Suppress("DEPRECATION") android.app.Notification.Builder(this)
        return builder.setContentTitle("X Game Assistant")
            .setContentText("Analisis layar aktif di perangkat")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
    }

    private fun beginCapture() {
        val metrics = android.util.DisplayMetrics()
        val wm = getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        val width = metrics.widthPixels.coerceAtLeast(1)
        val height = metrics.heightPixels.coerceAtLeast(1)
        thread = android.os.HandlerThread("x-game-assistant-ocr").also { it.start() }
        handler = android.os.Handler(thread!!.looper)
        reader = android.media.ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 2)
        reader?.setOnImageAvailableListener({ source ->
            val image = source.acquireLatestImage() ?: return@setOnImageAvailableListener
            if (busy) {
                image.close()
                return@setOnImageAvailableListener
            }
            try {
                busy = true
                recognizer.process(com.google.mlkit.vision.common.InputImage.fromMediaImage(image, 0))
                    .addOnSuccessListener { result ->
                        val text = result.text.take(4000)
                        getSharedPreferences("x_game_assistant", android.content.Context.MODE_PRIVATE)
                            .edit().putString("last_screen_text", text).apply()
                        broadcast(ACTION_OCR_RESULT, text)
                    }
                    .addOnFailureListener { broadcast(ACTION_STATUS, "OCR gagal membaca frame. Coba lagi.") }
                    .addOnCompleteListener {
                        image.close()
                        busy = false
                    }
            } catch (_: Exception) {
                busy = false
                image.close()
                broadcast(ACTION_STATUS, "Frame layar tidak dapat diproses.")
            }
        }, handler)
        display = projection?.createVirtualDisplay(
            "XGameAssistant", width, height, metrics.densityDpi,
            android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader?.surface, null, handler
        )
    }

    private fun broadcast(action: String, value: String) {
        sendBroadcast(android.content.Intent(action).setPackage(packageName).putExtra(EXTRA_TEXT, value))
    }

    private fun stopCapture() {
        try { display?.release() } catch (_: Exception) {}
        display = null
        try { reader?.close() } catch (_: Exception) {}
        reader = null
        val oldProjection = projection
        projection = null
        try { oldProjection?.stop() } catch (_: Exception) {}
        try { thread?.quitSafely() } catch (_: Exception) {}
        thread = null
        handler = null
        try { recognizer.close() } catch (_: Exception) {}
        if (android.os.Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE)
        else @Suppress("DEPRECATION") stopForeground(true)
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    companion object {
        const val ACTION_OCR_RESULT = "com.lucas.xgameassistant.OCR_RESULT"
        const val ACTION_STATUS = "com.lucas.xgameassistant.CAPTURE_STATUS"
        const val ACTION_STOP = "com.lucas.xgameassistant.STOP_CAPTURE"
        const val EXTRA_TEXT = "extra_text"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        private const val NOTIFICATION_ID = 4601
    }
}
