package th.ac.rmutto.finlove

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import th.ac.rmutto.finlove.databinding.ActivityAddphotoBinding
import th.ac.rmutto.finlove.ui.profile.ProfileFragment
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

class AddphotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddphotoBinding
    private val PICK_IMAGE = 1
    private val CAMERA_REQUEST = 2
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
    private var userID: Int = -1  // รับ userID ที่ส่งมาจาก ProfileFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddphotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addphoto)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // รับ userID จาก Intent
        userID = intent.getIntExtra("userID", -1)

        checkPermissions()

        binding.imageView.setOnClickListener {
            showImageChooser()
        }

        binding.confirmButton.setOnClickListener {
            val imageUri = (binding.imageView.tag as? Uri) ?: return@setOnClickListener
            sendImageForVerification(imageUri)
        }
    }

    private fun showImageChooser() {
        val options = arrayOf<CharSequence>("Take Photo", "Choose from Gallery", "Cancel")
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Select Option")
        builder.setItems(options) { dialog, item ->
            when {
                options[item] == "Take Photo" -> {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(cameraIntent, CAMERA_REQUEST)
                }
                options[item] == "Choose from Gallery" -> {
                    val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryIntent.type = "image/*"
                    startActivityForResult(galleryIntent, PICK_IMAGE)
                }
                options[item] == "Cancel" -> dialog.dismiss()
            }
        }
        builder.show()
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE), 101)
        }
    }

    private fun onNext(selectedImageUri: Uri) {
        binding.confirmButton.visibility = View.VISIBLE
        binding.imageView.tag = selectedImageUri
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE -> {
                    val selectedImage: Uri? = data?.data
                    selectedImage?.let {
                        binding.imageView.setImageURI(it)
                        binding.textViewFile.text = "รายละเอียดชื่อไฟล์: " + it.lastPathSegment
                        binding.textViewFile.visibility = View.VISIBLE
                        binding.textView.visibility = View.GONE
                        onNext(it)
                    }
                }
                CAMERA_REQUEST -> {
                    val photo: Bitmap = data?.extras?.get("data") as Bitmap
                    binding.imageViewshow.setImageBitmap(photo)
                    binding.imageView.visibility = View.GONE
                    binding.textViewFile.text = "Captured Image"
                    binding.textViewFile.visibility = View.VISIBLE
                    binding.textView.visibility = View.GONE
                    val placeholderUri = saveImageToExternalStorage(photo)
                    onNext(placeholderUri)
                }
            }
        }
    }

    private fun saveImageToExternalStorage(bitmap: Bitmap): Uri {
        val imagesFolder = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "YourImages")
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs()
        }

        val file = File(imagesFolder, "${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return Uri.fromFile(file)
    }

    private fun sendImageForVerification(imageUri: Uri) {
        val url = "http://192.168.1.53:5000/verify"

        // แปลง Uri ของไฟล์เป็น File ที่เข้าถึงได้
        val imagePath = imageUri.path ?: ""
        val imageFile = File(imagePath)

        if (!imageFile.exists()) {
            Toast.makeText(this, "File not found at: $imagePath", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("userID", userID.toString())  // ส่ง userID ไปยังเซิร์ฟเวอร์
            .build()

        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val isVerified = JSONObject(response.body?.string()).getBoolean("is_verified")
                    runOnUiThread {
                        Toast.makeText(
                            this@AddphotoActivity,
                            if (isVerified) "Verify สำเร็จ" else "Verify ไม่สำเร็จ",
                            Toast.LENGTH_SHORT
                        ).show()

                        // แทนที่ด้วยการกลับไปที่ ProfileFragment
                        val intent = Intent(this@AddphotoActivity, MainActivity::class.java)
                        intent.putExtra("userID", userID)
                        intent.putExtra("navigateToProfile", true)  // ส่งข้อมูลไปให้เปิด ProfileFragment
                        startActivity(intent)
                        finish() // ปิด Activity นี้หลังจากเริ่ม Intent ใหม่
                    }
                }
            }


            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AddphotoActivity, "Failed to verify image", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
