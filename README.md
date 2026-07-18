# ShieldSMS 🛡️

**A working messaging app (like Google Messages) that automatically hides the body of sensitive incoming texts — OTPs, bank alerts — until you unlock them with your fingerprint or default device auth.**

---

## Purpose of this app

Normally, any text message that arrives shows its full content right in the
notification bar and in your messaging app's list — no unlocking needed.
This app is a real messaging app. When a text arrives, it reads the message
first. If it looks sensitive (mentions "OTP", "verification code", "account
balance"), it hides the actual words — both in the notification bar and in
the app itself — and shows only "Content hidden — tap to unlock." You only
see the real message after your fingerprint confirms it's really you. Every
other, ordinary text (like "see you at 6?") shows up completely normally.



---

## Why this app?

This project was inspired by a scene from the web series **"Pritam and Pedro"**, where an attacker successfully carries out a **social engineering attack** to gain access to a victim's email account. Instead of exploiting a software vulnerability, the attacker relies on human behavior—convincing or tricking the victim into revealing a one-time password (OTP) that appears as a notification on their phone.

That scene highlights a common but often overlooked security problem: **even if a phone is locked, or a user is being cautious, sensitive information such as OTPs, bank alerts, verification codes, and account details can still be exposed through message previews and notifications.** Anyone nearby can read them with a quick glance, making shoulder surfing and social engineering attacks much easier.

ShieldSMS was built to address this specific problem. Rather than attempting to secure the entire messaging ecosystem, it focuses on protecting the most sensitive SMS content.

The goal of this project is not to replace existing messaging applications, but to demonstrate how Android's security features—such as the **Android Keystore**, **BiometricPrompt**, and **Room Database**—can be combined to reduce the risk of exposing sensitive information through SMS previews.

This project serves as a practical demonstration of applying Android security concepts to solve a real-world privacy problem, making it suitable as both a learning project and a proof-of-concept for secure messaging workflows.


## How it works (technical flow)

```
An SMS arrives on the phone
            │
            ▼
Android delivers it via the SMS_DELIVER broadcast - ONLY to the current
default SMS app (SmsDeliverReceiver.java)
            │
            ▼
Read the sender + full message text
            │
            ▼
Does the text match a preloaded sensitive keyword? ("OTP", "verification
code", "account balance", "CVV", ...)  ──No──▶  Store as plain text,
            │ Yes                                post a normal notification
            ▼                                    with the real preview
Encrypt the text with AES-256-GCM (CryptoHelper, backed by Android Keystore)
            │
            ▼
Save the encrypted text to a local database
            │
            ▼
Post a notification showing ONLY the sender - "Content hidden, tap to unlock"
(the message list inside the app shows the same thing - no real text anywhere)
            │
            ▼
User taps → UnlockActivity → fingerprint / face / PIN prompt
            │
            ▼
On success: decrypt from the database, display the real message
```

## Why the encryption is safe

The AES key used to scramble sensitive message text is generated **inside
the phone's secure hardware** (Android Keystore) and never leaves it - not
even this app's own code ever touches the raw key. Even if someone extracted
this app's files directly, they'd get unreadable ciphertext without the key.

---

## Project structure

```
app/src/main/java/com/kva/shieldsms/
├── receiver/
│   ├── SmsDeliverReceiver.java       ← core logic: reads every incoming SMS, redacts on match
│   ├── MmsReceiver.java              ← required stub (MMS out of scope for this demo)
│   └── HeadlessSmsSendService.java   ← required stub (quick-reply-from-dialer, out of scope)
├── detector/
│   └── SensitiveKeywordDetector.java ← preloaded keyword list
├── vault/
│   └── CryptoHelper.java             ← AES-256-GCM encrypt/decrypt via Android Keystore
├── data/
│   ├── MessageEntity.java            ← Room entity: one stored message
│   ├── MessageDao.java               ← Room queries
│   └── AppDatabase.java              ← Room database singleton
└── ui/
    ├── OnboardingActivity.java       ← requests + blocks on default-SMS-app status
    ├── MainActivity.java             ← message list (home screen)
    ├── ComposeActivity.java          ← required stub (sending out of scope)
    ├── UnlockActivity.java           ← biometric prompt + reveals decrypted content
    ├── MessageAdapter.java           ← binds messages to the list
    └── MessageViewModel.java         ← loads messages for the list (MVVM)
```

## What's deliberately out of scope (and why)

This is a focused demo of ONE idea - protecting incoming sensitive content -
not a full messaging app replacement. A few things are intentionally stubbed:

- **Sending messages** - `ComposeActivity` exists only because Android
  requires it to be present for default-SMS-app eligibility. It doesn't
  actually send anything.
- **MMS / picture messages** - `MmsReceiver` is a required stub; this demo
  only processes plain SMS.
- **Writing to the system SMS content provider** - a fully compliant default
  SMS app is expected to also insert received messages into Android's shared
  SMS database so other apps stay in sync. This demo stores messages only in
  its own local database, which keeps the code simple but means other apps
  (if you ever switch default SMS app again) won't see messages received
  while this app was active.

These are good, honest talking points for an interview - a security-focused
demo project intentionally narrows scope rather than trying to be a full
product.

## Threat model

**Protects against:**
- Shoulder-surfing (someone reading your screen without touching your phone)
- A stranger picking up your unlocked phone and browsing messages/notifications
- Someone social-engineering you into reading a code aloud from a visible banner

**Does NOT protect against:**
- Someone who has your fingerprint/PIN and physical access
- Malware already running with elevated (root) privileges on the device
- The sender's account/service being compromised at the source

---

## Setup (for developers)

1. Open in Android Studio (Giraffe or later).
2. Sync Gradle.
3. Run on a real device (recommended - default-SMS-app role selection can be
   inconsistent on emulators) running **API 26+**.
4. On first launch, tap "Set as Default Messaging App" and confirm in the
   system dialog. The app will not proceed to the message list until this
   is granted - there is no skip.
5. Send yourself a text containing a trigger word, e.g. *"Your OTP is 4521,
   do not share this code."* It should appear with the body hidden, both in
   the notification and inside the app. Send a normal text too, to confirm
   it shows up untouched.

## Future enhancements

- Auto-lock / re-lock already-viewed sensitive messages after N minutes.
- Let the user view/add custom keywords beyond the built-in list.
- Insert received messages into Android's shared SMS provider for full
      compliance with default-SMS-app expectations.
- Implement real sending (would remove the last stubbed component).

## Open for contribution
We welcome contributions from the community! Whether it's bug fixes, feature enhancements, or documentation improvements, your input is valued.

### How to Contribute

1. **Fork the Repository**
   ```bash
   Click "Fork" button on GitHub
   ```

2. **Create a Feature Branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```

3. **Make Your Changes**
   - Write clean, well-documented code
   - Follow Android coding conventions
   - Ensure your code doesn't break existing functionality

4. **Commit Your Changes**
   ```bash
   git commit -m 'Add amazing feature: description'
   ```

5. **Push to Your Fork**
   ```bash
   git push origin feature/amazing-feature
   ```

6. **Open a Pull Request**
   - Provide a clear description of changes
   - Reference any related issues
   - Include screenshots for UI changes
   - Wait for code review

### Contribution Guidelines

- Follow the existing code style and architecture
- Add unit tests for new features
- Update documentation as needed
- Ensure minimum SDK compatibility (API 24+)
- Test on multiple Android versions
- Don't introduce breaking changes without discussion

---
## Author
<div align="center">Kushal V Achar</div>

- Email: [kushalv1306@gmail.com](kushalv1306@gmail.com)
- GitHub: [@kushalvachar2006](https://github.com/kushalvachar2006)
- LinkedIn: [Connect with me](https://www.linkedin.com/in/kushal-v-achar-796049317/)
