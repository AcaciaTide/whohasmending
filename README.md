# Who has Mending?

A client-side utility mod that visualizes villager trades (only enchanted books) as nameplates above their heads. No more signs, no more manual naming, and no more forgetting "Who has Mending?".

## Key Features

*   **Auto-Detection**: Automatically records trade information just by opening the trade screen.
*   **Virtual Nameplates**: Displays what matters most (e.g., `[10]Mending`) directly above the villager's head.
*   **Smart Filtering**:
    *   **First Book Only**: Only displays the first (topmost) enchanted book in the trade list.
    *   **Enchanted Books Only**: Ignores junk trades like paper or glass.
    *   **Librarians Only**: Only tracks Librarians, ignoring farmers or clerics.
    *   **Empty Trade Hiding**: If a Librarian has no enchanted books, no nameplate is shown.
*   **Data Persistence**: Trade data is saved per world/server. Your records won't disappear!
*   **Customizable**: Toggle display with a keybind (Default: 'H', customizable in Controls).

## Installation

### Fabric
1.  Install **Fabric Loader**.
2.  Install **Fabric API**.
3.  Download `whohasmending-fabric-x.x.x-MCversion.jar` and place it in your `mods` folder.

### NeoForge
1.  Install **NeoForge**.
2.  Download `whohasmending-neoforge-x.x.x-MCversion.jar` and place it in your `mods` folder. (No other dependencies are required)

## How to Use

1.  **Find a Librarian**: Right-click to open their trade window.
2.  **Done!**: Close the window. The mod has already recorded the enchanted books.
3.  **Check Nameplates**: Look at the villager. You'll see `[Cost]Enchantment Name Level`.
    *   Example: `[10]Mending` or `[30]Sharpness V`

### Controls
*   **Toggle Display**: Default is **H**. Go to `Options > Controls > Key Binds > Miscellaneous > Toggle Villager Trade Display` to change the key binding if needed.
*   **Reset Data**: Use `/whohasmending reset` (or `/whm reset`) to clear all saved data for the current world.
*   **Backup Data**: Use `/whm backup` to manually create a backup of the current world trade data.
*   **Restore Data**: Use `/whm restore` to restore trade data from the latest backup.
*   **Validate Data**: Use `/whm validate` to check for and fix any data corruption.

## Compatibility
*   **Client-Side Only**: Works on servers without needing to be installed on the server. Of course, it also works in singleplayer.
*   **Version Support**: Currently supports **Minecraft 26.1 and 26.1.x** on both **Fabric** and **NeoForge**.

## License
MIT License. Feel free to use this in your modpacks!
