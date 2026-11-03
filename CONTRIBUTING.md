# Contributing to CoverIt

Thanks for helping. Bug reports, ideas and pull requests are all welcome. For anything big, open an
issue first so we can agree on the approach.

## Ground rules

- **No internet.** The app has no `INTERNET` permission and no library may add it. No analytics,
  no Google Play Services, no ML Kit or MediaPipe Tasks (they bring telemetry).
- **What the editor shows is what the file gets.** Previews and exports use the same `Painter`.
- **Covered means erased.** Never draw a box over text that stays in the file.
- **Every string in both languages**: English in `res/values/strings.xml`, Spanish in
  `res/values-es/strings.xml`.
- **Small, readable code.** Clear names instead of comments; a comment only where something is
  surprising and costly to rediscover.
- **Licenses compatible with GPL-3.0** for any new dependency.

## Building and testing

Requirements: JDK 17 and the Android SDK (compile SDK 37).

```
./gradlew testDebugUnitTest            # detection, office formats (fast, no device)
./gradlew connectedDebugAndroidTest    # PDF redaction and editor, on an emulator
./gradlew assembleRelease              # one APK per ABI
```

`connectedDebugAndroidTest` installs and then uninstalls the app: run it on an emulator, never on
a phone with documents you care about.

## How the code is laid out

All code lives in `app/src/main/java/com/coverit/app`.

| Folder | What it does |
|---|---|
| `detect/` | Finds personal data in plain text. `Detector` runs every rule; `Kind` lists the kinds of data |
| `detect/rules/` | The rules, one file per topic: `Ids`, `Money`, `Contact`, `Phones`, `Network`, `Dates`, `Places`, `Details` |
| `detect/check/` | Check digits and validity tests (IBAN, Luhn, national IDs, dates, IPs) |
| `detect/names/` | People's names without lists of people: labels, known given names and surnames, CV headings |
| `detect/text/` | Small text helpers (accent folding, context before a match) |
| `doc/` | A document being edited: `Loader` reads and detects, `Session` holds the marks and undo, `Exporter` writes the clean copy and checks it |
| `pdf/` | PDF text extraction, real redaction (`Redactor`) and the check of the result (`Verifier`) |
| `office/` | Word, Excel, PowerPoint, OpenDocument and plain text, edited in place without losing the format |
| `image/` | OCR (Tesseract), faces (BlazeFace on TensorFlow Lite), QR and bar codes (ZXing), photo metadata |
| `redact/` | `Painter`: black, pixelate, blur, label and watermark, the same on screen and in the file |
| `data/` | The library (documents, folders, spaces) and preferences |
| `ui/` | Compose screens, one folder per area: `home`, `library`, `editor`, `result`, `settings`, `onboarding`; shared pieces in `common` and `components`, colours and type in `theme` |

## Adding a kind of data to detect

1. Write the rule in the right file of `detect/rules/`. A `Rule` is a `Kind`, a regex, the group to
   cover, an `accept` check (to reject false positives) and a `cover` check (true covers it at
   once; false only suggests it).
2. Put any checksum in `detect/check/Validators.kt`.
3. If it needs a new `Kind`, add its label, tag, icon and hint in `ui/common/Labels.kt` and the
   strings in both languages.
4. Add tests in `app/src/test/java/com/coverit/app/detect/` with real-looking examples **and** with
   text that must not match. False positives annoy people as much as misses.

Order matters in `Detector`: the first rule that claims some text keeps it, so specific formats go
before generic ones.

## Pull requests

- One topic per pull request, with a short description of what changes for the user.
- `./gradlew testDebugUnitTest` must pass.
- Screenshots for anything visible, in light and dark theme, and in Spanish if the text is longer.
