package edu.ap.mapsaver;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MapSQLiteHelper extends SQLiteOpenHelper {

  private static final String DATABASE_NAME = "mapsaver.db";
  private static final String TABLE_ZONES = "zones";
  private static final int DATABASE_VERSION = 15;

  public MapSQLiteHelper(Context context) {
	  super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
	  String CREATE_USERS_TABLE = "CREATE TABLE " + TABLE_ZONES + "(_id INTEGER PRIMARY KEY, tariefkleur STRING, tariefzone STRING, geometry STRING)";
      db.execSQL(CREATE_USERS_TABLE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
	  db.execSQL("DROP TABLE IF EXISTS " + TABLE_ZONES);
      onCreate(db);
  }

    public ArrayList<Zone> getAllZones() {
        ArrayList allZones = new ArrayList<Zone>();
        SQLiteDatabase db = this.getReadableDatabase();
        //int count = db.rawQuery("select * from " + TABLE_ZONES, null).getCount();
        //Log.d("edu.ap.mapsaver", "Count : " + count);
        Cursor cursor = db.rawQuery("select * from " + TABLE_ZONES, null);
        if (cursor.moveToFirst()) {
            do {
                String tariefkleur = cursor.getString(1);
                String tariefzone = cursor.getString(2);
                String geometry = cursor.getString(3);
                allZones.add(new Zone(tariefkleur, tariefzone, geometry));
            } while (cursor.moveToNext());
        }

        return allZones;
    }
  
  public void saveZones(JSONArray allZones) {
      SQLiteDatabase db = this.getWritableDatabase();
          for (int i = 0; i < allZones.length(); i++) {
              try {
                  JSONObject obj = (JSONObject) allZones.get(i);
                  String tariefzone = obj.getString("tariefzone");
                  String tariefkleur = obj.getString("tariefkleur");
                  String geometry = obj.getString("geometry");

                  ContentValues values = new ContentValues();
                  values.put("tariefkleur", tariefkleur);
                  values.put("tariefzone", tariefzone);
                  values.put("geometry", geometry);

                  db.insert(TABLE_ZONES, null, values);
              }
              catch(Exception ex) {
                  Log.e("edu.ap.mapsaver", ex.getMessage());
              }
          }
          db.close();
      }
  }