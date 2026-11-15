package dev.hijae.carpoolmeter.data;

import ai.pleos.playground.vehicle.Vehicle;
import ai.pleos.playground.vehicle.constant.control.VehicleGear;
import ai.pleos.playground.vehicle.listener.GearSelectionListener;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import java.util.function.Consumer;

/**
 * Pleos Vehicle SDK gear selection (pleos.car.permission.CAR_DRIVE). Reports whether the vehicle
 * is in Park, or null when the gear cannot be read.
 */
public final class GearWatcher {
  private final Context context;
  private final Consumer<Boolean> onParkChanged;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private Vehicle vehicle;
  private GearSelectionListener listener;
  private long generation;

  /** @param onParkChanged called on the main thread: true in Park, false otherwise, null unknown */
  public GearWatcher(Context context, Consumer<Boolean> onParkChanged) {
    this.context = context.getApplicationContext();
    this.onParkChanged = onParkChanged;
  }

  public void start() {
    if (vehicle != null) return;
    if (context
        .getPackageManager()
        .queryIntentServices(new Intent("ai.pleos.playground.vehicle.VEHICLE_START"), 0)
        .isEmpty()) {
      onParkChanged.accept(null);
      return;
    }
    long token = ++generation;
    try {
      vehicle = new Vehicle(context);
      vehicle.initialize();
      listener =
          new GearSelectionListener() {
            public void onSelected(VehicleGear gear) {
              publish(token, gear);
            }

            public void onFailed(Exception error) {
              publish(token, null);
            }
          };
      vehicle
          .getGear()
          .getCurrentGearSelectionState(
              gear -> {
                publish(token, gear);
                return kotlin.Unit.INSTANCE;
              },
              error -> {
                publish(token, null);
                return kotlin.Unit.INSTANCE;
              });
      vehicle.getGear().registerGearSelection(listener);
    } catch (Throwable error) {
      stop();
      onParkChanged.accept(null);
    }
  }

  public void stop() {
    generation++;
    if (vehicle != null) {
      try {
        if (listener != null) vehicle.getGear().unregisterGearSelection(listener);
      } catch (Throwable ignored) {
      }
      try {
        vehicle.release();
      } catch (Throwable ignored) {
      }
    }
    vehicle = null;
    listener = null;
  }

  private void publish(long token, VehicleGear gear) {
    handler.post(
        () -> {
          if (token != generation) return;
          onParkChanged.accept(
              gear == null || gear == VehicleGear.GEAR_UNKNOWN
                  ? null
                  : Boolean.valueOf(gear == VehicleGear.GEAR_PARK));
        });
  }
}
