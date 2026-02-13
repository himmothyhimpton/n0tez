package com.n0tez.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
    private var quality = 90
    private var rotation = 0f

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            imageUri = it
            loadImage(it)
        }
    }

    private val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                loadImage(it)
                Toast.makeText(this, "Image cropped successfully", Toast.LENGTH_SHORT).show()
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(this, "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupUI()

        val imagePath = intent.getStringExtra("IMAGE_FILE_PATH")
        imageUri = intent.data

        if (imageUri != null) {
            loadImage(imageUri!!)
        } else if (imagePath != null) {
            loadImageFromPath(imagePath)
        } else {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Photo Editor"

        binding.btnSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnRotateLeft.setOnClickListener { rotateImage(-90f) }
        binding.btnRotateRight.setOnClickListener { rotateImage(90f) }
        binding.btnFlipHorizontal.setOnClickListener { flipImage(true) }
        binding.btnFlipVertical.setOnClickListener { flipImage(false) }
        binding.btnCrop.setOnClickListener { cropImage() }
        
        binding.seekBarQuality.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                quality = progress
                binding.tvQuality.text = "Quality: $quality%"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.btnSave.setOnClickListener { saveEditedImage() }
        binding.btnShare.setOnClickListener { shareEditedImage() }
        binding.btnCancel.setOnClickListener { finish() }
        binding.tvImageInfo.setOnClickListener { showResolutionDialog() }
        
        binding.tvQuality.text = "Quality: $quality%"
    }

    private fun loadImage(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                originalBitmap = BitmapFactory.decodeStream(inputStream)
                editedBitmap = originalBitmap?.copy(Bitmap.Config.ARGB_8888, true)
                imageUri = uri
                displayImage()
                Toast.makeText(this, "Image loaded", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Failed to load image: ${e.message}", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun displayImage() {
        editedBitmap?.let {
            binding.imageView.setImageBitmap(it)
            val sizeKB = it.byteCount / 1024
            binding.tvImageInfo.text = "📐 ${it.width}x${it.height} | ~$sizeKB KB (Tap to resize)"
        }
    }

    private fun rotateImage(degrees: Float) {
        editedBitmap?.let {
            rotation = (rotation + degrees) % 360
            val matrix = Matrix()
            matrix.postRotate(degrees)
            editedBitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
            displayImage()
        }
    }

    private fun flipImage(horizontal: Boolean) {
        editedBitmap?.let {
            val matrix = Matrix()
            if (horizontal) {
                matrix.postScale(-1f, 1f, it.width / 2f, it.height / 2f)
            } else {
                matrix.postScale(1f, -1f, it.width / 2f, it.height / 2f)
            }
            editedBitmap = Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
            displayImage()
        }
    }

    private fun cropImage() {
        editedBitmap?.let {
            try {
                val tempFile = File(cacheDir, "temp_crop_${System.currentTimeMillis()}.jpg")
                val outputFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
                
                FileOutputStream(tempFile).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
                
                val options = UCrop.Options().apply {
                    setFreeStyleCropEnabled(true)
                    setShowCropGrid(true)
                    setCompressionQuality(100)
                    // Set colors if possible, skipped for brevity
                }
                
                val uCropIntent = UCrop.of(Uri.fromFile(tempFile), Uri.fromFile(outputFile))
                    .withOptions(options)
                    .withMaxResultSize(4000, 4000)
                    .getIntent(this)
                
                cropLauncher.launch(uCropIntent)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to start crop: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "Please load an image first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResolutionDialog() {
        editedBitmap?.let { bitmap ->
            val resolutions = arrayOf(
                "Keep Original (${bitmap.width}x${bitmap.height})",
                "1920x1080 (Full HD)",
                "1280x720 (HD)",
                "854x480 (SD)",
                "640x480 (VGA)",
                "Custom Resolution"
            )
            
            AlertDialog.Builder(this)
                .setTitle("Change Image Resolution")
                .setItems(resolutions) { _, which ->
                    when (which) {
                        0 -> Toast.makeText(this, "Keeping original resolution", Toast.LENGTH_SHORT).show()
                        1 -> resizeImage(1920, 1080)
                        2 -> resizeImage(1280, 720)
                        3 -> resizeImage(854, 480)
                        4 -> resizeImage(640, 480)
                        5 -> showCustomResolutionDialog()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } ?: run {
            Toast.makeText(this, "Please load an image first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCustomResolutionDialog() {
        editedBitmap?.let { bitmap ->
            val widthInput = EditText(this).apply {
                hint = "Width (px)"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(bitmap.width.toString())
            }
            val heightInput = EditText(this).apply {
                hint = "Height (px)"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                setText(bitmap.height.toString())
            }
            
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 20, 50, 20)
                addView(widthInput)
                addView(heightInput)
            }
            
            AlertDialog.Builder(this)
                .setTitle("Custom Resolution")
                .setView(layout)
                .setPositiveButton("Apply") { _, _ ->
                    val width = widthInput.text.toString().toIntOrNull() ?: bitmap.width
                    val height = heightInput.text.toString().toIntOrNull() ?: bitmap.height
                    resizeImage(width, height)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun resizeImage(newWidth: Int, newHeight: Int) {
        editedBitmap?.let {
            try {
                editedBitmap = Bitmap.createScaledBitmap(it, newWidth, newHeight, true)
                displayImage()
                Toast.makeText(this, "Image resized to ${newWidth}x$newHeight", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to resize: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveEditedImage() {
        editedBitmap?.let {
            try {
                val imagesDir = File(filesDir, "media/images")
                if (!imagesDir.exists()) imagesDir.mkdirs()
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val imageFile = File(imagesDir, "edited_$timestamp.jpg")
                
                FileOutputStream(imageFile).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
                
                Toast.makeText(this, "Image saved: ${imageFile.name}", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No image to save", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareEditedImage() {
        editedBitmap?.let {
            try {
                val cachePath = File(cacheDir, "shared_images")
                cachePath.mkdirs()
                val file = File(cachePath, "shared_image_${System.currentTimeMillis()}.jpg")
                
                FileOutputStream(file).use { out ->
                    it.compress(Bitmap.CompressFormat.JPEG, quality, out)
                }
                
                val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(shareIntent, "Share Image"))
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(this, "No image to share", Toast.LENGTH_SHORT).show()
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
