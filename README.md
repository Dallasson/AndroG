# Android Challenge

 a dual-camera Android application that records simultaneously from both the front and back cameras, then merges the two video streams vertically into a single output. Built using modern Android development practices, it handles camera access, video recording, FFmpeg-based merging, and a custom vertical swipe video feed like TikTok.

---

##  Features

-  Simultaneous front and back camera recording using `MediaRecorder`
-  Automatic flipping of the front camera video for natural orientation
-  Merging of two video streams using **FFmpeg** vertically
-  Gallery view to explore recorded videos
-  Feed tab that mimics TikTok-style vertical scrolling using `RecyclerView` and `ExoPlayer`
-  Live data observation via MVVM and Kotlin Coroutines
-  Robust error handling with UI feedback
-  Clean UI with Material Design components

---

##  Technologies Used

- **Kotlin** – Modern language for Android development
- **Camera2 API** – Fine-grained control over camera access and recording
- **MediaRecorder** – Native Android API for recording audio and video
- **FFmpegKit** – Merges and flips videos using FFmpeg commands
- **Retrofit** – For API networking (JellyJelly video API)
- **ExoPlayer** – High-performance video playback engine
- **ViewModel + LiveData** – Lifecycle-aware UI data management
- **RecyclerView** – Efficient feed scrolling experience
- **Hilt (Dagger)** – Dependency injection
- **ViewBinding** – Type-safe view access

---

##  Key Classes and Responsibilities

### `CameraFragment.kt`
- Controls simultaneous video recording from both front and back cameras.
- Uses `Camera2` API to configure each camera and `MediaRecorder` to write the video.
- Disables tab navigation during recording to avoid UI issues.
- On stop, initiates video flipping and merging using FFmpegKit.

### `FeedFragment.kt`
- Parses videos from `https://jellyjelly.com/feed` using Retrofit
- Loads and plays videos using `ExoPlayer` inside a vertical `RecyclerView`.
- Handles swipe gestures and video lifecycle.

### `GalleryFragment.kt`
- Scans local media directory for saved videos.
- Uses `RecyclerView` to display video thumbnails.
- Observes directory changes via `FileObserver`.

### `VideoAdapter.kt`
- Adapter for `RecyclerView` used in both Feed and Gallery.
- Efficient video thumbnail and view count rendering.
- Supports item click events to play videos.

### `JellyViewModel.kt`
- Manages fetching and exposing feed data to the UI.
- Parses video metadata (title, views, thumbnails, URLs) using HTML parsing via Retrofit.
- Uses Kotlin Flows and LiveData.

---

##  How to Run

1. Clone the repo:
   ```bash
   git clone https://github.com/Dallasson/AndroG.git
   cd AndroG

