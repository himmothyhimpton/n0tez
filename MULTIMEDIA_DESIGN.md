# N0tez Multimedia Feature Design

## Overview
This document outlines the architecture and implementation plan for adding multimedia capabilities to the n0tez app, including voice recording/editing and photo/video editing features.

## Architecture

### 1. Data Model Extensions

#### MultimediaNote Extension
Extend the existing `Note` data class to support multimedia attachments:
```kotlin
data class Note(
    // ... existing fields ...
    var attachments: MutableList<MediaAttachment> = mutableListOf()
)

data class MediaAttachment(
    val id: String = UUID.randomUUID().toString(),
    val type: MediaType,
    val filePath: String,
    val thumbnailPath: String? = null,
    val duration: Long? = null, // for audio/video in milliseconds
    val width: Int? = null, // for images/videos
    val height: Int? = null, // for images/videos
    val createdAt: Long = System.currentTimeMillis()
)

enum class MediaType {
    AUDIO, IMAGE, VIDEO
}
```

### 2. New Activities

#### MultimediaActivity
Main hub for multimedia features with cards for:
- Voice Recorder
- Photo Editor
- Video Editor
- Multimedia Gallery

#### VoiceRecorderActivity
Features:
- Record audio with waveform visualization
- Play/pause/stop controls
- Trim audio (start/end points)
- Adjust volume
- Save to note or standalone

#### AudioEditorActivity
Features:
- Waveform display
- Trim controls (drag handles)
- Volume adjustment slider
- Speed control (0.5x - 2x)
- Save/export options

#### PhotoEditorActivity
Features:
- Image display with zoom/pan
- Crop tool with aspect ratio presets
- Quality adjustment (compression)
- Rotation and flip
- Basic filters (brightness, contrast, saturation)
- Save/export options

#### VideoEditorActivity
Features:
- Video player with timeline
- Trim controls (start/end markers)
- Quality adjustment (resolution/bitrate)
- Frame extraction
- Duration display
- Save/export options

### 3. File Storage Structure

```
/data/data/com.n0tez.app/files/
├── notes/
│   └── {note_id}.json
├── media/
│   ├── audio/
│   │   ├── {audio_id}.m4a
│   │   └── {audio_id}_edited.m4a
│   ├── images/
│   │   ├── {image_id}.jpg
│   │   ├── {image_id}_thumb.jpg
│   │   └── {image_id}_edited.jpg
│   └── videos/
│       ├── {video_id}.mp4
│       ├── {video_id}_thumb.jpg
│       └── {video_id}_edited.mp4
```

### 4. Required Permissions

Add to AndroidManifest.xml:
```xml
<!-- Audio Recording -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Camera and Media -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
<uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

<!-- For Android 12 and below -->
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" 
    android:maxSdkVersion="32" />

<!-- Camera feature -->
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.microphone" android:required="false" />
```

### 5. Dependencies to Add

```gradle
// Media recording and playback
implementation 'androidx.media3:media3-exoplayer:1.2.0'
implementation 'androidx.media3:media3-ui:1.2.0'

// Image processing
implementation 'com.github.yalantis:ucrop:2.2.8'

// Video processing
implementation 'com.arthenica:ffmpeg-kit-min:6.0-2'

// Audio visualization
implementation 'com.github.lincollincol:amplituda:2.2.2'

// Camera
implementation 'androidx.camera:camera-camera2:1.3.1'
implementation 'androidx.camera:camera-lifecycle:1.3.1'
implementation 'androidx.camera:camera-view:1.3.1'

// Glide for image loading
implementation 'com.github.bumptech.glide:glide:4.16.0'
```

## UI/UX Design

### Main Activity Update
Add new card between "My Notes" and "Settings":
```xml
<MaterialCardView id="cardMultimedia">
    <Icon: ic_multimedia>
    <Title: "Multimedia">
    <Description: "Record voice notes, edit photos and videos">
</MaterialCardView>
```

### MultimediaActivity Layout
Grid layout with 4 cards:
1. **Voice Recorder** - Microphone icon
2. **Photo Editor** - Image icon
3. **Video Editor** - Video icon
4. **Media Gallery** - Gallery icon

### Common UI Elements
- Material Design 3 components
- Consistent color scheme with existing app
- Bottom sheets for options
- Snackbars for feedback
- Progress indicators for processing

## Implementation Phases

### Phase 1: Voice Recording
1. Create VoiceRecorderActivity
2. Implement MediaRecorder wrapper
3. Add waveform visualization
4. Implement basic audio editing (trim, volume)
5. Integrate with Note model

### Phase 2: Photo Editing
1. Create PhotoEditorActivity
2. Implement image picker
3. Add crop functionality
4. Add quality adjustment
5. Implement rotation/flip
6. Add basic filters

### Phase 3: Video Editing
1. Create VideoEditorActivity
2. Implement video picker
3. Add video player with timeline
4. Implement trim functionality
5. Add quality adjustment
6. Add frame extraction

### Phase 4: Integration
1. Update Note model with attachments
2. Create MediaAttachment display in NoteEditor
3. Add multimedia gallery view
4. Implement file management
5. Add sharing capabilities

## Technical Considerations

### Performance
- Use background threads for media processing
- Implement progress indicators
- Cache thumbnails
- Lazy load media in lists

### Storage Management
- Implement file size limits
- Add cleanup for orphaned files
- Compress media appropriately
- Provide storage usage info

### Error Handling
- Permission denied scenarios
- Insufficient storage
- Unsupported formats
- Processing failures

### Security
- Validate file types
- Sanitize file names
- Implement secure file storage
- Respect privacy settings
