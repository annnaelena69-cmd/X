package com.lucas.xgameassistant

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("x_game_assistant", MODE_PRIVATE) }
    private lateinit var statsText: TextView
    private lateinit var tipText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 10, 13)
        window.navigationBarColor = Color.rgb(10, 10, 13)
        render()
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
        content.addView(TextView(this).apply {
            text = "Asisten game offline yang mencatat latihan dan menyesuaikan saran dari hasilmu. Tanpa API, akun, atau koneksi cloud."
            textSize = 14f
            setTextColor(Color.rgb(190, 190, 202))
            setLineSpacing(4f, 1f)
        })

        statsText = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(16, 16, 16, 16)
            setBackgroundColor(panel)
        }
        val statsCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 20, 0, 0)
            addView(statsText)
        }
        content.addView(statsCard)

        content.addView(sectionTitle("LATIHAN HARI INI", accent))
        content.addView(bodyText("Catat hasil latihan agar saran lokal bisa menyesuaikan progresmu. Data tersimpan di perangkat."))
        content.addView(actionButton("Latihan berhasil", accent) { record(true) })
        content.addView(actionButton("Latihan perlu diperbaiki", panel) { record(false) })

        content.addView(sectionTitle("SARAN ADAPTIF", accent))
        tipText = TextView(this).apply {
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(16, 16, 16, 16)
            setBackgroundColor(panel)
            setLineSpacing(5f, 1f)
        }
        content.addView(tipText)

        content.addView(sectionTitle("FITUR BERIKUTNYA", accent))
        content.addView(bodyText("• Profil untuk game yang kamu pilih\n• Catatan strategi dan target latihan\n• Analisis progres lokal\n• Kontrol otomatis hanya untuk game dan penggunaan yang kamu tentukan, dengan izin Android yang jelas"))
        content.addView(TextView(this).apply {
            text = "Catatan: bot otomatis yang benar-benar memainkan game perlu dirancang sesuai game tujuan. Aplikasi ini belum mengontrol game lain dan tidak melewati sistem anti-cheat."
            textSize = 12f
            setTextColor(Color.rgb(155, 155, 168))
            setPadding(0, 18, 0, 12)
        })

        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        refreshStats()
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
}
