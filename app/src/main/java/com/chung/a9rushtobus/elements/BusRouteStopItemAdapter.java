package com.chung.a9rushtobus.elements;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.chung.a9rushtobus.R;

import java.util.List;

public class BusRouteStopItemAdapter extends RecyclerView.Adapter<BusRouteStopItemAdapter.ViewHolder> {
    private Context context;
    private List<BusRouteStopItem> items;

    public BusRouteStopItemAdapter(Context context, List<BusRouteStopItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_bus_detail_stop_summary, parent, false);
        return new BusRouteStopItemAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusRouteStopItem item = items.get(position);
        holder.stopName.setText(item.getStopEn());
        holder.stopSeq.setText(String.valueOf(position + 1));
        holder.stopEta.setText("Testing");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stopName;
        TextView stopSeq;
        TextView stopEta;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            stopName = itemView.findViewById(R.id.item_stop_summary_stop_name);
            stopSeq = itemView.findViewById(R.id.item_stop_summary_stop_seq);
            stopEta = itemView.findViewById(R.id.item_stop_summary_stop_eta);
        }
    }

}
