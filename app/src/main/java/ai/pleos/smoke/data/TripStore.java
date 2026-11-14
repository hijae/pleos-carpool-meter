package ai.pleos.smoke.data;

import ai.pleos.smoke.MeterApplication;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public final class TripStore extends SQLiteOpenHelper {
  public record Trip(
      String id,
      long start,
      long end,
      long duration,
      double meters,
      long won,
      String tariff,
      String events) {}

  public TripStore(Context context) {
    super(context.getApplicationContext(), "trips.db", null, 1);
  }

  public void onCreate(SQLiteDatabase db) {
    db.execSQL(
        "CREATE TABLE trips(id TEXT PRIMARY KEY,start INTEGER NOT NULL,end INTEGER NOT"
            + " NULL,duration INTEGER NOT NULL,meters REAL NOT NULL,won INTEGER NOT NULL,tariff"
            + " TEXT NOT NULL,events TEXT NOT NULL)");
  }

  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    throw new IllegalStateException("지원하지 않는 기록 DB 버전");
  }

  public void save(MeterApplication s) {
    ContentValues v = new ContentValues();
    v.put("id", s.tripId);
    v.put("start", s.startedWall);
    v.put("end", System.currentTimeMillis());
    v.put("duration", s.duration());
    v.put("meters", s.engine.distanceMeters());
    v.put("won", s.engine.totalWon());
    v.put("tariff", TariffCodec.encode(s.engine.tariff).toString());
    v.put("events", s.events.toString());
    long row =
        getWritableDatabase()
            .insertWithOnConflict("trips", null, v, SQLiteDatabase.CONFLICT_IGNORE);
    // Duplicate finish is idempotent. Any other SQLite failure is thrown to the caller.
    if (row == -1 && get(s.tripId) == null) throw new IllegalStateException("주행 내역 저장 실패");
  }

  private Trip read(Cursor c) {
    return new Trip(
        c.getString(0),
        c.getLong(1),
        c.getLong(2),
        c.getLong(3),
        c.getDouble(4),
        c.getLong(5),
        c.getString(6),
        c.getString(7));
  }

  public List<Trip> list() {
    ArrayList<Trip> result = new ArrayList<>();
    try (Cursor c =
        getReadableDatabase().query("trips", null, null, null, null, null, "start DESC", "100")) {
      while (c.moveToNext()) result.add(read(c));
    }
    return result;
  }

  public Trip get(String id) {
    try (Cursor c =
        getReadableDatabase().query("trips", null, "id=?", new String[] {id}, null, null, null)) {
      return c.moveToFirst() ? read(c) : null;
    }
  }
}
