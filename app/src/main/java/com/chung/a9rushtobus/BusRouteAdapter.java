package com.chung.a9rushtobus;

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
        holder.tvDestination.setText(String.format("To %s", route.getDestEn()));
        holder.tvOrigin.setText(route.getOrigEn());
        holder.tvRouteBusCompany.setText("KMB");

        // Background and text color logic
        setTextColorAndBackground(holder.tvRouteName, routeNumber);
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
        TextView tvRouteName, tvOrigin, tvDestination, tvRouteBusCompany;
        LinearLayout busRouteItemView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            busRouteItemView = itemView.findViewById(R.id.item_bus_route_info_holder);
            tvRouteName = itemView.findViewById(R.id.tvRouteName);
            tvDestination = itemView.findViewById(R.id.tvRouteDestination);
            tvOrigin = itemView.findViewById(R.id.tvRouteOrigin);
            tvRouteBusCompany = itemView.findViewById(R.id.tvRouteBusCompany);

            busRouteItemView.setOnClickListener(view -> {
                Log.d("BusRouteAdapter", "Item clicked: " + tvRouteName.getText());
                Intent intent = new Intent(view.getContext(), BusRouteDetailViewActivity.class);
                startActivity(view.getContext(), intent, null);
            });

        }
    }
}