package com.accidentalspot.spot;

import android.Manifest;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.accidentalspot.spot.Models.LocationObject;
import com.accidentalspot.spot.Models.LocationObjects;
import com.accidentalspot.spot.api.APIService;
import com.accidentalspot.spot.api.APIUrl;
import com.accidentalspot.spot.services.LocationMonitoringService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;
    private GoogleApiClient mGoogleApiClient;
    private double latitude,longitude;
    private GoogleMap mMap;
    public static GoogleApiClient googleApiClient;
    private static final int LOCATION_REQUEST_CODE = 101;
    private ArrayList<LocationObject> locationsArrayList = new ArrayList<>();
    private MediaPlayer mMediaPlayer;
    private ArrayList<LocationObject> list;
    private ProgressDialog pDialog;

    private static final String TAG = MapsActivity.class.getSimpleName();

    /**
     * Code used in requesting runtime permissions.
     */
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;


    private boolean mAlreadyStartedService = false;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(checkPlayServices()){
            buildGoogleApiClient();
        }
        displayLocation();
        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


    private void fetchStoresByStoredType()
    {
     pDialog = ProgressDialog.show(MapsActivity.this, null, null, true);
     pDialog.setContentView(R.layout.progress_dialog);
     pDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        //calling the upload file method after choosing the file
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(APIUrl.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create()) //Here we are using the GsonConverterFactory to directly convert json data to object
                .build();

        APIService api = retrofit.create(APIService.class);

        Call<LocationObjects> call = api.getData();

        call.enqueue(new Callback<LocationObjects>() {
            @Override
            public void onResponse(Call<LocationObjects> call, retrofit2.Response<LocationObjects> response) {
                if (response.isSuccessful()) {
                    locationsArrayList = new ArrayList<>();
                    Toast.makeText(MapsActivity.this, "Success", Toast.LENGTH_SHORT).show();
                    ArrayList<LocationObject> locationsList = (ArrayList<LocationObject>) response.body().getLocationObject();
                 //   Toast.makeText(MapsActivity.this, "locationsArrayList"+locationsArrayList, Toast.LENGTH_SHORT).show();

                    for (int i = 0; i < locationsList.size(); i++) {
                        try {
                            Double lat = Double.valueOf(locationsList.get(i).getLat());
                         //   Log.d("url", String.valueOf(data.lat));
                            Double lng = Double.valueOf(locationsList.get(i).getLng());
                         //   Log.d("url", String.valueOf(data.lng));
                           String name = locationsList.get(i).getName();
                        //    Log.d("url", String.valueOf(data.name));
                            String colorCode = locationsList.get(i).getColorCode();
                         //   Log.d("url", String.valueOf(data.colorCode));

                            locationsArrayList.add(new LocationObject(name,lat, lng, colorCode,""));

                            saveArrayList(locationsArrayList, "array");

                        } catch (Exception e) {
                            pDialog.dismiss();
                            e.printStackTrace();
                        }
                    }
                    pDialog.dismiss();
                    DrawMarker(locationsArrayList);
                }else{
                    Toast.makeText(MapsActivity.this, "Failed to retrive data", Toast.LENGTH_SHORT).show();
                }
                pDialog.dismiss();
              //  Toast.makeText(MapsActivity.this, ""+locationsArrayList, Toast.LENGTH_SHORT).show();
            }


            @Override
            public void onFailure(Call<LocationObjects> call, Throwable t) {
                pDialog.dismiss();
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void saveArrayList(ArrayList<LocationObject> list, String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MapsActivity.this);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(key, json);
        editor.apply();     // This line is IMPORTANT !!!
    }


    private void DrawMarker(List<LocationObject> storelist)
    {
        Log.d("Tag", String.valueOf(storelist.size()));
        try {
            mMap.clear();
            Marker marker;
            if (storelist.size() != 0) {
                for (int i = 0; i < storelist.size(); i++)
                {
                    System.out.println("color: "+locationsArrayList.get(i).colorCode);

                    LatLng gps1 = new LatLng(locationsArrayList.get(i).getLat(), locationsArrayList.get(i).getLng());
//MarkerOptions are used to create a new Marker.You can specify location, title etc with MarkerOptions
                    MarkerOptions markerOptions1 = new MarkerOptions().position(gps1)
                            .title(locationsArrayList.get(i).getName())
                            .icon(BitmapDescriptorFactory.fromBitmap(changeBitmapColor(Color.parseColor(locationsArrayList.get(i).getColorCode()))));
                    mMap.addMarker(markerOptions1);


                }
                final LatLng latLng1 = new LatLng(latitude, longitude);
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng1, 16.0f));
            }
        }
        catch (Exception e)
        {
            Log.d("error_: ",e.getMessage());
            e.printStackTrace();
        }

    }

    private Bitmap changeBitmapColor(int color)
    {

        Bitmap ob = BitmapFactory.decodeResource(this.getResources(), R.drawable.marker);
        Bitmap overlay = BitmapFactory.decodeResource(this.getResources(), R.drawable.marker);
        Bitmap overlaym = Bitmap.createBitmap(overlay.getWidth(), overlay.getHeight(), Bitmap.Config.ARGB_8888);


        Canvas canvas = new Canvas(overlaym);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, android.graphics.PorterDuff.Mode.SRC_ATOP));
        canvas.drawBitmap(ob, 0f, 0f, paint);
        //canvas.drawBitmap(overlay, 0f, 0f, null);
        return overlaym;
    }


    @Override
    public void onMapReady(GoogleMap googleMap)
    {

        mMap = googleMap;
        //mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setBuildingsEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                mMap.setMyLocationEnabled(true);

                fetchStoresByStoredType();

                LocalBroadcastManager.getInstance(this).registerReceiver(
                        new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                String latitude = intent.getStringExtra(LocationMonitoringService.EXTRA_LATITUDE);
                                String longitude = intent.getStringExtra(LocationMonitoringService.EXTRA_LONGITUDE);

                                if (latitude != null && longitude != null) {
                                    //   mMsgView.setText(getString(R.string.msg_location_service_started) + "\n Latitude : " + latitude + "\n Longitude: " + longitude);
                                    checkLocationandAddToMap(latitude,longitude);
                                }
                            }
                        }, new IntentFilter(LocationMonitoringService.ACTION_LOCATION_BROADCAST)
                );

            }
            else
            {
                checkLocationPermission();
            }
        }
        else
        {
            mMap.setMyLocationEnabled(true);
        }

        final LatLng latLng1 = new LatLng(latitude, longitude);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng1, 16.0f));

        CameraPosition cameraPosition=new CameraPosition.Builder().target(latLng1).tilt(90).zoom(1).bearing(0).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        //fetchStoresByStoredType();

    }
    /**
     * Creating google api client object
     * */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLastLocation = LocationServices.FusedLocationApi
                .getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
        }
    }

    /**
     * Method to verify google play services on the device
     * */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    /**
     * Google api callback methods
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i("Tag", "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {

        // Once connected with google api, get the location
        displayLocation();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {

                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        startStep3();
    }


    /**
     * Step 3: Start the Location Monitor Service
     */
    private void startStep3() {

        //And it will be keep running until you close the entire application from task manager.
        //This method will executed only once.

        if (!mAlreadyStartedService) {
            //Start location sharing service to app server.........
            Intent intent = new Intent(this, LocationMonitoringService.class);
            startService(intent);
            mAlreadyStartedService = true;
            //Ends................................................
        }
    }

    @Override
    public void onDestroy() {
        //Stop location sharing service to app server.........
        stopService(new Intent(this, LocationMonitoringService.class));
        mAlreadyStartedService = false;
        //Ends................................................
        super.onDestroy();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private void checkLocationandAddToMap(String latitude, String longitude) {
        try {
//Fetching the last known location using the Fus
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            Location currentLocation = new Location("");//provider name is unnecessary
            currentLocation.setLatitude(Double.parseDouble(latitude));//your coords of course
            currentLocation.setLongitude(Double.parseDouble(longitude));

            for (int i = 0; i < locationsArrayList.size(); i++) {
                Location targetLocation = new Location("");//provider name is unnecessary
                targetLocation.setLatitude(locationsArrayList.get(i).getLat());//your coords of course
                targetLocation.setLongitude(locationsArrayList.get(i).getLng());

                float distance = targetLocation.distanceTo(currentLocation);

                if (distance < 50) {
                    playSound();
                    sendNotification();
                }

            }
        }catch (Exception e){}
    }

    private void playSound() {
        try {
            AssetFileDescriptor afd =  getAssets().openFd("raw/sound.mp3");
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            afd.close();
            mMediaPlayer.prepare();
            mMediaPlayer.start();
        } catch (Exception ex) {
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void sendNotification()
    {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.spot2)
                .setContentTitle("Alert")
                .setContentText("You are near spot")
                .setAutoCancel(true)
                .setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                .setLights(Color.RED, 3000, 3000)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }

    public ArrayList<LocationObject> getArrayList(String key){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Gson gson = new Gson();
        String json = prefs.getString(key, null);
        Type type = new TypeToken<ArrayList<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                finish();
                startActivity(new Intent(getApplicationContext(),MapsActivity.class));
                break;
            case R.id.action_allspots:
                startActivity(new Intent(getApplicationContext(), AllAccidentalSpots.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}

