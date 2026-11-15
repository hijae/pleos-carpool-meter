# Pleos Carpool Meter

Pleos Connect용 재미용 카풀 미터 앱. 결제·정산 기능은 없습니다.

## 구현된 기능

- **차량 값 기반 속도·거리**: `android.car`의 `PERF_VEHICLE_SPEED`(권한 `android.car.permission.CAR_SPEED`)를 시간으로 적분해 거리를 계산합니다. 권한이 없거나 속도가 오지 않으면 Pleos Vehicle SDK 오도미터(`pleos.car.permission.CAR_DRIVE`)로 대체하며, 둘 중 하나만 사용합니다. 위치(GPS) 기반 입력은 사용하지 않습니다. 첫 실행 때 속도 권한을 한 번 요청합니다.
- 지역별 기본 요금표: 서울(기본)·부산·대구·대전·인천·광주·울산·세종 중형 일반과 직접 설정. 지역별 출처와 미확인 항목은 [요금표 공식 출처](docs/tariff-sources.md)
- 기본요금, 거리/저속 시간 병산, 심야 자동 할증(요금제의 시간 구간)
- **시외할증은 완전 수동**: 메인의 버튼으로만 켜고 끄며, 새 운행은 꺼진 상태로 시작
- 증액 단위에 연동되는 숫자와 게이지: 200원 단위의 64%는 `128`
- 미터 시작·일시정지/재개·종료, 말/고양이/토끼/오리 단색 픽셀 애니메이션
- 주야간은 차량(시스템) 모드를 그대로 따르며 앱 설정으로 바꾸지 않습니다. Pleos Connect UI 가이드의 색상 토큰·버튼·스위치·타이포그래피 단계 적용
- 시스템 팝업은 전체 화면 기준으로 그려져 반쪽 패널에서 버튼이 잘리므로, 운행 종료 확인과 오류 안내는 앱 화면 안에서 보여줍니다
- 큰 화면(2560×1440 @160dpi)에서 앱 전체 배율 자동 확대
- 주행 중에도 미터 화면을 사용할 수 있도록 `distractionOptimized` 선언. 설정·주행 내역은 시스템이 주행 중 차단하므로 **P 기어에서만** 버튼을 켜서, 주행 중 눌러 미터가 가려지고 운행이 일시정지되는 일을 막습니다(기어는 Pleos Vehicle SDK `Gear`로 읽고, 읽을 수 없으면 켜 둠)
- 주행 내역 목록/상세, 운행별 요금표 스냅샷 및 할증 변경 이력 저장
- 진행 운행 임시 저장과 일시정지 상태 복원
- 설정에서 "미터로 돌아가기"(또는 뒤로가기)를 누르면 바로 저장. 별도 저장 버튼 없음

## 검증 범위

- 자동 검증: APK 빌드·서명, 계산/상태/요금표 테스트, Robolectric 화면·저장·복원 테스트, lint. 결과는 [검증 결과](docs/verification.md)
- **Pleos Connect Emulator v2.1.0(arm64) 실행 확인**: Apple Silicon Mac, AVD 2560×1440. 앱 설치·실행, 설정·주행 내역 화면, 가상 콘솔 주행(D 기어·속도)에서 미터 표시를 확인했습니다. 속도 경로(36km/h로 약 30초 → 0.29km)와 오도미터 경로(100.000→100.250km → 0.25km)로 거리가 계산되는 것도 확인했습니다. 절차는 [실행 체크리스트](docs/pleos-runtime-checklist.md)
- 오도미터: Vehicle SDK `PERF_ODOMETER`는 km 단위 float로 전달됩니다. 에뮬레이터에서 주입한 값이 그대로 읽혔지만 실차의 정밀도·갱신 주기는 **미검증**입니다. 속도를 쓸 수 없을 때의 대체 입력으로 쓰이며, 1초마다 읽어 거리 증가분과 10초 구간 평균 속도를 만듭니다.
- 속도: Vehicle SDK(최신 2.3.2)에 차속 API가 없어 `android.car` `CarPropertyManager`로 `PERF_VEHICLE_SPEED`를 읽습니다. 에뮬레이터에서 허용 후 초당 약 6.6회 갱신되고 주입한 속도가 그대로 읽혔습니다. 권한은 dangerous(런타임 허용, 창 문구는 "위치 정보")입니다. 오도미터를 `android.car`로 읽는 데는 `CAR_MILEAGE`(signature|privileged)가 필요해 일반 앱은 불가능합니다. **실차 검증은 아직 없습니다.**
- 실차를 쓸 수 없어 검증은 에뮬레이터까지입니다. 실차 동작과 Pleos의 앱 카테고리·지역별 주행 규제 적용 여부는 **확인하지 못했습니다**. 에뮬레이터는 실차의 모든 주행 제한을 재현하지 않을 수 있습니다.

