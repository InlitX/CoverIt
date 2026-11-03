<div align="center">

<img src="docs/banner.png" alt="CoverIt: cover personal data, share safely" width="100%">

# CoverIt

**Cover personal data in documents and photos before you share them. Fully offline.**

</div>

> [!WARNING]
> **CoverIt is in beta.** Detection can miss things or mark things that are not personal. Always look over the copy before you share it, and report bugs in the [issues](https://github.com/InlitX/CoverIt/issues).

<p align="center">
  <img src="docs/mockups/03-page.png" width="24%" alt="A page with the data covered in the label style">
  <img src="docs/mockups/02-found.png" width="24%" alt="What CoverIt found, by type">
  <img src="docs/mockups/03-zoom.png" width="24%" alt="Zoomed in to check every cover">
  <img src="docs/mockups/08-before-after.png" width="24%" alt="Before and after slider">
</p>

## What it does

- **Finds personal data on its own**: names, ID and account numbers, IBAN, cards, emails, phone numbers from any country, addresses, dates of birth, health details, faces and QR codes. Text in photos and scans is read on the device with OCR. You can teach it your own formats.
- **Lets you decide**: review every item one by one, cover by hand with the lasso, rectangle or brush, remove pages and add a watermark. Black, pixelate, blur or label styles. Undo, redo and edit again later.
- **Erases for real**: in PDFs the covered text no longer exists in the file; Office and LibreOffice files keep their format. Author, comments, tracked changes and photo GPS are removed, and the copy is checked again before you share it.
- **Only hidden data**: pick one or many files and get clean copies without covering anything.

Opens PDF, Word, Excel, PowerPoint, LibreOffice, plain text and photos, from the app or the Android share menu. English and Spanish, light and dark theme.

<p align="center">
  <img src="docs/mockups/01-home.png" width="24%" alt="Home">
  <img src="docs/mockups/06-library.png" width="24%" alt="Library with folders">
  <img src="docs/mockups/04-photo.png" width="24%" alt="A boarding pass with the name and QR code pixelated">
  <img src="docs/mockups/07-hidden.png" width="24%" alt="Hidden data in a photo: location, camera and date">
</p>

## Privacy

No internet permission, no analytics, no ads, no account. Your documents never leave your phone and are excluded from Android backups.

## Download

Get the APK from the [releases page](https://github.com/InlitX/CoverIt/releases). Almost every current phone needs `app-arm64-v8a-release.apk`.

## Building

Requirements: JDK 17 and the Android SDK.

```
./gradlew assembleRelease
./gradlew testDebugUnitTest
```

Want to help? See [CONTRIBUTING.md](CONTRIBUTING.md).

## Built with

- [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android): reading and writing PDF
- [Tesseract4Android](https://github.com/adaptech-cz/Tesseract4Android): OCR
- [TensorFlow Lite](https://www.tensorflow.org/lite) with MediaPipe's BlazeFace model: faces
- [ZXing](https://github.com/zxing/zxing): QR and bar codes
- [libphonenumber](https://github.com/google/libphonenumber): phone numbers

## License

[GPL-3.0](LICENSE). CoverIt is free and will stay free. If it helps you, you can [buy me a coffee](https://ko-fi.com/inlitx).
