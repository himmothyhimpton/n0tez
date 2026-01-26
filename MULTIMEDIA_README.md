# N0tez Multimedia Feature

## Overview
The multimedia feature adds comprehensive voice recording, audio editing, photo editing, and video editing capabilities to the n0tez app. Users can now create rich multimedia notes with audio recordings, edited images, and processed videos.

## Features

### 🎤 Voice Recorder
- **Record Audio**: High-quality audio recording with real-time timer
- **Pause/Resume**: Pause and resume recording (Android 7.0+)
- **Save/Discard**: Save recordings or discard unwanted ones
- **Format**: M4A format with AAC encoding at 128kbps

### 🎵 Audio Editor
- **Playback Controls**: Play, pause, and stop audio playback
- **Timeline Seeking**: Scrub through audio with seekbar
- **Trim Audio**: Set start and end points to trim audio
- **Volume Control**: Adjust playback volume
- **Save Edited**: Export trimmed audio (requires FFmpeg)

### 🖼️ Photo Editor
- **Image Selection**: Pick images from gallery
- **Rotate**: Rotate images left or right by 90°
- **Flip**: Flip images horizontally or vertically
- **Crop**: Crop images with aspect ratio control (requires UCrop)
- **Quality Control**: Adjust JPEG compression quality (10-100%)
- **Save Edited**: Export edited images

### 🎬 Video Editor
- **Video Selection**: Pick videos from gallery
- **Video Player**: Built-in video player with controls
- **Timeline Seeking**: Scrub through video with seekbar
- **Trim Video**: Set start and end points to trim video
- **Quality Selection**: Choose output quality (Original, 1080p, 720p, 480p)
- **Frame Extraction**: Extract current frame as image
- **Save Edited**: Export trimmed video (requires FFmpeg)

## Architecture

### Data Models

#### MediaAttachment
```kotlin
data class MediaAttachment(
    val id: String,
    val type: MediaType,        // AUDIO, IMAGE, VIDEO
    val filePath: String,
    val thumbnailPath: String?,
    val duration: Long?,        // for audio/video
    val width: Int?,           // for images/videos
    val height: Int?,          // for images/videos
    val fileSize: Long,
    val createdAt: Long,
    var title: String
)
```

#### Updated Note Model
```kotlin
data class Note(
    // ... existing fields ...
    var attachments: MutableList<MediaAttachment> = mutableListOf()
)
```

### File Organization
```
/data/data/com.n0tez.app/files/media/
├── audio/
│   ├── recording_20260126_143022.m4a
│   └── recording_20260126_143022_edited.m4a
├── images/
│   ├── edited_20260126_143045.jpg
│   └── frame_20260126_143100.jpg
└── videos/
    ├── edited_20260126_143130.mp4
    └── edited_20260126_143130_thumb.jpg
```

## User Flow

### Recording Voice Note
1. Open app → Tap "Multimedia"
2. Tap "Voice Recorder"
3. Grant microphone permission if needed
4. Tap "Record" to start recording
5. Tap "Pause" to pause (optional)
6. Tap "Stop" when finished
7. Tap "Save" to keep or "Discard" to delete

### Editing Photo
1. Open app → Tap "Multimedia"
2. Tap "Photo Editor"
3. Select image from gallery
4. Use editing tools (rotate, flip, crop, quality)
5. Tap "Save" to export edited image

### Editing Video
1. Open app → Tap "Multimedia"
2. Tap "Video Editor"
3. Select video from gallery
4. Use trim slider to set start/end points
5. Select output quality
6. Tap "Save" to export edited video

## Permissions Required

### Runtime Permissions
- **RECORD_AUDIO**: For voice recording
- **READ_MEDIA_IMAGES**: For accessing photos (Android 13+)
- **READ_MEDIA_VIDEO**: For accessing videos (Android 13+)
- **READ_MEDIA_AUDIO**: For accessing audio files (Android 13+)
- **CAMERA**: For taking photos/videos (future feature)

