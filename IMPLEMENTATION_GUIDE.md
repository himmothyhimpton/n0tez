# N0tez Multimedia Implementation Guide

## Overview
This guide provides step-by-step instructions for completing the multimedia feature implementation in the n0tez app.

## What Has Been Implemented

### 1. Core Architecture
- ✅ **MediaAttachment** data model for storing multimedia file information
- ✅ **Note** model updated to support attachments list
- ✅ **MultimediaActivity** as the main hub for all multimedia features
- ✅ **VoiceRecorderActivity** for audio recording with timer and controls
- ✅ **AudioEditorActivity** for audio playback, trimming, and volume control
- ✅ **PhotoEditorActivity** for image editing (rotate, flip, quality adjustment)
- ✅ **VideoEditorActivity** for video playback, trimming, and quality selection

### 2. UI Layouts
- ✅ All activity layouts created with Material Design 3 components
- ✅ Multimedia card added to MainActivity
- ✅ Consistent styling with existing app theme

### 3. Permissions
- ✅ Audio recording permission
- ✅ Camera permission
- ✅ Media access permissions (images, videos, audio)
- ✅ Storage permissions for older Android versions

### 4. AndroidManifest
- ✅ All new activities registered
- ✅ All required permissions declared
- ✅ Hardware features declared as optional

## What Needs to Be Completed

### Phase 1: Add Dependencies
Update `build.gradle` to include the following dependencies:

```gradle
dependencies {
    // ... existing dependencies ...
    
    // Media3 for audio/video playback
    implementation 'androidx.media3:media3-exoplayer:1.2.0'
    implementation 'androidx.media3:media3-ui:1.2.0'
    
    // UCrop for image cropping
    implementation 'com.github.yalantis:ucrop:2.2.8'
    
    // FFmpeg for audio/video processing
    implementation 'com.arthenica:ffmpeg-kit-min:6.0-2'
    
    // Glide for image loading
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
    
    // Material Slider (if not already included)
    implementation 'com.google.android.material:material:1.11.0'
}
```

### Phase 2: Implement Advanced Audio Editing
The AudioEditorActivity has placeholder code for FFmpeg integration. Complete the `saveEditedAudio()` method:

```kotlin
private fun saveEditedAudio() {
    val inputPath = audioFilePath ?: return
    val outputPath = inputPath.replace(".m4a", "_edited.m4a")
    
    val startSeconds = trimStart / 1000.0
    val duration = (trimEnd - trimStart) / 1000.0
    
    // FFmpeg command for trimming
    val command = "-i $inputPath -ss $startSeconds -t $duration -c copy $outputPath"
    
    FFmpegKit.executeAsync(command) { session ->
        val returnCode = session.returnCode
        if (ReturnCode.isSuccess(returnCode)) {
            runOnUiThread {
                Toast.makeText(this, "Audio saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            runOnUiThread {
                Toast.makeText(this, "Failed to save audio", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```

### Phase 3: Implement Image Cropping
The PhotoEditorActivity has placeholder code for UCrop integration. Complete the `cropImage()` method:

```kotlin
private val cropLauncher = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
) { result ->
    if (result.resultCode == RESULT_OK) {
        result.data?.let { data ->
            val resultUri = UCrop.getOutput(data)
            resultUri?.let { uri ->
                loadImage(uri)
            }
        }
    }
}

private fun cropImage() {
    editedBitmap?.let { bitmap ->
        val tempFile = File(cacheDir, "temp_crop.jpg")
        val outputFile = File(cacheDir, "cropped.jpg")
        
        // Save current bitmap to temp file
        FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }
        
        // Launch UCrop
        val options = UCrop.Options().apply {
            setCompressionQuality(quality)
            setToolbarColor(ContextCompat.getColor(this@PhotoEditorActivity, R.color.purple_500))
        }
        
        val intent = UCrop.of(Uri.fromFile(tempFile), Uri.fromFile(outputFile))
            .withOptions(options)
            .getIntent(this)
            
        cropLauncher.launch(intent)
    }
}
```

### Phase 4: Implement Video Editing
The VideoEditorActivity has placeholder code for FFmpeg integration. Complete the `saveEditedVideo()` method:

