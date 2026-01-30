# FaceShot-BuildingBlock - Product Requirements Document

## Original Problem Statement
The notepad doesn't open or close to the moveable widget. When you attempt to open the widget to the notepad the app will crash or hang or the widget just locks up, but is still able to be moved but it does nothing and is basically useless. The app should also be renamed to "FaceShot-BuildingBlock" and is now intended to become a tool for the FaceShot-ChopShop website. The app icon, color and logo should all follow the same style of the website. This APK will be submitted to Google Play for production so everything needs to be 100% functional and production ready, no placeholders or mock actions should remain.

## User Personas
1. **FaceShot-ChopShop Users** - Creators who use AI tools and need to copy prompts, URLs, IDs between apps
2. **General Power Users** - Anyone needing to copy information from one app to another using overlay notepad
3. **Privacy-Conscious Users** - Users who want local-only note storage with secure deletion

## Core Requirements (Static)
- ✅ Floating widget with transparent notepad overlay
- ✅ Tap bubble to open/close notepad (must not crash)
- ✅ Adjustable transparency (0-100%)
- ✅ Draggable bubble and notepad positioning
- ✅ Note creation, editing, saving
- ✅ PIN protection with encrypted storage
- ✅ Secure shred (military-grade deletion)
- ✅ FaceShot branding (purple/pink gradient theme)
- ✅ Production-ready (no placeholders/mocks)

## What's Been Implemented (Jan 30, 2025)

### Bug Fixes
- Fixed FloatingWidgetService state management causing crashes/lock-ups
- Added AtomicBoolean for thread-safe notepad toggle
- Added isTransitioning flag to prevent rapid toggle spam
- Added comprehensive try-catch error handling
- Fixed view cleanup and memory leak prevention
- Changed closeNotepad to use safe cleanup methods

### Rebranding (n0tez → FaceShot-BuildingBlock)
- App name: "FaceShot-BuildingBlock"
- Application ID: com.faceshot.buildingblock
- Color scheme: Purple (#6B35B8) to Pink (#EE4B8B) gradient
- Updated all drawables (bubble, notepad, icons)
- Updated strings.xml, colors.xml, themes
- Updated notification text and permission dialogs
- New 3D building block app icon with FaceShot colors

### Files Modified
- FloatingWidgetService.kt - Bug fixes + branding
- N0tezApplication.kt - Channel name update
- MainActivity.kt - Dialog text update
- NoteRepository.kt - Prefs name update
- PhotoEditorActivity.kt - UCrop colors update
- colors.xml - Full FaceShot color palette
- strings.xml - App name change
- All drawable/*.xml files - FaceShot gradient theme
- activity_main.xml - Toolbar title + accent color
- floating_notepad.xml - Header text + seekbar colors
- build.gradle - Application ID change
- README.md - Full documentation update
- PLAY_STORE_DESCRIPTIONS.md - Store listing content
- PROJECT_STATUS.md - Status update
- app_icon.svg - New FaceShot-branded icon

## Prioritized Backlog

### P0 (Critical) - DONE
- [x] Fix notepad open/close crash
- [x] Rebrand to FaceShot-BuildingBlock
- [x] Update color scheme
- [x] Update app icon

### P1 (Important) - User to complete
- [ ] Create signed release APK
- [ ] Generate app screenshots
- [ ] Add privacy policy URL
- [ ] Submit to Google Play Console

### P2 (Nice to have)
- [ ] Dark mode toggle in settings
- [ ] Widget size options
- [ ] Export notes feature
- [ ] Import/backup restore

## Technical Architecture
- **Platform**: Android (API 26-34)
- **Language**: Kotlin
- **UI Framework**: Material Design 3
- **Architecture**: MVVM pattern
- **Storage**: SharedPreferences + EncryptedSharedPreferences
- **Key Service**: FloatingWidgetService (foreground service with overlay)

## Next Tasks
1. Build release APK in Android Studio
2. Test on physical device
3. Create 8 screenshots for store listing
4. Host privacy policy and get URL
5. Upload to Google Play Console
6. Set up app signing
7. Submit for review
