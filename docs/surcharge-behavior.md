# Carpool meter: surcharge behavior

Implemented in the Android app with unit/UI tests and checked on the Pleos Connect Emulator v2.1.0. Real-vehicle behavior remains unverified. Settings and history are only reachable in Park (see docs/product-spec.md).

## Out-of-area (시외) surcharge: manual only

- The surcharge is applied only by the user. The meter has one on/off button ("시외할증 켜기/끄기") and a visible applied state. The header line shows the percentage when applied.
- There is no automatic detection. NaviHelper address reception, the address parser and the automatic-mode setting were removed: the emulator's navigation service returned district-level addresses without a city or province (for example "수정구 시흥동"), so a reliable administrative boundary could not be derived, and every region's boundary rules differ (see docs/tariff-sources.md).
- A new trip always starts with the surcharge off. The applied state is saved with the trip checkpoint and restored with a paused trip.
- Apply rate changes prospectively; do not recalculate previously accumulated fare. Rate and night/out-of-area stacking come from the selected tariff (all built-in regions assume additive stacking).
- Record surcharge transitions and time with the trip history. Older history entries that contain an automatic/manual "mode" field are still readable; the field is ignored.

## Fare increment display

Show accumulated monetary progress toward the active fare increment, without a unit or percent sign. Display floor(incrementAmount * normalizedProgress). At 64% gauge fill, 100-won increments display 64 and 200-won increments display 128. On completion, add the increment to total fare and reset the gauge and sub-counter to zero. The tariff determines the increment.

The base-distance allowance is consumed first by both distance and low-speed time units, so a stationary trip stays at the base fare for roughly the first six minutes (Seoul: about 12 units of 30 seconds) before the fare rises.

## Night surcharge

Automatically apply the selected tariff's time bands using local civil time, independent of vehicle display day/night theme. Bands may cross midnight and must not overlap. Re-evaluate when crossing time-band boundaries; apply changes prospectively. The HTML preview remains illustrative; the Android engine implements this clock-based calculation.
