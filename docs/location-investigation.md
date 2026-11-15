# 위치 SDK 검증 기록 (조사 기록)

> 위치 기반 속도·거리와 NaviHelper 연동은 제거했다. 현재는 차량 속도(`PERF_VEHICLE_SPEED`)를 쓰고 오도미터를 대체 입력으로 쓴다(README, product-spec 참고). 아래는 당시 조사 내용이다.

## FusedLocation 1.1.3

공식 AAR의 공개 API에는 좌표·heading·confidence·timestampMillis가 있고 속도 필드는 없다. 위치 변화/시간으로 추정한다. 신뢰도 값의 단위/범위를 추측해 임의 임계값을 적용하지 않고 진단에 표시한다. 현재 필터는 3m 미만 샘플 이동을 무시하고 10초 초과 공백을 연결하지 않는 보수적인 근사치다. 정차 및 저속 실제 수신으로 조정해야 한다.

## NaviHelper 3.0.5 / navi-data 3.0.4 (조사 기록, 현재 앱은 사용하지 않음)

> 시외할증을 완전 수동으로 바꾸면서 NaviHelper 연동과 주소 파서를 제거했다. 아래는 조사 당시 기록이다. 에뮬레이터는 `수정구 시흥동`처럼 시·도 없이 구·동만 있는 주소를 반환해 파서가 판정하지 못했다.

실제 공개 CurrentLocationInfo: latitude, longitude, heading, address, routeId, currentRouteIndex. **행정구역 코드와 측정 시각은 없다.** NAVI_ROUTE 권한이 필요하며 연결 콜백 성공 후 리스너를 등록하고 getCurrentLocationInfo를 5초마다 요청한다.

주소가 한국 시도/시군 형식인 경우에만 행정구역 이름 키로 정규화한다. 이 키는 공식 행정구역 코드가 아니다. 광역시는 구가 달라도 같은 키, 도 단위는 시/군까지 포함한다. 주소가 비어 있거나 알 수 없는 형식이면 자동 할증 판정을 보류한다. 응답 수신 시각은 측정 시각이 아니므로 내비의 캐시 동작도 실기 확인 필요.

주소가 없을 때의 대안은 역지오코딩 또는 행정구역 폴리곤이다. 현재 앱은 외부 API 키/서버 의존성을 새로 만들지 않고 수동 할증을 유지한다. 추가 경계 데이터/역지오코딩 공급자는 대상 환경의 실제 주소 제공 여부를 확인한 뒤 선정한다.

## 남은 외부 검증

- 실제 위치 timestamp epoch/갱신 주기와 confidence 의미 확인
- NAVI_ROUTE 권한 부여 가능 여부와 내비 서비스 주소 응답 확인
- 시도/시군 경계 이동 및 복귀 응답, 캐시/끊김 동작 확인
- 오도미터 단위·정밀도·갱신 주기 확인

이후 Apple Silicon Mac의 Pleos 에뮬레이터 v2.1.0에서 FusedLocation 수신과 오도미터 읽기를 확인했다(docs/verification.md). 실차 확인은 남아 있다. 에뮬레이터에서 오도미터 `PERF_ODOMETER`는 km 단위 float로 전달되며 차속 API는 Vehicle SDK에 없다.
