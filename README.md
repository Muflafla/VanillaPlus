## **VanillaPlus**

### *The Ultimate Survival Enhancement Toolkit*

**VanillaPlus** is a lightweight, high-performance plugin designed to refine the survival experience without breaking the "Vanilla" feel. Built for **Java 21** and optimized natively for **Paper**, it bundles essential utility, performance-minded features, and robust server management tools into one seamless package.

---

### **🚀 Key Features**

#### 🛏️ **Advanced Custom Sleep System**

Take complete control over how your server skips the night with a fully multi-world compatible sleep engine:

* **Action Bar Sleep Counter:** Displays a real-time action bar to all players showing exactly how many people are sleeping and how many are required to skip the night.
* **Multi-World Support:** Seamlessly handles cross-dimension alerts and resets across standard dimensions (`world`, `world_nether`, `world_the_end`) without crossing wires.
* **Flexible Sleep Modes:** Configure the skip to trigger based on a fixed player count or a dynamic percentage of online players.
* **Smart Time-Progression:** Advances the day to your exact configured wake time, even during daytime thunderstorms, without breaking vanilla statistics.
* **Global Phantom Protection:** Resets the rest timer for *every* player in that world upon a successful skip, preventing frustrating phantom spawns.
* **Weather Preservation:** Toggle whether sleeping clears the sky or naturally preserves ongoing rain and storms.

#### 🎒 **Portable Storage & Stashes (Upgraded!)**

Give your players an advanced personal vault with extensive configurations, elite optimization, and quality-of-life additions:

* **Flexible Stash Access & Custom Names:** Choose how players open their stash. You can independently enable or disable physical stash chests (naming a standard chest "Mystical Stash" via an anvil) and the stash book—disable both to rely strictly on command-only access. Supports custom stash names with color codes (e.g., `§5`).
* **Granular Stash Settings:** Configurable stash sizes, a global config option to drop stash items upon player death, and personal overflow toggle commands so players can manage overflow preferences individually.
* **Direct Stash Auto-Pickup:** A toggleable per-player option allowing blocks broken in the world to go straight from the ground into the stash (also known as overflow).
* **Shulker Block Picker:** Option to pick blocks from a shulker box while it's held in the player's off-hand.
* **Admin Stash Inspection:** Full administrative control allows operators to open, view, and safely manage the contents of any player's stash at any time.
* **Asynchronous Performance:** Completely lag-free. Both file-read and file-write operations run asynchronously off the main server thread, guaranteeing zero impact on your TPS alongside lower disk IO intensity.

#### 🗺️ **World Management, Warps & Chunkloading (New!)**

* **Spawn Management System:** Set precise world spawn locations down to the exact coordinates, complete with a dedicated teleport command.
* **Warp Management System:** Complete warp suite allowing players to create, delete, and list custom warps.
* **Offline Chunkloading:** Command-based chunkloading infrastructure enabling players to keep non-mob farms active while offline.

#### ⛏️ **Hardened Netherite Pickaxe (New!)**

* **Elite Mining Power:** Craft the craftable **Hardened Netherite Pickaxe**. When paired with an **Efficiency V** enchantment and the **Haste II** effect, it dynamically scales up to **Efficiency VIII**, allowing players to instantly mine tough blocks like deepslate. *(Note: The base Netherite Pickaxe used in the recipe will have its enchantments cleared).*
* **Crafting Recipe:**

```text
[NETHER_STAR, NETHERITE_INGOT, NETHER_STAR]
[NETHERITE_INGOT, NETHERITE_PICKAXE, NETHERITE_INGOT]
[NETHER_STAR, NETHERITE_INGOT, NETHER_STAR]

```

#### 🧩 **Mini-Blocks & Player Enhancements (New!)**

* **Mini-Blocks System:** Bundles default mini-blocks with a dedicated configuration option (`mini-blocks.yml`) optimized to inject exclusively at server boot, while the custom mini-blocks file (`custom-mini-blocks.yml`) dynamically re-injects upon reload.
* **Player Head Drops:** Enables natural player head drops upon player death.
* **Tool Health Warnings:** Automated notifications warning players when tool durability drops to dangerously low levels.

#### 📊 **Customizable Tab-List Stats & HUD**

* **Player-Specific Display Styles:** Players can use commands to choose their preferred layout (minimalist icons like `💀`, `🛌`, `📶` or detailed text strings) to track real-time deaths, days awake, and ping. All stats can also be individually toggled per player.
* **AFK Scoreboard Integration:** Player names automatically gray out on the scoreboard after 60 seconds of inactivity.
* **World Age & Playtime:** Dedicated command displaying the current world age and individual session time.

