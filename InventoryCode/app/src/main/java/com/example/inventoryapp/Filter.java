package com.example.inventoryapp;

// Filter.java
public class Filter {
    private String selectedDevice;
    private String selectedShelf;
    private String selectedTag;
    private Integer quantityMin;
    private Integer quantityMax;
    private FreezerDate dateAddedMin;
    private FreezerDate dateAddedMax;
    private FreezerDate expDateMin;
    private FreezerDate expDateMax;
    private String searchText;

    public boolean matches(FreezerItem item) {
        if (selectedDevice != null && !item.getDevice().equals(selectedDevice)) {
            return false;
        }

        if (selectedShelf != null && !item.getShelf().equals(selectedShelf)) {
            return false;
        }

        if (selectedTag != null && !item.getTags().contains(selectedTag)) {
            return false;
        }

        if (quantityMin != null && item.getQuantity() < quantityMin) {
            return false;
        }

        if (quantityMax != null && item.getQuantity() > quantityMax) {
            return false;
        }

        if (dateAddedMin != null && item.getDateAdded().compareTo(dateAddedMin) < 0) {
            return false;
        }

        if (dateAddedMax != null && item.getDateAdded().compareTo(dateAddedMax) > 0) {
            return false;
        }

        if (expDateMin != null && item.getExpirationDate().compareTo(expDateMin) < 0) {
            return false;
        }

        if (expDateMax != null && item.getExpirationDate().compareTo(expDateMax) > 0) {
            return false;
        }

        if (searchText != null && !searchText.isEmpty()) {
            String searchLower = searchText.toLowerCase();
            return item.getDescription().toLowerCase().contains(searchLower) ||
                    item.getTags().stream().anyMatch(tag ->
                            tag.toLowerCase().contains(searchLower));
        }

        return true;
    }

    // Setters
    public void setSelectedDevice(String selectedDevice) {
        this.selectedDevice = selectedDevice;
    }

    public void setSelectedShelf(String selectedShelf) {
        this.selectedShelf = selectedShelf;
    }

    public void setSelectedTag(String selectedTag) {
        this.selectedTag = selectedTag;
    }

    public void setQuantityMin(Integer quantityMin) {
        this.quantityMin = quantityMin;
    }

    public void setQuantityMax(Integer quantityMax) {
        this.quantityMax = quantityMax;
    }

    public void setDateAddedMin(FreezerDate dateAddedMin) {
        this.dateAddedMin = dateAddedMin;
    }

    public void setDateAddedMax(FreezerDate dateAddedMax) {
        this.dateAddedMax = dateAddedMax;
    }

    public void setExpDateMin(FreezerDate expDateMin) {
        this.expDateMin = expDateMin;
    }

    public void setExpDateMax(FreezerDate expDateMax) {
        this.expDateMax = expDateMax;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    // Getters
    public String getSelectedDevice() {
        return selectedDevice;
    }

    public String getSelectedShelf() {
        return selectedShelf;
    }

    public String getSelectedTag() {
        return selectedTag;
    }

    public Integer getQuantityMin() {
        return quantityMin;
    }

    public Integer getQuantityMax() {
        return quantityMax;
    }

    public FreezerDate getDateAddedMin() {
        return dateAddedMin;
    }

    public FreezerDate getDateAddedMax() {
        return dateAddedMax;
    }

    public FreezerDate getExpDateMin() {
        return expDateMin;
    }

    public FreezerDate getExpDateMax() {
        return expDateMax;
    }

    public String getSearchText() {
        return searchText;
    }
}