package com.example.inventoryapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DeviceManagerActivity extends AppCompatActivity {
    private List<StorageDevice> devices;
    private RecyclerView deviceList;
    private DeviceAdapter adapter;
    private static final String DEVICES_FILE = "storage_devices.json";
    private AlertDialog currentDeviceDialog;
    private ShelfAdapter currentShelfAdapter;
    private static final int REQUEST_CODE_OPEN_DEVICE_FILE = 2;
    private String currentDeviceNetworkPath = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_manager);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        devices = loadDevices();
        deviceList = findViewById(R.id.deviceList);
        FloatingActionButton addDeviceButton = findViewById(R.id.addDeviceButton);
        Button syncButton = findViewById(R.id.syncDevicesButton);

        // Set up RecyclerView
        deviceList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DeviceAdapter(devices, this);
        ItemTouchHelper.Callback callback = new DeviceItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(deviceList);
        deviceList.setAdapter(adapter);

        if (addDeviceButton != null) {
            addDeviceButton.setOnClickListener(v -> showAddDeviceDialog());
        }

        if (syncButton != null) {
            syncButton.setOnClickListener(v -> showDeviceSyncDialog());
        }
    }

    // Add a method to update the device display immediately
    private void updateDeviceDisplay(StorageDevice device) {
        int position = devices.indexOf(device);
        if (position >= 0) {
            adapter.notifyItemChanged(position);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAddDeviceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_device, null);
        EditText deviceNameInput = dialogView.findViewById(R.id.deviceNameInput);

        builder.setView(dialogView)
                .setTitle("Add Storage Device")
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = deviceNameInput.getText().toString().trim();
                    if (!name.isEmpty()) {
                        StorageDevice device = new StorageDevice(name, devices.size());
                        devices.add(device);
                        adapter.notifyDataSetChanged(); // Update the entire list
                        saveDevices();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateShelfList(StorageDevice device, int position) {
        if (currentShelfAdapter != null) {
            currentShelfAdapter.updateShelves(device.getShelves());
        }
        // Force update the device display to refresh shelf count
        adapter.notifyItemChanged(position);
    }

    public void showEditDeviceDialog(StorageDevice device, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_edit_device, null);

        EditText nameInput = dialogView.findViewById(R.id.deviceNameInput);
        nameInput.setText(device.getName());
        RecyclerView shelfList = dialogView.findViewById(R.id.shelfList);
        EditText newShelfInput = dialogView.findViewById(R.id.newShelfInput);
        Button addShelfButton = dialogView.findViewById(R.id.addShelfButton);

        // Set up shelves list
        currentShelfAdapter = new ShelfAdapter(
                new ArrayList<>(device.getShelves()), // Create new list to avoid reference issues
                (shelf, shelfPosition) -> showEditShelfDialog(device, position, shelf, shelfPosition),
                (shelf, shelfPosition) -> confirmShelfDeletion(device, position, shelf, shelfPosition)
        );

        shelfList.setLayoutManager(new LinearLayoutManager(this));
        shelfList.setAdapter(currentShelfAdapter);

        // Add new shelf functionality
        if (addShelfButton != null && newShelfInput != null) {
            addShelfButton.setOnClickListener(v -> {
                String shelfName = newShelfInput.getText().toString().trim();
                if (!shelfName.isEmpty()) {
                    if (device.addShelf(shelfName)) {
                        newShelfInput.setText("");
                        updateShelfList(device, position);
                        saveDevices();
                    } else {
                        Toast.makeText(this, "Shelf already exists",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        builder.setView(dialogView)
                .setTitle("Manage Device")
                .setPositiveButton("Done", (dialog, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    if (!newName.isEmpty() && !newName.equals(device.getName())) {
                        device.setName(newName);
                        updateShelfList(device, position);
                        saveDevices();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", (dialog, which) -> {
                    confirmDeviceDeletion(device, position);
                });

        currentDeviceDialog = builder.create();
        currentDeviceDialog.show();
    }

    private void showEditShelfDialog(StorageDevice device, int devicePosition,
                                     String shelf, int shelfPosition) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_edit_shelf, null);

        EditText nameInput = dialogView.findViewById(R.id.shelfNameInput);
        nameInput.setText(shelf);

        builder.setView(dialogView)
                .setTitle("Edit Shelf")
                .setPositiveButton("Save", null)  // We'll set this up after dialog creation
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", (dialog, which) -> {
                    confirmShelfDeletion(device, devicePosition, shelf, shelfPosition);
                });

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                String newName = nameInput.getText().toString().trim();
                if (!newName.isEmpty()) {
                    if (canRenameShelf(device, shelf, newName)) {
                        // Update the device
                        device.updateShelf(shelf, newName);

                        // Update any inventory items using this shelf
                        updateItemsAfterShelfRename(device, shelf, newName);

                        // Update the UI
                        if (currentShelfAdapter != null) {
                            currentShelfAdapter.updateShelfName(shelfPosition, newName);
                        }

                        // Update the device display to refresh shelf count if needed
                        adapter.notifyItemChanged(devicePosition);

                        // Save changes
                        saveDevices();

                        // Dismiss the dialog
                        dialog.dismiss();
                    } else {
                        Toast.makeText(DeviceManagerActivity.this,
                                "Cannot rename: shelf name already exists",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });

        dialog.show();
    }

    public boolean hasItemsInDevice(StorageDevice device) {
        try {
            List<FreezerItem> inventory = loadInventory();
            for (FreezerItem item : inventory) {
                if (item.getDevice().equals(device.getName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean hasItemsInShelf(StorageDevice device, String shelf) {
        try {
            List<FreezerItem> inventory = loadInventory();
            for (FreezerItem item : inventory) {
                if (item.getDevice().equals(device.getName()) &&
                        item.getShelf().equals(shelf)) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean canRenameShelf(StorageDevice device, String oldName, String newName) {
        return !device.hasShelf(newName) || oldName.equals(newName);
    }

    public void updateItemsAfterShelfRename(StorageDevice device,
                                            String oldShelf, String newShelf) {
        try {
            List<FreezerItem> inventory = loadInventory();
            boolean changed = false;

            for (FreezerItem item : inventory) {
                if (item.getDevice().equals(device.getName()) &&
                        item.getShelf().equals(oldShelf)) {
                    item.setShelf(newShelf);
                    changed = true;
                }
            }

            if (changed) {
                saveInventory(inventory);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<FreezerItem> loadInventory() {
        File file = new File(getFilesDir(), MainActivity.LOCAL_FILE_NAME);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                return FileHandler.loadInventory(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ArrayList<>();
    }

    private void saveInventory(List<FreezerItem> inventory) {
        try (FileOutputStream fos =
                     openFileOutput(MainActivity.LOCAL_FILE_NAME, MODE_PRIVATE)) {
            FileHandler.saveInventory(new ArrayList<>(inventory), fos);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<StorageDevice> loadDevices() {
        try {
            File file = new File(getFilesDir(), DEVICES_FILE);
            if (!file.exists()) {
                return new ArrayList<>();
            }

            StringBuilder json = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    json.append(line);
                }
            }

            Type type = new TypeToken<ArrayList<StorageDevice>>(){}.getType();
            return new Gson().fromJson(json.toString(), type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public void saveDevices() {
        try {
            String json = new Gson().toJson(devices);
            File file = new File(getFilesDir(), DEVICES_FILE);

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(json);
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error saving devices", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeviceDeletion(StorageDevice device, int position) {
        // First check if device has items
        if (hasItemsInDevice(device)) {
            new AlertDialog.Builder(this)
                    .setTitle("Cannot Delete Device")
                    .setMessage("This device contains items. Please remove or relocate all items before deleting.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete " + device.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    devices.remove(position);
                    adapter.notifyItemRemoved(position);
                    saveDevices();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void confirmShelfDeletion(StorageDevice device, int devicePosition,
                                      String shelf, int shelfPosition) {
        if (hasItemsInShelf(device, shelf)) {
            new AlertDialog.Builder(this)
                    .setTitle("Cannot Delete Shelf")
                    .setMessage("This shelf contains items. Please remove or relocate all items before deleting.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete this shelf?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    device.removeShelf(shelf);
                    updateShelfList(device, devicePosition);
                    saveDevices();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupSyncButton() {
        Button syncButton = findViewById(R.id.syncDevicesButton);
        if (syncButton != null) {
            syncButton.setOnClickListener(v -> showDeviceSyncDialog());
        }
    }

    private void showDeviceSyncDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sync Devices");

        if (currentDeviceNetworkPath == null) {
            builder.setMessage("Please select a network file for devices")
                    .setPositiveButton("Browse", (dialog, id) -> browseDeviceLANFile())
                    .setNegativeButton("Cancel", null);
            builder.create().show();
            return;
        }

        String[] options = {"Push to Network", "Pull from Network"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Push to Network
                    syncDevicesToNetwork();
                    break;
                case 1: // Pull from Network
                    syncDevicesFromNetwork();
                    break;
            }
        });
        builder.create().show();
    }

    private void browseDeviceLANFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, REQUEST_CODE_OPEN_DEVICE_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_DEVICE_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                currentDeviceNetworkPath = uri.toString();
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                showDeviceSyncDialog();
            }
        }
    }

    private void syncDevicesToNetwork() {
        if (currentDeviceNetworkPath != null) {
            try {
                Uri uri = Uri.parse(currentDeviceNetworkPath);
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    String json = new Gson().toJson(devices);
                    outputStream.write(json.getBytes());
                    outputStream.close();
                    Toast.makeText(this, "Successfully synced devices to network",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to sync devices to network: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void syncDevicesFromNetwork() {
        if (currentDeviceNetworkPath != null) {
            try {
                Uri uri = Uri.parse(currentDeviceNetworkPath);
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream));
                    StringBuilder jsonBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonBuilder.append(line);
                    }
                    reader.close();

                    Type type = new TypeToken<ArrayList<StorageDevice>>() {
                    }.getType();
                    List<StorageDevice> newDevices = new Gson().fromJson(
                            jsonBuilder.toString(), type);

                    // Update devices list
                    devices.clear();
                    devices.addAll(newDevices);
                    saveDevices();
                    adapter.notifyDataSetChanged();

                    Toast.makeText(this, "Successfully synced devices from network",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to sync devices from network: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}