# Scan POS Printer (Android helper)

A small, free Android app that prints Scan POS slips on a Bluetooth receipt printer over
**classic Bluetooth** (the same way the printer maker's own SDK does). Classic Bluetooth has
flow control built in, so slips never overflow the printer's memory and freeze it.

- Scan POS's **Print** button opens this app with the slip inside (a `scanposprint:` link,
  [Chrome's documented way to hand data from a web page to an app](https://developer.chrome.com/docs/android/intents)).
  It shows "Printing…", prints, and closes by itself.
- You can also **share a picture** (e.g. a receipt from Share receipt) to this app.
- No ads, no account, no internet permission. It only asks for **Nearby devices** (Android 12+),
  to connect to an already-paired printer.

## Building

GitHub builds it: see `.github/workflows/printer-app.yml`. Push this folder to a GitHub repository,
then open the repository's **Actions** tab. When the build finishes, the app is at
`https://github.com/<you>/<repo>/releases/download/printer-app/scanpos-printer.apk`.
Put that address in `src/brand.ts` (`PRINTER_APP_URL`) so Scan POS can link to it.

## Signing key

Android only installs updates signed with the same key. A key is included in `app/signing/`
so every build can update the last one. If this repository is **public**, anyone can see that key.
For your own key: make one with
`keytool -genkeypair -keystore my.jks -storetype PKCS12 -alias scanpos -keyalg RSA -keysize 2048 -validity 10000`,
then add two GitHub secrets (Settings > Secrets and variables > Actions): `KEYSTORE_BASE64`
(the file, base64-encoded) and `KEYSTORE_PASSWORD`. Phones then need the app uninstalled once
before installing a build with the new key.
