## **VanillaPlus**

### *The Ultimate Survival Enhancement Toolkit*

**VanillaPlus** is a lightweight, high-performance plugin designed to refine the survival experience without breaking the "Vanilla" feel. Built for **Java 21** and optimized natively for **Paper**, it bundles essential utility, performance-minded features, and robust server management tools into one seamless package.

---

### **🚀 Key Features**

#### 🛏️ **Advanced Custom Sleep System (Updated!)**

Take complete control over how your server skips the night with a fully multi-world compatible sleep engine:

* **Action Bar Sleep Counter:** Displays a real-time action bar to all players showing exactly how many people are sleeping and how many are required to skip the night.
* **Multi-World Support:** Seamlessly handles cross-dimension alerts and resets across standard dimensions (`world`, `world_nether`, `world_the_end`) without crossing wires.
* **Flexible Sleep Modes:** Configure the skip to trigger based on a fixed player count or a dynamic percentage of online players.
* **Smart Time-Progression:** Advances the day to your exact configured wake time, even during daytime thunderstorms, without breaking vanilla statistics.
* **Global Phantom Protection:** Resets the rest timer for *every* player in that world upon a successful skip, preventing frustrating phantom spawns.
* **Weather Preservation:** Toggle whether sleeping clears the sky or naturally preserves ongoing rain and storms.

#### 📊 **Customizable Tab-List Stats (New Look!)**

Keep your players informed with live statistics directly in the Tab menu, now with personalized formatting:

* **Player-Specific Display Styles:** Players can use commands to choose their preferred layout. They can display stats as clean, minimalist icons (`💀`, `🛌`, `📶`) or detailed full-context text strings.
* **Live Tracking:** Displays real-time player deaths, current days awake (time since last sleep), and exact ping.

#### 🎒 **Portable Storage (Upgraded!)**

Give your players an advanced personal vault with built-in server-owner protections and elite optimization:

* **Physical Stash Chests:** A toggleable feature allowing players to name a standard wooden chest "Mystical Stash" via an anvil to instantly open their global vault anywhere. Easily disabled in the config to preserve vanilla Ender Chest progression!
* **Admin Stash Inspection:** Full administrative control allows operators to open, view, and safely manage the contents of any player's stash at any time.
* **Asynchronous Performance:** Completely lag-free. Both file-read and file-write operations run asynchronously off the main server thread, guaranteeing zero impact on your TPS.
* **No Mods Required:** Uses a clever, native book-and-container interface—**no client-side mods or resource packs** required.

#### 🛡️ **Server Utility, Anti-Grief & Security**

You are in total command of how mobs and automated systems interact with your world:

* **Smart Auto-Ban:** A robust security system that triggers an immediate profile ban on unauthorized join attempts. Fully optimized to validate whitelists and player profiles correctly on both **online-mode** and **offline-mode** servers.
* **World Spawn Tweaks:** Set a precise, permanent spawn point for your world down to the exact coordinates.
* **100% Block Drops:** Creepers and Ghasts drop every single block they destroy as an item—keep the danger of the blast without losing your build materials.
* **Griefing Toggles:** Allow explosions to hurt players while keeping the terrain perfectly intact. Works for both Creepers and Ghasts!
* **Total Defusal:** Disable Creeper/Ghast explosions entirely.
* **Enderman Protection:** Stop them from picking up blocks. Includes customizable notification messages when they try!

#### 🗼 **Enhanced & Scaling Beacons**

* **Dynamic Range Modifiers:** Boost your base safely! Beacon range utilizes a scaling modifier that expands defaults naturally based on the beacon's tier level, covering your entire kingdom without requiring massive pyramid structures.

#### 👻 **Optional Horror Elements**

* Want to spice things up? Enable rare, "psychological horror" world events to keep your players on their toes. (Fully optional and toggleable!)

---

### **⚙️ Configuration & Performance**

Everything is **fully customizable**. From the welcome messages and player-specific greetings to the exact phrases the Endermen mutter when they fail to steal a block—you are in control.

* **bStats Integration:** Features native, anonymous telemetry to track plugin performance and usage statistics globally.
* **Optimized Loading:** Rewritten config loading and reload loops for faster execution and less overhead.
* **Live Update Checker:** Added an automated update checker so you always know when a new release is live on Hangar.

---

### **🛠 Technical Details**

* **Target:** Paper 1.21.x+ (Utilizes native Paper/Spigot event architectures)
* **Requirements:** **Java 21**
* **Code Quality:** Optimized event-driven, multi-threaded async storage IO for maximum performance.
* **License:** Licensed under the **MIT License**.

---

### **🔑 Permissions & Command Reference**

VanillaPlus features fine-grained permission nodes, allowing you to easily split access between regular survival players and server administration tools:

| Permission Node | Description | Default Target |
| --- | --- | --- |
| `VanillaPlus.hud` | Allows players to use commands to toggle and customize their Tab-HUD appearance. | OP (Admins Only) |
| `VanillaPlus.stash` | Allows administrators to inspect and manage other players' Mystical Stashes. | OP (Admins Only) |
| `VanillaPlus.spawn` | Allows administrators to set or modify the permanent world spawn point. | OP (Admins Only) |
| `VanillaPlus.reload` | Allows administrators to instantly reload plugin configurations via `/vp-reload`. | OP (Admins Only) |

#### **Example Permissions Configuration**

If you want to configure these nodes directly within your server's native permission settings or an external manager (like LuckPerms), use the following structure to set up seamless default access:

```yaml
server.player:
  description: "Standard permissions for regular survival players."
  default: true
  children:
    VanillaPlus.hud: true

```

---

### **How to Install**

1. Drop the `VanillaPlus.jar` into your server's `plugins` folder.
2. Restart your server.
3. Edit the generated `config.yml` to fine-tune your custom features, sleep settings, and stash preferences.
4. Use `/vp-reload` to reload the config instantly without restarting!

---

### **💡 Feature Requests & Support**

Have a cool idea for a new feature? Want more stats added to the customizable Tab-list? Feature requests are always welcome!
Please use the Discussions tab on GitHub to let me know what you'd like to see in the next update. I'm always looking to make VanillaPlus better for the community!

---

> ⚠️ `*` **Multi-World Setup Note:** Cross-dimension alerts and phantom statistic resets require standard dimension naming conventions (e.g., `world`, `world_nether`, `world_the_end`). Custom-named, completely unlinked dimensions will be treated as independent worlds.
