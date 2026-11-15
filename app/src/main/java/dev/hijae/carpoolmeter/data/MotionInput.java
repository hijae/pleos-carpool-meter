package dev.hijae.carpoolmeter.data;

import android.content.Context;

/**
 * Motion input for the meter: vehicle speed when android.car.permission.CAR_SPEED is granted and
 * the vehicle reports speed, otherwise odometer distance. Exactly one source runs at a time so
 * fare is never counted twice. There is no location-based input.
 */
public final class MotionInput {
  private final VehicleSpeedSource speed;
  private final OdometerSource odometer;
  private boolean usingSpeed;

  public MotionInput(Context context, VehicleSpeedSource.Observer observer) {
    speed = new VehicleSpeedSource(context, observer, this::speedUnavailable);
    odometer = new OdometerSource(context, observer);
  }

  public void start() {
    usingSpeed = speed.start();
    if (!usingSpeed)
      odometer.start(speed.isPermitted() ? "차량 속도를 읽을 수 없음" : "차량 속도 권한 없음");
  }

  public void stop() {
    speed.stop();
    odometer.stop();
    usingSpeed = false;
  }

  public boolean usesVehicleSpeed() {
    return usingSpeed;
  }

  private void speedUnavailable() {
    usingSpeed = false;
    odometer.start("차량 속도 없음");
  }
}
