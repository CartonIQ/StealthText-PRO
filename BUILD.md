# StealthText v1.0.0 - Build notes

Package: com.cartoniq.stealthtext | minSdk 29 | target/compile 35 | Kotlin 1.9.24 | AGP 8.5.2 | Compose BOM 2024.06.00

## Build in Termux
```
unzip StealthText.zip && cd StealthText
~/gradle-8.9/bin/gradle assembleDebug
```
APK: app/build/outputs/apk/debug/app-debug.apk (use this to test first).

## Release (signed with YOUR keystore)
```
keytool -genkeypair -v -keystore ~/stealthtext.jks -alias stealth -keyalg RSA -keysize 2048 -validity 10000
mkdir -p /storage/emulated/0/Download/My_Keystores && cp ~/stealthtext.jks /storage/emulated/0/Download/My_Keystores/
export KS_FILE=$HOME/stealthtext.jks KS_PASS='yourpass' KS_ALIAS=stealth
~/gradle-8.9/bin/gradle assembleRelease    # APK
~/gradle-8.9/bin/gradle bundleRelease      # AAB for Play (refuses without keystore)
```
Use the same password for store and key. Without the env vars, assembleRelease gives an UNSIGNED apk (cannot install).

## How the hiding works
- Photo pixels have R,G,B bytes. The lowest 1-3 bits of a byte change the colour almost invisibly.
- Your text is encrypted first: PBKDF2-SHA256 (200,000 rounds) -> AES-256-GCM, random salt and IV each time.
- Encrypted bytes are written into the low bits in a scattered order that depends on your password.
- A 21-byte header (salt + masked bit depth and length) has no readable signature.
- Output is always PNG (lossless) with EXIF removed. JPEG or app compression destroys the data.

## Features in this version (v1.0.0)
- Hide and reveal: text, PDF/TXT files, voice notes (MediaRecorder, raw bytes, compressed), watermark tag.
- 1-3 bit slider, capacity meter, laser scan line, PNG only, EXIF stripped.
- Decoy password (second fake message in the same photo), Split Secret (XOR, two images).
- App PIN, fake crash after 3 wrong PINs, self-destruct after N wrong PINs (3-10, default 5).
- Biometric private vault (BiometricPrompt), shake to lock, on-screen LOCK button, calculator disguise icon.
- FLAG_SECURE on the whole app, clipboard clears after 30 s, no INTERNET permission.
- NEW: Invisible Text (message hidden in normal text with invisible characters), Smart hiding (writes only in busy image areas, photos up to 8 MP), Key file + password (all hide/reveal screens), Shamir split (any K of 3-5 images).
- NEW: Languages English, Hindi, Urdu (RTL), Arabic (RTL), Bengali. Picker in Settings > Language. Translations are machine-quality; please have a native speaker review them.
- Permissions: RECORD_AUDIO (voice notes) and USE_BIOMETRIC (vault). Photos use the system photo picker, no storage permission.

## Not included
Burn after reading, Scan Image, dark mode, R8 shrinking (off for first build).
Untranslated (stay English): the long Tip and About paragraphs, status lines with numbers, the Voice timer, and legal text.

## Known limits
- Never test by sending through WhatsApp as a photo. Send as Document.
- Photos over 25 megapixels are refused (memory).
- Light theme only.
- Smart hiding capacity depends on the photo; the meter shows an estimate and encoding tells you if it does not fit. Smart images must not be edited or resized.
- Key file: lose the file and the secret is gone. Use the exact same file bytes when revealing.
- Invisible Text: some apps strip invisible characters. Test with the same app first.
- Shake and the LOCK button work only while the app is open. Without a PIN they close the app.
- Disguise needs a PIN. Some launchers take a few seconds to change the icon.
- Self-destruct wipes app data (vault, PIN, settings). PNGs already in the gallery are not deleted.
- Settings has About, Privacy Policy, Contact, Delete My Data. Terms, Licenses, Rate, Share, Update and Bug Report from your old list are not added.
- R8 shrinking is off for the first build; turn on isMinifyEnabled after it works.