시외할증을 자동 판정하지 않으므로 내비(NaviHelper) 주소 수신은 사용하지 않고, 위치 기반 입력도 제거했습니다. 속도·오도미터의 NaN·무한대·음수 값은 건너뛰고(오도미터가 거꾸로 가면 초기화로 보고 요금에 넣지 않음) 그 외 범위·급증 같은 비정상 판정은 두지 않습니다. 수신이 끊기거나 10초 이상 공백이면 요금 누적만 보류합니다. 앱이 화면에서 벗어나면 운행을 일시정지하며, 복귀 후 직접 다시 시작해야 합니다. 재미용 계산이며 공식 택시 계량기의 정확도를 보장하지 않습니다.

## 빌드 및 설치

JDK 17, Android SDK Platform 34, Build Tools 35.0.0이 필요합니다. Pleos 에뮬레이터 이미지 설치와 AVD 설정은 아래 "에뮬레이터"를 참고하세요. `ANDROID_HOME` 또는 `local.properties`의 `sdk.dir`을 설정합니다.

```sh
scripts/check.sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n ai.pleos.smoke/.MainActivity
```

Gradle 8.13 / Android Gradle Plugin 8.9.2 / minSdk 28 / targetSdk 34. 공식 Pleos Maven 저장소 접근이 필요하며 별도 로그인은 필요하지 않았습니다. Robolectric 첫 실행은 테스트용 Android 런타임을 다운로드합니다.

## 에뮬레이터

공식 안내: [에뮬레이터 설정](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator). Apple Silicon Mac에서는 arm64 이미지를 사용합니다.

- 이미지: `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp_car_arm64_emulator.zip`(약 4.7GB, 압축 해제 약 19.6GB). SDK 라이선스 동의가 필요합니다.
- Pleos 이미지가 정의한 기기는 12.8인치 2560×1440 160dpi입니다. 일반 자동차 프로파일(1024×768)로 만들면 해상도가 맞지 않습니다.
- RAM 4GB, 콜드 부트, 호스트 GPU.
- `adb`는 Android SDK의 `platform-tools`를 사용하세요. Homebrew의 adb가 멈추는 경우가 있었습니다.
- 가상 콘솔(주행·오도미터 주입): `adb forward tcp:8088 tcp:8088` 후 브라우저로 `http://127.0.0.1:8088/`. 브라우저 없이 쓰려면 1초마다 `POST /browser/session?client=<id>`를 보내 세션을 유지하고 `PUT /vehicle/gear`, `/vehicle/controls`, `/vehicle/odometer`를 호출합니다.

## 사용 흐름

1. 설정에서 요금 지역(기본 서울) 또는 직접 설정을 선택하고 "미터로 돌아가기"로 저장합니다.
2. 첫 실행 때 나오는 차량 속도 권한(`android.car.permission.CAR_SPEED`) 창에서 허용합니다. 허용하지 않으면 오도미터로 계산합니다.
3. 시작 후 거리·저속 시간에 따라 미터가 증가합니다. 기본요금 구간은 거리와 저속 시간으로 먼저 소진되며, 정차해도 캐릭터는 느리게 달립니다.
4. 시외 구간에서는 **시외할증 켜기** 버튼을 직접 누릅니다.
5. 종료하면 주행 내역에 요약과 요금제·할증 이력을 저장합니다.

## 문서와 산출물

- [요구사항](docs/product-spec.md)
- [할증 상태 규칙](docs/surcharge-behavior.md)
- [요금표 공식 출처](docs/tariff-sources.md)
- [위치·내비 SDK 조사 기록](docs/location-investigation.md) (현재 앱은 사용하지 않음)
- [로컬 Pleos 실행 체크리스트](docs/pleos-runtime-checklist.md)
- [기존 화면 설계 초안](docs/previews/carpool-meter.html): 예시 데이터로 탐색한 HTML fragment. 현재 Android 구현과 구분합니다.
- `artifacts/carpool-meter-debug.apk`: 현재 미터 앱 디버그 APK
- `artifacts/vehicle-sdk-smoke-debug.apk`: 초기 SDK 연동 확인용 APK, 기록용

## 공식 문서

- [에뮬레이터 설정](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
- [Vehicle SDK](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro)
