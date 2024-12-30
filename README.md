# Inventory App
Android app for inventory of food in freezers or other devices.  
Version 1.0.0  
Last Updated: December 30, 2024  

## About

### Purpose
This app helps you track items stored in your freezers, refrigerators, pantries, and other storage spaces. Keep track of what you have, where it's stored, and when it expires.

### Features
- Manage multiple storage devices and their shelves
- Track item quantities and expiration dates
- Filter and search functionality
- Tag-based organization
- Network file synchronization
- Automatic sorting

### Credits
#### App Development:
Original concept and implementation by Kelardry
#### Code Contributions:
Enhanced with assistance from Claude (Anthropic)
#### Graphics:
Images generated with craiyon.com were edited to create the main icon

## Instructions
### Installing:
Download the file inventory-app.apk to your Android device and run it. You might need to enable installing from local files.  
This is the same file as "inventory-app\InventoryCode\app\release\app-release.apk", just more conveniently located.

### Getting Started
1. Add your first storage device (freezer, fridge, etc.)
2. Add shelves to organize items within the device
3. Start adding items to your inventory

### Managing Storage Devices
- Press 'Add New Device' to create a storage device
- Tap a device to manage its shelves
- Add, rename, reorder, and delete shelves as needed
- Devices and shelves can't be deleted if they contain items

### Working with Items
#### Adding Items:
- Press '+' to add a new item
- Select the storage device and shelf
- Enter description, quantity, and other details
- Optional: add expiration date, tags, and comments

#### Editing Items:
- Tap any item to view options
- Choose to edit, duplicate, or delete
- All changes are saved automatically

Tip: When a device or shelf is filtered, new items will default to that location

#### Finding Items:
##### Quick Filter:
- Use the device dropdown at the top
- Use the search bar to find items by description
##### Advanced Filtering:
- Tap the filter icon for more options
- Filter by tags or quantity
- Filter by date added or expiration date
Note: Items are always automatically sorted by device, shelf, and description

### Syncing your Data
#### Inventory Sync:
- Tap the sync button on the main screen
- Choose or create a CSV file
- Select whether to push data to network or pull from network
#### Device Sync:
- Use 'Sync Devices' in device management
- Choose or create a JSON file
- Select sync direction
Tip: Regular syncing helps maintain a backup of your data

### Tips and Tricks
- Use tags to group related items across different locations
- The optional amount field can specify weights or volumes
- Duplicate items to quickly add similar entries
- Comments can store cooking instructions or other notes
- Check expiration dates regularly using the date filters