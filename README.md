# Project Delta — Android Port

Separate native Android port of the Project Delta endless runner (Kotlin + Jetpack Compose, game rendered on Canvas — no engine dependency).

- Package: `com.emckeon97.projectdelta`
- Min SDK 26, target SDK 34
- Same 6-character public-domain roster and coin prices as iOS; same persistence keys (`delta.coins`, `delta.selected`, `delta.unlocked`, `delta.highscore`)
- Same swipe controls: left/right = lane, up = jump, down = roll
- Ads: banner on menu, interstitial every 3rd game over, rewarded revive — **Google test ad IDs** for now; flip `AdManager.useTestIDs` to `false` and fill `REAL_*` in `ads/AdManager.kt` for production

## Build
Open `project-delta-android/` in Android Studio and run. Requires the Android SDK + Gradle (wrapper not included).
