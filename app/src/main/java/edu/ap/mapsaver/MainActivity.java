package edu.ap.mapsaver;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;


public class MainActivity extends Activity {

    private TextView searchField;
    private Button searchButton;
    private MapView mapView;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private RequestQueue mRequestQueue;
    private String urlSearch = "http://nominatim.openstreetmap.org/search?q=";
    private String urlZones = "http://datasets.antwerpen.be/v4/gis/paparkeertariefzones.json";
    ArrayList<Zone> allZones = new ArrayList<Zone>();
    MapSQLiteHelper helper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        helper = new MapSQLiteHelper(this);

        // https://github.com/osmdroid/osmdroid/wiki/How-to-use-the-osmdroid-library
        mapView = (MapView)findViewById(R.id.mapview);
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mapView.setBuiltInZoomControls(true);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18);

        // http://code.tutsplus.com/tutorials/an-introduction-to-volley--cms-23800
        mRequestQueue = Volley.newRequestQueue(this);
        searchField = (TextView)findViewById(R.id.search_txtview);
        // disable text suggestions
        searchField.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        searchButton = (Button)findViewById(R.id.search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String searchString = "";
                try {
                    searchString = URLEncoder.encode(searchField.getText().toString(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                JsonArrayRequest jr = new JsonArrayRequest(urlSearch + searchString + "&format=json", new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            hideSoftKeyBoard();
                            JSONObject obj = response.getJSONObject(0);
                            GeoPoint g = new GeoPoint(obj.getDouble("lat"), obj.getDouble("lon"));
                            mapView.getController().setCenter(g);
                        } catch (JSONException ex) {
                            Log.e("edu.ap.mapsaver", ex.getMessage());
                        }
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("edu.ap.mapsaver", error.getMessage());
                    }
                });

                mRequestQueue.add(jr);
            }
        });

        if(!getPreferences()) {
            // A JSONObject to post with the request. Null is allowed and indicates no parameters will be posted along with request.
            JSONObject obj = null;
            // haal alle parkeerzones op
            JsonObjectRequest jr = new JsonObjectRequest(Request.Method.GET, urlZones, obj, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    hideSoftKeyBoard();
                    try {
                        helper.saveZones(response.getJSONArray("data"));
                        setPreferences(true);
                        allZones = helper.getAllZones();
                        Log.d("edu.ap.mapsaver", "Zones saved to DB");
                    }
                    catch (JSONException e) {
                        Log.e("edu.ap.mapsaver", e.getMessage());
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("edu.ap.mapsaver", error.getMessage());
                }
            });
            mRequestQueue.add(jr);
        }
        else {
            allZones = helper.getAllZones();
            Log.d("edu.ap.mapsaver", "Zones retrieved from DB");
        }

        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if(!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ) {
            Toast.makeText(getApplicationContext(), "GPS not enabled!", Toast.LENGTH_SHORT).show();
            // default = meistraat
            mapView.getController().setCenter(new GeoPoint(51.2244, 4.38566));
        }
        else {
            locationListener = new MyLocationListener();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
            mapView.getController().setCenter(new GeoPoint(51.2244, 4.38566));
        }
    }

    private void setPreferences(boolean b) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("db_filled", b);
        editor.commit();
    }

    private boolean getPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPreferences.getBoolean("db_filled", false);
    }

    // http://codetheory.in/android-ontouchevent-ontouchlistener-motionevent-to-detect-common-gestures/
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int actionType = ev.getAction();
        switch (actionType) {
            case MotionEvent.ACTION_UP:
                // A Projection serves to translate between the coordinate system of
                // x/y on-screen pixel coordinates and that of latitude/longitude points
                // on the surface of the earth. You obtain a Projection from MapView.getProjection().
                // You should not hold on to this object for more than one draw, since the projection of the map could change.
                Projection proj = mapView.getProjection();
                GeoPoint loc = (GeoPoint)proj.fromPixels((int) ev.getX(), (int) ev.getY());
                findZone(loc);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void hideSoftKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if(imm.isAcceptingText()) { // verify if the soft keyboard is open
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }
    }

    // http://alienryderflex.com/polygon/
    // The basic idea is to find all edges of the polygon that span the 'x' position of the point you're testing against.
    // Then you find how many of them intersect the vertical line that extends above your point. If an even number cross above the point,
    // then you're outside the polygon. If an odd number cross above, then you're inside.
    public Zone contains(GeoPoint location) {
        Zone returnZone = null;
        if(location==null)
            return returnZone;
        if(allZones.size() == 0)
            return returnZone;

        for (Zone z: allZones) {
            ArrayList<GeoPoint> polyLoc = new ArrayList<GeoPoint>();
            JSONArray allPoints = null;
            try {
                allPoints = new JSONObject(z.getGeometry()).getJSONArray("coordinates").getJSONArray(0);
                for (int i = 0; i < allPoints.length(); i++) {
                    JSONArray point = allPoints.getJSONArray(i);
                    GeoPoint g = new GeoPoint(point.getDouble(1), point.getDouble(0));
                    //Log.d("edu.ap.mapsaver", "Clicked coordinate : " + g.toString());
                    polyLoc.add(g);
                }
            }
            catch (JSONException ex) {
                Log.e("edu.ap.mapsaver", ex.getMessage());
            }

            GeoPoint lastPoint = polyLoc.get(polyLoc.size() - 1);
            double x = location.getLongitude();

            for (GeoPoint point : polyLoc) {
                double x1 = lastPoint.getLongitude();
                double x2 = point.getLongitude();
                double dx = x2 - x1;

                if (Math.abs(dx) > 180.0) {
                    // we have, most likely, just jumped the dateline
                    // (could do further validation to this effect if needed).
                    // normalise the numbers.
                    if (x > 0) {
                        while (x1 < 0)
                            x1 += 360;
                        while (x2 < 0)
                            x2 += 360;
                    } else {
                        while (x1 > 0)
                            x1 -= 360;
                        while (x2 > 0)
                            x2 -= 360;
                    }
                    dx = x2 - x1;
                }

                if ((x1 <= x && x2 > x) || (x1 >= x && x2 < x)) {
                    double grad = (point.getLatitude() - lastPoint.getLatitude()) / dx;
                    double intersectAtLat = lastPoint.getLatitude() + ((x - x1) * grad);

                    if (intersectAtLat > location.getLatitude())
                        returnZone = z;
                }
                lastPoint = point;
            }
        }

        return returnZone;
    }

    private void findZone(GeoPoint clickPoint) {
        Zone z = contains(clickPoint);
        if(z != null) {
            Toast.makeText(getApplicationContext(), "Tariefzone : " + z.getTariefzone() + " Tariefkleur : " + z.getTariefkleur(), Toast.LENGTH_SHORT).show();
        }
        else {
             Toast.makeText(getApplicationContext(), "Geen tariefzone gevonden", Toast.LENGTH_SHORT).show();
        }
     }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            mapView.getController().setCenter(new GeoPoint(loc.getLatitude(), loc.getLongitude()));
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider,
                                    int status, Bundle extras) {
        }
    }
}