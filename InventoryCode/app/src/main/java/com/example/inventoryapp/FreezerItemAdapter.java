package com.example.inventoryapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class FreezerItemAdapter extends ArrayAdapter<FreezerItem> {
    private Context context;
    private ArrayList<FreezerItem> items;

    public FreezerItemAdapter(Context context, ArrayList<FreezerItem> items) {
        super(context, 0, items);
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        FreezerItem item = getItem(position);
        if (item == null) return convertView;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.list_item, parent, false);
        }

        try {
            TextView description = convertView.findViewById(R.id.itemDescription);
            TextView details = convertView.findViewById(R.id.itemDetails);
            TextView expDate = convertView.findViewById(R.id.expirationDate);
            TextView location = convertView.findViewById(R.id.itemLocation);
            TextView tags = convertView.findViewById(R.id.itemTags);
            TextView comments = convertView.findViewById(R.id.itemComments);

            if (description != null) {
                description.setText(item.getDescription());
            }

            if (details != null) {
                details.setText(String.format("Quantity: %d • Added: %s",
                        item.getQuantity(),
                        item.getDateAdded().format()));
            }

            if (expDate != null) {
                if (item.getExpirationDate() != null) {
                    expDate.setText(String.format("Expires: %s",
                            item.getExpirationDate().format()));
                    expDate.setVisibility(View.VISIBLE);
                } else {
                    expDate.setVisibility(View.GONE);
                }
            }

            if (location != null) {
                location.setText(String.format("%s - %s",
                        item.getDevice(),
                        item.getShelf()));
            }

            TextView amountDetails = convertView.findViewById(R.id.amountDetails);
            if (amountDetails != null) {
                if (item.getAmount() != null && !item.getAmount().isEmpty()) {
                    amountDetails.setText("Amount: " + item.getAmount());
                    amountDetails.setVisibility(View.VISIBLE);
                } else {
                    amountDetails.setVisibility(View.GONE);
                }
            }

            // Set tags if present
            if (tags != null) {
                if (!item.getTags().isEmpty()) {
                    tags.setText(String.format("Tags: %s",
                            String.join(", ", item.getTags())));
                    tags.setVisibility(View.VISIBLE);
                } else {
                    tags.setVisibility(View.GONE);
                }
            }

            // Set comments if present
            if (comments != null) {
                String commentText = item.getComments();
                if (commentText != null && !commentText.isEmpty()) {
                    comments.setText(commentText);
                    comments.setVisibility(View.VISIBLE);
                } else {
                    comments.setVisibility(View.GONE);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return convertView;
    }
}