```kotlin
private fun saveEditedVideo() {
    videoUri?.let { uri ->
        val inputPath = getRealPathFromUri(uri) ?: return
        val videosDir = File(filesDir, "media/videos")
        if (!videosDir.exists()) {
            videosDir.mkdirs()
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val outputFile = File(videosDir, "edited_$timestamp.mp4")
        
        val startSeconds = trimStart / 1000.0
        val duration = (trimEnd - trimStart) / 1000.0
        
        val quality = binding.spinnerQuality.selectedItemPosition
        val scale = when (quality) {
            1 -> "scale=1920:1080" // High
            2 -> "scale=1280:720"  // Medium
            3 -> "scale=854:480"   // Low
            else -> ""             // Original
        }
        
        val scaleFilter = if (scale.isNotEmpty()) "-vf $scale" else ""
        
        // FFmpeg command
        val command = "-i $inputPath -ss $startSeconds -t $duration $scaleFilter -c:v libx264 -c:a aac ${outputFile.absolutePath}"
        
        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                runOnUiThread {
                    Toast.makeText(this, "Video saved successfully", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "Failed to save video", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

// Helper method to get real path from URI
private fun getRealPathFromUri(uri: Uri): String? {
    var path: String? = null
    val projection = arrayOf(MediaStore.Video.Media.DATA)
    val cursor = contentResolver.query(uri, projection, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            path = it.getString(columnIndex)
        }
    }
    return path
}
```

### Phase 5: Integrate with Note System
Update `NoteEditorActivity` to display and manage media attachments:

1. Add RecyclerView to `activity_note_editor.xml` for displaying attachments
2. Create adapter for media attachments
3. Add buttons to attach media from multimedia section
4. Update save/load logic to handle attachments

### Phase 6: Create Media Gallery
Implement a gallery view to display all media files:

1. Create `MediaGalleryActivity`
2. Create layout with RecyclerView for grid display
3. Implement adapter with thumbnails
4. Add filtering by media type
5. Add options to view, edit, or delete media

### Phase 7: Add Waveform Visualization
For better audio recording experience:

1. Add custom WaveformView class
2. Integrate with MediaRecorder to capture amplitude
3. Update UI in real-time during recording

### Phase 8: Testing Checklist

#### Audio Recording
- [ ] Record audio with timer display
- [ ] Pause/resume recording (Android 7.0+)
- [ ] Save recording to file
- [ ] Discard recording
- [ ] Permission handling

#### Audio Editing
- [ ] Load audio file
- [ ] Play/pause/stop playback
- [ ] Seek through audio
- [ ] Trim audio (start/end points)
- [ ] Adjust volume
- [ ] Save edited audio

#### Photo Editing
- [ ] Select image from gallery
- [ ] Rotate left/right
- [ ] Flip horizontal/vertical
- [ ] Crop image
- [ ] Adjust quality
- [ ] Save edited image

#### Video Editing
- [ ] Select video from gallery
- [ ] Play/pause/stop playback
- [ ] Seek through video
- [ ] Trim video (start/end points)
- [ ] Select output quality
- [ ] Extract frame as image
- [ ] Save edited video

#### Integration
- [ ] Navigate from MainActivity to Multimedia
- [ ] Navigate between multimedia activities
- [ ] Back button navigation
- [ ] File storage organization
- [ ] Memory management (bitmap recycling)

### Phase 9: Optimization

#### Performance
- [ ] Implement background processing for media operations
- [ ] Add progress indicators for long operations
- [ ] Implement thumbnail caching
- [ ] Optimize bitmap loading (downsampling)

#### Storage Management
- [ ] Implement file size limits
- [ ] Add cleanup for temporary files
- [ ] Provide storage usage information
- [ ] Implement media compression options

#### Error Handling
- [ ] Handle permission denials gracefully
- [ ] Handle insufficient storage
- [ ] Validate file formats
- [ ] Handle processing failures
- [ ] Add user-friendly error messages

## Building and Testing

### Build the APK
```bash
cd /home/ubuntu/n0tez/n0tez
./gradlew assembleDebug
```

The APK will be generated at:
```
n0tez/app/build/outputs/apk/debug/app-debug.apk
```

### Install on Device
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Common Issues and Solutions

#### Issue: FFmpeg not working
**Solution**: Ensure you're using the correct FFmpeg-kit package and commands are properly formatted.

#### Issue: UCrop crashes
**Solution**: Check that file URIs are properly created and permissions are granted.

#### Issue: Out of memory errors
**Solution**: Implement proper bitmap recycling and use BitmapFactory.Options for downsampling.

#### Issue: Media files not found
**Solution**: Verify file paths and ensure directories are created before saving.

## Next Steps

1. **Add dependencies** to build.gradle
2. **Implement FFmpeg integration** for audio/video editing
3. **Implement UCrop integration** for image cropping
4. **Create media gallery** for viewing all files
5. **Integrate with notes** to attach media to notes
6. **Add waveform visualization** for better UX
7. **Test thoroughly** on different Android versions
8. **Optimize performance** and memory usage
9. **Add analytics** to track feature usage
10. **Prepare for release** with proper documentation

## Resources

- [FFmpeg-kit Documentation](https://github.com/arthenica/ffmpeg-kit)
- [UCrop Documentation](https://github.com/Yalantis/uCrop)
- [Android Media APIs](https://developer.android.com/guide/topics/media)
- [Material Design 3](https://m3.material.io/)

## Support

For questions or issues during implementation, refer to:
- Android Developer Documentation
- Stack Overflow
- GitHub Issues for specific libraries
