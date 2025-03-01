package com.chung.a9rushtobus;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

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
        BusRoute route = busRoutes.get(position);
        String routeNumber = route.getRoute();

        holder.tvRouteName.setText(routeNumber);
        holder.tvDestination.setText("To " + route.getDestEn());
        holder.tvOrigin.setText(route.getOrigEn());
        holder.tvRouteBusCompany.setText("KMB");

        if (routeNumber.startsWith("A") || routeNumber.startsWith("E")) {
            holder.tvRouteName.setBackgroundColor(context.getColor(R.color.externalBusBackground));
            holder.tvRouteName.setTextColor(context.getColor(R.color.externalBusForeground));
        } else if (routeNumber.startsWith("N")) {
            holder.tvRouteName.setBackgroundColor(Color.GRAY);
            holder.tvRouteName.setTextColor(Color.WHITE);
        } else if (routeNumber.matches("^[169]\\d{2}[A-Za-z]?$")) {
            // Matches 1XX, 1XX*, 6XX, 6XX* where X is number and * is letter
            holder.tvRouteName.setBackgroundColor(context.getColor(R.color.crossHarbourBusBackground));
            holder.tvRouteName.setTextColor(context.getColor(R.color.crossHarbourBusForeground));
        } else {
            holder.tvRouteName.setBackgroundColor(Color.TRANSPARENT);
            holder.tvRouteName.setTextColor(context.getColor(R.color.foreground));
        }

    }

    @Override
    public int getItemCount() {
        return busRoutes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRouteName, tvOrigin, tvDestination, tvRouteBusCompany;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
            tvDestination = itemView.findViewById(R.id.tvRouteDestination);
            tvOrigin = itemView.findViewById(R.id.tvRouteOrigin);
            tvRouteBusCompany = itemView.findViewById(R.id.tvRouteBusCompany);
        }
    }
}