#### 🗼 **Enhanced & Scaling Beacons (Reworked!)**

* **Dynamic Range & UI:** Reworked beacon class resolving underlying bugs. Beacon effects now cleanly display in the top right. *⚠️ Note: The old `beaconfile` must be manually removed for this update.*
* **Scaling Modifiers:** Beacon range expands naturally based on tier level to cover entire builds without massive pyramids.

#### 💰 **Global Economy & Server Utilities**

* **Global Villager Pricing:** Villagers dynamically track and offer the best price ever given by *any* player on the server, while individual reputation mechanics remain fully functional.
* **Smart Auto-Ban:** Instantly blocks unwhitelisted players from attempting to join the server before a join message even broadcasts.
* **Block Drops & Griefing Toggles:** Configure Creeper/Ghast explosions to drop 100% of destroyed blocks, hurt players without breaking terrain, or turn them off completely. Enderman block-stealing protection included, among other utilities.

---

### **⚙️ Configuration & Performance**

Everything is **fully customizable**.

* **Reload System Overhaul:** Cleanly reloads all configurations and block files directly from disk on normal `/reload`.
* **Atomic Config Patching:** Upgraded configuration file patching utilizing atomic swaps for ultimate data safety and integrity.
* **bStats Integration & Update Checker:** Features anonymous telemetry and live update checks for Hangar releases.

---

### **🛠 Technical Details**

* **Target:** Paper 1.21.x+ (Utilizes native Paper/Spigot event architectures)
* **Requirements:** **Java 21**
* **Code Quality:** Highly modularized codebase, optimized event-driven architecture, and multi-threaded async storage IO.
* **License:** Licensed under the **MIT License**.

---

### **🔑 Permissions & Command Reference**

VanillaPlus features fine-grained permission nodes, allowing you to easily split access between regular survival players and server administration tools:

| Permission Node | Description
| --- | --- |
| `VanillaPlus.hud` | Allows players to use commands to toggle and customize their Tab-HUD appearance. |
| `VanillaPlus.spawn` | Allows players to teleport directly to the world spawn point. |
| `VanillaPlus.setspawn` | Allows administrators to set or modify the permanent world spawn point. |
| `VanillaPlus.stash` | Allows players to access and use Mystical Stashes. |
| `VanillaPlus.overflow` | Allows players to toggle stash overflow preferences for themselves. |
| `VanillaPlus.world_age` | Allows players to check the world age and individual playtime stats. |
| `VanillaPlus.create_warp` | Allows players to create custom warps. |
| `VanillaPlus.delete_warp` | Allows players to delete their custom warps. |
| `VanillaPlus.warp_list` | Allows players to view available server warps. |
| `VanillaPlus.warp` | Allows players to teleport using warps. |
| `VanillaPlus.chunkload` | Allows players to utilize offline chunkloading for non-mob farms. |
| `VanillaPlus.reload` | Allows administrators to instantly reload plugin configurations via `/vp-reload`. |

#### **Example Permissions Configuration (`permissions.yml`)**

To configure these nodes directly within your server's native permission settings or an external manager (like LuckPerms), use the following structure to set up seamless default player access:

```yaml
server.player:
  description: "Standard permissions for regular players."
  default: true
  children:
    VanillaPlus.hud: true
    VanillaPlus.setspawn: true
    VanillaPlus.spawn: true
    VanillaPlus.stash: true
    VanillaPlus.overflow: true
    VanillaPlus.world_age: true
    VanillaPlus.create_warp: true
    VanillaPlus.delete_warp: true
    VanillaPlus.warp_list: true
    VanillaPlus.warp: true
    VanillaPlus.chunkload: true

```

---

### **How to Install**

1. Drop the `VanillaPlus.jar` into your server's `plugins` folder.
2. Restart your server.
3. Edit the generated `config.yml` to fine-tune your custom features, sleep settings, and stash preferences.
4. Use `/vp-reload` to reload configurations instantly without restarting!

---

### **💡 Feature Requests & Support**

Have a cool idea for a new feature? Want more stats added to the customizable Tab-list? Feature requests are always welcome!
Please use the Discussions tab on GitHub to let me know what you'd like to see in the next update. I'm always looking to make VanillaPlus better for the community!

---

> ⚠️ `*` **Multi-World Setup Note:** Cross-dimension alerts and phantom statistic resets require standard dimension naming conventions (e.g., `world`, `world_nether`, `world_the_end`). Custom-named, completely unlinked dimensions will be treated as independent worlds.