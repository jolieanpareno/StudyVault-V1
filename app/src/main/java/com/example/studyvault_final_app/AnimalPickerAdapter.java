package com.example.studyvault_final_app;

import android.graphics.Color;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AnimalPickerAdapter extends RecyclerView.Adapter<AnimalPickerAdapter.VH> {

    public interface OnAnimalSelected { void onSelected(String animal); }

    private final List<String> animals;
    private String selectedAnimal;
    private final OnAnimalSelected listener;

    public AnimalPickerAdapter(List<String> animals, String selectedAnimal, OnAnimalSelected listener) {
        this.animals       = animals;
        this.selectedAnimal = selectedAnimal;
        this.listener      = listener;
    }

    public void setSelected(String animal) {
        this.selectedAnimal = animal;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        TextView tv = new TextView(parent.getContext());
        tv.setTextSize(28f);
        tv.setGravity(android.view.Gravity.CENTER);

        int size = (int) (56 * parent.getContext().getResources().getDisplayMetrics().density);
        int margin = (int) (4 * parent.getContext().getResources().getDisplayMetrics().density);
        int radius = (int) (12 * parent.getContext().getResources().getDisplayMetrics().density);

        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
        params.setMargins(margin, margin, margin, margin);
        tv.setLayoutParams(params);
        tv.setPadding(0, 0, 0, 0);

        // Rounded background shape programmatically
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setCornerRadius(radius);
        tv.setBackground(bg);

        return new VH(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        String animal = animals.get(position);
        holder.tv.setText(animal);

        boolean isSelected = animal.equals(selectedAnimal);
        android.graphics.drawable.GradientDrawable bg =
                (android.graphics.drawable.GradientDrawable) holder.tv.getBackground();

        if (isSelected) {
            bg.setColor(Color.parseColor("#7F77DD"));   // purple highlight
            bg.setStroke(
                    (int)(2 * holder.tv.getContext().getResources().getDisplayMetrics().density),
                    Color.WHITE
            );
        } else {
            bg.setColor(Color.parseColor("#12253A"));   // dark card colour
            bg.setStroke(0, Color.TRANSPARENT);
        }

        holder.tv.setOnClickListener(v -> {
            selectedAnimal = animal;
            notifyDataSetChanged();
            listener.onSelected(animal);
        });
    }

    @Override public int getItemCount() { return animals.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tv;
        VH(TextView tv) { super(tv); this.tv = tv; }
    }
}