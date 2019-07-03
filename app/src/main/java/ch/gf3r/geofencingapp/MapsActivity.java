package ch.gf3r.geofencingapp;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback{

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private GeofencingClient geofencingClient;
    private static final long EXPIRATION_DURATION = 5 * 60 * 60 * 1000; // 5 Hours
    private static final int GEOFENCE_RADIUS = 300;
    private List<Geofence> geofenceList;
    private Map<String, LatLng> latLngMap;
    private PendingIntent mGeofencePendingIntent;
    private Marker marker;
    private final int REQUEST = 22;
    private Context mContext;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private OnSuccessListener successListener;
    private SharedPreferences mPrefs;
    private List<Circle> circlelist;
    private final String geofenceKey = "latlngHashMapV2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = getPreferences(MODE_PRIVATE);
        mContext = this;

        Map<String, LatLng> SavedLatLng = loadObject(geofenceKey);
        if(SavedLatLng != null){
            latLngMap = SavedLatLng;
        }else{
            latLngMap = new HashMap();
        }
        circlelist = new ArrayList<>();
        geofenceList = new ArrayList<>();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        geofencingClient = LocationServices.getGeofencingClient(this);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Permissions.Request_FINE_LOCATION(this, REQUEST);
            Permissions.Request_COARSE_LOCATION(this, REQUEST);
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceList);
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    public void registerAllGeofences() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Permissions.Request_FINE_LOCATION(this, REQUEST);
            Toast.makeText(this, "Permission were not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        System.out.println("succesfully added geofence, currently " + geofenceList.size() + " items");
                        Toast.makeText(mContext, "Geofencing started", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("failed to  add geofence");
                        Toast.makeText(mContext, "Geofencing failed", Toast.LENGTH_LONG).show();
                    }
                });
        saveObject(latLngMap, geofenceKey);
    }

    //The startup sequence
    private void drawAllCircles() {
        for(Circle circle: circlelist){
            circle.remove();
        }
        circlelist.clear();
        for(HashMap.Entry<String, LatLng> entry : latLngMap.entrySet()){
            drawCircle(entry.getValue());
            addTextMarker(entry.getKey(), entry.getValue());
        }
    }

    private void populateGeofences(){
        geofenceList.clear();
        for(HashMap.Entry<String, LatLng> entry : latLngMap.entrySet()){
            LatLng pos = entry.getValue();
            double longitude = pos.longitude;
            double latitude = pos.latitude;
            geofenceList.add(new Geofence.Builder()
                    .setRequestId(entry.getKey())
                    .setExpirationDuration(EXPIRATION_DURATION)
                    .setLoiteringDelay(0)
                    .setNotificationResponsiveness(0)
                    .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL)
                    .build());
        }
    }

    private void SetupMap() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Permissions.Request_FINE_LOCATION(this, REQUEST);
            Permissions.Request_COARSE_LOCATION(this, REQUEST);
            return;
        }
        locationCallback = new LocationCallback();
        locationRequest = new LocationRequest();
        locationRequest.setInterval(120000); // two minute interval
        locationRequest.setFastestInterval(120000);
        locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                mMap.setMyLocationEnabled(true);
            } else {
                checkLocationPermission();
            }
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
            mMap.setMyLocationEnabled(true);
        }

        successListener = new OnSuccessListener<Location>() {
            @SuppressLint("MissingPermission")
            @Override
            public void onSuccess(Location loc) {
                if(loc != null) {
                    setNewMarker(loc);
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(loc.getLatitude(), loc.getLongitude())));
                }

            }
        };
        if(latLngMap.size() > 0) {
            populateGeofences();
        }
        drawAllCircles();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, successListener);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        REQUEST);
                            }
                        })
                        .create()
                        .show();


            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST);
            }
        }
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latlng) {
                Location loc = new Location("");
                loc.setLatitude(latlng.latitude);
                loc.setLongitude(latlng.longitude);
                setNewMarker(loc);
            }
        });
        SetupMap();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == REQUEST) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
            }
            return;
        }
    }

    //The helper functions
    private static float getDistanceinMeters(LatLng latLng1, LatLng latLng2){
        Location loc1 = new Location("");
        loc1.setLongitude(latLng1.longitude);
        loc1.setLatitude(latLng1.latitude);
        Location loc2 = new Location("");
        loc2.setLongitude(latLng2.longitude);
        loc2.setLatitude(latLng2.latitude);
        return loc1.distanceTo(loc2);
    }

    private boolean listContainsOverlappingGeloc(LatLng position){
        for(LatLng latLng : latLngMap.values()){
            if(getDistanceinMeters(position, latLng) < GEOFENCE_RADIUS*2){
                return true;
            }
        }
        return false;
    }

    private void saveObject(Map<String, LatLng> hashmap, String key){
        Gson gson = new Gson();
        String hashMapString = gson.toJson(hashmap);
        //A work around since hashmaps can't simply be saved in preferences
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString(key, hashMapString);
        prefsEditor.commit();
    }

    private Map<String, LatLng> loadObject(String key){
        Gson gson = new Gson();
        String storedHashMapString = mPrefs.getString(key,"");
        //O no another work around
        java.lang.reflect.Type type = new TypeToken<HashMap<String, LatLng>>(){}.getType();
        HashMap<String, LatLng> hashMap = gson.fromJson(storedHashMapString, type);
        return hashMap;
    }

    private void setNewMarker(Location location) {
        LatLng latlng = new LatLng(location.getLatitude(), location.getLongitude());
        if (marker != null) {
            marker.remove();
        }
        marker = mMap.addMarker(new MarkerOptions()
                .position(latlng)
                .icon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_RED)).title(String.valueOf(latlng.longitude) + " " + latlng.latitude));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng));
        mMap.moveCamera(CameraUpdateFactory.zoomTo(15));
        System.out.println(latlng);
    }

    private void drawCircle(LatLng point) {
        CircleOptions circleOptions = new CircleOptions();
        circleOptions.center(point);
        circleOptions.radius(GEOFENCE_RADIUS);
        circleOptions.strokeColor(Color.BLACK);
        circleOptions.fillColor(0x30ff0000);
        circleOptions.strokeWidth(2);
        Circle circle = mMap.addCircle(circleOptions);
        circlelist.add(circle);
    }

    private void addTextMarker(String text, LatLng point){
        mMap.addMarker(new MarkerOptions()
                .position(point)
                .title(text)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                .alpha(0.3f));
    }

    public void moveToMarkerButtonHandler(View view) {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(successListener);
    }

    //The two buttons:
    public void addGeofencesButtonHandler(View view) {
        final LatLng pos = marker.getPosition();
        if(listContainsOverlappingGeloc(pos)){
            Toast.makeText(mContext, "Geofence overlapps, aborted", Toast.LENGTH_LONG).show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Geofence Name");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String text = input.getText().toString();
                if(text == null || text.isEmpty()) {
                    Toast.makeText(mContext,"no name entered, aborted", Toast.LENGTH_LONG).show();
                    return;
                }
                latLngMap.put(text, pos);
                populateGeofences();
                drawCircle(pos);
                addTextMarker(text, pos);
                registerAllGeofences();

            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.show();
    }
}


