package com.lucas.xgameassistant

import android.content.SharedPreferences
import java.util.Locale

/**
 * Offline heuristic strategy starter for 9 Kings.
 * It reasons only from visible OCR text, so it must not invent board state
 * or trigger taps without a verified screen-specific action map.
 */
class NineKingsStrategy(private val prefs: SharedPreferences) : GameStrategy {
    override fun analyze(visibleText: String): GameAdvice {
        val text = visibleText.lowercase(Locale.ROOT)
        val history = prefs.getInt("nk_observations", 0) + 1
        prefs.edit().putInt("nk_observations", history).apply()

        return when {
            containsAny(text, "choose a card", "select a card", "card reward", "pick a card", "pilih kartu") ->
                GameAdvice(
                    "PILIH KARTU",
                    "Bandingkan efek kartu dengan bangunan, pasukan, dan sinergi yang sudah terlihat sebelum memilih.",
                    "Teks yang terdeteksi menunjukkan tahap pemilihan kartu. Nama kartu dan efeknya perlu terbaca jelas untuk rekomendasi spesifik.",
                    55
                )
            containsAny(text, "upgrade", "upgrade building", "improve", "tingkatkan") ->
                GameAdvice(
                    "PENINGKATAN",
                    "Periksa biaya dan efek peningkatan. Utamakan peningkatan yang memperkuat pertahanan atau sinergi aktif tanpa menghabiskan seluruh emas.",
                    "Layar tampaknya menampilkan opsi peningkatan, tetapi saldo dan statistik papan belum dipastikan.",
                    45
                )
            containsAny(text, "battle", "enemy", "attack", "wave", "serangan", "musuh") ->
                GameAdvice(
                    "PERTEMPURAN",
                    "Biarkan pertempuran berjalan dan periksa titik pertahanan yang paling tertekan sebelum mengubah strategi berikutnya.",
                    "Teks terkait pertempuran terdeteksi. Pertempuran di 9 Kings sebagian besar berlangsung otomatis.",
                    40
                )
            containsAny(text, "victory", "you win", "defeated", "defeat", "game over", "kemenangan", "kalah") ->
                GameAdvice(
                    "HASIL RUN",
                    "Catat hasil dan kartu atau peningkatan yang digunakan. Gunakan catatan ini untuk membandingkan run berikutnya.",
                    "Teks hasil permainan terdeteksi; pastikan hasilnya benar sebelum menyimpan evaluasi.",
                    60
                )
            containsAny(text, "gold", "coins", "treasury", "emas") ->
                GameAdvice(
                    "EKONOMI",
                    "Jangan belanjakan semua sumber daya sekaligus. Bandingkan biaya opsi yang terlihat dengan kebutuhan pertahanan berikutnya.",
                    "Teks sumber daya terdeteksi, tetapi jumlah emas dan harga pilihan belum diekstrak dengan andal.",
                    35
                )
            else ->
                GameAdvice(
                    "LAYAR BELUM DIKENALI",
                    "Pastikan layar game terlihat jelas dan tidak tertutup menu lain. Modul akan memakai teks yang terbaca untuk memberi saran lokal.",
                    "Belum ada pola teks yang cukup kuat untuk menyarankan langkah tertentu.",
                    15
                )
        }
    }

    private fun containsAny(text: String, vararg terms: String): Boolean =
        terms.any { text.contains(it) }
}
