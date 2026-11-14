package ai.pleos.smoke.core;

import java.util.Map;

/** Conservative Korean address parser. Keys are normalized names, not official government codes. */
public final class AdministrativeArea {
    private static final Map<String,String> ALIASES = Map.ofEntries(
        Map.entry("서울", "서울특별시"), Map.entry("부산", "부산광역시"), Map.entry("대구", "대구광역시"),
        Map.entry("인천", "인천광역시"), Map.entry("광주", "광주광역시"), Map.entry("대전", "대전광역시"),
        Map.entry("울산", "울산광역시"), Map.entry("세종", "세종특별자치시"), Map.entry("경기", "경기도"),
        Map.entry("강원", "강원특별자치도"), Map.entry("강원도", "강원특별자치도"),
        Map.entry("충북", "충청북도"), Map.entry("충남", "충청남도"),
        Map.entry("전북", "전북특별자치도"), Map.entry("전라북도", "전북특별자치도"), Map.entry("전남", "전라남도"),
        Map.entry("경북", "경상북도"), Map.entry("경남", "경상남도"), Map.entry("제주", "제주특별자치도"));
    private AdministrativeArea() {}
    public static String fromAddress(String address) {
        if (address == null || address.isBlank()) return null;
        String[] words = address.trim().split("\\s+");
        int start = words[0].equals("대한민국") ? 1 : 0;
        if (start >= words.length) return null;
        String province = ALIASES.getOrDefault(words[start], words[start]);
        if (province.equals("세종특별자치시") || province.equals("서울특별시")
                || province.matches("(부산|대구|인천|광주|대전|울산)광역시")) return province;
        if (!province.matches("경기도|강원특별자치도|충청[남북]도|전북특별자치도|전라남도|경상[남북]도|제주특별자치도")) return null;
        if (words.length <= start+1 || !words[start+1].matches("[가-힣]+[시군]")) return null;
        return province + "/" + words[start+1];
    }
}