## Icon and Settings (added)
- Play Store icon: store/play_icon_512.png (512 px). Launcher: adaptive vector icon with a monochrome layer, plus PNG icons for mdpi to xxxhdpi.
- Settings has the 11 items: About (version from build.gradle), Privacy Policy, Terms and Conditions, Licenses and Copyright (Apache 2.0 notice for open-source parts), Data collection information, Delete My Data, Contact Support, Rate, Share, Check for Updates, Report a Bug (email with device info).
- No AdMob, no UMP, no INTERNET permission.
- Terms text is a draft: have it reviewed before publishing.

## Pre-flight check done
Sources were run through kotlinc 1.9.24 for syntax and structure only (Android and Compose libraries were not available, so full type checking was not possible). One real bug was found and fixed (PasswordField parameter order). The real build in Termux is the final test.

## Round 3 additions
- Duress PIN (Settings, needs an app PIN): entering it on the lock screen or in the calculator silently wipes the vault and temporary files, then opens the app empty. PIN and settings stay.
- Burn after reading (switch on hide screens): the receiving app opens it once, then refuses it. A vault copy is deleted after the first open. It works only inside this app on that phone; copies on other phones or after a reinstall can still open. Not a guarantee.
- Scan Image (Tools): finds StealthText images without a password and runs a statistical LSB check. Estimate only.
- Detectability (after hiding): Low / Medium / High estimate with a tip when High.
- Generate a cover photo: makes a film-grain style cover sized to fit your secret, good for Smart hiding.

## Languages (15)
English, Hindi, Urdu (RTL), Arabic (RTL), Bengali (full UI), plus Spanish, French, Indonesian, Turkish, Portuguese, Russian, Persian (RTL), Tamil, Telugu, Marathi (core UI: navigation, buttons, onboarding, Settings items, common messages). Anything not translated falls back to English. All translations are machine-quality: please have native speakers review them before release. Play Store listing text is not translated yet.

# VERSION 2.0.0 (versionCode 2): Samurai Glass

## New look
Samurai Glass theme: night-sky gradient (indigo, plum, wine; no black), vermilion sun, ink-wash mountains, sakura petals, frosted-glass cards (translucent gradients, since Compose has no true background blur), kanji icons (隠 開 巻 声 and more), hanko-style 守 LOCK button, katana-line dividers, floating glass bottom bar (家 蔵 具 設). Dark theme only; system bars use light icons.

## New features
1. Four Gates: up to 4 passwords on one photo, each opens a different message. The photo always holds 4 slots in this mode; unused slots are random filler, so nobody can tell how many are real. Capacity per message is a quarter of the photo. Photos in single mode are unchanged. Note: single-gate and Four-Gates photos can be told apart by a skilled analyst; what is hidden is how many gates are real.
2. Hanko Signature: sign any hidden item with a key kept in the Android Keystore (ECDSA P-256). Receivers see "Verified" if the signer is you or a saved contact. Manage your public key and contacts in Tools, Hanko. A signature adds about 70 bytes. Delete My Data also deletes the Hanko key.
3. Storm Mode (Beta): hides a SHORT encrypted text (about 70 to 120 characters, depends on the photo) in the average brightness of 16x16 blocks, repeated and majority-voted. Built to survive JPEG compression. The app scales the photo to 1280 px max and runs a JPEG 70 and JPEG 50 self-test before you send it. Resizing afterwards (Instagram) breaks it. Faint blocks may show on flat areas; use busy photos.

## Honest status
Not compiled or run on a phone. The Kotlin compiler check covered syntax and structure only. Storm Mode in particular needs real testing: try several photos and send through the apps you use. Hanko needs two phones to test properly (export key on one, add as contact on the other).

## Test list (V2 extras)
- Four Gates: hide with 3 gates, open with each password, try a wrong password.
- Hanko: sign, reveal on same phone (Verified: signed by you), add the public key on another phone and reveal there.
- Storm: hide, check the self-test lines, send as a photo through WhatsApp, reveal on the other phone. Also try the same via Telegram.
- Check every screen in the new look, with large and small fonts, and in Urdu or Arabic (right-to-left).


# FINAL 2.1.0 (versionCode 3)