### Legacy Permissions
- **READ_EXTERNAL_STORAGE**: For Android 12 and below
- **WRITE_EXTERNAL_STORAGE**: For Android 12 and below

## Technical Requirements

### Minimum SDK
- **minSdk**: 26 (Android 8.0)
- **targetSdk**: 34 (Android 14)

### Dependencies
```gradle
// Media3 for advanced playback
implementation 'androidx.media3:media3-exoplayer:1.2.0'
implementation 'androidx.media3:media3-ui:1.2.0'

// UCrop for image cropping
implementation 'com.github.yalantis:ucrop:2.2.8'

// FFmpeg for audio/video processing
implementation 'com.arthenica:ffmpeg-kit-min:6.0-2'

// Glide for image loading
implementation 'com.github.bumptech.glide:glide:4.16.0'
```

## Implementation Status

### ✅ Completed
- Core data models
- All activity classes
- All layout files
- Permission declarations
- AndroidManifest configuration
- MainActivity integration
- Basic recording functionality
- Basic playback functionality
- Image rotation and flip
- Video frame extraction

### 🚧 In Progress
- FFmpeg integration for audio trimming
- FFmpeg integration for video trimming
- UCrop integration for image cropping
- Waveform visualization
- Media gallery view

### 📋 Planned
- Attach media to notes
- Share media files
- Media compression options
- Advanced filters for images
- Video effects and transitions
- Cloud backup for media

## Performance Considerations

### Memory Management
- Bitmap recycling to prevent memory leaks
- Downsampling large images
- Lazy loading in galleries
- Thumbnail caching

### Storage Management
- Automatic cleanup of temporary files
- File size limits
- Storage usage monitoring
- Compression options

### Battery Optimization
- Efficient encoding settings
- Background processing with WorkManager
- Proper wake lock management

## Security & Privacy

### Data Protection
- All media files stored in app-private directory
- No automatic cloud upload
- User controls deletion
- Encrypted storage option (future)

### Permission Handling
- Runtime permission requests
- Clear permission rationale
- Graceful degradation if denied
- Settings shortcut for re-granting

## Testing

### Unit Tests
- MediaAttachment model tests
- File path generation tests
- Format conversion tests

### Integration Tests
- Activity navigation tests
- Permission flow tests
- File I/O tests

### Manual Testing
- Record audio on various devices
- Edit different image formats
- Process various video codecs
- Test on different Android versions
- Test with low storage scenarios

## Known Limitations

1. **FFmpeg Integration**: Audio and video editing requires FFmpeg library integration
2. **Crop Feature**: Image cropping requires UCrop library integration
3. **Waveform Display**: Real-time waveform visualization not yet implemented
4. **Media Gallery**: Comprehensive gallery view not yet implemented
5. **Note Integration**: Attaching media to notes not yet implemented

## Future Enhancements

### Short Term
- Complete FFmpeg integration
- Add UCrop for cropping
- Implement media gallery
- Integrate with notes system
- Add waveform visualization

### Medium Term
- Camera integration for direct capture
- Advanced audio effects (echo, reverb)
- Image filters and adjustments
- Video effects and transitions
- Batch processing

### Long Term
- AI-powered transcription
- Auto-enhancement for images
- Video stabilization
- Cloud sync and backup
- Collaborative editing

## Contributing

When contributing to the multimedia feature:

1. Follow existing code style
2. Add comments for complex logic
3. Test on multiple Android versions
4. Handle errors gracefully
5. Update documentation
6. Add unit tests where possible

## License

Same as the main n0tez app license.

## Support

For issues or questions:
- Check IMPLEMENTATION_GUIDE.md
- Review Android documentation
- Search Stack Overflow
- Open GitHub issue

---

**Last Updated**: January 26, 2026
**Version**: 1.0.0-beta
**Status**: In Development
