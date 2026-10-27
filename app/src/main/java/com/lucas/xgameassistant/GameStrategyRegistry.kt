package com.lucas.xgameassistant

import android.content.SharedPreferences
import java.util.Locale

interface GameStrategy {
    fun analyze(visibleText: String): GameAdvice
}

/**
 * Shared registry keeps game-specific decision logic separate.
 * Additional games can register dedicated strategies without changing screen capture.
 */
object GameStrategyRegistry {
    const val NINE_KINGS = "nine_kings"
    const val GENERIC = "generic"

    fun forGame(gameId: String, prefs: SharedPreferences): GameStrategy =
        when (gameId.lowercase(Locale.ROOT)) {
            NINE_KINGS -> NineKingsStrategy(prefs)
            else -> GenericGameStrategy()
        }
}

class GenericGameStrategy : GameStrategy {
    override fun analyze(visibleText: String): GameAdvice {
        val text = visibleText.lowercase(Locale.ROOT)
        return when {
            text.isBlank() -> GameAdvice(
                "BELUM ADA TEKS",
                "Pastikan layar game tidak tertutup dan teks cukup jelas.",
                "OCR belum menemukan teks yang dapat digunakan.",
                10
            )
            listOf("pause", "resume", "settings", "menu").any { text.contains(it) } -> GameAdvice(
                "MENU",
                "Periksa pilihan menu sebelum melanjutkan; modul generik tidak akan memilih tombol secara otomatis.",
                "Teks menu terdeteksi, tetapi fungsi tombol berbeda di setiap game.",
                25
            )
            listOf("victory", "defeat", "game over", "you win", "you lose").any { text.contains(it) } -> GameAdvice(
                "HASIL PERMAINAN",
                "Catat hasil dan perubahan strategi yang kamu coba untuk dibandingkan pada sesi berikutnya.",
                "Teks yang menyerupai hasil permainan terdeteksi.",
                35
            )
            else -> GameAdvice(
                "GAME BELUM DIKONFIGURASI",
                "Pembacaan teks aktif. Untuk saran atau tindakan yang akurat, game ini membutuhkan strategi khusus dan pengujian.",
                "Modul generik tidak mengetahui aturan atau posisi kontrol game ini.",
                15
            )
        }
    }
}
