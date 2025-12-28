# n0tez - Transparent Notepad App

A transparent notepad application for Android that allows you to create and manage notes with a floating widget interface. Perfect for copying phone numbers, passwords, or any information from one app to another.

## Key Features

### Floating Transparent Notepad
- **See-through backdrop**: The notepad background is transparent so you can see the content underneath while typing
- **Floating bubble icon**: A small, draggable icon that can be positioned anywhere on your screen
- **Adjustable transparency**: Control how transparent the notepad is (0-100%)
- **Bright, bold text**: Text remains clearly visible against the transparent background

### Note Management
- **Create & Edit Notes**: Full-featured note editor
- **Pin Notes**: Keep important notes visible
- **Save & Organize**: Store notes locally on your device
- **Delete Notes**: Standard deletion option
- **Shred Notes**: Secure deletion that encrypts and overwrites notes multiple times before removal - data cannot be recovered

### Security & Privacy
- **PIN Protection**: Optional 4-digit PIN to protect your notes
- **Encrypted Storage**: PINs are stored using Android's encrypted preferences
- **Secure Shred**: Military-grade secure deletion for sensitive notes
- **No Cloud**: All data stays on your device - no accounts required

## Technical Requirements

- **Minimum Android Version**: Android 8.0 (API 26)
- **Target Android Version**: Android 14 (API 34)
- **Architecture**: MVVM with Kotlin
- **UI**: Material Design 3

## Permissions

- `SYSTEM_ALERT_WINDOW`: For floating widget overlay (appears on top of other apps)
- `FOREGROUND_SERVICE`: For background widget service
- `POST_NOTIFICATIONS`: For Android 13+ notification support

## Building the App

### Using Android Studio (Recommended)
1. Open the `n0tez` folder in Android Studio
2. Wait for Gradle sync to complete
3. Select Build > Build Bundle(s) / APK(s) > Build APK(s)
4. Find the APK at `app/build/outputs/apk/debug/app-debug.apk`

### Using Command Line
```bash
# Make gradlew executable (Unix/Mac)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK (requires signing config)
./gradlew assembleRelease

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Google Play Store Submission

### Ready Assets
- ✅ Material Design 3 UI
- ✅ Proper permission handling
- ✅ Privacy-focused design
- ✅ App icon (adaptive icon)

### Required for Submission
1. **App Icon**: 512x512 PNG for Play Store listing (generate from adaptive icon)
2. **Screenshots**: 8 screenshots showing key features
3. **Privacy Policy**: Required URL for apps with overlay permission
4. **Short Description** (80 chars): "Transparent notepad with floating widget - copy info from any app"
5. **Full Description**: See PLAY_STORE_DESCRIPTIONS.md

## How to Use

1. **Start the Widget**: Tap "Start Floating Widget" from the main screen
2. **Grant Permission**: Allow the app to draw over other apps when prompted
3. **Use the Bubble**: A floating bubble icon appears - drag it anywhere
4. **Open Notepad**: Tap the bubble to open the transparent notepad
5. **Type Your Notes**: The background is see-through so you can copy info from underneath
6. **Adjust Transparency**: Use the slider to make the backdrop more or less transparent
7. **Save & Close**: Tap the close button - notes are auto-saved

## License

This project is licensed under the MIT License.
