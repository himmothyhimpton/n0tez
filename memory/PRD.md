# n0tez Android App - PRD

## Original Problem Statement
GitHub Actions build failing for the n0tez Android project with Gradle build error.

## Project Overview
n0tez is a transparent notepad Android app with floating widget functionality.

## Architecture
- **Platform:** Android (Kotlin)
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **Build System:** Gradle 8.2 with AGP 8.2.2
- **Dependencies:** AndroidX, Material Design 3, FFmpeg-kit, UCrop

## Core Features Implemented
- Transparent floating notepad widget
- Basic text editing (create, edit, save notes)
- 4-digit PIN security
- Photo/Video/Audio editing capabilities
- Dark/Light theme support
- Auto-save functionality

## What's Been Implemented
- [2026-01-30] Fixed GitHub Actions workflow path issue (removed incorrect `cd n0tez` commands)

## Issues Resolved
1. **GitHub Actions build failure** - Workflow was incorrectly navigating to `n0tez` subdirectory

## Next Action Items
- Push updated workflow to GitHub and verify build passes
- Monitor for any compilation errors in Kotlin source files

## Backlog
- P1: Test APK on physical device
- P2: Play Store submission
- P2: App icon design (1024x1024)
- P3: Screenshot generation for Play Store
