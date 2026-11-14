# Pleos Carpool Meter

Pleos Connect용 재미용 카풀 미터 앱. 결제·정산 기능은 없습니다.

## 구현된 기능

- FusedLocation 1.1.3 위치 수신과 근사 속도·거리 계산, 권한/수신 공백 처리
- NaviHelper 3.0.5 현재 주소 요청, 시·군 단위 행정구역 이름 식별
- 서울 중형 일반 요금제와 직접 설정: 기본요금·거리·저속 시간·심야 복수 구간·시외율·중복 적용
- 기본요금, 거리/저속 시간 병산, 심야 자동 할증과 시외 자동/수동 모드
- 메인 시외 버튼 조작 시 설정도 수동 모드로 전환
- 증액 단위에 연동되는 숫자와 게이지: 200원 단위의 64%는 `128`
- 미터 시작·일시정지/재개·종료, 말/고양이/토끼/오리 단색 픽셀 애니메이션과 LED 테두리
- 차량 시스템 주야간 연동 및 수동 화면 모드
- 주행 내역 목록/상세, 운행별 요금표 스냅샷 및 할증 변경 이력 저장
- 진행 운행 임시 저장과 일시정지 상태 복원

## 검증 범위

APK 빌드·서명, 계산/상태 테스트, Robolectric Android 화면·설정 동기화·SQLite 저장·복원 테스트와 lint를 검증합니다. 실제 Pleos 서비스 데이터 수신과 실차/전용 AVD 실행은 **미검증**입니다. 현재 환경은 가상화 장치가 없고 Pleos 이미지를 설치할 디스크 용량도 부족합니다.

NaviHelper의 실제 응답에는 주소 문자열이 있지만 공식 행정구역 코드/측정 시각은 없습니다. 주소 형식이 불명확하거나 수신이 끊기면 자동 시외 판단을 보류합니다. 행정구역 기준은 실제 택시 사업구역의 모든 예외를 표현하지 않습니다.

앱이 화면에서 벗어나면 운행을 일시정지합니다. 복귀 후 직접 다시 시작해야 하며 백그라운드 위치 수신을 전제로 과금하지 않습니다. 재미용 위치 추정이며 공식 택시 계량기의 정확도를 보장하지 않습니다.

## 빌드 및 설치

JDK 17, Android SDK Platform 34, Build Tools 35.0.0이 필요합니다. `ANDROID_HOME` 또는 `local.properties`의 `sdk.dir`을 설정합니다.

```sh
scripts/check.sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n ai.pleos.smoke/.MainActivity
```

Gradle 8.13 / Android Gradle Plugin 8.9.2 / minSdk 28 / targetSdk 34. 공식 Pleos Maven 저장소 접근이 필요합니다. Robolectric 첫 실행은 테스트용 Android 런타임을 다운로드합니다.

## 사용 흐름

1. 설정에서 서울 요금제 또는 직접 설정을 선택합니다.
2. 위치/내비 권한을 허용합니다. 설정의 **수신 진단**에서 실제 응답을 확인할 수 있습니다.
3. 자동 시외 모드에서는 출발 행정구역 확인 후 시작합니다. 내비 미지원/주소 불명확 환경에서는 설정에서 자동 모드를 끄고 수동 시외 버튼을 사용합니다.
4. 시작 후 거리·저속 시간에 따라 미터가 증가합니다. 정차해도 캐릭터는 느리게 달립니다.
5. 종료하면 주행 내역에 요약과 요금제·할증 이력을 저장합니다.

## 문서와 산출물

- [요구사항](docs/product-spec.md)
- [할증 상태 규칙](docs/surcharge-behavior.md)
- [요금표 공식 출처](docs/tariff-sources.md)
- [위치 SDK 조사 및 실제 수신 검증 한계](docs/location-investigation.md)
- [로컬 Pleos 실행 체크리스트](docs/pleos-runtime-checklist.md)
- [기존 화면 설계 초안](docs/previews/carpool-meter.html): 예시 데이터로 탐색한 HTML fragment. 현재 Android 구현과 구분합니다.
- `artifacts/carpool-meter-debug.apk`: 현재 미터 앱 디버그 APK
- `artifacts/vehicle-sdk-smoke-debug.apk`: 초기 SDK 연동 확인용 APK, 기록용

## 공식 문서

- [에뮬레이터 설정](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
- [Vehicle SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro)
- [FusedLocation SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro)
- [NaviHelper SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)
