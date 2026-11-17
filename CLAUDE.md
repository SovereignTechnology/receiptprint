# receiptprint

Android app (AIDL service + kiosk) built without Gradle, using `build.sh`.

## Build

```
./build.sh
```

Output: `receiptprint.apk` at the repo root (debug-signed).

### Toolchain

`build.sh` expects a toolchain directory (default `./toolchain`, override with
`RECEIPT_TOOLCHAIN`) containing:

- `jdk/` (used as `JAVA_HOME`)
- `android-sdk/build-tools/34.0.0`
- `android-sdk/platforms/android-34` (provides `android.jar`, `framework.aidl`)

### Steps performed by `build.sh`

1. `aidl` compiles `aidl/*.aidl` to Java
2. `javac` (source/target 8) compiles `src/` plus generated AIDL sources
3. `d8` dexes the classes (min API 29)
4. `aapt2 compile` processes `res/`
5. `aapt2 link` with `AndroidManifest.xml` (min SDK 29, target SDK 31)
6. Adds `classes.dex` to the APK
7. `zipalign`
8. Signs with `debug.keystore` (generated via `keytool` if missing) using `apksigner`

Intermediate files go in `build/`, which is wiped on every run.
