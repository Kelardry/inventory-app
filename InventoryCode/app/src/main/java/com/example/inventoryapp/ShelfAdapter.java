package com.example.inventoryapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ShelfAdapter extends RecyclerView.Adapter<ShelfAdapter.ShelfViewHolder> {
    private List<String> shelves;
    private final OnShelfClickListener editListener;
    private final OnShelfClickListener deleteListener;

    public interface OnShelfClickListener {
        void onShelfClick(String shelf, int position);
    }

    public ShelfAdapter(List<String> shelves, OnShelfClickListener editListener,
                        OnShelfClickListener deleteListener) {
        this.shelves = new ArrayList<>(shelves);
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    public void updateShelves(List<String> newShelves) {
        // Create a new list instance to ensure proper update
        this.shelves = new ArrayList<>(newShelves);
        // Force a complete refresh of the RecyclerView
        notifyDataSetChanged();
    }

    public void updateShelfName(int position, String newName) {
        if (position >= 0 && position < shelves.size()) {
            shelves.set(position, newName);
            notifyItemChanged(position);
        }
    }

    @Override
    public ShelfViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.shelf_list_item, parent, false);
        return new ShelfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ShelfViewHolder holder, int position) {
        String shelf = shelves.get(position);
        holder.shelfName.setText(shelf);

        if (editListener != null) {
            holder.editButton.setOnClickListener(v ->
                    editListener.onShelfClick(shelf, holder.getAdapterPosition()));
        }

        if (deleteListener != null) {
            holder.deleteButton.setOnClickListener(v ->
                    deleteListener.onShelfClick(shelf, holder.getAdapterPosition()));
        }
    }

    @Override
    public int getItemCount() {
        return shelves.size();
    }

    static class ShelfViewHolder extends RecyclerView.ViewHolder {
        TextView shelfName;
        ImageButton editButton;
        ImageButton deleteButton;

        ShelfViewHolder(View itemView) {
            super(itemView);
            shelfName = itemView.findViewById(R.id.shelfName);
            editButton = itemView.findViewById(R.id.editShelfButton);
            deleteButton = itemView.findViewById(R.id.deleteShelfButton);
        }
    }
}