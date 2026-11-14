package ai.pleos.smoke.core;

/** Location-based, approximate motion. Missing samples are never treated as stationary time. */
public final class MotionEstimator {
    public record Sample(double latitude, double longitude, long timestampMillis, float confidence) {}
    public record Result(boolean valid, double speedKmh, double distanceMeters, long intervalMillis,
                         String status) {}
    private Sample previous;
    private double speed;

    public synchronized void reset() { previous = null; speed = 0; }

    public synchronized Result accept(Sample sample, long nowMillis) {
        if (!Double.isFinite(sample.latitude()) || !Double.isFinite(sample.longitude())
                || Math.abs(sample.latitude()) > 90 || Math.abs(sample.longitude()) > 180
                || !Float.isFinite(sample.confidence()) || sample.timestampMillis() <= 0
                || nowMillis - sample.timestampMillis() > 10_000
                || sample.timestampMillis() - nowMillis > 2_000) return invalid("위치가 오래되었거나 유효하지 않음");
        if (previous == null) { previous = sample; return invalid("다음 위치 대기"); }
        long dt = sample.timestampMillis() - previous.timestampMillis();
        if (dt <= 0) return invalid("중복/역순 위치 무시");
        if (dt > 10_000) { previous = sample; speed = 0; return invalid("수신 공백 · 거리 연결 생략"); }
        if (dt < 500) return invalid("위치 간격 대기");
        double distance = distance(previous.latitude(), previous.longitude(), sample.latitude(), sample.longitude());
        if (distance / (dt / 1000.0) > 60) { previous = sample; speed = 0; return invalid("비정상 위치 이동 제외"); }
        previous = sample;
        // Small GPS drift does not advance the fare; this is deliberately an approximate meter.
        if (distance < 3) distance = 0;
        double rawKmh = distance / (dt / 1000.0) * 3.6;
        speed = rawKmh == 0 ? speed * 0.4 : speed * 0.45 + rawKmh * 0.55;
        if (speed < 1) speed = 0;
        return new Result(true, speed, distance, dt, "위치 수신 중");
    }

    private Result invalid(String message) { return new Result(false, speed, 0, 0, message); }
    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double a = Math.pow(Math.sin(Math.toRadians(lat2-lat1)/2), 2)
                + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(Math.toRadians(lon2-lon1)/2), 2);
        return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
    }
}
