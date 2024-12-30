package com.example.inventoryapp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

// FileHandler.java
public class FileHandler {
    private static final String CSV_DELIMITER = "|";
    private static final String[] CSV_HEADERS = {
            "Device",
            "Shelf",
            "DateAddedYear",
            "DateAddedMonth",
            "Description",
            "Quantity",
            "ExpYear",
            "ExpMonth",
            "Tags",
            "Comments",
            "Amount"
    };
    public static void saveInventory(List<FreezerItem> inventory, OutputStream outputStream)
            throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream)) {
            // Write headers
            writer.write(String.join(CSV_DELIMITER, CSV_HEADERS) + "\n");

            // Write data rows
            for (FreezerItem item : inventory) {
                List<String> values = new ArrayList<>();
                values.add(item.getDevice());
                values.add(item.getShelf());
                values.add(String.valueOf(item.getDateAdded().getYear()));
                values.add(String.valueOf(item.getDateAdded().getMonth()));
                values.add(item.getDescription());
                values.add(String.valueOf(item.getQuantity()));

                // Handle optional expiration date
                FreezerDate expDate = item.getExpirationDate();
                values.add(expDate != null ? String.valueOf(expDate.getYear()) : "");
                values.add(expDate != null ? String.valueOf(expDate.getMonth()) : "");

                // Handle tags
                values.add(String.join(";", item.getTags()));

                // Handle optional fields
                values.add(item.getComments() != null ? item.getComments() : "");
                values.add(item.getAmount() != null ? item.getAmount() : "");

                writer.write(String.join(CSV_DELIMITER, values) + "\n");
            }
        }
    }

    public static List<FreezerItem> loadInventory(InputStream inputStream) throws IOException {
        List<FreezerItem> inventory = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        String line = reader.readLine(); // Skip header row
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) continue;

            String[] values = line.split("\\|", -1); // -1 to keep empty trailing fields
            if (values.length < 11) continue; // Skip malformed lines

            try {
                // Parse required fields
                String device = values[0];
                String shelf = values[1];
                int dateAddedYear = Integer.parseInt(values[2]);
                int dateAddedMonth = Integer.parseInt(values[3]);
                String description = values[4];
                int quantity = Integer.parseInt(values[5]);

                // Create FreezerDate objects
                FreezerDate dateAdded = new FreezerDate(dateAddedYear, dateAddedMonth);
                FreezerDate expDate = null;
                if (!values[6].isEmpty() && !values[7].isEmpty()) {
                    int expYear = Integer.parseInt(values[6]);
                    int expMonth = Integer.parseInt(values[7]);
                    expDate = new FreezerDate(expYear, expMonth);
                }

                // Parse tags
                Set<String> tags = new TreeSet<>();
                if (!values[8].isEmpty()) {
                    tags.addAll(Arrays.asList(values[8].split(";")));
                }

                // Get optional fields
                String comments = values[9].isEmpty() ? null : values[9];
                String amount = values[10].isEmpty() ? null : values[10];

                // Create and add item
                FreezerItem item = new FreezerItem(device, shelf, dateAdded, description,
                        quantity, expDate, tags, comments, amount);
                inventory.add(item);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // Skip lines with parsing errors
                e.printStackTrace();
            }
        }
        return inventory;
    }

    public static String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append('\n');
            }
        }
        return content.toString();
    }

    public static void writeFileContent(File file, String content) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(content);
        }
    }
}