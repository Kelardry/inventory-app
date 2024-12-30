package com.example.inventoryapp;

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

// FreezerItem.java
public class FreezerItem implements Comparable<FreezerItem> {
    private String device;      // Storage device name
    private String shelf;       // Shelf name within the device
    private FreezerDate dateAdded;
    private String description;
    private int quantity;
    private FreezerDate expirationDate;
    private Set<String> tags;
    private String comments;
    private String amount;

    public FreezerItem(String device, String shelf, FreezerDate dateAdded,
                       String description, int quantity, FreezerDate expirationDate,
                       Set<String> tags, String comments, String amount) {
        this.device = device;
        this.shelf = shelf;
        this.dateAdded = dateAdded;
        this.description = description;
        this.quantity = quantity;
        this.expirationDate = expirationDate;  // Can be null now
        this.tags = new TreeSet<>(tags);
        this.comments = comments != null ? comments : "";
        this.amount = amount != null ? amount : "";
    }

    @Override
    public int compareTo(FreezerItem other) {
        // Multi-level sorting: device -> shelf -> date added -> expiration -> quantity -> description
        int compare = this.device.compareTo(other.device);
        if (compare != 0) return compare;

        compare = this.shelf.compareTo(other.shelf);
        if (compare != 0) return compare;

        compare = this.dateAdded.compareTo(other.dateAdded);
        if (compare != 0) return compare;

        // Handle null expiration dates (treat as far future)
        if (this.expirationDate == null && other.expirationDate == null) {
            compare = 0;
        } else if (this.expirationDate == null) {
            compare = 1;  // this item goes after
        } else if (other.expirationDate == null) {
            compare = -1;  // this item goes before
        } else {
            compare = this.expirationDate.compareTo(other.expirationDate);
        }
        if (compare != 0) return compare;

        compare = Integer.compare(this.quantity, other.quantity);
        if (compare != 0) return compare;

        return this.description.compareToIgnoreCase(other.description);
    }

    // CSV methods
    public String toCSV() {
        String expDateStr = expirationDate != null ?
                String.format("%d|%d", expirationDate.getYear(), expirationDate.getMonth()) :
                "||";

        return String.format("%s|%s|%d|%d|%s|%d|%s|%s|%s|%s",
                device,
                shelf,
                dateAdded.getYear(), dateAdded.getMonth(),
                description,
                quantity,
                expDateStr,
                String.join(";", tags),
                comments.replace("|", "\\|").replace("\n", "\\n"),
                amount.replace("|", "\\|"));
    }

    public static FreezerItem fromCSV(String csvLine) {
        try {
            String[] values = csvLine.split("\\|(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            Set<String> tags = new TreeSet<>();
            if (values.length > 7 && !values[7].isEmpty()) {
                tags.addAll(Arrays.asList(values[7].split(";")));
            }

            String comments = "";
            if (values.length > 8) {
                comments = values[8].replace("\\|", "|").replace("\\n", "\n");
            }

            String amount = "";
            if (values.length > 9) {
                amount = values[9].replace("\\|", "|");
            }

            FreezerDate expDate = null;
            if (!values[6].isEmpty() && !values[6].equals("|")) {
                String[] expDateParts = values[6].split("\\|");
                expDate = new FreezerDate(
                        Integer.parseInt(expDateParts[0]),
                        Integer.parseInt(expDateParts[1])
                );
            }

            return new FreezerItem(
                    values[0],  // device
                    values[1],  // shelf
                    new FreezerDate(
                            Integer.parseInt(values[2]),
                            Integer.parseInt(values[3])
                    ),  // dateAdded
                    values[4],  // description
                    Integer.parseInt(values[5]),  // quantity
                    expDate,   // expirationDate can be null
                    tags,
                    comments,
                    amount
            );
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            return null;
        }
    }

    // Getters and setters
    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getShelf() {
        return shelf;
    }

    public void setShelf(String shelf) {
        this.shelf = shelf;
    }

    public FreezerDate getDateAdded() {
        return dateAdded;
    }
    public void setDateAdded(FreezerDate dateAdded) {
        this.dateAdded = dateAdded;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }


    public FreezerDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(FreezerDate expirationDate) {
        this.expirationDate = expirationDate;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = new TreeSet<>(tags);
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments != null ? comments : "";
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount != null ? amount : "";
    }
}