package com.example.studyvault_final_app;

import android.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Map;

public class StudySetAdapter extends RecyclerView.Adapter<StudySetAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> set);
    }

    public interface OnItemDeleteListener {
        void onItemDelete(Map<String, Object> set, int position);
    }

    private final List<Map<String, Object>> sets;
    private final OnItemClickListener listener;
    private OnItemDeleteListener deleteListener;

    public StudySetAdapter(List<Map<String, Object>> sets, OnItemClickListener listener) {
        this.sets     = sets;
        this.listener = listener;
    }

    public void setOnItemDeleteListener(OnItemDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_study_set, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> set = sets.get(position);
        holder.tvTitle.setText((String) set.getOrDefault("title", "Untitled"));
        Object count = set.get("cardCount");
        holder.tvSub.setText(count != null ? count + " cards" : "Tap to open");

        holder.itemView.setOnClickListener(v -> listener.onItemClick(set));

        holder.itemView.setOnLongClickListener(v -> {
            if (deleteListener == null) return false;
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Study Set")
                    .setMessage("Delete \"" + set.getOrDefault("title", "this set") + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_ID) {
                            deleteListener.onItemDelete(set, pos);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() { return sets.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvSub;
        ViewHolder(View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvSetTitle);
            tvSub   = v.findViewById(R.id.tvSetSub);
        }
    }
}