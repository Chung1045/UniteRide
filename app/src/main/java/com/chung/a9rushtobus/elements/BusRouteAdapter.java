package com.chung.a9rushtobus.elements;

import static androidx.core.content.ContextCompat.startActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chung.a9rushtobus.BusRouteDetailViewActivity;
import com.chung.a9rushtobus.R;

import java.util.List;
import java.util.Objects;

public class BusRouteAdapter extends RecyclerView.Adapter<BusRouteAdapter.ViewHolder> {
    private Context context;
    private List<BusRoute> busRoutes;

    public BusRouteAdapter(Context context, List<BusRoute> busRoutes) {
        this.context = context;
        this.busRoutes = busRoutes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bus_route, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusRoute routeInfo = busRoutes.get(position);
        String routeNumber = routeInfo.getRoute();
        holder.tvRouteName.setText(routeNumber);
        holder.tvDestination.setText(String.format("To %s", routeInfo.getDestEn()));
        holder.tvOrigin.setText(routeInfo.getOrigEn());
        
        // Debug log for each route being displayed
        Log.d("BusRouteAdapter", "Displaying route: " + routeNumber + 
              ", Company: " + routeInfo.getCompany() + 
              ", Origin: " + routeInfo.getOrigEn() + 
              ", Destination: " + routeInfo.getDestEn());

        if (routeInfo.getCompany().equals("kmb")) {
            holder.tvRouteBusCompany.setText("KMB");
            holder.tvRouteRemarks.setVisibility(View.GONE);
        } else if (routeInfo.getCompany().equals("ctb")) {
            holder.tvRouteBusCompany.setText("CTB");
            holder.tvRouteRemarks.setVisibility(View.GONE);
        } else if (routeInfo.getCompany().equals("GMB")) {
            holder.tvRouteBusCompany.setText("GMB");
            holder.tvRouteBusCompany.setText("GMB-" + routeInfo.getGmbRouteRegion());
            Log.d("BusRouteAdapter", "GMB Route Description: " + routeInfo.getDescriptionEn());
            if (routeInfo.getDescriptionEn().equals("Normal Route") || routeInfo.getDescriptionEn().equals("Normal Departure")) {
                // TODO: Add multi language support
                holder.tvRouteRemarks.setVisibility(View.GONE);
            } else {
                holder.tvRouteRemarks.setVisibility(View.VISIBLE);
                holder.tvRouteRemarks.setText(routeInfo.getDescriptionEn());
            }

        }


        holder.bind(routeInfo);

        if (routeInfo.getCompany().equals("kmb") && !Objects.equals(routeInfo.getServiceType(), "1")){
            holder.routeSpecialIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.routeSpecialIndicator.setVisibility(View.GONE);
        }

        // Background and text color logic
        if (routeInfo.getCompany().equals("kmb") || routeInfo.getCompany().equals("ctb")) {
            setTextColorAndBackground(holder.tvRouteName, routeNumber);
        }
    }

    private void setTextColorAndBackground(TextView textView, String routeNumber) {
        if (routeNumber.startsWith("A") || routeNumber.startsWith("E")) {
            textView.setBackgroundColor(context.getColor(R.color.externalBusBackground));
            textView.setTextColor(context.getColor(R.color.externalBusForeground));
        } else if (routeNumber.startsWith("N")) {
            textView.setBackgroundColor(Color.GRAY);
            textView.setTextColor(Color.WHITE);
        } else if (routeNumber.matches("^[169]\\d{2}[A-Za-z]?$")) {
            textView.setBackgroundColor(context.getColor(R.color.crossHarbourBusBackground));
            textView.setTextColor(context.getColor(R.color.crossHarbourBusForeground));
        } else {
            textView.setBackgroundColor(Color.TRANSPARENT);
            textView.setTextColor(context.getColor(R.color.foreground));
        }
    }

    @Override
    public int getItemCount() {
        return busRoutes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRouteName, tvOrigin, tvDestination, tvRouteBusCompany, routeSpecialIndicator, tvRouteRemarks;
        LinearLayout busRouteItemView;
        private BusRoute routeInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            busRouteItemView = itemView.findViewById(R.id.item_bus_route_info_holder);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
            tvDestination = itemView.findViewById(R.id.tvRouteDestination);
            tvOrigin = itemView.findViewById(R.id.tvRouteOrigin);
            tvRouteBusCompany = itemView.findViewById(R.id.tvRouteBusCompany);
            routeSpecialIndicator = itemView.findViewById(R.id.tvRouteBusSpecialIndicator);
            tvRouteRemarks = itemView.findViewById(R.id.tvRouteRemarks);
            tvDestination.setSelected(true);
            tvOrigin.setSelected(true);

            busRouteItemView.setOnClickListener(view -> {
                Log.d("BusRouteAdapter", "Item clicked: " + tvRouteName.getText());
                Intent intent = new Intent(view.getContext(), BusRouteDetailViewActivity.class);
                intent.putExtra("route", routeInfo.getRoute());
                intent.putExtra("destination", routeInfo.getDestEn());
                intent.putExtra("bound", routeInfo.getBound());
                intent.putExtra("serviceType", routeInfo.getServiceType());
                intent.putExtra("company", routeInfo.getCompany());
                intent.putExtra("description", routeInfo.getDescriptionEn());

                if (routeInfo.getCompany().equals("GMB")) {
                    intent.putExtra("gmbRouteID", routeInfo.getGmbRouteID());
                    intent.putExtra("gmbRouteSeq", routeInfo.getBound());

                    Log.d("BusRouteAdapter", "GMB Route ID: " + routeInfo.getGmbRouteID());
                    Log.d("BusRouteAdapter", "GMB Route Seq: " + routeInfo.getBound());

                }


                Log.e("BusRouteAdapter", "Item clicked: " + "Route " + routeInfo.getRoute() + " Destination " + routeInfo.getDestEn() + " Bound " + routeInfo.getBound() + " ServiceType " + routeInfo.getServiceType() + " Company " + routeInfo.getCompany());
                Log.e("BusRouteAdapter", "Attempting to start new BusRouteDetailViewActivity");
                startActivity(view.getContext(), intent, null);
            });
        }

        public void bind(BusRoute route) {
            this.routeInfo = route;
        }

    }
}