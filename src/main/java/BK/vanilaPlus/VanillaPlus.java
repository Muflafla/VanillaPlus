package BK.vanilaPlus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class VanillaPlus extends JavaPlugin implements Listener
{
    public int EndermanAmount = 0;
    public BukkitTask EndermanTask;
    public BukkitTask creepyTask;
    public ConfigLoader configLoader;

    private final NamespacedKey ShowDeathsKey = new NamespacedKey(this, "hud-deaths");
    private final NamespacedKey ShowSleepKey = new NamespacedKey(this, "hud-sleep");
    private final NamespacedKey ShowPingKey = new NamespacedKey(this, "hud-ping");

    @Override
    public void onEnable()
    {
        //enable bStats
        int pluginId = 32274;
        new Metrics(this, pluginId);

        //Create the updater and call it
        ConfigUpdater updater = new ConfigUpdater(this);
        updater.update();
        //Create the config loader
        this.configLoader = new ConfigLoader(this);

        //register this plugin events
        Bukkit.getPluginManager().registerEvents(this, this);
        //register the BeaconManager and also make it register events
        BeaconManager beaconManager = new BeaconManager(this);
        Bukkit.getPluginManager().registerEvents(beaconManager, this);
        //register the SleepManager and also make it register events
        SleepManager sleepManager = new SleepManager(this);
        Bukkit.getPluginManager().registerEvents(sleepManager, this);

        String version = this.getDescription().getVersion();
        this.getLogger().warning("VanillaPlus Plugin Enabled! Version:" + version);


        //Load the config into memory.
        configLoader.LoadAllVars();
        //init the plugin
        PluginInit();

        //fire up the update checker
        UpdateChecker checker = new UpdateChecker(this, "VanillaPlus");
        checker.checkForUpdates();

        //Load the daytracker in the SleepManager
        sleepManager.DayTracker();
    }

    //////////////////////////////////////// init the plugin /////////////////////////////////////////////
    public void PluginInit()
    {
        //the timer checks if its enabled or not, this is needed for if it's changed in while server is on so timer knows to stop
        startCreepyTimer();


        if(EndermanTask != null)
        {
            EndermanTask.cancel();
            EndermanTask = null;
        }
        if (this.configLoader.Show_Enderman_Messages)
        {
            // Task to broadcast Enderman griefing prevention stats
            EndermanTask = new BukkitRunnable()
            {
                @Override
                public void run()
                {

                    if (EndermanAmount > 0)
                    {
                        //get the messages from the config
                        List<String> messages = getConfig().getStringList("Enderman-Messages");
                        if(!messages.isEmpty())
                        {
                            String rawMessage = messages.get(new Random().nextInt(messages.size()));
                            String finalMessage = String.format(rawMessage, EndermanAmount);
                            //Bukkit.broadcast(Component.text(finalMessage)); use this if this actually ever gets removed.
                            for (Player player : Bukkit.getOnlinePlayers())
                            {
                                player.sendMessage(finalMessage);
                            }
                        }
                        else
                        {
                            for (Player player : Bukkit.getOnlinePlayers())
                            {
                                player.sendMessage("§aNo enderman MSG's set");

                            }
                        }

                        //reset enderman amount
                        EndermanAmount = 0;
                    }
                }
            }.runTaskTimer(this, 0L, 6000L);
        }
        //Check if custom recipes are enabled.
        if(this.configLoader.Enable_Custom_Recipes)
        {
            //register the recipes.
            this.registerRecipes();
        }

        // ust for the players currently online during a reload
        for (Player p : Bukkit.getOnlinePlayers())
        {
            Update_ScoreBoard(p);
        }
    }
    ////////////////////////////////////////Custom recipes/////////////////////////////////////////////
    private void registerRecipes()
    {
        //Wipe ALL existing recipes from this plugin first. needed for config changes long story
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext())
        {
            Recipe VP_Recipe = it.next();
            if (VP_Recipe instanceof ShapedRecipe shaped)
            {
                if (shaped.getKey().getNamespace().equalsIgnoreCase(this.getName().toLowerCase()))
                {
                    this.getLogger().info("Removing old recipe: " + shaped.getKey().toString());
                    it.remove();
                }
            }
        }

        // Check if Custom recipes are enabled
        boolean Custom_Recipes_Enabled = getConfig().getBoolean("Enable-Custom-Recipes", true);
        this.getLogger().info("Custom Recipes Enabled: " + Custom_Recipes_Enabled);

        if (Custom_Recipes_Enabled)
        {
            //get the recipes
            ConfigurationSection Custom_Recipes = getConfig().getConfigurationSection("Custom-Recipes");


            if (Custom_Recipes != null)
            {
                for (String recipeKey : Custom_Recipes.getKeys(false))
                {
                    // Skip the Explanation-ITEM so it doesn't try to register a "dummy" recipe
                    if (recipeKey.equalsIgnoreCase("Explanation-ITEM"))
                    {
                        continue;
                    }

                    this.getLogger().info("Found recipe key: " + recipeKey);

                    ConfigurationSection section = Custom_Recipes.getConfigurationSection(recipeKey);
                    if (section == null) continue;

                    //Get Result and Amount, so we know which item and how many to give
                    String resultName = section.getString("Result", "DIRT");
                    this.getLogger().info("Attempting to register: " + recipeKey + " (Result: " + resultName + ")");
                    int amount = section.getInt("Amount", 1);
                    Material resultMat = Material.matchMaterial(resultName);

                    if (resultMat == null)
                    {
                        //no item matched the item name used in result so display a msg
                        this.getLogger().warning("Skipping recipe '" + recipeKey + "': Invalid Material '" + resultName + "'");
                        continue;
                    }

                    //Setup the Item and Custom Name
                    ItemStack resultItem = new ItemStack(resultMat, amount);
                    //setup base meta
                    ItemMeta meta = resultItem.getItemMeta();

                    if (meta != null)
                    {
                        //Check if we need to do special Player Head logic
                        if (resultMat.equals(Material.PLAYER_HEAD))
                        {
                            String Player_Head_Name = section.getString("Player-Head-Name", "");
                            if(!Player_Head_Name.isEmpty())
                            {
                                OfflinePlayer Offline_Player_Head = Bukkit.getOfflinePlayer(Player_Head_Name);
                                // We "cast" the existing meta to SkullMeta to access head features
                                SkullMeta skullMeta = (SkullMeta) meta;
                                skullMeta.setOwningPlayer(Offline_Player_Head);
                            }
                        }
                        //Set Display name
                        String customName = section.getString("Display-Name", "");
                        if (!customName.isEmpty())
                        {
                            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', customName));
                            resultItem.setItemMeta(meta);
                        }
                    }

                    // 3. Create the Recipe
                    NamespacedKey key = new NamespacedKey(this, "vp_" + recipeKey.toLowerCase());
                    ShapedRecipe recipe = new ShapedRecipe(key, resultItem);

                    // 4. Set the Shape
                    List<String> shape = section.getStringList("Shape");
                    if (shape.size() == 3)
                    {
                        recipe.shape(shape.get(0), shape.get(1), shape.get(2));
                    }
                    else
                    {
                        this.getLogger().warning("Recipe '" + recipeKey + "' has an invalid shape (must be 3 rows).");
                        continue;
                    }

                    // 5. Map the Ingredients
                    ConfigurationSection ingredients = section.getConfigurationSection("Ingredients");
                    if (ingredients != null)
                    {
                        for (String charKey : ingredients.getKeys(false))
                        {
                            String matName = ingredients.getString(charKey);
                            Material mat = Material.matchMaterial(matName);

                            if (mat != null && !mat.isAir())
                            {
                                recipe.setIngredient(charKey.charAt(0), mat);
                            }
                        }
                    }

                    // 6. Register it!
                    this.getLogger().info("Successfully registered recipe: " + recipeKey);
                    Bukkit.addRecipe(recipe);
                }
                this.getLogger().info("--- Finished Recipe Registration ---");
            }
            else
            {
                this.getLogger().warning("Could not find 'Custom-Recipes' section in config.yml!");
                return;
            }
        }
    }
    //////////////////////////////////////////////////////Commands////////////////////////////////////////////////////
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (command.getName().equalsIgnoreCase("vp-reload"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.reload"))
            {
                sender.sendMessage("§cYou do not have permission to reload this plugin.");
                return true;
            }

            //reload the recipes
            this.registerRecipes();
            //let the config loader get all the vars fresh from file
            configLoader.LoadAllVars();
            //init the plugin
            PluginInit();

            sender.sendMessage("§aVanillaPlus configuration reloaded!");
            return true;
        }
        if (command.getName().equalsIgnoreCase("vp-setspawn"))
        {
            // Enforce permission check
            if (!sender.hasPermission("VanillaPlus.setspawn"))
            {
                sender.sendMessage("§cYou do not have permission to change the world spawn.");
                return true;
            }

            // Must be a player to grab coordinates
            if (!(sender instanceof Player))
            {
                sender.sendMessage("§cThis command can only be executed by a player in-game.");
                return true;
            }

            Player player = (Player) sender;
            Location loc = player.getLocation();
            World world = loc.getWorld();

            if (world != null)
            {
                //Set the native world spawn location
                world.setSpawnLocation(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
                //Remove spawn radius for this world
                org.bukkit.GameRule<Integer> rule = org.bukkit.GameRule.SPAWN_RADIUS;
                world.setGameRule(rule, 0);

                player.sendMessage("§aWorld spawn for §e" + world.getName() + " §ahas been set to your current location!");
            }
            else
            {
                player.sendMessage("§cError: Could not determine your current world context.");
            }

            return true;
        }
        if (command.getName().equalsIgnoreCase("vp-stash"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.stash"))
            {
                sender.sendMessage("§cYou do not have permission to inspect stashes.");
                return true;
            }

            // Make sure the console isn't trying to open a visual screen inventory
            if (!(sender instanceof Player))
            {
                sender.sendMessage("§cThis command can only be executed by a player in-game.");
                return true;
            }

            // Make sure they provided a player name argument
            if (args.length < 1)
            {
                sender.sendMessage("§cUsage: /vp-stash <player>");
                return true;
            }

            String targetName = args[0];
            Player admin = (Player) sender;
            Player TargetOnline = Bukkit.getPlayer(targetName);

            //if TargetOnline is not null open the online player stash fastest
            if(TargetOnline != null)
            {
                openMysticalStash(TargetOnline,admin);
                admin.sendMessage("§aOpening virtual stash for §e" + TargetOnline.getName() + "§a.");
                return true;
            }

            //Check if we have the offline player cached still faster than lookup online
            OfflinePlayer TargetOffline = Bukkit.getOfflinePlayerIfCached(targetName);

            //if it's not cached do the online lookup
            if(TargetOffline == null)
            {
                OfflinePlayer HeavyLookUp = Bukkit.getOfflinePlayer(targetName);
                if(HeavyLookUp != null && HeavyLookUp.hasPlayedBefore())
                {
                    TargetOffline = HeavyLookUp;
                }
            }

            if(TargetOffline != null)
            {
                openMysticalStash(TargetOffline,admin);
                admin.sendMessage("§aOpening offline virtual stash for §e" + TargetOffline.getName() + "§a.");
            }
            else
            {
                admin.sendMessage("§cPlayer '" + targetName + "' has never joined this server.");
            }
            return true;
        }
        if (command.getName().equalsIgnoreCase("vp-hud"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.hud"))
            {
                sender.sendMessage("§cYou do not have permission to change your hud.");
                return true;
            }

            if (!(sender instanceof Player))
            {
                sender.sendMessage("§cOnly players can change their HUD settings!");
                return true;
            }

            Player player = (Player) sender;

            if (args.length < 1)
            {
                player.sendMessage("§cUsage: /vp-hud <deaths | sleep | ping> [ 0 | 1 | 2 ]");
                return true;
            }

            var PDC = player.getPersistentDataContainer();
            NamespacedKey targetKey;
            String optionName = args[0].toLowerCase();

            switch (optionName)
            {
                case "deaths":
                    targetKey = ShowDeathsKey;
                    break;

                case "sleep":
                    targetKey = ShowSleepKey;
                    break;

                case "ping":
                    targetKey = ShowPingKey;
                    break;

                default:
                    player.sendMessage("§cUnknown option! Use: deaths, sleep, or ping.");
                    return true;
            }

            byte newState;

            //If they provided a specific mode (e.g., /vp-hud deaths 1)
            if (args.length >= 2)
            {
                try
                {
                    byte parsedState = Byte.parseByte(args[1]);

                    if (parsedState < 0 || parsedState > 2)
                    {
                        player.sendMessage("§cInvalid type! Choose 0 (Disabled), 1 (Standard), or 2 (Compact).");
                        return true;
                    }

                    newState = parsedState;
                }
                catch (NumberFormatException e)
                {
                    player.sendMessage("§cPlease provide a valid number (0, 1, or 2).");
                    return true;
                }
            }
            //If they just typed '/vp-hud deaths' without a number, turn it off
            else
            {
                newState = 0;
            }

            PDC.set(targetKey, PersistentDataType.BYTE, newState);

            //Format the confirmation message based on the chosen mode
            String statusWord;

            if (newState == 0)
            {
                statusWord = "§cDISABLED";
            }
            else if (newState == 1)
            {
                statusWord = "§aENABLED (Standard)";
            }
            else
            {
                statusWord = "§bENABLED (Compact)";
            }

            player.sendMessage("§aHUD option '§e" + optionName + "§a' is now " + statusWord);

            Update_ScoreBoard(player);
            return true;
        }
        return false;
    }
    /////////////////////////////////////////////ScoreBoard Stuff/////////////////////////////////////////////////////
    public void Update_ScoreBoard(Player player)
    {
        var PDC = player.getPersistentDataContainer();

        //check the settings for the player
        byte ShowDeaths = java.util.Objects.requireNonNullElse(PDC.get(ShowDeathsKey, PersistentDataType.BYTE), (byte) 1);
        byte ShowSleep  = java.util.Objects.requireNonNullElse(PDC.get(ShowSleepKey, PersistentDataType.BYTE), (byte) 1);
        byte ShowPing  = java.util.Objects.requireNonNullElse(PDC.get(ShowPingKey, PersistentDataType.BYTE), (byte) 1);


        // Get the time since last sleep (24000 ticks = 1 day)
        int ticksSinceRest = player.getStatistic(Statistic.TIME_SINCE_REST);
        int daysAwake = ticksSinceRest / 24000;

        //Create StringBuilder
        StringBuilder Status = new StringBuilder();

        Status.append(" ").append(player.getName());
        //Show Deaths
        if(ShowDeaths != 0)
        {
            // Get the deaths from the built-in statistics
            int deathCount = player.getStatistic(Statistic.DEATHS);
            if(ShowDeaths == 1)
            {
                //add to the string
                Status.append(" §7[§cDeaths: ").append(deathCount).append("§7]");
            }
            else if (ShowDeaths == 2)
            {
                //add to the string
                Status.append(" §7[").append("§c💀 ").append(deathCount).append("§7]");
            }
        }
        //Show Sleep
        if(ShowSleep != 0)
        {
            if(ShowSleep == 1)
            {
                //Set the days awake
                Status.append(" §e[").append(daysAwake).append("d Awake]");
            }
            else if(ShowSleep == 2)
            {
                //Set the days awake
                Status.append(" §e[").append("🛌 ").append(daysAwake).append("d]");
            }
        }
        if(ShowPing != 0)
        {
            //Get player ping
            int ping = player.getPing();
            //Dynamic color ping
            String PingColor;
            if(ping < 50) PingColor = "§a"; // Good Green
            else if (ping < 80) PingColor = "§e"; // Average Yellow
            else PingColor = "§c"; // Bad Red
            //Show Ping

            if(ShowPing == 1)
            {
                //Set Ping
                Status.append(" §7[").append(PingColor).append("Ping: ").append(ping).append("ms§7]");
            }
            else if(ShowPing == 2)
            {
                //Set Ping
                Status.append(" §7[").append(PingColor).append("📶 ").append(ping).append("ms§7]");
            }
        }

        //apply the stats
        player.setPlayerListName(Status.toString());
    }
    @EventHandler
    public void onPlayerJoins(PlayerJoinEvent event)
    {
        //Set the stats of this player in the scoreboard
        Update_ScoreBoard(event.getPlayer());
    }
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        Player Victim_Player = event.getEntity();

        // We wait 20 ticks so the server has time to increment the death stat
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                Update_ScoreBoard(Victim_Player);
            }
        }.runTaskLater(this, 20L);
    }
    /////////////////////////////////////Mystical Stash//////////////////////////////////////
    //this is here just as an id to pass for the stash when opened using the book
    public static class StashHolder implements InventoryHolder
    {
        //this is here just to please bukkit/spigot do not remove this, although it seems a death end
        @Override
        public Inventory getInventory()
        {
            return null;
        }
    }
    //this stashHolder is for admin usage so we know an admin opened the inv of another player with some info about the opened inv
    public class AdminStashHolder extends StashHolder
    {
        private final UUID targetUUID;
        private final String targetName;

        public AdminStashHolder(UUID targetUUID, String targetName)
        {
            this.targetUUID = targetUUID;
            this.targetName = targetName;
        }

        public UUID getTargetUUID()
        {
            return targetUUID;
        }

        public String getTargetName()
        {
            return targetName;
        }
    }

    @EventHandler
    public void giveCustomBookIfMissing(PlayerJoinEvent event)
    {
        //check if enabled
        if(this.configLoader.Enable_Mystical_Stash)
        {
            Player player = event.getPlayer();
            boolean hasBook = false;
            boolean hasSpace = false;

            for (ItemStack item : player.getInventory().getContents())
            {
                if (item == null || item.getType() == Material.AIR)
                {
                    hasSpace = true;
                }
                else if (item.getType() == Material.WRITTEN_BOOK)
                {
                    BookMeta meta = (BookMeta) item.getItemMeta();
                    if (meta.hasAuthor() && meta.getAuthor().equals("§bServer Wizard"))
                    {
                        hasBook = true;
                    }
                }
            }

            if (!hasBook && hasSpace)
            {
                ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
                BookMeta meta = (BookMeta) book.getItemMeta();
                meta.setDisplayName("§dMystical Stash");
                meta.setTitle("§6VanillaPlus Extra inv");
                meta.setAuthor("§bServer Wizard");

                List<String> pages = new ArrayList<>();
                pages.add("§dNothing to see here!");
                meta.setPages(pages);

                book.setItemMeta(meta);
                player.getInventory().addItem(book);
                player.sendMessage("§aYou have received the §dMystical Stash§a!");
            }
            else if (!hasSpace && !hasBook)
            {
                player.sendMessage("§aINV IS FULL NO §dMystical Stash§a! FOR YOU");
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if(this.configLoader.Enable_Mystical_Stash)
        {
            ItemStack item = event.getItem();

            if (item != null && item.getType() == Material.WRITTEN_BOOK)
            {
                ItemMeta meta = item.getItemMeta();
                if (meta instanceof BookMeta)
                {
                    BookMeta bookMeta = (BookMeta) meta;
                    if (bookMeta.hasDisplayName())
                    {
                        if (bookMeta.hasAuthor() && bookMeta.getAuthor().equals("§bServer Wizard"))
                        {
                            event.setCancelled(true);
                            openMysticalStash(event.getPlayer(),null);
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        if(this.configLoader.Enable_Mystical_Stash)
        {
            //check if the inv title is Mystical Stash
            if (event.getView().getTitle().equals("Mystical Stash"))
            {
                //Check if this is opened by admin via command if so don't do anything
                if (event.getInventory().getHolder() instanceof AdminStashHolder)
                {
                    return;
                }
                //check if this is the book or a chest by checking if its labeled StashHolder
                if (!(event.getInventory().getHolder() instanceof StashHolder))
                {
                    //Check if we allow usage of named chest for the opening of the Mystical Stash
                    if(configLoader.Enable_Mystical_Stash_Chests)
                    {
                        // Stop the player from opening the physical chest
                        event.setCancelled(true);
                        //Open the "Real" virtual stash instead
                        openMysticalStash((Player) event.getPlayer(),null);
                    }
                    //Let the chest open normally
                }
                // If it IS a StashHolder/the book, we do nothing and let it open normally!
            }
        }
    }

    private final java.util.Set<java.util.UUID> activelyInspectedStashes = new java.util.HashSet<>();
    private void openMysticalStash(OfflinePlayer player, Player admin)
    {
        if(this.configLoader.Enable_Mystical_Stash)
        {
            //Check if player stash is currently being inspected by an admin and thus is locked
            if (admin == null && activelyInspectedStashes.contains(player.getUniqueId()))
            {
                Player OnlineTarget = player.getPlayer();
                if(OnlineTarget != null)
                {
                    OnlineTarget.sendMessage("§cAn admin is currently managing your stash. Access denied.");
                }
                return;//return here admin is accessing the stash now
            }

            //Jump of main thread to handle file loading
            Bukkit.getScheduler().runTaskAsynchronously(this, () ->
            {
                File file = new File(getDataFolder(), player.getUniqueId() + ".yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);

                //Jump back to main thread to perform ui actions safely
                Bukkit.getScheduler().runTask(this, () ->
                {
                    //Check it again for desync reasons with async schedular
                    if (admin == null && activelyInspectedStashes.contains(player.getUniqueId()))
                    {
                        Player OnlineTarget = player.getPlayer();
                        if(OnlineTarget != null)
                        {
                            OnlineTarget.sendMessage("§cAn admin is currently managing your stash. Access denied.");
                        }
                        return;//return here admin is accessing the stash now
                    }

                    Inventory extraInv;

                    if(admin == null)
                    {
                        extraInv = Bukkit.createInventory(new StashHolder(), 54, "Mystical Stash");
                    }
                    else
                    {
                        extraInv = Bukkit.createInventory(new AdminStashHolder(player.getUniqueId(), player.getName()), 54, "Mystical Stash");
                        //lock player out
                        activelyInspectedStashes.add(player.getUniqueId());
                        Player OnlineTarget = player.getPlayer();
                        if (OnlineTarget != null)
                        {
                            String PlainTitle = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize((OnlineTarget.getOpenInventory().title()));
                            if (PlainTitle.equals("Mystical Stash"))
                            {
                                OnlineTarget.closeInventory();
                                OnlineTarget.sendMessage("§cAn admin has accessed your Mystical Stash.");
                            }
                        }
                    }

                    for (int i = 0; i < extraInv.getSize(); ++i)
                    {
                        extraInv.setItem(i, config.getItemStack("inventory." + i));
                    }

                    //open the inv for the player who asked for it
                    if (admin == null)
                    {
                        Player OnlineTarget = player.getPlayer();
                        if (OnlineTarget != null)
                        {
                            OnlineTarget.openInventory(extraInv);
                        }
                    }
                    else
                    {
                        admin.openInventory(extraInv);

                    }
                });
            });
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event)
    {
        if(this.configLoader.Enable_Mystical_Stash)
        {
            if (event.getView().getTitle().equals("Mystical Stash"))
            {
                Inventory inv = event.getInventory();
                UUID PlayerUUID;
                String PlayerName;

                if (inv.getHolder() instanceof AdminStashHolder)
                {
                    AdminStashHolder holder = (AdminStashHolder) inv.getHolder();
                    PlayerUUID = holder.getTargetUUID();
                    PlayerName = holder.getTargetName();

                    //Remove the lock so the regular player can safely open their stash again
                    this.activelyInspectedStashes.remove(holder.getTargetUUID());
                }
                else if (inv.getHolder() instanceof StashHolder)
                {
                    Player player = (Player) event.getPlayer();
                    PlayerUUID = player.getUniqueId();
                    PlayerName = player.getName();
                }

                else
                {
                    return; // not one of our custom stashes no holder found
                }

                ItemStack[] LiveContents = event.getInventory().getContents();
                ItemStack[] ClonedContents = new ItemStack[LiveContents.length];
                //make a clone of the content so we can pass this clone for saving so no corruption can occur
                for (int i = 0; i < LiveContents.length; ++i)
                {
                    if (LiveContents[i] != null)
                    {
                        ClonedContents[i] = LiveContents[i].clone();
                    }
                }

                //do the saving on a separate thread;
                new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                        Save_Stash_To_File(PlayerUUID, ClonedContents, PlayerName);
                    }
                }.runTaskAsynchronously(this);
            }
        }
    }

    private void Save_Stash_To_File(UUID PlayerUUID, ItemStack[] Contents, String Player_Name)
    {
        if(this.configLoader.Enable_Mystical_Stash)
        {
            File file = new File(this.getDataFolder(), PlayerUUID + ".yml");
            File TMPfile = new File(this.getDataFolder(), PlayerUUID + ".yml.tmp");

            //load the existing config file if it exists
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            for (int i = 0; i < Contents.length; ++i)
            {
                config.set("inventory." + i, Contents[i]);
            }

            try
            {
                //Try saving to temp file
                config.save(TMPfile);

                //When successfully saved to temp file swap the temp file and the normal stash file
                Files.move(TMPfile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            catch (IOException e)
            {
                this.getLogger().severe("Could not save stash for " + Player_Name + ": " + e.getMessage());

                // Clean up the leftover temp file if the operation failed midway
                if (TMPfile.exists())
                {
                    TMPfile.delete();
                }
            }
        }
    }
    /////////////////////////////////////End of Mystical Stash//////////////////////////////////////

    /////////////////////////////////////Ban on join////////////////////////////////////////////////
    @EventHandler
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event)
    {
        //Check if ban on join is enabled
        if (this.configLoader.Ban_On_Join)
        {
            //Get data directly from the event safely
            UUID playerUuid = event.getUniqueId();
            String Banned_Player_Name = event.getName();
            org.bukkit.profile.PlayerProfile profile = event.getPlayerProfile();

            //Check if whitelist is enabled first
            if (Bukkit.hasWhitelist())
            {
                //Fail-safe check matching both UUID and Name to protect offline-mode testers
                boolean isWhitelisted = false;
                for (OfflinePlayer whitelistedPlayer : Bukkit.getWhitelistedPlayers())
                {
                    if (whitelistedPlayer.getUniqueId().equals(playerUuid) ||
                            (whitelistedPlayer.getName() != null && whitelistedPlayer.getName().equalsIgnoreCase(Banned_Player_Name)))
                    {
                        isWhitelisted = true;
                        break;
                    }
                }

                if (!isWhitelisted)
                {
                    if (!Bukkit.getBanList(BanList.Type.PROFILE).isBanned(profile.toString()))
                    {
                        //Do the actual ban using the Profile
                        Bukkit.getBanList(BanList.Type.PROFILE).addBan(profile.toString(), this.configLoader.Ban_Reason, null, null);

                        //Boot them out instantly from this connection attempt
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Component.text(this.configLoader.Ban_Reason));

                        String Fresh_Ban_MSG = "Fresh BAN Event Triggered for player by name: " + Banned_Player_Name;
                        this.getLogger().warning(Fresh_Ban_MSG);

                        //Send the msg using the main thread to avoid issues
                        Bukkit.getScheduler().runTask(this, () ->
                        {
                            for (Player player : Bukkit.getOnlinePlayers())
                            {
                                player.sendMessage(Component.text(Fresh_Ban_MSG, NamedTextColor.RED));
                            }
                        });
                    }
                    else
                    {
                        //Kick them instantly since they are already on the ban list
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, Component.text(this.configLoader.Ban_Reason));

                        String Already_Banned_MSG = "An Already Banned Player by name of: " + Banned_Player_Name + " Tried To Join";
                        this.getLogger().warning(Already_Banned_MSG);

                        //Send the msg using the main thread to avoid issues
                        Bukkit.getScheduler().runTask(this, () ->
                        {
                            for (Player player : Bukkit.getOnlinePlayers())
                            {
                                player.sendMessage(Component.text(Already_Banned_MSG, NamedTextColor.RED));
                            }
                        });
                    }
                }
            }
        }
    }
    /////////////////////////////////////////////Welcoming stuff/////////////////////////////////////////////////////
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final Player player = event.getPlayer();

        //if true show login msg's
        if (this.configLoader.Show_Login_Messages)
        {
            boolean customMessageSent = false;
            //if true try to send a custom msg to this player first
            if (this.configLoader.Show_Player_Specific_Login_Messages)
            {
                //get the Custom Player messages
                ConfigurationSection Custom_Player_Messages = getConfig().getConfigurationSection("Custom-Player-Messages");

                if (Custom_Player_Messages != null)
                {
                    for (String Player_MSG_Key : Custom_Player_Messages.getKeys(false))
                    {
                        ConfigurationSection section = Custom_Player_Messages.getConfigurationSection(Player_MSG_Key);
                        if (section == null) continue;

                        //Get the Player uuid string and compare it to the joining players uuid
                        String UUID_String = section.getString("Player-UUID");
                        if (UUID_String == null) continue;
                        try
                        {
                            UUID Player_UUID = UUID.fromString(UUID_String);
                            if (player.getUniqueId().equals(Player_UUID))
                            {
                                //Get the msg list for this player
                                List<String> Custom_Messages = section.getStringList("Custom-Messages");
                                if (!Custom_Messages.isEmpty())
                                {
                                    //Get the random string
                                    String rawMessage = Custom_Messages.get(new Random().nextInt(Custom_Messages.size()));
                                    //Swap %s for the player name
                                    String Formatted = String.format(rawMessage, player.getName());

                                    player.sendMessage(Formatted);
                                    customMessageSent = true;
                                }
                                break;//this makes sure that if the uuid matched the joining player uuid we stop looking in the list
                            }
                        }
                        catch (IllegalArgumentException e)
                        {
                            this.getLogger().warning("Invalid UUID format for player: " + Player_MSG_Key);
                        }
                    }
                }
            }
            //Fallback to normal messages
            if (!customMessageSent)
            {
                //get the messages from the config
                List<String> messages = getConfig().getStringList("Login-Messages");
                if (!messages.isEmpty())
                {
                    String rawMessage = messages.get(new Random().nextInt(messages.size()));
                    //Swap %s for the player name
                    String finalMessage = String.format(rawMessage, player.getName());

                    player.sendMessage(finalMessage);
                }
            }
        }

        //if true launch fireworks upon join
        if(this.configLoader.Enable_Login_Fireworks)
        {
            this.spawnWelcomeFireworks(player);
        }
    }

    private void spawnWelcomeFireworks(Player player)
    {
        final World world = player.getWorld();
        final Random random = new Random();

        new BukkitRunnable()
        {
            int fireworkCount = 10;

            @Override
            public void run()
            {
                if (this.fireworkCount <= 0)
                {
                    this.cancel();
                    return;
                }

                this.fireworkCount--;
                double x = player.getLocation().getX() + (random.nextDouble() * 20.0) - 10.0;
                double y = player.getLocation().getY();
                double z = player.getLocation().getZ() + (random.nextDouble() * 20.0) - 10.0;
                Location loc = new Location(world, x, y, z);

                Firework fw = (Firework) world.spawn(loc, Firework.class);
                FireworkMeta fwm = fw.getFireworkMeta();

                fwm.addEffect(FireworkEffect.builder()
                        .withColor(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
                        .withFade(Color.fromRGB(random.nextInt(256), random.nextInt(256), random.nextInt(256)))
                        .with(FireworkEffect.Type.BALL_LARGE)
                        .trail(true)
                        .build());

                fwm.setPower(random.nextInt(2) + 1);
                fw.setFireworkMeta(fwm);
            }
        }.runTaskTimer(this, 0L, 10L);
    }
    //////////////////////////////////////creepers/enderman fixes///////////////////////////////////////
    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event)
    {
        //if true stop creepers from spawning.
        if (this.configLoader.Disable_Creeper_Spawn)
        {
            if (event.getEntityType().equals(EntityType.CREEPER))
            {
                event.setCancelled(true);
            }
        }
        if(this.configLoader.Disable_Phantom_Spawn)
        {
            if (event.getEntityType().equals(EntityType.PHANTOM))
            {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void ExplosionPrime(ExplosionPrimeEvent event)
    {
        //if true stop creepers from exploding.
        if (this.configLoader.Disable_Creeper_Explosions)
        {
            if (event.getEntityType() == EntityType.CREEPER)
            {
                //Cancel Explode event
                event.setCancelled(true);

                //Get event location and play a sound there and delete the creeper
                Location loc = event.getEntity().getLocation();
                World world = loc.getWorld();
                if (world !=null && event.getEntity() instanceof Creeper creeper)
                {
                    //For maximum fun do a random pitch every time
                    float randomPitch = 0.5f + (float) Math.random();
                    world.playSound(loc,Sound.ENTITY_CHICKEN_EGG,1.0f,randomPitch);
                    creeper.remove();
                }

            }
        }
    }
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event)
    {
        //if true disable creepers from doing block damage.
        if (this.configLoader.Disable_Creeper_Block_Damage)
        {
            if (event.getEntityType() == EntityType.CREEPER)
            {
                //clear block list so no blocks get destroyed
                event.blockList().clear();
                return;
            }
        }
        //if true make it so creepers give back all blocks they destroy
        if (this.configLoader.Creeper_Gives_all_Blocks)
        {
            this.getLogger().warning("creeper returns all blocks");
            if (event.getEntityType() == EntityType.CREEPER)
            {
                event.setYield(100.0f);
            }
        }

        //check for fireballs
        if (event.getEntityType() == EntityType.FIREBALL)
        {
            //check if it's a large fireball
            if (event.getEntity() instanceof LargeFireball fireball)
            {
                //check if a ghast shot it
                if (fireball.getShooter() instanceof Ghast)
                {
                    if(this.configLoader.Disable_Ghast_Explosions)
                    {
                        event.setCancelled(true);

                        //Get enitiy event location if entity is null because out of render distance then get event location so we always have a location
                        Location loc = (event.getEntity() != null) ? event.getEntity().getLocation() : event.getLocation();
                        World world = loc.getWorld();
                        if (world !=null)
                        {
                            //For maximum fun do a random pitch every time
                            float randomPitch = 0.5f + (float) Math.random();
                            world.playSound(loc,Sound.ENTITY_CHICKEN_EGG,1.0f,randomPitch);
                            fireball.remove();
                        }
                    }
                    else if(this.configLoader.Disable_Ghast_Block_Damage)
                    {
                        //clear block list so no blocks get destroyed
                        event.blockList().clear();
                    }
                    else if (this.configLoader.Ghast_Gives_all_Blocks)
                    {
                        event.setYield(100.0f);

                        // 1. Create a list to remember all block locations that are exploding
                        List<Location> explodedLocations = new ArrayList<>();
                        for (Block block : event.blockList())
                        {
                            explodedLocations.add(block.getLocation());
                        }

                        // 2. Wait 1 tick for the explosion to complete and spawn its fire blocks
                        Bukkit.getScheduler().runTaskLater(this, () ->
                        {
                            for (Location loc : explodedLocations)
                            {
                                Block block = loc.getBlock();
                                Block above = block.getRelative(BlockFace.UP);

                                // Snuff out fire inside the exploded crater hole
                                if (block.getType() == Material.FIRE)
                                {
                                    block.setType(Material.AIR);
                                }

                                // Snuff out fire sitting on the rim/edge of the crater
                                if (above.getType() == Material.FIRE)
                                {
                                    above.setType(Material.AIR);
                                }
                            }
                        }, 1L);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event)
    {
        //if true stops enderman from griefing.
        if (this.configLoader.Stop_Enderman_From_Griefing)
        {
            if (event.getEntityType() == EntityType.ENDERMAN)
            {
                event.setCancelled(true);
                EndermanAmount++;
            }
        }
    }


    /////////////////////////////////Just for fun////////////////////////////////////////
    private Player Player_Picker()
    {
        //Get all online players into a list
        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty())
        {
            return null;
        }
        //Pick a random number between 0 and the number of players
        int randomIndex = new Random().nextInt(players.size());
        //Return the lucky (or unlucky) winner
        return players.get(randomIndex);
    }

    public void startCreepyTimer()
    {
        //Check if a timer is not already running.
        if(this.creepyTask != null)
        {
            this.creepyTask.cancel();
        }
        //Check if we need to run a fresh timer.
        if(this.configLoader.Enable_Creepy_Stuff)
        {
            //Run this every 10 to 20 minutes (12000 to 24000 ticks)
            this.creepyTask = new BukkitRunnable()
            {
                @Override
                public void run()
                {
                    TriggerCreepyEvents();
                }
            }.runTaskTimer(this, 200L, 12000L);
        }
    }
    @EventHandler
    public void onPlayerBed(PlayerBedEnterEvent event)
    {
        //Check if this feature is enabled first.
        if (this.configLoader.Enable_Creepy_Stuff)
        {
            //Return the lucky (or unlucky) winner.
            Player bedPlayer = Player_Picker();
            if (bedPlayer != null)
            {
                if (Math.random() < 0.6)
                {
                    bedPlayer.sendMessage("§cYou feel uneasy... something's watching you.");

                    new BukkitRunnable()
                    {
                        int beats = 0;

                        @Override
                        public void run()
                        {
                            if (this.beats >= 6)
                            {
                                this.cancel();
                            }
                            else
                            {
                                bedPlayer.playSound(bedPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
                                this.beats++;
                            }
                        }
                    }.runTaskTimer(this, 0L, 10L);

                    new BukkitRunnable()
                    {
                        @Override
                        public void run()
                        {
                            bedPlayer.playSound(bedPlayer.getLocation(), Sound.ENTITY_CREEPER_PRIMED, 100.0F, 1.0F);
                            if (Math.random() < 0.8)
                            {
                                bedPlayer.getWorld().createExplosion(bedPlayer.getLocation(), 0.0F);
                            }
                        }
                    }.runTaskLater(this, 60L);
                }
            }
        }
    }

    public void TriggerCreepyEvents()
    {
        //Get the player to run this on.
        Player Victim = Player_Picker();
        if(Victim != null)
        {
            //Ghost footsteps (behind the player).
            if(Math.random() < 0.6)
            {
                Location Loc_Behind_Player = Victim.getLocation().subtract(Victim.getLocation().getDirection().multiply(2));
                Victim.playSound(Loc_Behind_Player, Sound.BLOCK_DEEPSLATE_STEP, 1.0F, 0.5F);
                //Now make a fake player leave so it seems a player was running behind them and left.
                String FakePlayer_Leaver_Name = "HeroBrine";
                Victim.sendMessage("§e" + FakePlayer_Leaver_Name + " left the game");
            }
            else if(Math.random() < 0.6)
            {
                //Spawn an enderman slightly away
                Location stalkerLoc = Victim.getLocation().add(Victim.getLocation().getDirection().multiply(15));
                Enderman stalker = Victim.getWorld().spawn(stalkerLoc, Enderman.class);

                //Remove him almost immediately so they only see a glimpse.
                new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                        stalker.remove();
                    }
                }.runTaskLater(this, 30L); // 1.5 seconds later
            }
            else
            {
                fakeWhisper(Victim);
            }
        }
    }

    public void fakeWhisper(Player Victim)
    {
        //Check for players within 5 blocks.
        for (Entity entity : Victim.getNearbyEntities(5, 5, 5))
        {
            if (entity instanceof Player nearbyPlayer && !nearbyPlayer.equals(Victim))
            {
                //Format it to look like a real /msg or /tell.
                Victim.sendMessage("§d[" + nearbyPlayer.getName() + " -> me] why are you staring at me?");
                break;
            }
        }
    }
}