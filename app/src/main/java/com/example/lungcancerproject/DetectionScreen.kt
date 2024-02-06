package com.example.lungcancerproject

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class DetectionScreen : AppCompatActivity() {
    private val REQUEST_PICK_IMAGE = 2
    private val CAMERA_PERMISSION_REQUEST_CODE = 1
    private val STORAGE_PERMISSION_REQUEST_CODE = 2
    private val CAMERA_REQUEST_CODE = 3
    private lateinit var image: ImageView
    private lateinit var openCamera: Button
    private lateinit var openGallery: Button
    private lateinit var detect: Button
    private var imageFile: MultipartBody.Part? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detection_screen)

        image = findViewById(R.id.cancerImage)
        openGallery = findViewById(R.id.openGallery)
        openCamera = findViewById(R.id.openCamera)
        detect = findViewById(R.id.detect)
        openCamera.setOnClickListener { checkCameraPer() }
        openGallery.setOnClickListener { checkGalleryPer() }
        detect.setOnClickListener {
            if (imageFile == null){
                Toast.makeText(this ,"please select image first", Toast.LENGTH_SHORT).show()
            }else{
                uploadImage()
            }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            openCamera()
        } else pickImage()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_IMAGE && resultCode == Activity.RESULT_OK) {
            val selectedImageUri = data?.data
            val selectedImagePath = getRealPathFromURI(selectedImageUri)
            val file = File(selectedImagePath)
            val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), file)
            val imageBody = MultipartBody.Part.createFormData("file", file.name, requestFile)
            image.setImageURI(selectedImageUri)
            imageFile = imageBody
            DetectionModel.uri = data?.data
            DetectionModel.bitmap = null

        } else if (resultCode == Activity.RESULT_OK) {
            val photo: Bitmap? = data?.extras?.get("data") as? Bitmap
            DetectionModel.bitmap = photo
            DetectionModel.uri = null

            image.setImageBitmap(photo)
            imageFile = bitmapToMultipart(photo!!)
        }

    }

    private fun bitmapToMultipart(bitmap: Bitmap): MultipartBody.Part {
        // Convert Bitmap to File
        val file = bitmapToFile(bitmap)

        // Create RequestBody
        val requestFile: RequestBody = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)

        // Create MultipartBody.Part
        return MultipartBody.Part.createFormData("file", file.name, requestFile)
    }

    private fun bitmapToFile(bitmap: Bitmap): File {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "image.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return file
    }

    private fun getRealPathFromURI(uri: Uri?): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri!!, projection, null, null, null)
        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        val imagePath = columnIndex?.let { cursor?.getString(it) }
        cursor?.close()
        return imagePath ?: ""
    }

    private fun checkCameraPer() {
// Check and request CAMERA permission
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            openCamera()
        }
    }

    private fun checkGalleryPer() {
        // Check and request READ_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_REQUEST_CODE
            )
        } else {
            pickImage()
        }
    }

    private fun openCamera() {
        try {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.resolveActivity(packageManager)?.let {
                startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
            }
        } catch (e: Exception) {
            Log.e("crash", "openCamera: ", e)
        }
    }


    private fun uploadImage() {
        val interceptor = HttpLoggingInterceptor(PrettyLogger())
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val retrofit = Retrofit.Builder()
            .baseUrl("http://16.16.124.148:80/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(
                OkHttpClient.Builder()
                .connectTimeout(30 , TimeUnit.SECONDS)
                .readTimeout(30 , TimeUnit.SECONDS)
                .addInterceptor(interceptor)
                .build())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val call: Call<ResponseModel> = apiService.uploadImage(imageFile!!)
        call.enqueue(object : Callback<ResponseModel> {
            override fun onResponse(call: Call<ResponseModel>, response: Response<ResponseModel>) {
                if (response.isSuccessful) {
                    Log.d("TAG", "onResponse: "+response.body()?.prediction)
                    DetectionModel.prediction = response.body()?.prediction
                    startActivity(Intent(this@DetectionScreen , ResultScreen::class.java))
                    // Image upload successful
                    // Handle the response as needed
                } else {
                    // Image upload failed
                    // Handle the error response
                    Log.d("failed", "onResponse: "+response.body()?.prediction)

                }
            }

            override fun onFailure(call: Call<ResponseModel>, t: Throwable) {
                Log.e("TAG", "onFailure: ",t )
                // Image upload failed due to network or other issues
                // Handle the failure
            }
        })
    }
}