package com.chung.a9rushtobus.elements;

import android.content.Context;
import android.graphics.Color;
import android.transition.TransitionManager;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.chung.a9rushtobus.DataFetcher;
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

    @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusRouteStopItem item = items.get(position);
        holder.stopName.setText(item.getStopEn());
        holder.stopSeq.setText(String.valueOf(position + 1));
//        View etaLayout = LayoutInflater.from(context).inflate(R.layout.item_bus_detail_stop_etas, holder.etaLayout, false);
//        holder.etaLayout.addView(etaLayout);

        for (int i = 0; i < 3; i++) {
            TextView etaTextView = new TextView(context);
            etaTextView.setText("15:40  40min  KMB");
            etaTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            etaTextView.setTextColor(Color.BLACK);
            etaTextView.setPadding(8, 0, 8, 0);
            holder.etaLayout.addView(etaTextView);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView stopName;
        TextView stopSeq;
        TextView stopEta;
        ConstraintLayout mainLayout, detailLayout;
        LinearLayout etaLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mainLayout = itemView.findViewById(R.id.stopRouteItem_main_layout);
            detailLayout = itemView.findViewById(R.id.stopRouteItem_details_layout);
            stopName = itemView.findViewById(R.id.item_stop_summary_stop_name);
            stopSeq = itemView.findViewById(R.id.item_stop_summary_stop_seq);
            stopEta = itemView.findViewById(R.id.item_stop_summary_stop_eta);
            etaLayout = itemView.findViewById(R.id.eta_container);

            listenerInit();
        }

        private void listenerInit() {
            DataFetcher dataFetcher = new DataFetcher();
            dataFetcher.fetchStopEtaInfo();
            mainLayout.setOnClickListener(v -> {
                // Create a transition
                TransitionManager.beginDelayedTransition((ViewGroup) detailLayout.getParent());

                // Toggle visibility with animation
                if (detailLayout.getVisibility() == View.GONE) {
                    detailLayout.setVisibility(View.VISIBLE);
                } else {
                    detailLayout.setVisibility(View.GONE);
                }
            });
        }

    }

}
