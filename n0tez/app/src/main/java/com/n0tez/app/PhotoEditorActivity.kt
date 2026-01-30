package com.n0tez.app

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.n0tez.app.databinding.ActivityPhotoEditorBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class PhotoEditorActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPhotoEditorBinding
    private var originalBitmap: Bitmap? = null
    private var editedBitmap: Bitmap? = null
    private var imageUri: Uri? = null
    private var rotation: Float = 0f
    private var quality: Int = 90
    
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            loadImage(it)
        }
    }
    
    private val cropLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let { uri ->
                loadImage(uri)
                Toast.makeText(this, "Image cropped successfully", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(this, "Crop error: ${cropError?.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        
        // Check if image path was passed
        intent.getStringExtra("IMAGE_FILE_PATH")?.let { path ->
            loadImageFromPath(path)
        } ?: run {
            // Prompt user to select image
            pickImageLauncher.launch("image/*")
        }
    }
    
    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Photo Editor"
        
        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        
        binding.btnRotateLeft.setOnClickListener {
            rotateImage(-90f)
        }
        
        binding.btnRotateRight.setOnClickListener {
            rotateImage(90f)
        }
        
        binding.btnFlipHorizontal.setOnClickListener {
            flipImage(horizontal = true)
        }
        
        binding.btnFlipVertical.setOnClickListener {
            flipImage(horizontal = false)
        }
        
        binding.btnCrop.setOnClickListener {
            cropImage()
        }
        
        binding.seekBarQuality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                quality = progress
                binding.tvQuality.text = "Quality: $quality%"
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        binding.btnSave.setOnClickListener {
            saveEditedImage()
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
        
        // Initialize quality display
        binding.tvQuality.text = "Quality: $quality%"
    }
    
    private fun loadImage(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            editedBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            imageUri = uri
            displayImage()
            
            Toast.makeText(this, "Image loaded", Toast.LENGTH_SHORT).show()
            
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun loadImageFromPath(path: String) {
        try {
            originalBitmap = BitmapFactory.decodeFile(path)
            editedBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
            imageUri = Uri.fromFile(File(path))
            displayImage()
            
            Toast.makeText(this, "Image loaded", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun displayImage() {
        editedBitmap?.let {
            binding.imageView.setImageBitmap(it)
            
            // Update image info
            val width = it.width
            val height = it.height
            val sizeKB = (it.byteCount / 1024)
            binding.tvImageInfo.text = "Size: ${width}x${height} | ~${sizeKB}KB"
        }
    }
    
    private fun rotateImage(degrees: Float) {
        editedBitmap?.let { bitmap ->
            rotation += degrees
            rotation %= 360
            
            val matrix = Matrix().apply {
                postRotate(degrees)
            }
            
            editedBitmap = Bitmap.createBitmap(
                bitmap,
                0, 0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            
            displayImage()
        }
    }
    
    private fun flipImage(horizontal: Boolean) {
        editedBitmap?.let { bitmap ->
            val matrix = Matrix().apply {
                if (horizontal) {
                    postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                } else {
                    postScale(1f, -1f, bitmap.width / 2f, bitmap.height / 2f)
                }
            }
            
            editedBitmap = Bitmap.createBitmap(
                bitmap,
                0, 0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
            
            displayImage()
        }
    }
    
    private fun cropImage() {
        editedBitmap?.let { bitmap ->
            try {
                // Create temp file for current edited bitmap
                val tempFile = File(cacheDir, "temp_crop_${System.currentTimeMillis()}.jpg")
                val outputFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                
                // Save current bitmap to temp file
                FileOutputStream(tempFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                
                val sourceUri = Uri.fromFile(tempFile)
                val destinationUri = Uri.fromFile(outputFile)
                
                // Configure UCrop with FaceShot theme colors
                val options = UCrop.Options().apply {
                    setToolbarColor(resources.getColor(R.color.faceshot_purple_primary, theme))
                    setStatusBarColor(resources.getColor(R.color.faceshot_purple_dark, theme))
                    setToolbarWidgetColor(resources.getColor(R.color.white, theme))
                    setActiveControlsWidgetColor(resources.getColor(R.color.faceshot_pink_accent, theme))
                    setFreeStyleCropEnabled(true)
                    setShowCropGrid(true)
                    setShowCropFrame(true)
                    setHideBottomControls(false)
                    setCompressionQuality(100)
                }
                
                // Launch UCrop
                val uCropIntent = UCrop.of(sourceUri, destinationUri)
                    .withOptions(options)
                    .withMaxResultSize(4000, 4000)
                    .getIntent(this)
                
                cropLauncher.launch(uCropIntent)
                
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to start crop: ${e.message}", Toast.LENGTH_LONG).show()
            }
        } ?: run {
            Toast.makeText(this, "Please load an image first", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveEditedImage() {
        editedBitmap?.let { bitmap ->
            try {
                // Create images directory if it doesn't exist
                val imagesDir = File(filesDir, "media/images")
                if (!imagesDir.exists()) {
                    imagesDir.mkdirs()
                }
                
                // Generate filename with timestamp
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val imageFile = File(imagesDir, "edited_$timestamp.jpg")
                
                // Save bitmap with specified quality
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
                
                Toast.makeText(
                    this,
                    "Image saved: ${imageFile.name}",
                    Toast.LENGTH_LONG
                ).show()
                
                finish()
                
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(
                    this,
                    "Failed to save image: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        } ?: run {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        originalBitmap?.recycle()
        editedBitmap?.recycle()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
