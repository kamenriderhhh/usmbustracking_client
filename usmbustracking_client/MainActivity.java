package wow.usmbustracking_client;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.firebase.messaging.FirebaseMessaging;
import com.miguelcatalan.materialsearchview.MaterialSearchView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    MaterialSearchView searchView;
    private static GoogleMap mMap;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private View mapView;
    // Create a LatLngBounds that includes USM.
    private LatLngBounds USM = new LatLngBounds(
            new LatLng(5.353347, 100.287447), new LatLng(5.362477, 100.307042));

    // Create Geofence variable
    private GeofencingClient mGeofencingClient;
    private PendingIntent mGeofencePendingIntent;
    private List<Geofence> mGeofenceList;
    //make static to pass data to IntentService
    public static NearBusStopReceiver nearBusStopReceiver;
    private int fenceStatus = 0;
    private static String mDeviceID;
    private String nearBusStop = null;
    private String destBusStop = null;
    private boolean loading = false;
    private static Map<String,Marker> busLocationList = new HashMap<>();
    private static Map<String,Polyline> busRouteList = new HashMap<>();


    private LinkedList<TextView> tv_routePathList;
    private LinkedList<TextView> tv_routePathLabelList;

    private Handler updateOnlineStatusHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("USM BusTracking");
        toolbar.setTitleTextColor(Color.parseColor("#FFFFFF"));


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);



        tv_routePathList = new LinkedList<>();
        tv_routePathList.addLast((TextView)findViewById(R.id.tv_routePath1));
        tv_routePathList.addLast((TextView)findViewById(R.id.tv_routePath2));
        tv_routePathList.addLast((TextView)findViewById(R.id.tv_routePath3));
        tv_routePathList.addLast((TextView)findViewById(R.id.tv_routePath4));
        tv_routePathList.addLast((TextView)findViewById(R.id.tv_routePath5));


        tv_routePathLabelList = new LinkedList<>();
        tv_routePathLabelList.addLast((TextView)findViewById(R.id.tv_routePathLabel1));
        tv_routePathLabelList.addLast((TextView)findViewById(R.id.tv_routePathLabel2));
        tv_routePathLabelList.addLast((TextView)findViewById(R.id.tv_routePathLabel3));
        tv_routePathLabelList.addLast((TextView)findViewById(R.id.tv_routePathLabel4));
        tv_routePathLabelList.addLast((TextView)findViewById(R.id.tv_routePathLabel5));



        // Initialize search view
        setSearchView();

        // Geofence initiate
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mGeofencingClient = LocationServices.getGeofencingClient(this);
        }
        nearBusStopReceiver = new NearBusStopReceiver(new Handler());

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                mDeviceID = task.getResult().getId();

                // For Google Map Fragment
                // Obtain the SupportMapFragment and get notified when the locpin is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
                mapFragment.getMapAsync(MainActivity.this);
                mapView = mapFragment.getView(); //for my location button
            }
        });

        //test whether is first run
        SharedPreferences sharedPreferences = getSharedPreferences("notificationRegistration", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("firstrun", true)) { //return true if not found
            sharedPreferences.edit().putBoolean("firstrun", false).commit(); //record first run occured
            //subscribe to notification topic
            FirebaseMessaging.getInstance().subscribeToTopic("busFailure").addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    String msg = "Notification topic subscribed";
                    if (!task.isSuccessful()) {
                        msg = "Notification topic subscription failed";
                    }
                    Log.d("Subscribe noti topic", msg);
                    //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else if (searchView.isSearchOpen()){
            searchView.closeSearch();
        }
        else {
            super.onBackPressed();
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.search_menu, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        searchView.setMenuItem(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_search:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_dashboard)
        {
            // Handle the camera action
        }
        else if (id == R.id.nav_busRoute)
        {
            // draw bus route
            startActivity(new Intent(MainActivity.this, BusRoute.class));
        }
        else if (id == R.id.nav_timetable)
        {
            startActivity(new Intent(MainActivity.this, BusTimetable.class));
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Manipulates the locpin once available. This callback is triggered when the locpin is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install it inside the SupportMapFragment. This method will only be triggered once the user has installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng usm = new LatLng(5.355938, 100.302502);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(usm));
        mMap.setMinZoomPreference(16);
        // Constrain the camera target to the Adelaide bounds.
        mMap.setLatLngBoundsForCameraTarget(USM);

        populateBusStopList();
        grantPermissionActivateMyLocationBtn();
    }

    // Pin the bus stop on the locpin
    private void populateBusStopList() {
        db.collection("BusStop").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    mGeofenceList = new ArrayList<>();
                    for(DocumentSnapshot document : task.getResult()){
                        double latitude = document.getGeoPoint("location").getLatitude();
                        double longitude =  document.getGeoPoint("location").getLongitude();
                        mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longitude))
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).title(document.getId()));
                        //Log.d("getBusStop", document.getId() + document.getData());

                        mGeofenceList.add(new Geofence.Builder()
                            // Set the request ID of the geofence. This is a string to identify this geofence.
                            .setRequestId(document.getId())

                            .setCircularRegion(
                                    latitude,
                                    longitude,
                                    50 //Constants.GEOFENCE_RADIUS_IN_METERS
                            )
                            .setExpirationDuration(Geofence.NEVER_EXPIRE)
                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                                    Geofence.GEOFENCE_TRANSITION_EXIT)
                            .build());
                    }
                    // After collected the ID add to Geofence function
                    addGeoFences();
                    // Get and display bus live location
                    listenerGetBusLiveLocation();
                }
                else {
                    Log.d("getBusStop", "Error: " + task.getException());
                }
            }
        });
    }

    // Grant permission for using myLocation button
    private void grantPermissionActivateMyLocationBtn()
    {
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mMap.setMyLocationEnabled(true);
            View locationButton = ((View) mapView.findViewById(Integer.parseInt("1")).getParent()).findViewById(Integer.parseInt("2"));
            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
            // position on right bottom
            rlp.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
            rlp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);rlp.setMargins(0,0,30,150);
        }
        else
        {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    private void markBusLiveLocation(Map<String, GeoPoint> busLocGeoPnt)
    {
        if(busLocationList.size()==0) {
            for (Map.Entry<String, GeoPoint> entry : busLocGeoPnt.entrySet()) {
                busLocationList.put(entry.getKey(), mMap.addMarker(new MarkerOptions().position(new LatLng(entry.getValue().getLatitude(),
                        entry.getValue().getLongitude())).title("Bus "+entry.getKey()).icon(BitmapDescriptorFactory.fromResource(R.drawable.bus_logo))));
            }
        }
        else
        {
            for (Map.Entry<String, GeoPoint> entry : busLocGeoPnt.entrySet()) {
                busLocationList.get(entry.getKey()).setPosition(new LatLng(entry.getValue().getLatitude(), entry.getValue().getLongitude()));
            }
        }
    }

    private void listenerGetBusLiveLocation()
    {
        final Handler handler = new Handler();
        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        db.collection("BusLocation").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                            Map<String, GeoPoint> busLocationList = new HashMap<>();

                            @Override
                            public void onComplete(@NonNull Task<QuerySnapshot> task) {

                                for(DocumentSnapshot document : task.getResult())
                                {
                                    busLocationList.put(document.getId(), document.getGeoPoint("location"));
                                    Log.d("listenerGetBusLiveLoc", document.getId() + document.getGeoPoint("location").toString());
                                }
                                markBusLiveLocation(busLocationList);
                            }

                        });
                        Log.d("Thread listener", ""+Thread.currentThread().getId());
                        handler.postDelayed(this, 1500);
                    }
                },
                1500
        );
    }

    // Search View Initialization
    private void setSearchView(){
        // Initialize search view and search query function
        searchView = (MaterialSearchView) findViewById(R.id.search_view);
        searchView.setEllipsize(true);
        setSuggestion();
        findViewById(R.id.search_view).bringToFront();
        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener(){

            @Override
            public boolean onQueryTextSubmit(String query) {
                destBusStop = query;
                if(nearBusStop != null)
                {
                    estimatedTimeArrival(nearBusStop, destBusStop);
                    Toast.makeText(getApplicationContext(), "Filtering for possible bus to destination bus stop " + destBusStop + "...", Toast.LENGTH_LONG).show();
                }
                else
                    Toast.makeText(getApplicationContext(), "Sorry, you are not in any bus stop...T_T", Toast.LENGTH_SHORT).show();
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                //searchFilter(newText);
                return true;
            }
        });

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() { /* do something*/ }
            @Override
            public void onSearchViewClosed() { /* do something*/ }
        });
    }

    // Get bus stop list and sent to search suggestion
    private void setSuggestion(){
        final ArrayList<String> busStopList = new ArrayList<>();
        // Gather the bus stops into a list
        db.collection("BusStop").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult()) {
                        busStopList.add(document.getId());
                    }
                    searchView.setSuggestions(busStopList.toArray(new String[busStopList.size()]));
                }
                else {
                    Log.d("filterBusStop", "Error: " + task.getException());
                }
            }
        });
    }

    // Set GeofenceRequest
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(mGeofenceList);
        return builder.build();
    }

    // Set GeofencePendingIntent
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }

        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    // Add the Geofence to the client / locpin
    private void addGeoFences(){
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent()).addOnSuccessListener
                (this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences added ...
                        Log.d("~~GFence~~", "Geofence Success!!!!");
                    }
                }).addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to add geofences ...
                        e.printStackTrace();
                    }
                });
        }
    }

    // Create a class for extending the result receiver to the intent service
    private class NearBusStopReceiver extends ResultReceiver {
        public NearBusStopReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            TextView tv_nearBusStop = findViewById(R.id.tv_nearBusStop);
            switch (resultCode) {

                case GeofenceTransitionsIntentService.ENTRY:
                    nearBusStop = resultData.getString("nearBusStop");
                    tv_nearBusStop.setText(nearBusStop);
                    tv_nearBusStop.setGravity(Gravity.CENTER_HORIZONTAL);
                    estimatedTimeArrival(nearBusStop, null);
                    fenceStatus++;

                    Map<String, Object> onlineStatus = new HashMap<>();
                    onlineStatus.put("currentStop", nearBusStop);
                    onlineStatus.put("time", new Timestamp(new Date()));
                    WriteBatch writeBatch = db.batch();
                    writeBatch.set(db.collection("OnlineUsers").document(mDeviceID), onlineStatus);
                    writeBatch.set(db.collection("BusStopUsageLog").document(), onlineStatus);
                    writeBatch.commit();
                    updateOnlineStatusHandler.removeCallbacksAndMessages(null);
                    updateOnlineStatusHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Map<String, Object> onlineStatus = new HashMap<>();
                            onlineStatus.put("currentStop", nearBusStop);
                            onlineStatus.put("time", new Timestamp(new Date()));
                            db.collection("OnlineUsers").document(mDeviceID).set(onlineStatus);
                            if(destBusStop !=null)
                                estimatedTimeArrival(nearBusStop, destBusStop);
                            else
                                estimatedTimeArrival(nearBusStop, null);
                            updateOnlineStatusHandler.postDelayed(this, 1*60*1000);
                        }
                    }, 1*60*1000);
                    break;

                case GeofenceTransitionsIntentService.EXIT:
                    fenceStatus--;
                    if(fenceStatus==0)
                    {
                        nearBusStop = null;
                        destBusStop = null;
                        tv_nearBusStop.setText("Far from any Bus Stop");
                        updateOnlineStatusHandler.removeCallbacksAndMessages(null);
                        for(int i = 0; i < 5; i++) {
                            tv_routePathList.get(i).setText("");
                            tv_routePathLabelList.get(i).setText("");
                        }
                    }

                    break;

                case GeofenceTransitionsIntentService.ERROR:
                    tv_nearBusStop.setText("Error");
                    break;

            }
            Log.d("fenceStatus", String.valueOf(fenceStatus));
            super.onReceiveResult(resultCode, resultData);
        }
    }

    //function overloading for destination provided
    //private void estimatedTimeArrival(String waitingStop, String destinationStop) {
    // Estimated time arrival
    static Map<String, Integer> etaData = new HashMap<>();
    static int numBusApplicable = 0;
    private void estimatedTimeArrival(final String waitingStop, final String destinationStop) {
        etaData.clear();
        numBusApplicable = 0;
        loading = true;

        // Loading...
        loadingScreen();

        // Reset the display ETA
        for(int i = 0; i < 5; i++) {
            tv_routePathList.get(i).setText("");
            tv_routePathLabelList.get(i).setText("");
        }

        db.collection("BusLocation").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                for(QueryDocumentSnapshot busDocumentSnapshot : task.getResult())
                {
                    final String busRoute = busDocumentSnapshot.getId();
                    final int distanceTravelled = ((Long)busDocumentSnapshot.get("distanceTravelled")).intValue();
                    final String currentStop = (String)busDocumentSnapshot.get("currentStop");

                    if(currentStop == waitingStop) //bus reached at waiting stop
                    {
                        etaData.put(busRoute, 0);
                        continue;
                    }
                    else if(currentStop.equals("-")) //if bus is not running
                    {
                        continue;
                    }

                    db.collection("BusRoute").document(busRoute).collection("Stop").document(waitingStop).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {

                            DocumentSnapshot waitStopDocumentSnapshot = task.getResult();
                            if(waitStopDocumentSnapshot.getData()==null) { return; }
                            final int waitAccuDistanceBetween = ((Long)waitStopDocumentSnapshot.get("accuDistanceBetween")).intValue();
                            final int waitAccuTimeBetween = ((Long)waitStopDocumentSnapshot.get("accuTimeBetween")).intValue();

                            if(destinationStop == null)
                            {
                                numBusApplicable++;
                                db.collection("BusRoute").document(busRoute).collection("Stop").document(currentStop).get()
                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        DocumentSnapshot currStopDocumentSnapshot = task.getResult();
                                        int currAccuDistanceBetween = ((Long)currStopDocumentSnapshot.get("accuDistanceBetween")).intValue();
                                        int currDistanceBetween = ((Long)currStopDocumentSnapshot.get("distanceBetween")).intValue();
                                        final int currAccuTimeBetween = ((Long)currStopDocumentSnapshot.get("accuTimeBetween")).intValue();
                                        int currTimeBetween = ((Long)currStopDocumentSnapshot.get("timeBetween")).intValue();

                                        final int eta = waitAccuTimeBetween-currAccuTimeBetween-(distanceTravelled-currAccuDistanceBetween)/currDistanceBetween*currTimeBetween;
                                        if(waitAccuDistanceBetween - currAccuDistanceBetween < 0){
                                            db.collection("BusRoute").document(busRoute).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                    DocumentSnapshot timetableDocument = task.getResult();
                                                    List<Long> timetableList = (List<Long>) timetableDocument.get("timetable");

                                                    Calendar timeNow = Calendar.getInstance();
                                                    int currentHour = timeNow.get(Calendar.HOUR_OF_DAY);
                                                    int currentMinute = timeNow.get(Calendar.MINUTE);

                                                    int currentTime = currentHour * 100 + currentMinute;
                                                    int nextDepartDuration=0;

                                                    if (timetableList != null) {
                                                        for (long time : timetableList) {
                                                            nextDepartDuration = currentTime - (int)time;
                                                            if (nextDepartDuration >= 0)
                                                                continue;
                                                            else {
                                                                nextDepartDuration = ((int)time / 100) * 60 * 60 + ((int)time % 100) * 60 - (currentHour * 60 * 60 + currentMinute * 60);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    etaData.put(busRoute, waitAccuTimeBetween+nextDepartDuration);
                                                    if (etaData.size() == numBusApplicable)
                                                    {
                                                        int counter =0;
                                                        //call function to display eta data
                                                        for (Map.Entry<String,Integer> entry : etaData.entrySet()) {
                                                            String key = entry.getKey();
                                                            Integer value = entry.getValue();
                                                            Log.d("ETA", "ETA key: " + key + " value: " + value);
                                                            //tv_routePath1.setText("Bus " + key + ", Remaining time: " + value.toString());
                                                            tv_routePathLabelList.get(counter).setText(key );
                                                            tv_routePathList.get(counter++).setText(((value/60==0)?1:value/60) + " min");
                                                        }
                                                        for(int i = numBusApplicable; i < 5; i++) {
                                                            tv_routePathList.get(i).setText("");
                                                            tv_routePathLabelList.get(i).setText("");
                                                        }

                                                        loading = false;
                                                    }

                                                }
                                            });
                                        }
                                        else
                                        {
                                            etaData.put(busRoute, eta);
                                            if (etaData.size() == numBusApplicable)
                                            {
                                                int counter = 0;
                                                //call function to display eta data
                                                for (Map.Entry<String,Integer> entry : etaData.entrySet()) {
                                                    String key = entry.getKey();
                                                    Integer value = entry.getValue();
                                                    Log.d("ETA", "ETA key: " + key + " value: " + value);
                                                    //    tv_routePath2.setText("Bus " + key + ", Remaining time: " + value.toString());
                                                    tv_routePathLabelList.get(counter).setText(key );
                                                    tv_routePathList.get(counter++).setText(((value/60==0)?1:value/60) + " min");
                                                }
                                                for(int i = numBusApplicable; i < 5; i++) {
                                                    tv_routePathList.get(i).setText("");
                                                    tv_routePathLabelList.get(i).setText("");
                                                }

                                                loading = false;
                                            }

                                        }
                                    }
                                });
                            }
                            else
                            {
                                db.collection("BusRoute").document(busRoute).collection("Stop").document(destinationStop).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        DocumentSnapshot destStopDocumentSnapshot = task.getResult();
                                        if(destStopDocumentSnapshot.getData()==null) return;
                                        numBusApplicable++;
                                        db.collection("BusRoute").document(busRoute).collection("Stop")
                                                .document(currentStop).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {

                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                DocumentSnapshot currStopDocumentSnapshot = task.getResult();
                                                int currAccuDistanceBetween = ((Long)currStopDocumentSnapshot.get("accuDistanceBetween")).intValue();
                                                int currDistanceBetween = ((Long)currStopDocumentSnapshot.get("distanceBetween")).intValue();
                                                int currAccuTimeBetween = ((Long)currStopDocumentSnapshot.get("accuTimeBetween")).intValue();
                                                int currTimeBetween = ((Long)currStopDocumentSnapshot.get("timeBetween")).intValue();

                                                final int eta = waitAccuTimeBetween-currAccuTimeBetween-(distanceTravelled-currAccuDistanceBetween)/currDistanceBetween*currTimeBetween;
                                                if(waitAccuDistanceBetween - currAccuDistanceBetween < 0){
                                                    db.collection("BusRoute").document(busRoute).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                        @Override
                                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                            DocumentSnapshot timetableDocument = task.getResult();
                                                            List<Long> timetableList = (List<Long>) timetableDocument.get("timetable");

                                                            Calendar timeNow = Calendar.getInstance();
                                                            int currentHour = timeNow.get(Calendar.HOUR_OF_DAY);
                                                            int currentMinute = timeNow.get(Calendar.MINUTE);

                                                            int currentTime = currentHour * 100 + currentMinute;
                                                            int nextDepartDuration=0;

                                                            if (timetableList != null) {
                                                                for (long time : timetableList) {
                                                                    nextDepartDuration = currentTime - (int)time;
                                                                    if (nextDepartDuration >= 0)
                                                                        continue;
                                                                    else {
                                                                        nextDepartDuration = ((int)time / 100) * 60 * 60 + ((int)time % 100) * 60 - (currentHour * 60 * 60 + currentMinute * 60);
                                                                        break;
                                                                    }
                                                                }
                                                            }
                                                            etaData.put(busRoute, waitAccuTimeBetween+nextDepartDuration);
                                                            if (etaData.size() == numBusApplicable)
                                                            {
                                                                int counter =0;
                                                                //call function to display eta data
                                                                for (Map.Entry<String,Integer> entry : etaData.entrySet()) {
                                                                    String key = entry.getKey();
                                                                    Integer value = entry.getValue();
                                                                    Log.d("ETA", "ETA key: " + key + " value: " + value);
                                                                    //tv_routePath1.setText("Bus " + key + ", Remaining time: " + value.toString());
                                                                    tv_routePathLabelList.get(counter).setText(key );
                                                                    tv_routePathList.get(counter++).setText(((value/60==0)?1:value/60) + " min");
                                                                }
                                                                for(int i = numBusApplicable; i < 5; i++) {
                                                                    tv_routePathList.get(i).setText("");
                                                                    tv_routePathLabelList.get(i).setText("");
                                                                }

                                                                loading = false;
                                                            }

                                                        }
                                                    });
                                                }
                                                else
                                                {
                                                    etaData.put(busRoute, eta);
                                                    if (etaData.size() == numBusApplicable)
                                                    {
                                                        int counter = 0;
                                                        //call function to display eta data
                                                        for (Map.Entry<String,Integer> entry : etaData.entrySet()) {
                                                            String key = entry.getKey();
                                                            Integer value = entry.getValue();
                                                            Log.d("ETA", "ETA key: " + key + " value: " + value);
                                                            //    tv_routePath2.setText("Bus " + key + ", Remaining time: " + value.toString());
                                                            tv_routePathLabelList.get(counter).setText(key );
                                                            tv_routePathList.get(counter++).setText(((value/60==0)?1:value/60) + " min");
                                                        }
                                                        for(int i = numBusApplicable; i < 5; i++) {
                                                            tv_routePathList.get(i).setText("");
                                                            tv_routePathLabelList.get(i).setText("");
                                                        }
                                                        loading = false;
                                                    }

                                                }
                                            }
                                        });
                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    private void loadingScreen(){

        //Loading part as alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        View view = getLayoutInflater().inflate(R.layout.loading_screen, null);

        builder.setView(view);
        final AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();

        final Handler handler = new Handler();
        final int delay = 2;

        //Load screen and check every 2 sec if the data are set
        handler.postDelayed(new Runnable(){
            public void run(){
                if (!loading){
                    dialog.dismiss();
                }
                else
                    handler.postDelayed(this, delay * 1000);
            }
        }, delay * 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGeofencingClient.removeGeofences(getGeofencePendingIntent());
        //stop handler also, lazy to fix
        mMap.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mMap!=null) {
            populateBusStopList();
        }
    }
}