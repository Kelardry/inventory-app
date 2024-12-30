package com.example.inventoryapp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// StorageDevice.java
public class StorageDevice implements Comparable<StorageDevice> {
    private String name;
    private List<String> shelves;
    private int orderIndex;

    public StorageDevice(String name, int orderIndex) {
        this.name = name;
        this.shelves = new ArrayList<>();
        this.orderIndex = orderIndex;
    }

    public boolean addShelf(String shelfName) {
        if (shelves.contains(shelfName)) {
            return false;  // Shelf already exists
        }
        shelves.add(shelfName);
        return true;  // Shelf added successfully
    }

    public void removeShelf(String shelfName) {
        shelves.remove(shelfName);
    }

    public void updateShelf(String oldName, String newName) {
        int index = shelves.indexOf(oldName);
        if (index != -1) {
            shelves.set(index, newName);
        }
    }

    public void reorderShelf(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(shelves, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(shelves, i, i - 1);
            }
        }
    }

    public boolean hasShelf(String shelfName) {
        return shelves.contains(shelfName);
    }

    // Modified getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getShelves() { return new ArrayList<>(shelves); }
    public void setShelves(List<String> shelves) { this.shelves = new ArrayList<>(shelves); }
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    @Override
    public int compareTo(StorageDevice other) {
        return Integer.compare(this.orderIndex, other.orderIndex);
    }
}