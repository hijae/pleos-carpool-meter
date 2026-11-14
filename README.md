# Pleos Carpool Meter

Pleos Connect용 재미용 카풀 미터 앱. 결제 기능은 제공하지 않습니다.

## 현재 상태

- **완료:** 공식 Vehicle SDK 2.3.2 연동, 오도미터 조회 코드 컴파일, 디버그 APK 빌드 및 서명 검증.
- **완료:** 미터·설정·주행 내역 화면의 인터랙티브 디자인 초안과 기능 명세.
- **예정:** Android 미터 화면, 요금 계산 엔진, FusedLocation/NaviHelper 연동, 주행 내역 저장.
- **미검증:** Pleos 에뮬레이터/실차 실행 및 실제 차량 데이터 수신.

현재 Android 앱은 SDK 연동 확인용 화면입니다. 디자인 초안의 미터 기능이 APK에 구현된 상태는 아닙니다.

## 구성

- `app/`: Vehicle SDK 초기화 및 오도미터 조회 예제
- `docs/product-spec.md`: 합의한 화면·요금·할증·데이터 연동 요구사항
- `docs/surcharge-behavior.md`: 시외할증 상태 전환 규칙
- `docs/sdk-build-verification.md`: SDK 연동 방법과 검증 범위
- `docs/previews/carpool-meter.html`: 최신 대화형 화면 초안 원본
- `artifacts/vehicle-sdk-smoke-debug.apk`: 빌드/서명 검증을 통과한 SDK 연동 확인용 APK

화면 초안은 대화 내 미리보기용 HTML fragment입니다. 브라우저에서 열어 기본 동작을 볼 수도 있지만, 호스트 전용 테마·디자인 제어는 대화 환경에서만 제공됩니다. 금액·기록·위치 전환은 예시 데이터입니다.

## 빌드

JDK 17, Android SDK Platform 34, Build Tools 35.0.0을 설치하고 `ANDROID_HOME`을 설정하거나 `local.properties`에 `sdk.dir`을 지정합니다.

```sh
./gradlew assembleDebug
```

Gradle 8.13 / Android Gradle Plugin 8.9.2 / minSdk 28 / targetSdk 34.

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n ai.pleos.smoke/.MainActivity
```

Vehicle Service 2.3.13 이상이 필요합니다. SDK 초기화 성공과 실제 차량 서비스 연결 성공은 별개입니다.

## 다음 작업

1. FusedLocation 연동과 위치 수신 진단 화면 빌드
2. NaviHelper 현재 위치 응답의 행정구역 필드 및 갱신 방식 확인
3. 지역별 요금 모델·심야/시외할증 계산 엔진 구현
4. 화면 초안을 Android 화면으로 구현하고 기록 저장
5. 로컬 Pleos 에뮬레이터에서 수신/계산/주야간 전환 검증

## 참고

- [Pleos 개발 환경](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
- [Vehicle SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro)
- [FusedLocation SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro)
- [NaviHelper SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)
