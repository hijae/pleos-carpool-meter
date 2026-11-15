package ai.pleos.smoke.core;

import ai.pleos.smoke.core.Tariff.NightBand;
import ai.pleos.smoke.core.Tariff.Stacking;
import java.util.List;

/**
 * Built-in regional medium-taxi tariffs. Every figure comes from the sources noted per region
 * (docs/tariff-sources.md). Unverified values are called out in {@code note}; they are never
 * borrowed from another city. The low-speed threshold is derived as distance unit / time unit,
 * which reproduces Seoul's published 15.72 km/h.
 */
public final class TariffPresets {
  public record Preset(String id, Tariff tariff, String note) {}

  public static final String DEFAULT_ID = "seoul";

  private static final List<Preset> ALL =
      List.of(
          new Preset(
              "seoul",
              Tariff.seoul(),
              "서울시 공식 안내 · 2023-02-01 적용 기준\n심야·시외 합산 최대 60%"),
          preset(
              "busan",
              "부산 · 중형 일반",
              "busan-sedan-2023-06-01-v1",
              "https://www.busan.go.kr/nbtnewsBU/1565618",
              "2023-06-01",
              4800,
              2000,
              132,
              33,
              30,
              List.of(
                  new NightBand(1380, 0, 20), new NightBand(0, 120, 30), new NightBand(120, 240, 20)),
              "부산시 보도자료(2023-05-13) 기준\n미확인: 시외 30%는 비공식 자료, 중복 합산 방식"),
          preset(
              "daegu",
              "대구 · 중형 일반",
              "daegu-sedan-2025-02-22-v1",
              "https://www.idaegu.co.kr/news/articleView.html?idxno=489507",
              "2025-02-22",
              4500,
              1700,
              125,
              31,
              35,
              List.of(
                  new NightBand(1380, 0, 20), new NightBand(0, 120, 30), new NightBand(120, 240, 20)),
              "대구시 공고 보도(2025-02) 기준\n미확인: 시간요금 31초는 2023-01 보도 값, 중복 합산 방식"),
          preset(
              "daejeon",
              "대전 · 중형 일반",
              "daejeon-sedan-2026-11-01-v1",
              "https://www.ajunews.com/view/20261028174132460",
              "2026-11-01",
              4800,
              1650,
              125,
              29,
              30,
              List.of(
                  new NightBand(1380, 0, 20), new NightBand(0, 120, 30), new NightBand(120, 240, 20)),
              "대전시 보도(2026-10) 기준\n미확인: 심야·시외 할증은 2026-03 단일 보도, 중복 합산 방식"),
          preset(
              "incheon",
              "인천 · 중형 일반",
              "incheon-sedan-2023-07-01-v1",
              "https://incheon.go.kr/traffic/TR060101/view?curPage=4&nttNo=2042727",
              "2023-07-01",
              4800,
              1600,
              135,
              33,
              30,
              List.of(
                  new NightBand(1320, 1380, 20),
                  new NightBand(1380, 120, 40),
                  new NightBand(120, 240, 20)),
              "인천시 요금 안내(2023-07) 보도 기준\n미확인: 공식 원문 미열람, 중복 합산 방식"),
          preset(
              "gwangju",
              "광주 · 중형 일반",
              "gwangju-sedan-2025-10-22-v1",
              "https://gjkoreaja.com/253100",
              "2025-10-22",
              4800,
              1700,
              132,
              32,
              35,
              List.of(
                  new NightBand(1380, 0, 20), new NightBand(0, 120, 30), new NightBand(120, 240, 20)),
              "광주시 보도(2025-10) 기준\n미확인: 시외 35%는 단일 보도(인접 시·군 40%), 중복 합산 방식"),
          preset(
              "ulsan",
              "울산 · 중형 일반",
              "ulsan-sedan-2025-03-10-v1",
              "https://www.iusm.co.kr/news/articleView.html?idxno=1048976",
              "2025-03-10",
              4500,
              2000,
              125,
              30,
              30,
              List.of(new NightBand(1320, 240, 20)),
              "울산시 안내(2025-03) 보도 기준\n미확인: 공식 원문 미열람, 중복은 합산 방식으로 가정"),
          preset(
              "sejong",
              "세종 · 중형 일반",
              "sejong-sedan-2024-08-01-v1",
              "https://www.sejong.go.kr/bbs/R0079/view.do?nttId=B000000114574Nb1cL1k",
              "2024-08-01",
              4000,
              1400,
              97,
              29,
              30,
              List.of(new NightBand(1320, 240, 30)),
              "세종시 보도자료(2024-07-26) 기준\n미확인: 기본거리 1.4km는 단일 보도, 심야·시외 30%는 합산 수치, 중복 합산 방식"));

  private TariffPresets() {}

  public static List<Preset> all() {
    return ALL;
  }

  public static Preset byId(String id) {
    for (Preset p : ALL) if (p.id().equals(id)) return p;
    return ALL.get(0);
  }

  public static int indexOf(String id) {
    for (int i = 0; i < ALL.size(); i++) if (ALL.get(i).id().equals(id)) return i;
    return 0;
  }

  private static Preset preset(
      String id,
      String name,
      String version,
      String source,
      String date,
      long baseWon,
      double baseMeters,
      double distanceUnit,
      long timeUnitSeconds,
      int outsidePercent,
      List<NightBand> bands,
      String note) {
    double lowSpeed = Math.round(distanceUnit / timeUnitSeconds * 3.6 * 100) / 100.0;
    return new Preset(
        id,
        new Tariff(
            name,
            version,
            source,
            date,
            baseWon,
            baseMeters,
            100,
            distanceUnit,
            timeUnitSeconds * 1000,
            lowSpeed,
            outsidePercent,
            Stacking.ADDITIVE,
            bands,
            10,
            100,
            "Asia/Seoul"),
        note);
  }
}
