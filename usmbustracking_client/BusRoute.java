package wow.usmbustracking_client;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by KeroNG on 25/11/2018.
 */
public class BusRoute extends AppCompatActivity implements OnMapReadyCallback{

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();
    private static GoogleMap mMap;
    private LatLngBounds USM = new LatLngBounds(
            new LatLng(5.353347, 100.287447), new LatLng(5.362477, 100.307042));
    private View mapView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bus_route);

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                // For Google Map Fragment
                // Obtain the SupportMapFragment and get notified when the locpin is ready to be used.
                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapRoute);
                mapFragment.getMapAsync(BusRoute.this);
                mapView = mapFragment.getView(); //for my location button
            }
        });

        populateBusRouteList();
    }

    private void populateBusRouteList()
    {
        final Map<String,Route> busRouteData = new LinkedHashMap<>();
        //final TextView tvRoutePath = (TextView) findViewById(R.id.tv_routePath);
        final Spinner spinnerRouteList = findViewById(R.id.busRouteSpinner);
        db.collection("BusRoute").orderBy("order").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(DocumentSnapshot document : task.getResult()){
                        Map<String, Object> routeObject = document.getData();
                        Route route = new Route();
                        List<LatLng> polylineList = PolyUtil.decode(routeObject.get("polyline").toString());
                        Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(polylineList).color((int)((long)routeObject.get("colour"))).clickable(true));
                        polyline.setVisible(false);
                        route.polyline = polyline;
                        busRouteData.put(document.getId(), route);
                        Log.d("getBusStop", document.getId() + document.getData());

                    }
                }
                else
                {
                    Log.d("getBusRoute", "Error: " + task.getException());
                }

                ArrayAdapter<String> spinnerRouteListAdapter = new ArrayAdapter<>(BusRoute.this, android.R.layout.simple_dropdown_item_1line, busRouteData.keySet().toArray(new String[]{}));
                spinnerRouteList.setAdapter(spinnerRouteListAdapter);

                spinnerRouteList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        for(Map.Entry<String,Route> entry : busRouteData.entrySet())
                        {
                            entry.getValue().polyline.setVisible(false);
                        }

                        busRouteData.get(parent.getItemAtPosition(position).toString()).polyline.setVisible(true);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {

                    }
                });

            }
        });
    }

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
    }

    private void populateBusStopList() {
        db.collection("BusStop").get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){

                    for(DocumentSnapshot document : task.getResult()){
                        double latitude = document.getGeoPoint("location").getLatitude();
                        double longitude =  document.getGeoPoint("location").getLongitude();
                        mMap.addMarker(new MarkerOptions().position(new LatLng(latitude,longitude))
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).title(document.getId()));
                        //Log.d("getBusStop", document.getId() + document.getData());
                    }
                }
                else {
                    Log.d("getBusStop", "Error: " + task.getException());
                }
            }
        });
    }

}
