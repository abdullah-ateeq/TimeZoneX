# TimeZoneX 🌍⏱️

**TimeZoneX** is a modern, offline-first Android application designed for global time tracking, multi-city time comparison, and international meeting/business hour overlap planning. Built completely with **Kotlin** and **Jetpack Compose (Material 3)**, TimeZoneX ensures high precision across all global time zones.

---

## ✨ Features

- 🕒 **Single-Instant Precision Engine**: Driven by `java.time.ZoneId` and `ZonedDateTime`. Automatically handles Daylight Saving Time (DST), half-hour offsets (e.g., India UTC+5:30, Iran UTC+3:30), and 45-minute offsets (e.g., Nepal UTC+5:45).
- 🏙️ **Multi-City Time Comparison**: Compare up to 12 cities side-by-side with digital clocks, date transition badges (`+1 day` / `-1 day`), UTC offsets, and reorderable lists.
- 💼 **Business Hour Overlap Calculator**: Find optimal meeting windows across multiple time zones by calculating overlapping working hours for distributed teams.
- ✈️ **100% Offline & Lightweight**: Built-in IANA city database requiring zero internet access or external web API calls.
- 🔒 **Privacy-First**: No location permissions required, no cloud sync, no tracking, and no ads.
- 🎨 **Material Design 3 & Personalization**:
  - Support for Dark, Light, and System themes
  - Dynamic Color (Material You on Android 12+)
  - 12-hour / 24-hour time format toggle
  - Day & Night status indicators
  - Customizable working hours range
  - Searchable global city directory & Favorites management

---

## 📱 Screenshots & Application Architecture

The app follows modern Android development best practices with **MVVM (Model-View-ViewModel)** architecture:

```
com.example.timezonex
 ├── data           # Repository layer, Local preferences, Built-in City Database
 ├── domain         # Business logic, Time calculations, Overlap engine, DST logic
 ├── ui
 │    ├── components  # Reusable Compose UI components (Time Cards, Bottom Sheets, Timelines)
 │    ├── navigation  # Compose Navigation routes & graph
 │    ├── screens     # HomeScreen, CitiesScreen, SettingsScreen, AboutScreen, SplashScreen
 │    ├── theme       # Material 3 Color palette, Typography, Theme switching
 │    └── viewmodel   # TimeZoneViewModel managing UI state via StateFlow
 └── MainActivity.kt # Single Activity entry point with Navigation & Edge-to-Edge support
```

---

## 🛠️ Tech Stack & Libraries

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: MVVM + Clean Architecture principles
- **Asynchronous Flow**: Kotlin Coroutines & `StateFlow`
- **Time Engine**: Java 8+ `java.time` API (`Instant`, `ZonedDateTime`, `ZoneId`, `ZoneRules`)
- **Navigation**: Jetpack Navigation Compose
- **Persistence**: Android `SharedPreferences` / Preference Store
- **Build System**: Gradle Version Catalogs (`libs.versions.toml`)

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 11 or JDK 17
- Android SDK 26 (Android 8.0) minimum, compiled with SDK 37

### Building from Source

1. **Clone the repository**:
   ```bash
   git clone https://github.com/abdullah-ateeq/TimeZoneX.git
   cd TimeZoneX
   ```

2. **Open in Android Studio**:
   Open Android Studio and select **Open**, then navigate to the cloned `TimeZoneX` folder.

3. **Build & Run**:
   Sync Gradle and run the app on an Android Emulator or physical device running Android 8.0 (API 26) or higher.

Alternatively, build via command line:
```bash
./gradlew assembleDebug
```

---

## 📄 License

```
Copyright 2025 Abdullah Ateeq

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License embodied in the License for the specific language governing
permissions and limitations under the License.
```
