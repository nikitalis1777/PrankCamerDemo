package com.example.prankcamerdemo

import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
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
    private var isRunning = false

    // Массив с шутками и приколами
    private val jokes = arrayOf(
        "🤡 Ты попал в ловушку! Шучу... или нет?",
        "💧 Вода течёт, время идёт, а ты всё ещё здесь!",
        "😂 Если ты читаешь это - ты уже проиграл!",
        "🎭 Это не баг, это фича!",
        "⏰ Тик-так... время не ждёт!",
        "🎪 Добро пожаловать в цирк!",
        "🎯 Цель найдена. Отступления нет!",
        "🚫 Выход запрещён до завершения таймера!",
        "😱 Паника? Ещё нет? Сейчас будет!",
        "🎮 Game Over... шучу, продолжай ждать!",
        "🤖 Роботы захватывают мир... а ты ждёшь таймер!",
        "💀 Это конец... или начало?",
        "🌟 Ты избран... ждать!",
        "🎁 Подарок внутри... пустота!",
        "🔥 Огонь, вода и медные трубы!"
    )

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
                .setTitle("⚠️ Подтвердите действие")
                .setMessage("ВНИМАНИЕ! После запуска нельзя будет выйти до завершения таймера! Продолжить?")
                .setPositiveButton("Да, я готов!") { _, _ -> startSimulation() }
                .setNegativeButton("Нет", null)
                .setCancelable(false)
                .show()
        }

        stopBtn.setOnClickListener {
            if (isRunning) {
                AlertDialog.Builder(this)
                    .setTitle("🚫 Выход запрещён!")
                    .setMessage("Нельзя остановить таймер досрочно! Жди окончания!")
                    .setPositiveButton("Понятно", null)
                    .show()
            }
        }
    }

    private fun startSimulation() {
        isRunning = true
        statusText.text = "🔒 БЛОКИРОВКА АКТИВИРОВАНА!"
        
        // Воспроизводим звук
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        // Показываем первую шутку
        showRandomJoke()

        timer = object : CountDownTimer(60000, 3000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000
                statusText.text = "⏰ Осталось: $sec сек.\n\n" + jokes.random()
                showRandomJoke()
            }
            override fun onFinish() {
                statusText.text = "✅ СИМУЛЯЦИЯ ЗАВЕРШЕНА!\nТы выжил! 🎉"
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                isRunning = false
            }
        }.start()
    }

    private fun showRandomJoke() {
        AlertDialog.Builder(this)
            .setTitle("🎲 Случайная шутка")
            .setMessage(jokes.random())
            .setPositiveButton("OK", null)
            .setCancelable(false)
            .show()
    }

    private fun stopSimulation() {
        if (!isRunning) return
        timer?.cancel()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        isRunning = false
        statusText.text = "Симуляция остановлена пользователем"
    }

    override fun onBackPressed() {
        if (isRunning) {
            AlertDialog.Builder(this)
                .setTitle("🚫 Нельзя выйти!")
                .setMessage("Таймер ещё не завершён! Жди окончания!")
                .setPositiveButton("OK", null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        mediaPlayer?.release()
    }
}
