package com.example.ecolens;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

// Import the inner class from EventDecorator
import com.example.ecolens.EventDecorator.HistoryItem;

public class DiaryAdapter extends RecyclerView.Adapter<DiaryAdapter.ViewHolder> {

    private final List<HistoryItem> items;

    public DiaryAdapter(List<HistoryItem> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        String title = item.getWasteType();
        boolean isAchievement = item.getWasteType() != null && item.getWasteType().contains("Unlocked");

        if (isAchievement) {
            holder.imgIcon.setImageResource(R.drawable.ic_trophy);
            holder.imgIcon.setColorFilter(0xFFFFD700);
            holder.tvTitle.setText(item.getWasteType());
            holder.tvAmount.setVisibility(View.GONE);

            if (title.contains("Bronze")) {
                holder.imgIcon.setColorFilter(0xFFCD7F32); // Bronze Color
            } else if (title.contains("Silver")) {
                holder.imgIcon.setColorFilter(0xFF9E9E9E); // Darker Silver (visible on white)
            } else if (title.contains("Gold")) {
                holder.imgIcon.setColorFilter(0xFFFFD700); // Gold
            } else if (title.contains("Platinum")) {
                holder.imgIcon.setColorFilter(0xFF455A64); // Platinum (Blue-Grey)
            } else if (title.contains("Diamond")) {
                holder.imgIcon.setColorFilter(0xFF00B0FF); // Diamond (Bright Blue)
            } else {
                holder.imgIcon.setColorFilter(0xFFFFD700); // Default to Gold
            }
        } else {
            holder.imgIcon.setImageResource(R.drawable.ic_leaf_streak);
            holder.imgIcon.setColorFilter(null);
            holder.tvTitle.setText(item.getWasteType());
            holder.tvAmount.setVisibility(View.VISIBLE);
            holder.tvAmount.setText(String.format(Locale.getDefault(), "%.2f kg", item.getAmount()));
        }

        if (item.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            holder.tvTime.setText(sdf.format(item.getTimestamp().toDate()));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgIcon;
        TextView tvTitle, tvAmount, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgIcon = itemView.findViewById(R.id.img_log_icon);
            tvTitle = itemView.findViewById(R.id.tv_log_title);
            tvAmount = itemView.findViewById(R.id.tv_log_amount);
            tvTime = itemView.findViewById(R.id.tv_log_time);
        }
    }
}