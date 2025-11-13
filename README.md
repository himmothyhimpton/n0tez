# n0tez - Transparent Notepad App

A transparent notepad application for Android that allows you to create and manage notes with a floating widget interface.

## Features

- **Transparent UI**: Fully customizable transparency levels
- **Floating Widget**: Positionable floating widget that overlays other apps
- **Text Editing**: Create, edit, and save notes
- **Copy/Paste**: Optimized copy/paste functionality
- **PIN Security**: Optional 4-digit PIN protection
- **Material Design 3**: Modern Android UI design
- **Dark/Light Theme**: Support for both themes
- **Cloud Backup**: Optional cloud backup functionality
- **Crash Reporting**: Firebase Crashlytics integration

## Technical Requirements

- **Minimum API Level**: 33 (Android 13)
- **Target API Level**: 34 (Android 14)
- **Architecture**: MVVM with Kotlin
- **Dependencies**: Material Design 3, Firebase, Room Database

## Permissions

- `SYSTEM_ALERT_WINDOW`: For floating widget functionality
- `FOREGROUND_SERVICE`: For background widget service
- `WRITE_EXTERNAL_STORAGE`: For note storage
- `INTERNET`: For Firebase services
- `POST_NOTIFICATIONS`: For Android 13+ notifications

## Building the App

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle files
4. Build and run on device/emulator

## Google Play Store Submission

The app includes all necessary components for Google Play Store submission:

- High-quality app icon (1024x1024)
- Screenshots for Play Store listing
- Proper permissions declaration
- Firebase Crashlytics integration
- Material Design 3 compliance

## License

This project is licensed under the MIT License.