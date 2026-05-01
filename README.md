---
AIGC:
    ContentProducer: Minimax Agent AI
    ContentPropagator: Minimax Agent AI
    Label: AIGC
    ProduceID: "00000000000000000000000000000000"
    PropagateID: "00000000000000000000000000000000"
    ReservedCode1: 3045022100b90fa29a51196361bf208b541d82f9c6043783f1518055cb73fd12a442c4f3730220124d5331c9f3f9feda1edeea6abe552873037421f3f07d3b6c0e02f6f7dbc644
    ReservedCode2: 3045022046638ae779b8a06f3d82b6f23a42b566f4b0f56312415f3e1a8cf319463e3b22022100c17160583f87bcd692a894694b42f9ab97c8c182c18dbcf1549da5c2bfaeb857
---

# GitHub Explorer,文件和预览3:7比列显示

A native Android application for browsing GitHub repositories and managing GitHub Actions workflows.

## Features

- **GitHub Token Authentication**: Secure login using Personal Access Token
- **Repository Browser**: View and search your GitHub repositories
- **File Tree Navigation**: Browse repository files with tree structure
- **File Content Preview**: View file contents directly in the app
- **GitHub Actions Management**:
  - View workflow list
  - Trigger workflows
  - View workflow runs with status filtering
  - View job logs
  - Download artifacts

## Tech Stack

- **Language**: Kotlin
- **UI**: Material Design Components, ViewPager2
- **Architecture**: ViewModel + LiveData
- **Networking**: Retrofit + OkHttp
- **Async**: Kotlin Coroutines
- **Data**: Gson for JSON parsing

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or newer
- Android SDK 34
- Kotlin 1.9.20

### Building

1. Open the project in Android Studio
2. Sync Gradle files
3. Build the project using Build > Make Project

Or use the command line:

```bash
./gradlew assembleDebug
```

### Installation

Transfer the APK from `app/build/outputs/apk/debug/` to your Android device and install it.

## Usage

1. Launch the app
2. Enter your GitHub Personal Access Token (with repo and workflow scopes)
3. Browse your repositories or search for public repositories
4. Tap on a repository to view details
5. Use the "Files" tab to browse repository contents
6. Use the "Actions" tab to manage GitHub Actions workflows

## Permissions

- `INTERNET`: Required for API calls to GitHub
- `WRITE_EXTERNAL_STORAGE`: For downloading artifacts (Android 8 and below)

## License

MIT License
