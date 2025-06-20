# Styx 2 Mobile Client

The new mobile client for yet another mediaserver stack.<br>
Truly native on Android.<br>
iOS should technically be working already (besides the player), but I haven't found the time to finish it up.

## Features & Architecture
- Entire UI written in Kotlin using Compose Multiplatform
	<br>With most of the code located in a [common](https://github.com/Vodes/Styx-Common) and a [common-compose](https://github.com/Vodes/Styx-Common-Compose) library
- Material 3 Design
	<br>Loosely following official guidelines
- No constant connection required
	- All data is synced on startup or with a manual refresh[^1]<br>
	- All images are cached locally[^1]<br>
	- Watch progress and favourites are always local-first and synced to the server when a connection is possible

#### Android specific
- Custom video player based on [libmpv](https://mpv.io) (see screenshots)
	<br>Leveraging [these](https://github.com/jarnedemeulemeester/libmpv-android) bindings by the findroid author


## Screenshots / Videos
<img src="https://github.com/user-attachments/assets/51a6f2e2-24bf-4f8d-b6bd-268b1471b3e6" title="about view" height="500"/>

<details>
	<summary><h3>More (Phone)</h3></summary>

  ### Search
  
https://github.com/user-attachments/assets/f7f39b50-9dde-4752-a299-7323d5bf09fb

 ### Detail view & tracking
 
https://github.com/user-attachments/assets/56445f56-bb18-442c-8eb6-5e253c132568

  ### Settings

https://github.com/user-attachments/assets/90cf1bef-f77f-48a6-9335-5d52bc3f8524

  #### Player view
  <img src="https://github.com/user-attachments/assets/08a04653-baab-4b4b-9a00-d393c13b8355" title="player view" height="300" />
</details>

## How do I use this?
<details>
	<summary>Short answer</summary>
	You don't.
</details>

<details>
	<summary>Long answer</summary>
  
There is no public instance for this.<br>
You will have to build every part of the [ecosystem](https://github.com/Vodes?tab=repositories&q=Styx&language=kotlin) yourself and run it on your own server.
</details>


## How to build
### Before running (if on MacOS)
 - check your system with [KDoctor](https://github.com/Kotlin/kdoctor)
 - install JDK 17 on your machine
 - add `local.properties` file to the project root and set a path to Android SDK there

#### Android
To run the application on android device/emulator:  
 - open project in Android Studio and run imported android run configuration

To build the application bundle:
 - run `./gradlew :composeApp:assembleDebug`
 - find `.apk` file in `styx2m/build/outputs/apk/debug/styx2m-debug.apk`

#### iOS
To run the application on iPhone device/simulator:
 - Open `iosApp/iosApp.xcproject` in Xcode and run standard configuration
 - Or use [Kotlin Multiplatform Mobile plugin](https://plugins.jetbrains.com/plugin/14936-kotlin-multiplatform-mobile) for Android Studio

[^1]: ##### I realize this may be infeasible when working with a huge library but this is not a concern for me and I'm building this just for me. With my current library of ~10TB I'm sitting at around 30MB of cached images and ~7MB of other data on the clientside.
