package com.example.inventoryapp;

// MainActivity.java
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends AppCompatActivity {
    public static final String LOCAL_FILE_NAME = "freezer_inventory.csv";
    private ArrayList<FreezerItem> inventory;
    private ListView listView;
    private FreezerItemAdapter adapter;
    private String currentNetworkPath = null;
    private static final int REQUEST_CODE_OPEN_FILE = 1;
    private static final String PREFS_NAME = "InventoryPrefs";
    private static final String PREF_INVENTORY_PATH = "inventoryNetworkPath";
    private Filter currentFilter = new Filter();
    private Spinner deviceFilter, shelfFilterMain, shelfFilter, tagFilter;
    private EditText quantityMinFilter, quantityMaxFilter;
    private List<StorageDevice> devices;
    private StorageDevice selectedDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // First initialize basic views
        listView = findViewById(R.id.inventoryList);
        Button addButton = findViewById(R.id.addButton);
        Button syncButton = findViewById(R.id.syncButton);
        Button filterButton = findViewById(R.id.filterButton);
        Button manageDevicesButton = findViewById(R.id.manageDevicesButton);
        EditText searchField = findViewById(R.id.searchField);
        deviceFilter = findViewById(R.id.deviceFilter);

        // Initialize data
        devices = loadDevices();
        inventory = new ArrayList<>();

        // Set up adapter
        adapter = new FreezerItemAdapter(this, inventory);
        listView.setAdapter(adapter);

        // Load inventory
        loadLocalInventory();

        // Set up the UI components after data is loaded
        initializeUIComponents();

        // Set up button listeners
        if (addButton != null) {
            addButton.setOnClickListener(v -> showAddItemDialog());
        }

        if (syncButton != null) {
            syncButton.setOnClickListener(v -> showSyncDialog());
        }

        if (filterButton != null) {
            filterButton.setOnClickListener(v -> showFilterDialog());
        }

        if (manageDevicesButton != null) {
            manageDevicesButton.setOnClickListener(v -> {
                startActivity(new Intent(this, DeviceManagerActivity.class));
            });
        }

        // Search functionality
        if (searchField != null) {
            searchField.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    currentFilter.setSearchText(s.toString());
                    applyFiltersAndSort();
                }
            });
        }

        setupListViewClickListener();
    }


    private void initializeUIComponents() {
        if (deviceFilter == null) {
            return; // Safety check
        }

        // Set up device filter
        updateDeviceFilterOptions();

        // Find and initialize shelf filter
        shelfFilterMain = findViewById(R.id.shelfFilterMain);
        if (shelfFilterMain != null) {
            // Initialize shelf filter with empty adapter
            shelfFilterMain.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, new ArrayList<>()));
            shelfFilterMain.setEnabled(false); // Initially disabled until device is selected
        }

        deviceFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDeviceName = position == 0 ? null : parent.getItemAtPosition(position).toString();
                currentFilter.setSelectedDevice(selectedDeviceName);

                // Update shelf filter based on device selection
                if (shelfFilterMain != null) {
                    if (selectedDeviceName == null) {
                        shelfFilterMain.setEnabled(false);
                        shelfFilterMain.setAdapter(new ArrayAdapter<>(MainActivity.this,
                                android.R.layout.simple_spinner_item, new ArrayList<>()));
                    } else {
                        shelfFilterMain.setEnabled(true);
                        updateShelfFilterOptions(shelfFilterMain, selectedDeviceName);
                    }
                }

                applyFiltersAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentFilter.setSelectedDevice(null);
                if (shelfFilterMain != null) {
                    shelfFilterMain.setEnabled(false);
                }
                applyFiltersAndSort();
            }
        });

        // Set up shelf filter listener
        if (shelfFilterMain != null) {
            shelfFilterMain.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (!shelfFilterMain.isEnabled()) return;
                    String selectedShelf = position == 0 ? null : parent.getItemAtPosition(position).toString();
                    currentFilter.setSelectedShelf(selectedShelf);
                    applyFiltersAndSort();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    currentFilter.setSelectedShelf(null);
                    applyFiltersAndSort();
                }
            });
        }
    }

    private List<StorageDevice> loadDevices() {
        try {
            File file = new File(getFilesDir(), "storage_devices.json");
            if (!file.exists()) {
                return new ArrayList<>();
            }
            String json = FileHandler.readFileContent(file);
            Type type = new TypeToken<ArrayList<StorageDevice>>(){}.getType();
            return new Gson().fromJson(json, type);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private boolean validateAndSaveItem(View dialogView, FreezerItem existingItem, Dialog dialog) {
        try {
            Spinner deviceSpinner = dialogView.findViewById(R.id.deviceSpinner);
            Spinner shelfSpinner = dialogView.findViewById(R.id.shelfSpinner);
            EditText descInput = dialogView.findViewById(R.id.descriptionInput);
            EditText quantityInput = dialogView.findViewById(R.id.quantityInput);
            Spinner dateAddedMonthSpinner = dialogView.findViewById(R.id.dateAddedMonthSpinner);
            Spinner dateAddedYearSpinner = dialogView.findViewById(R.id.dateAddedYearSpinner);
            Spinner expMonthSpinner = dialogView.findViewById(R.id.expMonthSpinner);
            Spinner expYearSpinner = dialogView.findViewById(R.id.expYearSpinner);
            EditText tagsInput = dialogView.findViewById(R.id.tagsInput);
            EditText commentsInput = dialogView.findViewById(R.id.commentsInput);
            EditText amountInput = dialogView.findViewById(R.id.amountInput);
            CheckBox expDateCheckbox = dialogView.findViewById(R.id.expDateCheckbox);

            // Validate all required fields
            if (deviceSpinner == null || deviceSpinner.getSelectedItem() == null ||
                    shelfSpinner == null || shelfSpinner.getSelectedItem() == null) {
                showValidationErrors(Collections.singletonList("Please select device and shelf"));
                return false;
            }

            String device = deviceSpinner.getSelectedItem().toString();
            String shelf = shelfSpinner.getSelectedItem().toString();
            String description = descInput.getText().toString().trim();
            int quantity = Integer.parseInt(quantityInput.getText().toString());
            String comments = commentsInput != null ? commentsInput.getText().toString().trim() : "";
            String amount = amountInput != null ? amountInput.getText().toString().trim() : "";

            FreezerDate dateAdded = new FreezerDate(
                    Integer.parseInt(dateAddedYearSpinner.getSelectedItem().toString()),
                    dateAddedMonthSpinner.getSelectedItemPosition() + 1
            );

            FreezerDate expDate = null;
            if (expDateCheckbox.isChecked()) {
                expDate = new FreezerDate(
                        Integer.parseInt(expYearSpinner.getSelectedItem().toString()),
                        expMonthSpinner.getSelectedItemPosition() + 1
                );
            }

            Set<String> tags = new TreeSet<>();
            if (tagsInput != null && !tagsInput.getText().toString().trim().isEmpty()) {
                tags.addAll(Arrays.asList(
                        tagsInput.getText().toString().toLowerCase().split("\\s*,\\s*")));
            }

            // Validate input
            ArrayList<String> errors = new ArrayList<>();
            if (description.isEmpty()) {
                errors.add("Description is required");
            }
            if (quantity < 1) {
                errors.add("Quantity must be at least 1");
            }
            if (expDate != null && expDate.compareTo(dateAdded) < 0) {
                errors.add("Expiration date must be after date added");
            }

            if (!errors.isEmpty()) {
                showValidationErrors(errors);
                return false;
            }

            // Create or update item
            FreezerItem item = new FreezerItem(device, shelf, dateAdded, description,
                    quantity, expDate, tags, comments, amount);

            if (existingItem != null) {
                inventory.remove(existingItem);
            }
            inventory.add(item);

            saveLocalInventory();
            applyFiltersAndSort();
            return true;

        } catch (Exception e) {
            showValidationErrors(Collections.singletonList("Error: " + e.getMessage()));
            return false;
        }
    }



    private void showSyncDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sync Options");

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentNetworkPath = prefs.getString(PREF_INVENTORY_PATH, null);

        if (currentNetworkPath == null) {
            builder.setMessage("Please select a network file first")
                    .setPositiveButton("Browse", (dialog, id) -> browseLANFile())
                    .setNegativeButton("Cancel", null);
            builder.create().show();
            return;
        }

        // Show current path and confirm or change
        builder.setMessage("Current network file:\n" + currentNetworkPath)
                .setPositiveButton("Use This File", (dialog, id) -> {
                    showSyncDirectionDialog();
                })
                .setNegativeButton("Browse New File", (dialog, id) -> {
                    browseLANFile();
                })
                .setNeutralButton("Cancel", null);
        builder.create().show();
    }

    private void showSyncDirectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Sync Direction")
                .setItems(new String[]{"Push to Network", "Pull from Network"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            syncToNetwork();
                            break;
                        case 1:
                            syncFromNetwork();
                            break;
                    }
                });
        builder.create().show();
    }

    private void browseLANFile() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/*");
        startActivityForResult(intent, REQUEST_CODE_OPEN_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OPEN_FILE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                currentNetworkPath = uri.toString();
                // Save the path
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putString(PREF_INVENTORY_PATH, currentNetworkPath).apply();
                // Get persistent permission for the URI
                getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                showSyncDirectionDialog();
            }
        }
    }

    private void syncToNetwork() {
        if (currentNetworkPath != null) {
            try {
                Uri uri = Uri.parse(currentNetworkPath);
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                if (outputStream != null) {
                    FileHandler.saveInventory(inventory, outputStream);
                    Toast.makeText(this, "Successfully synced to network",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to sync to network: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void syncFromNetwork() {
        if (currentNetworkPath != null) {
            try {
                Uri uri = Uri.parse(currentNetworkPath);
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    inventory.clear();
                    inventory.addAll(FileHandler.loadInventory(inputStream));
                    saveLocalInventory(); // Save the new data locally
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "Successfully synced from network",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "Failed to sync from network: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadLocalInventory() {
        File file = new File(getFilesDir(), LOCAL_FILE_NAME);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                inventory.clear();
                inventory.addAll(FileHandler.loadInventory(fis));
                adapter.notifyDataSetChanged();
            } catch (IOException e) {
                Toast.makeText(this, "Error loading local inventory",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveLocalInventory() {
        try (FileOutputStream fos = openFileOutput(LOCAL_FILE_NAME, MODE_PRIVATE)) {
            FileHandler.saveInventory(inventory, fos);
        } catch (IOException e) {
            Toast.makeText(this, "Error saving local inventory",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddItemDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
        builder.setView(dialogView);

        // Get references to all input fields
        final Spinner shelfSpinner = dialogView.findViewById(R.id.shelfSpinner);
        final Spinner dateAddedMonthSpinner = dialogView.findViewById(R.id.dateAddedMonthSpinner);
        final Spinner dateAddedYearSpinner = dialogView.findViewById(R.id.dateAddedYearSpinner);
        final EditText descriptionInput = dialogView.findViewById(R.id.descriptionInput);
        final EditText quantityInput = dialogView.findViewById(R.id.quantityInput);
        final Spinner expMonthSpinner = dialogView.findViewById(R.id.expMonthSpinner);
        final Spinner expYearSpinner = dialogView.findViewById(R.id.expYearSpinner);
        final EditText tagsInput = dialogView.findViewById(R.id.tagsInput);

        // Initialize the dialog fields with default values
        initializeDialogFields(dialogView, null);

        builder.setPositiveButton("Add", null); // We'll set this up after dialog creation
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                if (validateAndSaveItem(dialogView, null, dialog)) {
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void filterInventory(String query) {
        ArrayList<FreezerItem> filteredList = new ArrayList<>();
        for (FreezerItem item : inventory) {
            if (item.getDescription().toLowerCase().contains(query.toLowerCase()) ||
                    String.valueOf(item.getShelf()).contains(query)) {
                filteredList.add(item);
            }
        }
        adapter = new FreezerItemAdapter(this, filteredList);
        listView.setAdapter(adapter);
    }

    private void initializeFilters() {
        // We only need to initialize the device filter since it's the only one
        // that remains in the main layout
        deviceFilter = findViewById(R.id.deviceFilter);
        updateDeviceFilterOptions();
    }

    private void setupDateFilter(Spinner minSpinner, Spinner maxSpinner, boolean isDateAdded) {
        ArrayList<String> dateOptions = generateDateOptions(isDateAdded);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, dateOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        minSpinner.setAdapter(adapter);
        maxSpinner.setAdapter(adapter);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                View parentView = (View) parent.getParent();
                updateDateFilters(isDateAdded, parentView);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                View parentView = (View) parent.getParent();
                updateDateFilters(isDateAdded, parentView);
            }
        };

        minSpinner.setOnItemSelectedListener(listener);
        maxSpinner.setOnItemSelectedListener(listener);
    }

    private ArrayList<String> generateDateOptions(boolean isDateAdded) {
        ArrayList<String> options = new ArrayList<>();
        options.add("Any");

        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        int currentMonth = cal.get(Calendar.MONTH);

        int startYear = isDateAdded ? currentYear - 10 : currentYear;
        int endYear = isDateAdded ? currentYear : currentYear + 10;

        for (int year = startYear; year <= endYear; year++) {
            for (int month = 1; month <= 12; month++) {
                options.add(new FreezerDate(year, month).format());
            }
        }

        return options;
    }

    private void updateDeviceFilterOptions() {
        if (deviceFilter == null) return;

        ArrayList<String> deviceOptions = new ArrayList<>();
        deviceOptions.add("Any"); // Default option

        // Add all device names
        for (StorageDevice device : devices) {
            deviceOptions.add(device.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, deviceOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceFilter.setAdapter(adapter);

        // Restore previous selection if possible
        if (currentFilter.getSelectedDevice() != null) {
            int position = deviceOptions.indexOf(currentFilter.getSelectedDevice());
            if (position >= 0) {
                deviceFilter.setSelection(position);
            }
        }
    }

    private void updateShelfFilterOptions(Spinner shelfFilter, String selectedDevice) {
        ArrayList<String> shelfOptions = new ArrayList<>();
        shelfOptions.add("Any"); // Default option

        if (selectedDevice != null) {
            // Find the selected device and get its shelves
            for (StorageDevice device : devices) {
                if (device.getName().equals(selectedDevice)) {
                    shelfOptions.addAll(device.getShelves());
                    break;
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, shelfOptions);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        shelfFilter.setAdapter(adapter);

        // Restore previous selection if possible
        if (currentFilter.getSelectedShelf() != null) {
            int position = shelfOptions.indexOf(currentFilter.getSelectedShelf());
            if (position >= 0) {
                shelfFilter.setSelection(position);
            }
        }
    }

    // Update the validation method in MainActivity
    private boolean validateItemLocation(String device, String shelf) {
        // Find the device
        StorageDevice storageDevice = null;
        for (StorageDevice d : devices) {
            if (d.getName().equals(device)) {
                storageDevice = d;
                break;
            }
        }

        // Validate device exists and has the specified shelf
        if (storageDevice == null) {
            Toast.makeText(this, "Selected device does not exist",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!storageDevice.hasShelf(shelf)) {
            Toast.makeText(this, "Selected shelf does not exist in this device",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void updateTagFilterOptions(Spinner tagFilter) {
        if (tagFilter == null) return;

        Set<String> tags = new TreeSet<>();
        tags.add("Any");
        for (FreezerItem item : inventory) {
            tags.addAll(item.getTags());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new ArrayList<>(tags));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tagFilter.setAdapter(adapter);
    }

    private void updateQuantityFilters() {
        try {
            String minText = quantityMinFilter.getText().toString();
            String maxText = quantityMaxFilter.getText().toString();

            currentFilter.setQuantityMin(minText.isEmpty() ? null : Integer.parseInt(minText));
            currentFilter.setQuantityMax(maxText.isEmpty() ? null : Integer.parseInt(maxText));

            applyFiltersAndSort();
        } catch (NumberFormatException e) {
            // Invalid number format - ignore
        }
    }

    private void updateDateFilters(boolean isDateAdded, View section) {
        if (section == null) return;

        Spinner minSpinner = section.findViewById(R.id.dateMinSpinner);
        Spinner maxSpinner = section.findViewById(R.id.dateMaxSpinner);

        if (minSpinner == null || maxSpinner == null) return;
        if (minSpinner.getSelectedItem() == null || maxSpinner.getSelectedItem() == null) return;

        String minSelection = minSpinner.getSelectedItem().toString();
        String maxSelection = maxSpinner.getSelectedItem().toString();

        FreezerDate minDate = "Any".equals(minSelection) ? null : parseDateString(minSelection);
        FreezerDate maxDate = "Any".equals(maxSelection) ? null : parseDateString(maxSelection);

        if (isDateAdded) {
            currentFilter.setDateAddedMin(minDate);
            currentFilter.setDateAddedMax(maxDate);
        } else {
            currentFilter.setExpDateMin(minDate);
            currentFilter.setExpDateMax(maxDate);
        }
    }

    private void applyFiltersAndSort() {
        ArrayList<FreezerItem> filteredList = new ArrayList<>();
        for (FreezerItem item : inventory) {
            if (currentFilter.matches(item)) {
                filteredList.add(item);
            }
        }

        Collections.sort(filteredList);
        adapter = new FreezerItemAdapter(this, filteredList);
        listView.setAdapter(adapter);
    }

    private FreezerDate parseDateString(String dateStr) {
        String[] parts = dateStr.split(" ");
        int year = Integer.parseInt(parts[0]);
        String monthStr = parts[1];

        // Convert month abbreviation to number (1-12)
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int month = Arrays.asList(months).indexOf(monthStr) + 1;

        return new FreezerDate(year, month);
    }

    private void showValidationErrors(List<String> errors) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Validation Errors");
        builder.setMessage(String.join("\n", errors));
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void initializeDialogFields(View dialogView, FreezerItem item) {
        Spinner deviceSpinner = dialogView.findViewById(R.id.deviceSpinner);
        Spinner shelfSpinner = dialogView.findViewById(R.id.shelfSpinner);
        EditText descInput = dialogView.findViewById(R.id.descriptionInput);
        EditText quantityInput = dialogView.findViewById(R.id.quantityInput);
        Spinner dateAddedMonthSpinner = dialogView.findViewById(R.id.dateAddedMonthSpinner);
        Spinner dateAddedYearSpinner = dialogView.findViewById(R.id.dateAddedYearSpinner);
        Spinner expMonthSpinner = dialogView.findViewById(R.id.expMonthSpinner);
        Spinner expYearSpinner = dialogView.findViewById(R.id.expYearSpinner);
        EditText tagsInput = dialogView.findViewById(R.id.tagsInput);
        EditText commentsInput = dialogView.findViewById(R.id.commentsInput);
        EditText amountInput = dialogView.findViewById(R.id.amountInput);
        CheckBox expDateCheckbox = dialogView.findViewById(R.id.expDateCheckbox);
        View expDateContainer = dialogView.findViewById(R.id.expDateContainer);

        // Set up quantity buttons
        ImageButton quantityUp = dialogView.findViewById(R.id.quantityUp);
        ImageButton quantityDown = dialogView.findViewById(R.id.quantityDown);

        // Initialize spinners with values
        setupDeviceSpinner(deviceSpinner, item);
        setupDateSpinners(dateAddedMonthSpinner, dateAddedYearSpinner, true);
        setupDateSpinners(expMonthSpinner, expYearSpinner, false);

        // Set up expiration date checkbox listener
        expDateCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            expDateContainer.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Set current values if editing existing item
        if (item != null) {
            descInput.setText(item.getDescription());
            quantityInput.setText(String.valueOf(item.getQuantity()));
            setDateSpinners(dateAddedMonthSpinner, dateAddedYearSpinner, item.getDateAdded());
            setDateSpinners(expMonthSpinner, expYearSpinner, item.getExpirationDate());
            tagsInput.setText(String.join(", ", item.getTags()));

            // Set comments, amount, expiration date if they exist
            if (commentsInput != null && item.getComments() != null) {
                commentsInput.setText(item.getComments());
            }

            if (amountInput != null) {
                amountInput.setText(item.getAmount());
            }

            if (item.getExpirationDate() != null) {
                expDateCheckbox.setChecked(true);
                expDateContainer.setVisibility(View.VISIBLE);
                setDateSpinners(expMonthSpinner, expYearSpinner, item.getExpirationDate());
            } else {
                expDateCheckbox.setChecked(false);
                expDateContainer.setVisibility(View.GONE);
            }
        } else {
            // Set defaults for new item
            quantityInput.setText("1");

            // Set current date as date added
            FreezerDate now = FreezerDate.now();
            setDateSpinners(dateAddedMonthSpinner, dateAddedYearSpinner, now);

            // Set default expiration date to one year from now
            setDateSpinners(expMonthSpinner, expYearSpinner, FreezerDate.plusYears(now, 1));

            // Set shelf from filter if active
            if (currentFilter.getSelectedShelf() != null) {
                shelfSpinner.setSelection(getShelfPosition(currentFilter.getSelectedShelf(), shelfSpinner));
            }

            // Set tag from filter if active
            if (currentFilter.getSelectedTag() != null) {
                tagsInput.setText(currentFilter.getSelectedTag());
            }

            expDateCheckbox.setChecked(false);  // Default to unchecked for new items
            expDateContainer.setVisibility(View.GONE);
        }

        // Set up quantity increment/decrement buttons
        quantityUp.setOnClickListener(v -> {
            int currentQty = Integer.parseInt(quantityInput.getText().toString());
            quantityInput.setText(String.valueOf(currentQty + 1));
        });

        quantityDown.setOnClickListener(v -> {
            int currentQty = Integer.parseInt(quantityInput.getText().toString());
            if (currentQty > 1) {
                quantityInput.setText(String.valueOf(currentQty - 1));
            }
        });
    }

    private void setupShelfSpinner(Spinner spinner, String deviceName, FreezerItem item) {
        List<String> shelves = new ArrayList<>();

        if (deviceName != null) {
            for (StorageDevice device : devices) {
                if (device.getName().equals(deviceName)) {
                    shelves = device.getShelves(); // Will maintain order
                    break;
                }
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, shelves);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set selection if editing existing item or if filter is active
        if (item != null && deviceName != null && deviceName.equals(item.getDevice())) {
            int position = shelves.indexOf(item.getShelf());
            if (position >= 0) {
                spinner.setSelection(position);
            }
        } else if (currentFilter.getSelectedShelf() != null) {
            int position = shelves.indexOf(currentFilter.getSelectedShelf());
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }

    private int getShelfPosition(String shelf, Spinner spinner) {
        if (spinner == null || spinner.getAdapter() == null) return 0;

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equals(shelf)) {
                return i;
            }
        }
        return 0;
    }

    private void setupDateSpinners(Spinner monthSpinner, Spinner yearSpinner, boolean isDateAdded) {
        // Set up month spinner
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, FreezerDate.getMonthsList());
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);

        // Set up year spinner
        ArrayList<String> years = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);

        int startYear = isDateAdded ? currentYear - 10 : currentYear;
        int endYear = isDateAdded ? currentYear : currentYear + 10;

        for (int year = startYear; year <= endYear; year++) {
            years.add(String.valueOf(year));
        }

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);
    }

    private void setDateSpinners(Spinner monthSpinner, Spinner yearSpinner, FreezerDate date) {
        if (date == null) return;  // Skip setting spinners if date is null

        monthSpinner.setSelection(date.getMonth() - 1);

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) yearSpinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (Integer.parseInt(adapter.getItem(i)) == date.getYear()) {
                yearSpinner.setSelection(i);
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_instructions) {
            startActivity(new Intent(this, InstructionsActivity.class));
            return true;
        } else if (itemId == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload devices in case they were modified
        devices = loadDevices();
        updateDeviceFilterOptions();
    }

    private void setupDeviceSpinner(Spinner deviceSpinner, FreezerItem item) {
        ArrayList<String> deviceNames = new ArrayList<>();
        Collections.sort(devices); // Sort by orderIndex
        for (StorageDevice device : devices) {
            deviceNames.add(device.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, deviceNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        deviceSpinner.setAdapter(adapter);

        // Set selection based on filter or item
        int position = -1;
        if (currentFilter.getSelectedDevice() != null) {
            position = deviceNames.indexOf(currentFilter.getSelectedDevice());
        } else if (item != null) {
            position = deviceNames.indexOf(item.getDevice());
        }

        if (position >= 0) {
            deviceSpinner.setSelection(position);
        } else if (!deviceNames.isEmpty()) {
            deviceSpinner.setSelection(0); // Select first device by default
        }

        // Update shelf spinner when device changes
        deviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDevice = parent.getItemAtPosition(position).toString();
                Spinner shelfSpinner = ((View) deviceSpinner.getParent())
                        .findViewById(R.id.shelfSpinner);
                setupShelfSpinner(shelfSpinner, selectedDevice, item);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Spinner shelfSpinner = ((View) deviceSpinner.getParent())
                        .findViewById(R.id.shelfSpinner);
                setupShelfSpinner(shelfSpinner, null, item);
            }
        });
    }

    private void showFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.filter_dialog, null);
        builder.setView(dialogView)
                .setTitle("Filter Options");

        // Initialize filter components
        Spinner tagFilter = dialogView.findViewById(R.id.tagFilter);
        EditText quantityMinFilter = dialogView.findViewById(R.id.quantityMinFilter);
        EditText quantityMaxFilter = dialogView.findViewById(R.id.quantityMaxFilter);

        // Initialize and set up date filters
        updateDateFilterLayout(dialogView);
        updateFilterDialogForDates(dialogView);

        // Update tag filter options
        if (tagFilter != null) {
            updateTagFilterOptions(tagFilter);
        }

        // Set existing values
        if (currentFilter.getSelectedTag() != null && tagFilter != null) {
            int position = ((ArrayAdapter<String>) tagFilter.getAdapter())
                    .getPosition(currentFilter.getSelectedTag());
            if (position >= 0) {
                tagFilter.setSelection(position);
            }
        }

        if (currentFilter.getQuantityMin() != null && quantityMinFilter != null) {
            quantityMinFilter.setText(currentFilter.getQuantityMin().toString());
        }
        if (currentFilter.getQuantityMax() != null && quantityMaxFilter != null) {
            quantityMaxFilter.setText(currentFilter.getQuantityMax().toString());
        }

        // Set up buttons
        Button applyButton = dialogView.findViewById(R.id.applyFiltersButton);
        Button clearButton = dialogView.findViewById(R.id.clearFiltersButton);

        AlertDialog dialog = builder.create();

        if (applyButton != null) {
            applyButton.setOnClickListener(v -> {
                if (tagFilter != null && tagFilter.getSelectedItem() != null) {
                    currentFilter.setSelectedTag(tagFilter.getSelectedItemPosition() == 0 ? null :
                            tagFilter.getSelectedItem().toString());
                }

                if (quantityMinFilter != null && quantityMaxFilter != null) {
                    try {
                        String minText = quantityMinFilter.getText().toString();
                        String maxText = quantityMaxFilter.getText().toString();
                        currentFilter.setQuantityMin(minText.isEmpty() ? null : Integer.parseInt(minText));
                        currentFilter.setQuantityMax(maxText.isEmpty() ? null : Integer.parseInt(maxText));
                    } catch (NumberFormatException e) {
                        Toast.makeText(MainActivity.this, "Invalid quantity values", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                saveDateFilters(dialogView);
                applyFiltersAndSort();
                dialog.dismiss();
            });
        }

        if (clearButton != null) {
            clearButton.setOnClickListener(v -> {
                clearAllFilters();
                dialog.dismiss();
            });
        }

        dialog.show();
    }

    private void clearAllFilters() {
        currentFilter = new Filter();
        if (deviceFilter != null) {
            deviceFilter.setSelection(0); // Set to "Any"
        }
        if (shelfFilterMain != null) {
            shelfFilterMain.setEnabled(false);
            shelfFilterMain.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, new ArrayList<>()));
        }

        // Clear other filters...
        applyFiltersAndSort();
    }

    private void updateDateFilterValues(Spinner minSpinner, Spinner maxSpinner, boolean isDateAdded) {
        String minSelection = minSpinner.getSelectedItem().toString();
        String maxSelection = maxSpinner.getSelectedItem().toString();

        FreezerDate minDate = "Any".equals(minSelection) ? null : parseDateString(minSelection);
        FreezerDate maxDate = "Any".equals(maxSelection) ? null : parseDateString(maxSelection);

        if (isDateAdded) {
            currentFilter.setDateAddedMin(minDate);
            currentFilter.setDateAddedMax(maxDate);
        } else {
            currentFilter.setExpDateMin(minDate);
            currentFilter.setExpDateMax(maxDate);
        }
    }

    private void setupListViewClickListener() {
        listView.setOnItemClickListener((parent, view, position, id) -> {
            FreezerItem item = adapter.getItem(position);
            if (item != null) {
                showItemOptionsDialog(item);
            }
        });
    }

    private void showItemOptionsDialog(FreezerItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(item.getDescription())
                .setItems(new String[]{"Edit", "Duplicate", "Delete"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Edit
                            showEditItemDialog(item);
                            break;
                        case 1: // Duplicate
                            duplicateItem(item);
                            break;
                        case 2: // Delete
                            showDeleteConfirmationDialog(item);
                            break;
                    }
                });
        builder.show();
    }

    private void duplicateItem(FreezerItem originalItem) {
        // Create new description by adding "- Copy" or incrementing copy number
        String newDescription = createDuplicateDescription(originalItem.getDescription());

        // Create new item with copied values but new description
        FreezerItem newItem = new FreezerItem(
                originalItem.getDevice(),
                originalItem.getShelf(),
                originalItem.getDateAdded(),
                newDescription,
                originalItem.getQuantity(),
                originalItem.getExpirationDate(),
                originalItem.getTags(),
                originalItem.getComments(),
                originalItem.getAmount()
        );

        // Add to inventory and save
        inventory.add(newItem);
        saveLocalInventory();
        applyFiltersAndSort();

        Toast.makeText(this, "Item duplicated", Toast.LENGTH_SHORT).show();
    }

    // Helper method to create description for duplicate item
    private String createDuplicateDescription(String originalDesc) {
        if (originalDesc.matches(".*- Copy(\\s+\\d+)?$")) {
            // If already has "- Copy" or "- Copy X", increment number
            int copyNum = 2;
            String baseDesc = originalDesc.replaceAll("- Copy(\\s+\\d+)?$", "").trim();

            // Find highest existing copy number
            Pattern pattern = Pattern.compile(Pattern.quote(baseDesc) + "- Copy(\\s+\\d+)?$");
            for (FreezerItem item : inventory) {
                String desc = item.getDescription();
                if (pattern.matcher(desc).matches()) {
                    Matcher matcher = Pattern.compile("\\d+$").matcher(desc);
                    if (matcher.find()) {
                        copyNum = Math.max(copyNum, Integer.parseInt(matcher.group()) + 1);
                    } else {
                        copyNum = Math.max(copyNum, 2);
                    }
                }
            }

            return String.format("%s- Copy %d", baseDesc, copyNum);
        } else {
            // First copy
            return originalDesc + "- Copy";
        }
    }

    private void showEditItemDialog(FreezerItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_item, null);
        builder.setView(dialogView)
                .setTitle("Edit Item");

        // Initialize the dialog fields with the item's current values
        initializeDialogFields(dialogView, item);

        builder.setPositiveButton("Save", null); // We'll set this up after dialog creation
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dialogInterface -> {
            Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            button.setOnClickListener(view -> {
                if (validateAndSaveItem(dialogView, item, dialog)) {
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void showDeleteConfirmationDialog(FreezerItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Item")
                .setMessage("Are you sure you want to delete \"" + item.getDescription() + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    inventory.remove(item);
                    saveLocalInventory();
                    applyFiltersAndSort();
                    Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateDateFilterLayout(View dialogView) {
        // Since we now have direct EditText fields instead of included layouts
        EditText dateAddedMinInput = dialogView.findViewById(R.id.date_added_min);
        EditText dateAddedMaxInput = dialogView.findViewById(R.id.date_added_max);
        EditText expDateMinInput = dialogView.findViewById(R.id.exp_date_min);
        EditText expDateMaxInput = dialogView.findViewById(R.id.exp_date_max);

        if (dateAddedMinInput != null) {
            dateAddedMinInput.addTextChangedListener(createDateWatcher());
        }
        if (dateAddedMaxInput != null) {
            dateAddedMaxInput.addTextChangedListener(createDateWatcher());
        }
        if (expDateMinInput != null) {
            expDateMinInput.addTextChangedListener(createDateWatcher());
        }
        if (expDateMaxInput != null) {
            expDateMaxInput.addTextChangedListener(createDateWatcher());
        }
    }


    private TextWatcher createDateWatcher() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                if (!text.isEmpty() && !isValidDateFormat(text)) {
                    ((EditText) getCurrentFocus()).setError("Use MM/YYYY or YYYY format");
                }
            }
        };
    }

    private boolean isValidDateFormat(String date) {
        return date.matches("\\d{4}") ||  // YYYY
                date.matches("\\d{2}/\\d{4}"); // MM/YYYY
    }

    private FreezerDate parseDateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }

        try {
            if (input.matches("\\d{4}")) {
                // Year only format
                int year = Integer.parseInt(input);
                return new FreezerDate(year, 1);
            } else if (input.matches("\\d{2}/\\d{4}")) {
                // MM/YYYY format
                String[] parts = input.split("/");
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt(parts[1]);
                if (month >= 1 && month <= 12) {
                    return new FreezerDate(year, month);
                }
            }
        } catch (NumberFormatException e) {
            // Return null for any parsing errors
        }
        return null;
    }

    private void updateFilterDialogForDates(View dialogView) {
        EditText dateAddedMinInput = dialogView.findViewById(R.id.date_added_min);
        EditText dateAddedMaxInput = dialogView.findViewById(R.id.date_added_max);
        EditText expDateMinInput = dialogView.findViewById(R.id.exp_date_min);
        EditText expDateMaxInput = dialogView.findViewById(R.id.exp_date_max);

        // Set existing values if present
        if (currentFilter.getDateAddedMin() != null) {
            dateAddedMinInput.setText(String.format("%02d/%d",
                    currentFilter.getDateAddedMin().getMonth(),
                    currentFilter.getDateAddedMin().getYear()));
        }
        if (currentFilter.getDateAddedMax() != null) {
            dateAddedMaxInput.setText(String.format("%02d/%d",
                    currentFilter.getDateAddedMax().getMonth(),
                    currentFilter.getDateAddedMax().getYear()));
        }
        if (currentFilter.getExpDateMin() != null) {
            expDateMinInput.setText(String.format("%02d/%d",
                    currentFilter.getExpDateMin().getMonth(),
                    currentFilter.getExpDateMin().getYear()));
        }
        if (currentFilter.getExpDateMax() != null) {
            expDateMaxInput.setText(String.format("%02d/%d",
                    currentFilter.getExpDateMax().getMonth(),
                    currentFilter.getExpDateMax().getYear()));
        }
    }

    // Update method to save date filters
    private void saveDateFilters(View dialogView) {
        EditText dateAddedMinInput = dialogView.findViewById(R.id.date_added_min);
        EditText dateAddedMaxInput = dialogView.findViewById(R.id.date_added_max);
        EditText expDateMinInput = dialogView.findViewById(R.id.exp_date_min);
        EditText expDateMaxInput = dialogView.findViewById(R.id.exp_date_max);

        currentFilter.setDateAddedMin(parseDateInput(dateAddedMinInput.getText().toString()));
        currentFilter.setDateAddedMax(parseDateInput(dateAddedMaxInput.getText().toString()));
        currentFilter.setExpDateMin(parseDateInput(expDateMinInput.getText().toString()));
        currentFilter.setExpDateMax(parseDateInput(expDateMaxInput.getText().toString()));
    }
}