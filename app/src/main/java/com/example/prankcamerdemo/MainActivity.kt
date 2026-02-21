package com.example.prankcamerdemo

import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private var mediaPlayer: MediaPlayer? = null
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        startBtn = findViewById(R.id.startBtn)
        stopBtn = findViewById(R.id.stopBtn)

        val placeholder: ImageView = findViewById(R.id.placeholderImage)
        placeholder.setImageResource(R.drawable.placeholder)

        startBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Подтвердите действие")
                .setMessage("Это демонстрация. Приложение будет симулировать навязчивое поведение. Продолжить?")
                .setPositiveButton("Да") { _, _ -> startSimulation() }
                .setNegativeButton("Нет", null)
                .show()
        }

        stopBtn.setOnClickListener {
            stopSimulation()
        }
    }

    private fun startSimulation() {
        statusText.text = "Симуляция запущена (без доступа к камере/микрофону)"
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound)
        mediaPlayer?.start()

        timer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000
                statusText.text = "Симуляция: осталось $sec сек. (можно остановить)"
            }
            override fun onFinish() {
                statusText.text = "Симуляция завершена"
            }
        }.start()
    }

    private fun stopSimulation() {
        timer?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        statusText.text = "Симуляция остановлена пользователем"
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        mediaPlayer?.release()
    }
}
