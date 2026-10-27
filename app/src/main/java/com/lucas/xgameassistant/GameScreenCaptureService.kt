package com.lucas.xgameassistant

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.atomic.AtomicBoolean

class GameScreenCaptureService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var reader: ImageReader? = null
    private var worker: HandlerThread? = null
    private var textRecognizer: TextRecognizer? = null
    private var lastAnalysisAt = 0L
    private val analyzing = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopCapture()
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = if (Build.VERSION.SDK_INT >= 33) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }
        if (resultData == null) {
            publishStatus("Izin berbagi layar tidak diterima.")
            stopSelf()
            return START_NOT_STICKY
        }
        beginCapture(resultCode, resultData)
        return START_NOT_STICKY
    }

    private fun beginCapture(resultCode: Int, data: Intent) {
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = manager.getMediaProjection(resultCode, data)
            textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels.coerceAtLeast(1)
            val height = metrics.heightPixels.coerceAtLeast(1)
            val density = metrics.densityDpi
            worker = HandlerThread("XGameScreenReader").apply { start() }
            val handler = Handler(worker!!.looper)
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            reader?.setOnImageAvailableListener({ source ->
                val now = SystemClock.elapsedRealtime()
                if (now - lastAnalysisAt < 1400L || !analyzing.compareAndSet(false, true)) {
                    source.acquireLatestImage()?.close()
                    return@setOnImageAvailableListener
                }
                val image = source.acquireLatestImage() ?: run {
                    analyzing.set(false)
                    return@setOnImageAvailableListener
                }
                lastAnalysisAt = now
                try {
                    val plane = image.planes[0]
                    val buffer = plane.buffer
                    val pixelStride = plane.pixelStride
                    val rowStride = plane.rowStride
                    val rowPadding = rowStride - pixelStride * image.width
                    val paddedWidth = image.width + rowPadding / pixelStride
                    val padded = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
                    padded.copyPixelsFromBuffer(buffer)
                    val bitmap = Bitmap.createBitmap(padded, 0, 0, image.width, image.height)
                    padded.recycle()
                    image.close()
                    val recognizer = textRecognizer
                    if (recognizer == null) {
                        bitmap.recycle()
                        analyzing.set(false)
                        return@setOnImageAvailableListener
                    }
                    recognizer.process(InputImage.fromBitmap(bitmap, 0))
                        .addOnSuccessListener { result -> publishText(result.text) }
                        .addOnFailureListener { error ->
                            publishStatus("OCR lokal gagal: ${error.localizedMessage ?: "kesalahan tidak diketahui"}")
                        }
                        .addOnCompleteListener {
                            bitmap.recycle()
                            analyzing.set(false)
                        }
                } catch (error: Exception) {
                    try { image.close() } catch (_: Exception) {}
                    analyzing.set(false)
                    publishStatus("Gagal membaca frame layar.")
                }
            }, handler)
            display = projection?.createVirtualDisplay(
                "XGameAssistantCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader?.surface,
                null,
                handler
            )
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    publishStatus("Berbagi layar berhenti.")
                    stopCapture()
                    stopSelf()
                }
            }, handler)
            publishStatus("Pembacaan layar aktif. Teks diproses di perangkat.")
        } catch (error: Exception) {
            publishStatus("Tidak dapat memulai pembacaan layar: ${error.localizedMessage ?: "kesalahan"}")
            stopCapture()
            stopSelf()
        }
    }

    private fun publishText(text: String) {
        sendBroadcast(Intent(ACTION_OCR_RESULT).setPackage(packageName).putExtra(EXTRA_TEXT, text))
    }

    private fun publishStatus(status: String) {
        sendBroadcast(Intent(ACTION_STATUS).setPackage(packageName).putExtra(EXTRA_TEXT, status))
    }

    private fun stopCapture() {
        try { display?.release() } catch (_: Exception) {}
        display = null
        try { reader?.setOnImageAvailableListener(null, null) } catch (_: Exception) {}
        try { reader?.close() } catch (_: Exception) {}
        reader = null
        try { projection?.stop() } catch (_: Exception) {}
        projection = null
        try { worker?.quitSafely() } catch (_: Exception) {}
        worker = null
        try { textRecognizer?.close() } catch (_: Exception) {}
        textRecognizer = null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun buildNotification(): Notification {
        val channelId = "x_game_capture"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "X Game Assistant screen analysis",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("X Game Assistant")
                .setContentText("Membaca teks layar secara lokal")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("X Game Assistant")
                .setContentText("Membaca teks layar secara lokal")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build()
        }
    }

    override fun onDestroy() {
        stopCapture()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.lucas.xgameassistant.START_CAPTURE"
        const val ACTION_STOP = "com.lucas.xgameassistant.STOP_CAPTURE"
        const val ACTION_OCR_RESULT = "com.lucas.xgameassistant.OCR_RESULT"
        const val ACTION_STATUS = "com.lucas.xgameassistant.CAPTURE_STATUS"
        const val EXTRA_RESULT_CODE = "result_code"
        const val EXTRA_RESULT_DATA = "result_data"
        const val EXTRA_TEXT = "text"
        const val NOTIFICATION_ID = 7124
    }
}
