<div align="center">
  <img src="store/play_icon_512.png" width="160" alt="StealthText Logo" style="border-radius: 24px; box-shadow: 0 8px 16px rgba(0,0,0,0.3);">
  
  <h1>StealthText PRO 🕵️‍♂️</h1>
  <p><b>Your Secrets. Your Control.</b></p>
  
  <p>
    <img src="https://img.shields.io/badge/Platform-Android-brightgreen?style=for-the-badge&logo=android" alt="Platform" />
    <img src="https://img.shields.io/badge/Privacy-100%25_Offline-blue?style=for-the-badge&logo=shield" alt="Privacy" />
    <img src="https://img.shields.io/badge/Security-AES--256--GCM-red?style=for-the-badge&logo=lock" alt="Security" />
    <img src="https://img.shields.io/badge/Developer-CartoniQ-purple?style=for-the-badge" alt="Developer" />
  </p>
</div>

---

## 🚀 The Mission
In a world where every app wants your data, your location, and your identity, **StealthText** is a rebellion. It is a powerful, privacy-first Android application designed to hide your most sensitive secrets in plain sight. 

Using advanced **steganography** and **military-grade AES-256-GCM encryption**, StealthText allows you to hide encrypted text, PDFs, and voice notes inside ordinary-looking photos. 

No servers. No accounts. No trackers. Everything happens locally on your device. 

## 🛠️ The Arsenal (Features)

### 🎭 Ultimate Hiding
*   **Encode & Reveal:** Hide text, files (PDF/TXT up to 20MB), or voice notes inside images.
*   **Smart Hiding:** The algorithm intelligently finds "busy" textured areas of a photo to hide data, making it mathematically invisible to the human eye.
*   **Invisible Text:** Hide a secret message inside normal text using invisible unicode characters.
*   **Cover Photo Generator:** Don't have a photo? The app generates a film-grain cover photo perfectly sized to fit your secret.

### 🔐 Spy-Grade Security
*   **Four Gates:** Hide up to 4 completely different messages inside a *single* photo, each unlocked by a different password.
*   **Decoy Password:** If forced to open the app, a second password reveals a harmless fake message while the real secret stays hidden.
*   **Dead-Man Switch:** An optional self-destruct timer that wipes your entire vault if the app goes unused for 7, 30, or 90 days.
*   **Fake Crash Screen:** 3 wrong PIN attempts trigger a simulated Android "App keeps stopping" crash screen to deter intruders.

### ⚡ Advanced Spy Tools
*   **Split Secret (XOR):** Split a secret into two images. You need *both* images to reveal the secret.
*   **Shamir Secret Sharing:** Split a secret into 3-5 images. Any chosen number of them (e.g., 3 out of 5) can rebuild the secret.
*   **Storm Mode (Beta):** A highly experimental mode that hides a short text inside the block brightness of an image, designed to survive JPEG compression.

---

## 🧠 How The Magic Works (The Science)
StealthText doesn't just hide files; it mathematically embeds them into the pixels of your photos.
1.  **Bit Manipulation:** It alters the lowest 1 to 3 bits of the RGB color channels of an image. This changes the color so slightly that the human eye cannot detect it.
2.  **Encryption:** Your secret is packed, compressed (Deflate), and encrypted using **AES-256-GCM** with a 256-bit key derived via **PBKDF2-HMAC-SHA256** (200,000 rounds).
3.  **Anti-Forensics:** The header of the hidden file is masked with a hash of the password, making it look like random image noise to forensic analyzers. 
4.  **Scattering:** The encrypted data isn't written sequentially. It is scattered across the image using a mathematical formula derived from your password.

---

## 🛡️ The Privacy Manifesto
We believe your secrets belong *only* to you. 
*   **100% Offline Core:** Hiding, locking, and revealing work completely without the internet.
*   **Zero Data Collection:** We do not collect, store, or share your personal data, analytics, or secrets.
*   **Open Transparency:** While the source code is private to protect the security algorithms, the app is fully auditable in its behavior. 

*(Note: The app includes Google AdMob to support the developer. Ads only load when you are online. You can block internet access to the app and the core features will still work perfectly.)*

---

## 📱 The Phone-First Journey (Termux)
How was this built? **Not on a MacBook. Not in a high-rise office.**

**StealthText** was entirely designed, coded, compiled, and signed on a **Xiaomi Android phone** using **Termux**, a Linux terminal emulator. No laptop. No desktop. No team. 

Using **Kotlin**, **Jetpack Compose**, and an isolated **Gradle 8.9** build environment, the developer manually patched ARM64 binaries, debugged Kotlin compiler clashes, and signed the final APK using a mobile keystore. This is the definition of mobile-first engineering.

## 📥 Download & Install
Get the latest signed APK directly from the **[Releases Page](https://github.com/CartonIQ/StealthText-PRO/releases/latest)**. 

*Requires Android 10 (API 29) or higher.*

---

## 👑 About The Developer
**StealthText** was created by **Mohd Arsh**, the Founder of **CartoniQ**.

> *"While the tech world builds inside high-rise offices with dual monitors and expensive machines, I build from my pocket. Just a terminal, an idea, and the stubborn belief that great software doesn't need a big office—it needs a clear mind. Every app is designed, coded, and signed by hand, on the exact same device it runs on. This is what one person with grit can build."*

## 🤝 Connect With CartoniQ
Have a question, found a bug, or want to partner with us? Reach out:

*   📧 **Email:** [cartoniq.business@gmail.com](mailto:cartoniq.business@gmail.com)
*   🌐 **Website:** [cartoniq.runs-at.dev](https://cartoniq.runs-at.dev)
*   🐙 **GitHub:** [@CartonIQ](https://github.com/CartonIQ)

---
<div align="center">
  <i>Copyright © 2026 CartoniQ. All rights reserved. Built from a phone with pure grit.</i>
</div>