## Fixes and additions
1. **Card text clipping fixed.** Home and Tools cards no longer have a fixed height. They use a minimum height and grow to fit the text; the two cards in a Home row stay equal height (IntrinsicSize.Max). `wrapContentHeight()` was not used on the card itself because it would break the equal heights in a row.
2. **AdMob added** (Google test IDs for now):
   - Order at startup: UMP consent first, then the ads SDK. Nothing starts while the phone is offline; ads start when internet comes back. Core features never wait for ads.
   - Banner at the bottom of Home, Tools and Vault (only shown when ready and online; no empty space offline).
   - Interstitial after a successful hide or reveal, with a 2-minute cooldown (kept across restarts).
   - "Ad privacy options" appears in Settings when Google's consent flow says it is required.
   - **Before release:** replace the test App ID in AndroidManifest.xml (`com.google.android.gms.ads.APPLICATION_ID`) and the two unit IDs in `Ads.kt` with your real AdMob IDs. Never click your own live ads.
3. **10 new Settings** (card "Preferences"): default bit depth, Smart hiding by default, Private vault by default, auto-lock delay, clipboard clear time, longest voice note, text size, screenshot protection (turn off to take Play Store screenshots), close app on panic, show passwords while typing.
4. **New logo:** a vermilion sun with a keyhole behind a torii gate over mountains; adaptive icon (vector background and foreground, monochrome layer), PNGs for all densities, and `store/play_icon_512.png`.

## What ads change (please read)
- The app now declares `INTERNET` and `ACCESS_NETWORK_STATE`. Earlier versions had no INTERNET permission; that selling point is gone.
- Play Console: the Data Safety form must now say data IS collected and shared by ads (device or other IDs, approximate location, app interactions, diagnostics, as AdMob describes), the privacy page at cartoniq.github.io/privacy/ must mention AdMob and consent, and you must declare the Advertising ID. Your own code still collects nothing and uploads nothing.
- Updated in the app: About, Terms, Licenses, Data collection texts (English) and the 4th onboarding line (all 15 languages). Not updated: PLAY_LISTING claims, so review them (see PLAY_LISTING.md).
- Ads use Google's SDK, which can make the app larger and slower to start. Test on a real phone with and without internet.

## Test list (final)
- Home and Tools cards on a small phone and with text size XL: no cut-off lines.
- Airplane mode: no banner, no gaps, everything works. Turn internet on: consent form (if in the EU or when required), then test banner.
- Hide a secret, then another within 2 minutes: only one interstitial. Wait 2 minutes: the next success shows another.
- Each of the 10 new settings.
- Screenshot protection off: screenshots work. On: they are blocked.


# 2.2.0 (versionCode 4)

## Build fixes (from your Termux build)
1. `build.gradle.kts`: there is no `java.time.LocalDate` anywhere in this project (searched all files). If your copy has one, it came from a local edit; if you want a build date, use `System.currentTimeMillis()`.
2. `HideScreens.kt`: `import androidx.compose.runtime.mutableStateListOf` added (my earlier edit skipped it by mistake).
3. `AppScreens.kt`: `ChoiceRow` is defined again (a later edit of mine had cut it off).
4. `AppScreens.kt`: `FloatArray.indexOf` replaced with `indexOfFirst { abs(it - value) < 0.001f }`.

## New in 2.2.0
- **Direct share and download** on every result: Share as file (sent untouched, no compression), Share as image, Save to Downloads. Works from the vault too (through a FileProvider), and multi-image results (Split, Shamir) share all parts at once. Vault items have a Share button.
- **Preferences look:** refined segmented controls (dark glass bar, thin gold edge, gold selected segment, no wrapping) and gold toggle switches. The "Now" label no longer breaks into two lines.
- **5 new features**
  1. Cover Scout (Tools): pick up to 6 photos, get them ranked by texture and size, tap "Use for hiding".
  2. Clean Photo (Tools): shows hidden location, camera and time data, then saves a clean JPEG or PNG copy.
  3. Dead-man switch (Settings): wipe vault, PIN and settings if the app was not used for 7, 30 or 90 days. Checked only when the app is opened; changing the phone clock can bypass it.
  4. Quick Capture (hide screens, "Take a photo"): shoot a cover photo with the camera app. No camera permission is needed.
  5. Password Forge (hide screens and Storm Mode): generates pronounceable random passwords and shows a rough strength meter.
- Manifest: FileProvider and a camera query were added.

## Test list
Share as file to WhatsApp, then reveal on the other phone; Save to Downloads; vault Share; each segmented control and switch; Cover Scout with 3 photos; Clean Photo on a camera photo with GPS; Quick Capture; Forge a password and use it; set the dead-man switch to 7 d and check it does not wipe on normal use.
