# DuoLink – Production Android Private Messaging & Calling Application

**DuoLink** is a sovereign, production-ready, peer-to-peer encrypted communication application engineered exclusively for **TWO authorized individuals**.

---

## 📱 Final Deliverables & Artifacts

- **Official GitHub Repository**: [`https://github.com/nydatamind/DuoLink`](https://github.com/nydatamind/DuoLink)
- **Official Releases**: [`https://github.com/nydatamind/DuoLink/releases`](https://github.com/nydatamind/DuoLink/releases)
- **Production APK**: [`DuoLink.apk`](file:///d:/My_Projects/BlueTalk/DuoLink.apk) (71.1 MB, signed production release build with bundled Emoji2 font)
- **Cryptographic SHA-256**: `516c6691fd4c8c34007e446226b01fdb6d4bf38fbbc05ce1f49826db78aeebcf`
- **Application Label**: `DuoLink`
- **Application ID / Package**: `com.duolink.app`
- **Username Discovery & Mutual Pairing**: Symmetrical username-based search (`@username`) and instant real-time pairing request/accept. Zero phone-role or first/second device selection.
- **Separate Sign In & Sign Up**: Clean segmented switcher with mandatory unique username setup on first signup.
- **Minimalist Permissions UI**: Clean, streamlined cards with direct Android runtime permission contracts and Allowed/Not Allowed badges. Zero story paragraphs.
- **Telegram / iOS-Style Emoji Engine**: Bundled Google Noto Color Emoji (`androidx.emoji2:emoji2-bundled`) guaranteeing uniform emoji rendering across all Android versions without broken glyphs.
- **In-App Auto-Updater**: Native GitHub Releases updater with SHA-256 hash validation and certificate matching.
- **Official 3D Mascot**: Premium, original **DuoLink Owl** with dark charcoal plumage and crimson red accents (`duolink_owl_logo.png`).
- **Dynamic Eye State**: 3-stage expressive eye states (Alert, Sleepy, Sleeping) synchronizing partner activity via Supabase Realtime.
- **Dynamic Launcher Icon**: Android launcher icon switches dynamically based on partner presence using `<activity-alias>` and `PackageManager`.
- **First-Time Permission Wizard**: Professional permission setup screen explaining and requesting all required hardware and battery optimization exemptions.
- **Visual Design**: Dark charcoal and vibrant crimson red neumorphic UI matching the design reference screenshot.
- **Alert Engine**: Silent hardware-based vibration alert system with CPU WakeLock backup (no visible banners, no audible ringtones, no lock-screen previews).
- **Authentication**: Private Google OAuth Sign-in with Supabase Auth (`duolink://auth/callback`) featuring zero identity leakage.
- **Identity Privacy**: Independent custom nicknames (e.g. "My Person ❤️", "Nitin ❤️"); Gmail address and Google profile info are never shared.
- **Synchronized Deletion**: Strictly "Delete for Everyone" across both devices; no "Delete for me" option; Telegram-style "Clear ALL History for Both".
- **Backend Options**: 
  - Dual-mode support for **Supabase Realtime** (`https://kypyymlubmylmomoqyoq.supabase.co`)
  - **Self-Hosted WebSocket Signaling Server** located in [`server/`](file:///d:/My_Projects/BlueTalk/server/)

---

## 🦉 3D Owl Mascot & Dynamic Status System

1. **Original Mascot Design**:
   - An expressive, cute, yet sleek 3D character owl designed in DuoLink's brand palette: dark charcoal plumage (`#181B22`), obsidian slate body, and vibrant crimson red accents (`#FF384C`).
   - Completely original design created for DuoLink.

2. **3 Dynamic Eye Expressions**:
   - **State 1 – Fully Open Eyes (`owl_open.png`)**: Bright, glowing, alert expression indicating partner was active recently (0–5 minutes).
   - **State 2 – Half-Closed Eyes (`owl_half.png`)**: Heavy, sleepy eyelids indicating partner has been inactive for some time (5 minutes – 1 hour).
   - **State 3 – Completely Closed Eyes (`owl_closed.png`)**: Serene sleeping curved eyelids indicating partner has been inactive for over 1 hour.

3. **Dynamic Android Launcher Icon Switching**:
   - DuoLink utilizes Android `<activity-alias>` declarations (`MainActivityAliasOpen`, `MainActivityAliasHalf`, `MainActivityAliasClosed`) with `PackageManager.setComponentEnabledSetting(..., DONT_KILL_APP)`.
   - Your Android phone's home-screen app icon dynamically switches to reflect your partner's alertness level without duplicate icons or app crashes.
   - Inside the app, the dynamic mascot is rendered in the `ChatScreen` header, `HomeScreen` partner card, and `SplashScreen`.
   - Interactive testing buttons are available in **Settings > Dynamic Owl Mascot & Launcher Icon** to preview each state and icon on demand.

4. **Privacy-Preserving Presence**:
   - Synchronized over Supabase Realtime using lightweight heartbeat signals without transmitting exact timestamps, location, or private device data.

---

## 😀 Telegram / iOS-Style Emoji Keyboard

1. **Bundled Noto Color Emoji Engine**:
   - Integrated `androidx.emoji2:emoji2-bundled:1.5.0` directly into the APK.
   - Ensures consistent, pixel-perfect, high-fidelity emoji rendering across all Android devices (Android 8.0 through Android 15), completely eliminating missing font glyphs or hollow square boxes (tofu).
   - Instant loading with zero network dependency.

2. **Full-Featured Bottom Sheet Keyboard**:
   - Built with Telegram and iOS keyboard styling tailored to DuoLink's dark neumorphic aesthetic.
   - **8 Comprehensive Categories**: Recent (`🕒`), Smileys & Emotion (`😀`), People & Gestures (`👋`), Animals & Mascot (`🦉`), Food & Drink (`🍔`), Travel & Places (`🚀`), Activities (`⚽`), Hearts & Symbols (`❤️`), and Flags (`🏁`).
   - **Tactile Haptic Feedback**: Every emoji tap triggers an ultra-subtle mechanical haptic tick (`HapticFeedbackType.TextHandleMove`).
   - **Live Keyword Search**: Instant filtering across emoji descriptions (e.g. typing "owl" immediately shows `🦉`, typing "heart" displays all romantic symbols).
   - **Recent Emoji Persistence**: Automatically tracks and prioritizes your most frequently used emojis in local storage.
   - **Dedicated Backspace Button**: Smooth one-tap backspace deletion integrated directly into the emoji keyboard header.

---

## 🔄 GitHub Releases Distribution & In-App Auto-Update

1. **Official Distribution Channel**:
   - All stable binaries are published directly to [`https://github.com/nydatamind/DuoLink/releases`](https://github.com/nydatamind/DuoLink/releases).
   - Every release includes the signed production APK, version number, changelog, min/target SDK metadata, and SHA-256 checksum.

2. **Automated In-App Update Engine**:
   - **Startup & Settings Checks**: Automatically checks `https://api.github.com/repos/nydatamind/DuoLink/releases/latest` in the background on startup, and on demand via **Settings > Diagnostics & Security > Check for Updates**.
   - **Semantic Version Comparator**: Accurately compares installed vs. published versions (e.g. `1.0.0` ➔ `1.0.1`).
   - **Non-Intrusive Dismissal**: Selecting "Later" snoozes the update dialog for that version, preventing annoying repetitive popups on every app launch.

3. **Strict Cryptographic Integrity & Security**:
   - **Real-Time Byte Streaming**: Live download progress bar displaying exact megabytes received, total file size, and percentage.
   - **SHA-256 Verification**: Computes the SHA-256 hash of the downloaded binary on the fly and compares it against the published release hash before opening.
   - **Signature Verification**: Verifies `signingInfo.apkContentsSigners` against the installed app's signature certificate to guarantee that only authentic DuoLink builds signed by the original key can be updated.
   - **Official Android Package Installer**: Hands verified binaries directly to Android's official Package Installer via secure `FileProvider` (`com.duolink.app.provider`).
   - **Preserves All User Data**: In-place APK update preserves all Room encrypted chat records, Keystore cryptographic keys, settings, and login credentials without data loss.

4. **Automated Publishing Script**:
   - Release automation script available at [`scripts/publish_release.ps1`](file:///d:/My_Projects/BlueTalk/scripts/publish_release.ps1). Run:
     ```powershell
     powershell -ExecutionPolicy Bypass -Command "& '.\scripts\publish_release.ps1' -Version '1.0.0'"
     ```

---

## 🛡️ First-Time Permission Setup Wizard

On first launch (and accessible anytime in Settings), DuoLink guides users through:
- **Silent Vibration & Notifications**: Enables hardware vibration engine for stealth incoming alerts without audible ringtones or visible popups.
- **Microphone**: End-to-end encrypted voice calling and voice notes.
- **Camera**: Scanning one-time pairing QR codes and HD video calls.
- **Bluetooth & Audio Routing**: Smooth handoff between speakerphone, earpiece, and Bluetooth headphones.
- **Battery Optimization Exemption (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)**: Explicit user consent to exempt DuoLink from OEM aggressive battery management, guaranteeing repeating vibrations even when the phone is locked.

---

## 🔒 Private Google Login & Identity Privacy

1. **Google Account Authentication**:
   - Built on Supabase Authentication with Google OAuth (`duolink://auth/callback`).
   - Authenticates each user securely on Android.
   - Extracts an internal unique account ID for secure pairing verification.
   - Prevents unauthorized third accounts from joining an existing conversation.

2. **Complete Identity Privacy (Zero Leakage)**:
   - The application **NEVER** displays or transmits:
     - Gmail address
     - Google account name / display name
     - Google profile photo / avatar
     - Personal Google account details
   - Google account data is strictly confined to local authentication and session management.
   - Zero tokens or Google profile metadata are shared between paired devices or logged into databases.

3. **Independent Custom Display Names**:
   - Each user can assign any custom nickname to the other person locally:
     - Phone A can display Phone B as *"My Person ❤️"*
     - Phone B can display Phone A as *"Nitin ❤️"*
   - Nicknames are completely independent and stored only on the local device.
   - Changing a display nickname on one phone **never** alters the name displayed on the other phone.

---

## 🗑️ Synchronized Deletion & Telegram-Style History Wiping

1. **Delete for Everyone (Both Devices)**:
   - Standard "Delete for me" option has been removed.
   - Every message deletion permanently deletes the message from **BOTH** devices.
   - When User A deletes a message, it immediately disappears from both User A and User B's screens.
   - Deletion events are cryptographically signed and synchronized in real-time over WebRTC / Supabase Realtime.
   - If the recipient device is offline, a persistent `deletion_tombstones` record and `pending_deletions` queue ensure the message is immediately wiped upon reconnection.

2. **Clear Chat History for Both**:
   - Provides a confirmation dialog: *"Clear chat history for both users?"*
   - Clears all chat messages, voice notes, and media attachments on both phones.
   - Deletes cached local audio and image files from internal disk storage.

3. **Separate Call History (Outside Chat Timeline)**:
   - Call logs are stored in a dedicated **Call History** screen and **never** mixed into the chat timeline.
   - Features a dedicated *"Clear Call History for Both"* option that deletes call records on both phones.

4. **Clear ALL History for Both (Telegram Style)**:
   - Available in **Settings > Synchronized History & Privacy**.
   - Confirmation dialog: *"Clear ALL history for BOTH users? This will permanently delete ALL chat messages, voice notes, media files, and call history from BOTH your phone and your partner's phone. Nothing can be recovered."*
   - Immediately purges both phones' databases and local storage.

---

## 🎨 Exact UI Alignment (Reference Screenshot Fidelity)

| UI Component | Implementation Detail |
|---|---|
| **Background** | Deep dark charcoal (`#181B22`) with soft slate highlights |
| **Outgoing Messages** | Vibrant crimson red (`#FF384C`) with soft 3D neumorphic shadow and crisp white typography |
| **Incoming Messages** | Dark charcoal (`#232732`) with subtle neumorphic rim lighting and light grey typography |
| **Timestamps** | Inline micro-timestamps (e.g. `4:09 pm`) next to bubble content |
| **Date Separator** | Centered soft grey `Today` label between day intervals |
| **Top Header** | Red back arrow, circular user avatar, contact name, `Typing...` indicator, and top-right circular neumorphic button with red phone icon |
| **Bottom Input Bar** | Dark pill container (`#232732`) with red emoji smiley, "Type a message" placeholder, red paperclip attachment, red camera icon |
| **Voice Button** | Floating circular red action button (`#FF384C`) with white microphone icon; smoothly morphs into Send icon when text is entered |

---

## 📳 Silent Vibration Alert Engine (No Visible Notifications)

Per the strict privacy specification:
1. **Zero Visible Banners**: The app never displays popup heads-up notification banners or lock-screen text.
2. **Zero Audible Ringtones**: Neither messages nor calls trigger audible sounds.
3. **Repeated Message Vibration**:
   - When a new message arrives, the receiving device executes a distinct double-buzz vibration pattern (`220ms on, 140ms off, 260ms on, 2200ms pause`).
   - Repeats in the background until the user opens the application or acknowledges the message.
   - Vibration immediately terminates the instant the chat screen is opened.
   - Built-in deduplication prevents repeat vibrations for the same message or messages sent by the local device.
4. **Urgent Call Vibration**:
   - When the paired partner initiates a call, the receiving phone executes a continuous 1-second pulsing vibration (`1000ms on, 600ms off`).
   - Rings visually on-screen inside DuoLink if the app is active.
   - Stops immediately when the call is answered, declined, or ended.
5. **Vibration Diagnostics Screen**:
   - Available in **Settings > Silent Vibration Alert System**. Users can test both message vibration and call vibration patterns directly.

---

## 🔐 Cryptography & Security Architecture

1. **Hardware-Backed Identity**:
   - Cryptographic keypair generated using **NIST P-256 (`secp256r1`)** via Android Keystore.
   - Private keys are stored in hardware-backed TEE / StrongBox.
2. **Two-Person Ephemeral Pairing**:
   - User A generates a one-time QR code containing their public key, device ID, signaling URL, and a cryptographically signed token valid for 10 minutes.
   - User B scans the QR code using CameraX.
   - Signatures are verified with `SHA256withECDSA`.
   - Both devices compute a shared secret `Z` via **ECDH (Elliptic Curve Diffie-Hellman)**.
   - AES-256 symmetric session key derived using **RFC 5869 HKDF-SHA256**.
   - Pairing is permanently stored in encrypted local Room SQLite; unpairing destroys keys and wipes chat data.
3. **Authenticated Message Encryption**:
   - All messages, voice notes, and media envelopes are encrypted using **AES-256-GCM** with a unique 12-byte IV and 128-bit authentication tag.
   - Even when routed through Supabase Realtime or the self-hosted server, the backend sees only opaque ciphertext.
4. **Media Security**:
   - Real-time voice and video calls use WebRTC **DTLS-SRTP** for end-to-end media encryption.

---

## 📡 Dual Signaling Options

### Option 1: Supabase Realtime (Preconfigured Default)
- **URL**: `https://kypyymlubmylmomoqyoq.supabase.co`
- **Key**: `sb_publishable_wJt7PY1A73Od_VLvVMdJkw_89vY-6pM`
- Communicates over secure WebSockets using the Phoenix channel protocol.
- Zero server setup required for mobile data to Wi-Fi connectivity.

### Option 2: Self-Hosted Lightweight WebSocket Server
- Located in directory [`server/`](file:///d:/My_Projects/BlueTalk/server/).
- Run locally or on a private VPS:
  ```bash
  cd server
  npm install
  npm start
  ```
- Or deploy via Docker Compose:
  ```bash
  docker compose up -d
  ```

---

## 🚀 How to Install on Two Android Phones

1. Transfer [`DuoLink.apk`](file:///d:/My_Projects/BlueTalk/DuoLink.apk) to Phone A and Phone B via USB, ADB, or file transfer:
   ```bash
   adb install -r DuoLink.apk
   ```
2. Open **DuoLink** on **Phone A**:
   - Tap **Begin Secure Pairing**.
   - Stay on **Display My QR** tab.
3. Open **DuoLink** on **Phone B**:
   - Tap **Begin Secure Pairing** and switch to **Scan Partner QR** tab.
   - Point Phone B's camera at Phone A's screen.
4. Both devices will immediately verify cryptographic signatures, establish the shared secret session, and transition to the **Home** screen.
5. You are now permanently paired in an exclusive, private, end-to-end encrypted link!

