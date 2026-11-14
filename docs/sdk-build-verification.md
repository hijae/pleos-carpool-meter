> 초기 SDK 연동 확인 시점의 기록입니다. 현재 미터 앱의 구현 상태는 README.md와 verification.md를 참고하세요.

# Pleos Vehicle SDK build check

Minimal Android Automotive app linked against the official Pleos Vehicle SDK. This is a compile/package verification, not a verified vehicle runtime integration.

## Integration

- Official Maven repository: https://nexus-playground.pleos.ai/repository/maven-releases/
- Dependency: `ai.pleos.playground:Vehicle:2.3.2`
- On launch: `new Vehicle(context)`, `initialize()`, `getOdometer()`.
- Read odometer button: `Odometer.getCurrentDistanceDriven(onComplete, onFailed)`.
- Callback success/error is displayed on the UI thread. Returned values are shown without an assumed unit.
- Manifest permission: `pleos.car.permission.CAR_DRIVE`, confirmed from the SDK method's `@RequiresPermission` annotation.
- The SDK contributes its binding permission, service discovery query, and version metadata via manifest merging.
- The activity releases the SDK on destruction.

An initialization call returning does not prove that the Vehicle Service is connected. Only a successful query can confirm data access. The current SDK's manifest requires Vehicle Service >= 2.3.13 (version code 65); current docs describe service 2.3.21. The emulator image's 2.1.0 label alone does not establish its installed service version.

## Build

Use JDK 17 and Android SDK Platform 34 / Build Tools 35.0.0. Gradle 8.13 and AGP 8.9.2 are configured; minSdk is 28.

```sh
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## Run

Install and start the Pleos emulator following https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator then:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n ai.pleos.smoke/.MainActivity
```

Tap **Read odometer** to request actual SDK data. Check the displayed error if permissions, service version, service connection, or feature support prevent access. There are no fake odometer readings.

## Environment limitation

This workspace has no `/dev/kvm` and approximately 8 GB total disk. The Pleos x86_64 system image declares 19,643,095,958 bytes uncompressed (4,087,751,964 bytes compressed). Pleos was not installed or launched here; service binding, runtime permissions and actual vehicle readings remain unverified.

## References

- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object
- https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission

## Verified build result

`assembleDebug`: BUILD SUCCESSFUL, with the official Vehicle 2.3.2 Maven dependency and typed odometer query code compiled and packaged. APK signature verification passed. Final manifest inspection confirmed CAR_DRIVE, the SDK binding permission, discovery intent, and SDK/service version metadata. This does not verify runtime service connection or odometer values.
