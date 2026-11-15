package dev.hijae.carpoolmeter.data;

import android.car.Car;
import android.car.VehiclePropertyIds;
import android.car.hardware.CarPropertyValue;
import android.car.hardware.property.CarPropertyManager;
import android.content.Context;
import java.util.function.Consumer;

/**
 * AAOS CarPropertyManager PERF_VEHICLE_SPEED (m/s). Kept in its own class so devices without the
 * android.car library never load it. Needs android.car.permission.CAR_SPEED (dangerous).
 */
@SuppressWarnings({"rawtypes", "deprecation"})
final class CarSpeedBackend {
  private final Car car;
  private final CarPropertyManager manager;
  private final CarPropertyManager.CarPropertyEventCallback callback;

  CarSpeedBackend(Context context, Consumer<Float> sink) {
    Car created = Car.createCar(context.getApplicationContext());
    if (created == null) throw new IllegalStateException("Car 연결 실패");
    try {
      if (!created.isConnected()) throw new IllegalStateException("Car 연결 안 됨");
      CarPropertyManager found =
          (CarPropertyManager) created.getCarManager(Car.PROPERTY_SERVICE);
      if (found == null) throw new IllegalStateException("CarPropertyManager 없음");
      callback =
          new CarPropertyManager.CarPropertyEventCallback() {
            public void onChangeEvent(CarPropertyValue value) {
              if (value.getPropertyId() != VehiclePropertyIds.PERF_VEHICLE_SPEED) return;
              Object speed = value.getValue();
              if (speed instanceof Float f) sink.accept(f);
            }

            public void onErrorEvent(int propertyId, int areaId) {}
          };
      found.registerCallback(
          callback, VehiclePropertyIds.PERF_VEHICLE_SPEED, CarPropertyManager.SENSOR_RATE_UI);
      manager = found;
      car = created;
    } catch (RuntimeException e) {
      try {
        created.disconnect();
      } catch (Throwable ignored) {
      }
      throw e;
    }
  }

  void close() {
    try {
      manager.unregisterCallback(callback);
    } catch (Throwable ignored) {
    }
    try {
      car.disconnect();
    } catch (Throwable ignored) {
    }
  }
}
