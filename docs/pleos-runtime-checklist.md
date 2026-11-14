# Pleos 실행 검증 체크리스트

## 준비

- JDK 17, Android SDK Platform 34, Build Tools 35.0.0, 네트워크 연결
- Pleos 전용 AVD와 실행 가능한 가상화 환경. 시스템 이미지 비압축 약 19.6GB 외에도 AVD/SDK/앱 공간이 필요하므로 35GB 이상의 여유 공간 권장
- Vehicle Service >= 2.3.13. FusedLocation 및 NaviHelper 호환 서비스 버전은 각 SDK의 병합 매니페스트에서 확인
- `ANDROID_HOME` 또는 local.properties의 sdk.dir 설정
- `scripts/check.sh` 실행으로 테스트·lint·APK 생성 및 서명 검증

## 설치

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -W -n ai.pleos.smoke/.MainActivity
```

## 실기에서 확인해야 완료할 항목

- [ ] 최초 실행 시 미터/설정/내역 화면이 정상 표시
- [ ] FUSED_LOCATION, NAVI_ROUTE 권한 요청과 거절 흐름
- [ ] FusedLocation 지원 여부, 실제 좌표 timestamp/갱신 주기/confidence 확인
- [ ] 수신 진단으로 정차·저속·이동·위치 차단·수신 공백 확인
- [ ] 오도미터 단위·정밀도·갱신 주기 확인 후 거리 소스 확정
- [ ] NaviHelper 주소 응답, 주소 파싱 성공 및 실제 위치와 일치 확인
- [ ] 행정구역 이탈 10초 안정화 후 시외 적용, 복귀 시 해제
- [ ] 구역 경계 흔들림과 응답 끊김에 오작동하지 않음
- [ ] 메인 시외 버튼 조작 후 설정 자동 스위치가 꺼짐; 자동 재선택은 설정에서만
- [ ] 선택한 요금표에 따른 기본/거리/저속 시간 증액, 100원·200원 단위 숫자 확인
- [ ] 심야 시간대 진입/이탈·자정·시외 중복·일시정지 동안 할증 전환 확인
- [ ] 정차에서도 캐릭터가 느리게 달리고 주행 시 빨라짐
- [ ] 차량 주야간 전환, 주간/야간 강제 설정, 가로 화면 가독성
- [ ] 화면 이탈 시 일시정지, 복귀 후 수동 재개. 화면 재생성 시 운행 유지
- [ ] 종료 기록 1건 저장, 재시작 후 목록/상세와 요금표/할증 이력 유지
- [ ] 프로세스 종료 후 진행 운행은 일시정지 상태로 복원; 누락 시간 요금 누적 없음

검증 결과에는 사용한 이미지·차량 모델·서비스 버전·APK 커밋과 실제 수신 로그를 함께 남긴다. 키/좌표 전체 경로 등 불필요한 개인정보는 이슈에 올리지 않는다.

## 현재 환경의 한계

이 작업 환경은 /dev/kvm이 없고 전체 디스크가 약 8GB다. Pleos AVD 실제 실행은 불가능해 위 항목은 미확인 상태다. Robolectric Android 화면/저장 통합 테스트는 가능하지만 Pleos 서비스 실제 수신 검증을 대체하지 않는다.
