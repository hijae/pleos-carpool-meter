package ai.pleos.smoke.core;
import org.junit.Test;
import static org.junit.Assert.*;
public class AdministrativeAreaTest {
    @Test public void districtsInSameCityHaveSameKey() {
        assertEquals(AdministrativeArea.fromAddress("서울 강남구 테헤란로 1"),AdministrativeArea.fromAddress("서울특별시 종로구 1"));
        assertEquals("경기도/수원시",AdministrativeArea.fromAddress("경기도 수원시 영통구 광교로 1"));
        assertNotEquals(AdministrativeArea.fromAddress("경기도 수원시"),AdministrativeArea.fromAddress("경기도 용인시"));
    }
    @Test public void unknownAddressesDoNotGuessArea() {
        assertNull(AdministrativeArea.fromAddress("")); assertNull(AdministrativeArea.fromAddress(null));
        assertNull(AdministrativeArea.fromAddress("경기도")); assertNull(AdministrativeArea.fromAddress("123 Main Street"));
        assertEquals("강원특별자치도/춘천시",AdministrativeArea.fromAddress("대한민국 강원도 춘천시"));
    }
}
