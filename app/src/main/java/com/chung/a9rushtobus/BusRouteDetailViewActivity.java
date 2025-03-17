package com.chung.a9rushtobus;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Scroller;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.chung.a9rushtobus.elements.BusRouteAdapter;
import com.chung.a9rushtobus.elements.BusRouteStopItem;
import com.chung.a9rushtobus.elements.BusRouteStopItemAdapter;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

public class BusRouteDetailViewActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private BusRouteStopItemAdapter adapter;
    private RecyclerView busRouteStopRecyclerView;
    private List<BusRouteStopItem> busRouteStopItems;
    private String routeNumber, routeDestination, routeBound, routeServiceType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_bus_route_detail_view);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        routeNumber = getIntent().getStringExtra("route");
        routeDestination = getIntent().getStringExtra("destination");
        routeBound = getIntent().getStringExtra("bound");
        routeServiceType = getIntent().getStringExtra("serviceType");
        
        initView();
        initBusRouteStopRecyclerView();
        initListener();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Add points
        LatLng point1 = new LatLng(22.345415, 114.192640); // 竹園邨總站
        LatLng point2 = new LatLng(22.345076, 114.190023); // 天虹小學

        // Change Pin Icon
        mMap.addMarker(new MarkerOptions()
                .position(point1)
                .title("竹園邨總站")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        mMap.addMarker(new MarkerOptions().position(point2).title("天虹小學"));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point1, 16f));
    }

    public void initView(){
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        busRouteStopRecyclerView = findViewById(R.id.recyclerView2);

        TextView busRouteNumber = findViewById(R.id.bus_detail_activity_route_number);
        TextView busRouteDestination = findViewById(R.id.bus_detail_activity_route_dest);
        busRouteDestination.setSingleLine(true);
        busRouteDestination.setMarqueeRepeatLimit(-1);
        busRouteDestination.setScroller(new Scroller(this));
        busRouteDestination.setMovementMethod(LinkMovementMethod.getInstance());
        busRouteDestination.setSelected(true);

        busRouteNumber.setText(routeNumber);
        busRouteDestination.setText(routeDestination);
        Log.d("LogBusRouteDetailView", "Route: " + routeNumber + " Destination: " + routeDestination + " Bound: " + routeBound + " Service Type: " + routeServiceType);
    }

    public void initListener(){
        ImageView backBtn = findViewById(R.id.bus_detail_activity_back_button);
        backBtn.setOnClickListener(v -> finish());
    }

    public void initBusRouteStopRecyclerView(){
        busRouteStopItems = new java.util.ArrayList<>();
        busRouteStopItems.add(new BusRouteStopItem("10", "Outbound", "Express", "Wong Tai Sin Bus Terminus", "沙田站", "沙田站", "S123456789"));
        busRouteStopItems.add(new BusRouteStopItem("10", "Inbound", "Regular", "Lai Tak Estate", "深水埗站", "深水埗站", "D123456789"));
        adapter = new BusRouteStopItemAdapter(this, busRouteStopItems);
        busRouteStopRecyclerView.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        busRouteStopRecyclerView.setAdapter(adapter);
    }
}