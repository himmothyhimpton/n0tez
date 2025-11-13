# n0tez Android App Build Instructions

## Prerequisites

1. **Android Studio** (Recommended) or Android SDK
2. **Java JDK 17** or higher
3. **Android SDK** with API level 33+

## Build Methods

### Method 1: Using Android Studio (Recommended)

1. **Open Android Studio**
2. **Open the project** by selecting the `n0tez` folder
3. **Wait for Gradle sync** to complete
4. **Build APK**:
   - Go to `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - The APK will be generated in `app/build/outputs/apk/debug/`

### Method 2: Command Line Build (Advanced)

If you have Android SDK installed:

```bash
# Set Android SDK path
export ANDROID_HOME=/path/to/android-sdk

# Make gradlew executable (Linux/Mac)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

### Method 3: Using Gradle Wrapper (Windows)

```cmd
# Build debug APK
gradlew.bat assembleDebug

# Build release APK  
gradlew.bat assembleRelease
```

## Build Output

After successful build, the APK will be located at:
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`

## Installation

### Install on Device/Emulator

1. **Enable Developer Options** on your Android device
2. **Enable USB Debugging**
3. **Connect device** via USB
4. **Install APK**:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Install via Android Studio

1. **Connect device** or start emulator
2. **Click Run button** in Android Studio
3. **Select device** from the deployment dialog

## Build Configuration

### Debug Build
- Includes debugging symbols
- Enables logging
- Uses debug signing key
- Firebase Crashlytics enabled

### Release Build
- Optimized with ProGuard
- Minified code
- Requires signing configuration
- Firebase Crashlytics enabled

## Troubleshooting

### Common Issues:

1. **Gradle Sync Failed**:
   - Check internet connection
   - Update Android Studio
   - Clear Gradle cache: `File` → `Invalidate Caches and Restart`

2. **Build Failed - Missing Dependencies**:
   - Ensure all dependencies are properly declared in `build.gradle`
   - Check that repositories are accessible

3. **Permission Issues**:
   - Grant necessary permissions on device
   - Check AndroidManifest.xml permissions

4. **Firebase Configuration**:
   - Update `google-services.json` with your Firebase project
   - Enable Firebase services in Firebase Console

### Required Permissions:
- `SYSTEM_ALERT_WINDOW` - For floating widget
- `FOREGROUND_SERVICE` - For background service
- `WRITE_EXTERNAL_STORAGE` - For note storage
- `INTERNET` - For Firebase services

## Testing

### Manual Testing Checklist:
- [ ] App launches successfully
- [ ] Floating widget appears and functions
- [ ] Transparency controls work
- [ ] Notes can be created, edited, saved
- [ ] PIN security works (if enabled)
- [ ] Copy/paste functionality
- [ ] Settings screen functions
- [ ] Dark/light theme switching
- [ ] Widget positioning and sizing
- [ ] Auto-save functionality

### Device Compatibility:
- Test on Android 13+ devices
- Test different screen sizes
- Test various screen densities
- Test on tablets and phones

## Google Play Store Preparation

### Before Publishing:
1. **Update app version** in `app/build.gradle`
2. **Create release build** with proper signing
3. **Generate app bundle** (AAB format recommended)
4. **Prepare store listing** with descriptions and screenshots
5. **Test on multiple devices**
6. **Ensure policy compliance**

### Required Assets:
- App icon (1024x1024 PNG)
- Feature graphic (1024x500 PNG)
- Screenshots (minimum 2, recommended 8)
- App description and feature list
- Privacy policy URL

The app is now ready for building and testing!