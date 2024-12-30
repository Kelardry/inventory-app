package com.example.inventoryapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collections;
import java.util.List;

public class DeviceAdapter extends RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder> {
    private final List<StorageDevice> devices;
    private final DeviceManagerActivity activity;
    private Context context;

    public DeviceAdapter(List<StorageDevice> devices, DeviceManagerActivity activity) {
        this.devices = devices;
        this.context = activity;
        this.activity = activity;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.device_list_item, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceViewHolder holder, int position) {
        StorageDevice device = devices.get(position);
        holder.deviceName.setText(device.getName());
        // Update shelf count text
        int shelfCount = device.getShelves().size();
        holder.shelfCount.setText("Shelves: " + shelfCount);

        holder.itemView.setOnClickListener(v ->
                activity.showEditDeviceDialog(device, holder.getAdapterPosition()));
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public void showEditDeviceDialog(StorageDevice device, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_edit_device, null);

        EditText nameInput = dialogView.findViewById(R.id.deviceNameInput);
        nameInput.setText(device.getName());

        builder.setView(dialogView)
                .setTitle("Edit Storage Device")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        device.setName(newName);
                        notifyItemChanged(position);
                        activity.saveDevices();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", (dialog, which) -> {
                    confirmDeviceDeletion(device, position);
                })
                .show();
    }

    private void confirmDeviceDeletion(StorageDevice device, int position) {
        // Check if device has items
        if (activity.hasItemsInDevice(device)) {
            new AlertDialog.Builder(context)
                    .setTitle("Cannot Delete Device")
                    .setMessage("This device contains items. Please remove or relocate all items before deleting.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete " + device.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    devices.remove(position);
                    notifyItemRemoved(position);
                    activity.saveDevices();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showShelvesDialog(StorageDevice device, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_manage_shelves, null);

        RecyclerView shelfList = dialogView.findViewById(R.id.shelfList);
        EditText newShelfInput = dialogView.findViewById(R.id.newShelfInput);
        Button addShelfButton = dialogView.findViewById(R.id.addShelfButton);

        ShelfAdapter shelfAdapter = new ShelfAdapter(device.getShelves(),
                (shelf, shelfPosition) -> {
                    showEditShelfDialog(device, position, shelf, shelfPosition);
                },
                (shelf, shelfPosition) -> {
                    confirmShelfDeletion(device, position, shelf, shelfPosition);
                });

        shelfList.setLayoutManager(new LinearLayoutManager(context));
        shelfList.setAdapter(shelfAdapter);

        addShelfButton.setOnClickListener(v -> {
            String shelfName = newShelfInput.getText().toString().trim();
            if (!shelfName.isEmpty()) {
                if (device.hasShelf(shelfName)) {
                    Toast.makeText(context, "Shelf already exists",
                            Toast.LENGTH_SHORT).show();
                } else {
                    device.addShelf(shelfName);
                    shelfAdapter.notifyItemInserted(device.getShelves().size() - 1);
                    newShelfInput.setText("");
                    activity.saveDevices();
                }
            }
        });

        builder.setView(dialogView)
                .setTitle("Manage Shelves")
                .setPositiveButton("Done", null)
                .show();
    }

    private void showEditShelfDialog(StorageDevice device, int devicePosition,
                                     String shelf, int shelfPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context)
                .inflate(R.layout.dialog_edit_shelf, null);

        EditText nameInput = dialogView.findViewById(R.id.shelfNameInput);
        nameInput.setText(shelf);

        builder.setView(dialogView)
                .setTitle("Edit Shelf")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        // Check if we can safely rename
                        if (activity.canRenameShelf(device, shelf, newName)) {
                            device.updateShelf(shelf, newName);
                            activity.updateItemsAfterShelfRename(device, shelf, newName);
                            notifyItemChanged(devicePosition);
                            activity.saveDevices();
                        } else {
                            Toast.makeText(context,
                                    "Cannot rename: shelf name already exists",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", (dialog, which) -> {
                    confirmShelfDeletion(device, devicePosition, shelf, shelfPosition);
                })
                .show();
    }

    private void confirmShelfDeletion(StorageDevice device, int devicePosition,
                                      String shelf, int shelfPosition) {
        if (activity.hasItemsInShelf(device, shelf)) {
            new AlertDialog.Builder(context)
                    .setTitle("Cannot Delete Shelf")
                    .setMessage("This shelf contains items. Please remove or relocate all items before deleting.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this shelf?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    device.removeShelf(shelf);
                    notifyItemChanged(devicePosition);
                    activity.saveDevices();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(devices, i, i + 1);
                devices.get(i).setOrderIndex(i);
                devices.get(i + 1).setOrderIndex(i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(devices, i, i - 1);
                devices.get(i).setOrderIndex(i);
                devices.get(i - 1).setOrderIndex(i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        activity.saveDevices();
    }

    public static class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView shelfCount;
        ImageButton editShelvesButton;

        DeviceViewHolder(View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(R.id.deviceName);
            shelfCount = itemView.findViewById(R.id.shelfCount);
        }
    }
}

