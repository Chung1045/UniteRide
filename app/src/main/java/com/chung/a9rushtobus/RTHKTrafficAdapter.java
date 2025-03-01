package com.chung.a9rushtobus;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RTHKTrafficAdapter extends RecyclerView.Adapter<RTHKTrafficAdapter.ViewHolder> {
    private List<RTHKTrafficEntry> entries;

    public RTHKTrafficAdapter(List<RTHKTrafficEntry> entries) {
        this.entries = entries;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_rthk_traffic_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RTHKTrafficEntry entry = entries.get(position);
        holder.bind(entry);
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public void updateEntries(List<RTHKTrafficEntry> newEntries) {
        entries.clear();
        entries.addAll(newEntries);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView newsTextView;
        private final TextView dateTextView;

        ViewHolder(View itemView) {
            super(itemView);
            newsTextView = itemView.findViewById(R.id.text_news);
            dateTextView = itemView.findViewById(R.id.text_date);
        }

        void bind(RTHKTrafficEntry entry) {
            newsTextView.setText(entry.getNews());
            dateTextView.setText(entry.getDate());
        }
    }
}