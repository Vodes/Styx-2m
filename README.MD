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


## Screenshots
<details>
	<summary>Click here</summary>

  #### Search
  ![search](https://i.ibb.co/wCzf1qj/Screenshot-20240325-235619.webp)

  #### Detail view
  ![detail view](https://i.ibb.co/CBTJMDp/Screenshot-20240325-235453.webp)

  #### General settings
  ![settings](https://i.ibb.co/kJcLwWv/Screenshot-20240325-235756.webp)

  #### Player settings
  ![player/mpv settings](https://i.ibb.co/rFCyDF3/Screenshot-20240325-235808.webp)

  #### Player view
  ![player view](https://i.ibb.co/w77YMD7/Screenshot-20240325-235545.webp)
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
