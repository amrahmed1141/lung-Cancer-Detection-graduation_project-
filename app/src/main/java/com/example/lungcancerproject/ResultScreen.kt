package com.example.lungcancerproject

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ResultScreen : AppCompatActivity() {
    private lateinit var cancerImage: ImageView
    private lateinit var back: ImageView
    private lateinit var result: TextView
    private lateinit var detect: Button
    private lateinit var resultText: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_screen)
        detect = findViewById(R.id.detect)
        result = findViewById(R.id.result)
        cancerImage = findViewById(R.id.cancerImage)
        back = findViewById(R.id.back)
        initView()
    }

    private fun initView(){
        lifecycleScope.launch(Dispatchers.Main) {
            delay(200)
            if (DetectionModel.prediction.equals("0")) {
                resultText = "Bengin"
            } else if (DetectionModel.prediction.equals("1")) {
                resultText = "Malignant"
            } else if (DetectionModel.prediction.equals("2")) {
                resultText = "Normal"
            }else{
                resultText = "Couldn't Detect"
            }
            result.text = resultText
            if (DetectionModel.uri == null){
                cancerImage.setImageBitmap(DetectionModel.bitmap)
            }else{
                cancerImage.setImageURI(DetectionModel.uri)
            }
            back.setOnClickListener { onBackPressed() }
            detect.setOnClickListener { onBackPressed() }
        }
    }

}