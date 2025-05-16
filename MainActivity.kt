package com.example.textocr1
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var cameraImage: ImageView
    private lateinit var captureImgBtn: Button
    private lateinit var resultText: TextView
    private lateinit var copyTextBtn: Button
    
    private var currentPhotoPath: String? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        cameraImage = findViewById(R.id.cameraImage)
        captureImgBtn = findViewById(R.id.captureImgBtn)
        resultText = findViewById(R.id.resultText)
        copyTextBtn = findViewById(R.id.copyTextBtn)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContacts.RequestPermission()){
            isGranted ->
            if(isGranted){
                captureImage()
            } else{
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()){
            sucess ->
            if(sucess){
                currentPhotoPath?.let{ path ->
                    val bitmap = BitmapFactory.decodeFile(path)
                    cameraImage.setImageBitmap(bitmap)
                    recognizeText(bitmap)
                }
            }
        }
        captureImgBtn.setOnClickListener{
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    private fun createImageFile(): File{
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).apply {
            currentPhotoPath = absolutePath
        }
    }
    private fun captureImage(){
        val photoFile: File? = try {
            createImageFile()
        } catch(ex: IOException){
            Toast.makeText(this,"Error occured while creating the file", Toast.LENGTH_SHORT).show()
            null
        }
        photoFile?.also{
            val photoUri: Uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", it)
            takePictureLauncher.launch(photoUri)
        }
    }
    private fun recognizeText(bitmap: Bitmap){

        val image = InputImage.fromBitmp(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image).addOnSuccessListener{ ocrText ->
            resultText.text = ocrText.text
            resultText.movementMethod = ScrollingMovementMethod
            copyTextBtn.setOnClickListener{
                val clipboard = ContextCompact.getSystemService(this,android.content.ClipboardManager::class.java)
                val clip = android.content.ClipData.newPlainText("recognized text", ocrText.text)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(this, "Text copied", Toast.LENGTH_SHORT).show()
            }
        } .addOnFailureListener{ e ->
            Toast.makeText(this, "Failed to recognize Text: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
