# Looma - Privacy-Focused Android Dashboard

## Project Overview

Looma is a privacy-first Android application that gives users transparency into their app permissions and usage patterns. All data processing happens on-device - nothing is ever transmitted externally.

**Current Status**: Early development/prototype phase
**Target Platform**: Android 8.0+ (API level 26+)

## Core Features

1. **Permission Scanner**
   - Scans all installed apps for dangerous permissions
   - Categories: Camera, Microphone, Location, Contacts, Calendar, SMS, Storage, etc.
   - Real-time permission status display

2. **Usage Tracking**
   - 24-hour app screen time breakdown
   - Powered by Android's UsageStatsManager API

3. **Privacy Score**
   - Algorithm based on permission usage patterns
   - Simple, user-friendly scoring system

4. **On-Device Processing**
   - Zero network requests for user data
   - No analytics or tracking
   - Complete data privacy

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Design System**: Material Design 3
- **Key APIs**:
  - `PackageManager` - for app and permission queries
  - `UsageStatsManager` - for usage statistics
- **Build System**: Gradle

## Project Structure

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/looma/
│   │   │   ├── ui/           # Compose UI components
│   │   │   ├── data/         # Data models and repositories
│   │   │   ├── permissions/  # Permission scanning logic
│   │   │   ├── usage/        # Usage stats logic
│   │   │   └── score/        # Privacy score calculation
│   │   ├── res/              # Resources (layouts, strings, etc.)
│   │   └── AndroidManifest.xml
│   └── test/                 # Unit tests
│   └── androidTest/          # Instrumentation tests
├── build.gradle.kts
└── proguard-rules.pro
```

## Development Setup

### Prerequisites

1. **Android Studio**: Latest stable version (Hedgehog or later recommended)
2. **JDK**: JDK 17 or later
3. **Android SDK**: API 26+ (with latest build tools)
4. **Kotlin**: 1.9.0+

### Build & Run

```bash
# Build the project
./gradlew build

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Build APK
./gradlew assembleDebug

# Build release APK (signed)
./gradlew assembleRelease

# Install on device
./gradlew installDebug
```

### Testing on Device/Emulator

1. The app requires "Usage Access" permission to function
2. After installation, navigate to: Settings → Apps → Special app access → Usage access
3. Enable permission for Looma

## Coding Conventions

### Kotlin Style
- Follow official [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful variable names (avoid single-letter names except in lambdas)
- Prefer immutability (`val` over `var`)
- Use data classes for models
- Leverage Kotlin's null safety features

### Compose UI
- Component files should be named with `Screen` or `Component` suffix
- Keep composables small and focused
- Extract reusable UI elements into separate composables
- Use preview annotations for development: `@Preview`
- Follow Material 3 design guidelines

### Architecture
- **Pattern**: MVVM (Model-View-ViewModel) recommended
- **State Management**: Compose State / ViewModel LiveData
- **Dependency Injection**: Consider Hilt/Dagger if project grows
- **Repository Pattern**: Use for data access layer

### File Organization
- One class per file (except small related classes/sealed classes)
- Group related functionality in packages
- Keep `MainActivity` minimal - delegate to composables

## Privacy & Security Guidelines

**Critical**: This app's core value proposition is privacy. All code changes must respect:

1. **No Network Requests** for user data (app list, permissions, usage stats)
2. **No Analytics Libraries** that transmit data
3. **Minimal Permissions** - only request what's absolutely necessary
4. **No Data Storage** in cloud/external services
5. **Transparent Behavior** - user should understand what data is accessed and why

### Acceptable Network Use
- Checking for app updates (if implemented)
- Documentation/help links

### Testing Privacy Compliance
Before committing features that access sensitive data:
- Document what data is accessed and why
- Verify no network transmission occurs
- Add comments explaining privacy implications

## Common Tasks

### Adding a New Permission Category
1. Update permission list in `PermissionScanner`
2. Add UI representation in permission list composable
3. Update privacy score algorithm if needed
4. Test with apps that have/don't have the permission

### Modifying Privacy Score Algorithm
1. Document the scoring logic changes
2. Add unit tests for edge cases
3. Ensure score remains user-friendly (avoid complex calculations)
4. Update any UI that displays score explanations

### Adding Usage Analytics Features
1. Ensure all processing uses `UsageStatsManager` API
2. No data leaves the device
3. Test with various usage patterns
4. Handle edge cases (no permission granted, no data available)

## Known Issues & Gotchas

- **UsageStatsManager**: Requires special permission that can't be granted via normal runtime permissions
- **Permission Changes**: Apps can revoke permissions at any time - always check current state
- **Android Versions**: Permission system changed significantly in Android 6.0, 10, 11, and 12 - test across versions
- **Background Restrictions**: Modern Android limits background app behavior - design accordingly

## Roadmap

Current planned features (see README for full list):
- [ ] Improved privacy score algorithm
- [ ] More permission categories
- [ ] Historical tracking (permission/usage over time)
- [ ] Data export (CSV/JSON)
- [ ] Network traffic monitoring (future consideration)

## Release Process

1. Update version in `build.gradle`
2. Run full test suite
3. Build signed release APK
4. Test APK on physical device
5. Create GitHub release with APK attached
6. Update README download link

## Resources

- [Android Permission Documentation](https://developer.android.com/guide/topics/permissions/overview)
- [UsageStatsManager API](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)

## Support & Contributions

- **Issues**: File on GitHub Issues
- **Maintainer**: u/-Pluko- on Reddit
- **License**: MIT (see LICENSE file)

---

**Remember**: Every feature should ask "Does this respect user privacy?" before implementation.
