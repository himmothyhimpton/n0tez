# Building n0tez for Google Play Store

## Prerequisites

1. **Android Studio** (latest version recommended)
2. **JDK 17** or higher
3. **Android SDK** with API 34 installed

## Building Debug APK

### Using Android Studio:
1. Open the `n0tez` folder in Android Studio
2. Wait for Gradle sync to complete
3. Select **Build > Build Bundle(s) / APK(s) > Build APK(s)**
4. APK will be at: `app/build/outputs/apk/debug/app-debug.apk`

### Using Command Line:
```bash
cd n0tez

# Make gradlew executable (Mac/Linux)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

## Building Release APK (For Play Store)

### Step 1: Create Signing Key
```bash
keytool -genkey -v -keystore n0tez-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias n0tez
```
Save this key file safely - you'll need it for all future updates!

### Step 2: Configure Signing in build.gradle

Add to `app/build.gradle` inside `android { }` block:

```groovy
signingConfigs {
    release {
        storeFile file("path/to/n0tez-release-key.jks")
        storePassword "your-store-password"
        keyAlias "n0tez"
        keyPassword "your-key-password"
    }
}

buildTypes {
    release {
        signingConfig signingConfigs.release
        minifyEnabled true
        shrinkResources true
        proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    }
}
```

### Step 3: Build Release APK
```bash
./gradlew assembleRelease
```

APK will be at: `app/build/outputs/apk/release/app-release.apk`

### Alternative: Build AAB (Recommended for Play Store)
```bash
./gradlew bundleRelease
```

AAB will be at: `app/build/outputs/bundle/release/app-release.aab`

## Google Play Store Checklist

### Required Assets
- [ ] **App Icon**: 512x512 PNG (you can export from Android Studio)
- [ ] **Feature Graphic**: 1024x500 PNG
- [ ] **Screenshots**: Min 2, up to 8 (phone: 1080x1920 or similar)
- [ ] **Short Description**: 80 characters max
- [ ] **Full Description**: 4000 characters max
- [ ] **Privacy Policy URL**: Required for overlay permission

### Content Rating
Complete the content rating questionnaire in Play Console.
This app should qualify for "Everyone" rating.

### App Signing
Google Play will manage your app signing for new apps.
Upload your signing key or let Google generate one.

### Testing
1. Upload to Internal Testing track first
2. Test on multiple devices
3. Verify overlay permission flow works
4. Test all features
5. Promote to Production when ready

## Generating App Icon

To generate app icon in various sizes:

1. In Android Studio, right-click `res` folder
2. Select **New > Image Asset**
3. Choose **Launcher Icons (Adaptive and Legacy)**
4. Use the vector drawable or create a custom image
5. Click Next and Finish

## Testing the APK

```bash
# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# Install release version
adb install app/build/outputs/apk/release/app-release.apk
```

## Troubleshooting

**Build fails with Java version error:**
- Ensure JDK 17 is installed
- Set `JAVA_HOME` environment variable

**Overlay permission not working:**
- Ensure `SYSTEM_ALERT_WINDOW` is in manifest
- Test on Android 8.0+ device

**APK too large:**
- Enable ProGuard (already configured)
- Remove unused resources
