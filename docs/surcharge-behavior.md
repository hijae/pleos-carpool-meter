# Carpool meter: surcharge behavior

Implemented in the Android app with unit/UI tests; live Pleos service reception remains unverified.

- Settings: automatic out-of-area surcharge toggle.
- Meter: one out-of-area surcharge on/off button and visible applied state. Explicit automatic/manual mode selection exists only in Settings; no resume-automatic button on the meter. The main surcharge toggle implicitly switches to manual mode.
- Automatic detection: use NaviHelper.getCurrentLocationInfo as the intended location information source. The inspected SDK returns address text, not an administrative code. Poll the request/response API every 5 seconds, conservatively normalize Korean city/county names, and withhold automatic changes when the address cannot be interpreted. Reverse geocoding/polygon integration is not currently enabled.
- Baseline: administrative area at trip start, distinct from the selected tariff region. Freeze its administrative code for that trip.
- Use the same administrative level on both sides; a district change within the same city should not accidentally trigger a city-level rule.
- Leaving the baseline area enables the surcharge; returning disables it. Debounce changes to avoid boundary jitter. Missing/stale location does not imply a boundary crossing.
- Only two out-of-area modes: automatic and manual. Main-screen on/off action switches the setting to manual and changes the applied state. It remains manual until automatic is enabled in Settings; do not reset mode on a new trip. Disabling automatic in Settings preserves the current applied state.
- Apply rate changes prospectively; do not recalculate previously accumulated fare. Rate and night/out-of-area stacking come from the selected tariff.
- Record surcharge transitions, time, source and tariff version with the trip history.
- Preview contains simulated area transitions, static fare and no live NaviHelper connection.

- Next fare increment: show accumulated monetary progress toward the active fare increment, without a unit or percent sign. Display floor(incrementAmount * normalizedProgress). At 64% gauge fill, 100-won increments display 64 and 200-won increments display 128. On completion, add the increment to total fare and reset the gauge and sub-counter to zero. The tariff determines the increment; preview controls are illustrative only.

- Night surcharge: automatically apply the selected tariff’s time bands using local civil time, independent of vehicle display day/night theme. Re-evaluate when crossing time-band boundaries; apply changes prospectively. The HTML preview remains illustrative; the Android engine implements this clock-based calculation.
