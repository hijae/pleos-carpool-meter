# Pleos 실행 검증 체크리스트

## 준비

- JDK 17, Android SDK Platform 34, Build Tools 35.0.0, 네트워크 연결
- Pleos Connect Emulator v2.1.0 이미지와 AVD(2560×1440 @160dpi, RAM 4GB, 콜드 부트). 이미지 압축 해제 약 19.6GB이므로 35GB 이상의 여유 공간 권장. 설정 방법은 README의 "에뮬레이터"
- Vehicle Service >= 2.3.13(SDK 병합 매니페스트 기준)
- `ANDROID_HOME` 또는 local.properties의 sdk.dir 설정. `adb`는 SDK platform-tools 사용
- `scripts/check.sh` 실행으로 테스트·lint·APK 생성 및 서명 검증

## 설치

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n ai.pleos.smoke/.MainActivity
```

## 확인 항목

에뮬레이터(✅)와 실차를 구분해 기록합니다.

- [x] 최초 실행 시 미터/설정/내역 화면이 정상 표시 (에뮬레이터)
- [x] 속도 권한 허용 시 `PERF_VEHICLE_SPEED`로 거리 계산 (에뮬레이터: 36km/h 약 30초 → 0.29km). 실차 미확인
- [x] 속도 권한 거부("허용하지 않고 다시 묻지 않음") 시 오도미터로 계산, 상태 줄에 "차량 속도 권한 없음 · 오도미터 …" 표시 (에뮬레이터: 100.000→100.250km → 0.25km)
- [ ] 오도미터 단위·정밀도·갱신 주기(실차). 에뮬레이터는 km float로 12.345를 그대로 전달
- [x] 차속 `PERF_VEHICLE_SPEED`를 `android.car`로 읽음, CAR_SPEED 런타임 허용 창 확인 (에뮬레이터, 약 6.6Hz). 실차 확인 필요
- [x] 오도미터를 `android.car`로 읽으면 CAR_MILEAGE 때문에 거부됨(SDK로만 가능)
- [ ] 메인 시외 버튼 조작 시 요율이 켜지고 꺼짐, 새 운행은 꺼진 상태로 시작
- [ ] 선택한 요금표에 따른 기본/거리/저속 시간 증액, 100원·200원 단위 숫자 확인
- [ ] 심야 시간대 진입/이탈·자정·시외 중복·일시정지 동안 할증 전환 확인
- [ ] 정차에서도 캐릭터가 느리게 달리고 주행 시 빨라짐
- [x] 차량 주야간 전환 따라가기: `adb shell cmd uimode night yes|no`로 시스템 모드를 바꿔 어두운/밝은 테마 확인 (에뮬레이터)
- [x] 가로 화면 가독성, 전체 폭과 반쪽 패널에서의 레이아웃 (에뮬레이터)
- [x] 운행 종료 확인이 앱 안에서 보이고 종료 시 주행 내역에 저장 (에뮬레이터)
- [x] 캐릭터 변경(고양이)이 메인에 반영 (에뮬레이터)
- [x] 설정·주행 내역 버튼이 P 기어에서만 켜짐, D 기어에서 꺼짐 (에뮬레이터, 가상 콘솔). 실차 확인 필요
- [x] 주행(D 기어, 40km/h) 중 메인 화면이 차단되지 않음 (에뮬레이터, 가상 콘솔). Pleos 앱 카테고리·지역별 주행 규제는 실차 확인 필요
- [ ] 화면 이탈 시 일시정지, 복귀 후 수동 재개. 화면 재생성 시 운행 유지
- [ ] 종료 기록 1건 저장, 재시작 후 목록/상세와 요금표/할증 이력 유지
- [ ] 프로세스 종료 후 진행 운행은 일시정지 상태로 복원; 누락 시간 요금 누적 없음
- [ ] 설정에서 지역 변경 후 새 운행에 해당 요금표가 적용됨

## 가상 콘솔로 주행시키기

```sh
adb forward tcp:8088 tcp:8088
# 1초마다 세션 신호를 보내야 콘솔이 활성화됨 (브라우저로 연 경우 자동)
while true; do curl -sS -X POST "http://127.0.0.1:8088/browser/session?client=cli" >/dev/null; sleep 1; done &
curl -X PUT "http://127.0.0.1:8088/vehicle/gear?gear=Drive"
curl -X PUT "http://127.0.0.1:8088/vehicle/controls?speed=40&accelerator=10&brake=0&steering=0"
curl -X PUT "http://127.0.0.1:8088/vehicle/odometer?traveled_distance_km=12.345"
```

복귀: `gear=Park`, `speed=0`. 콘솔의 오도미터 조절 UI는 1km 단위이지만 API는 소수값을 받습니다.

검증 결과에는 사용한 이미지·차량 모델·서비스 버전·APK 커밋과 실제 수신 로그를 함께 남긴다. 키/좌표 전체 경로 등 불필요한 개인정보는 이슈에 올리지 않는다.

## 현재 환경

Apple Silicon Mac에서 Pleos arm64 에뮬레이터를 실행해 위 표시 항목을 확인했다. 실차는 사용하지 않았다.
