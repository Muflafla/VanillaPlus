package BK.vanilaPlus;

import java.io.File;
import java.io.IOException;
import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.BanList.Type;
import org.bukkit.block.Block;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;

public class VanillaPlus extends JavaPlugin implements Listener
{
    public int EndermanAmount = 0;
    public BukkitTask EndermanTask;
    public BukkitTask creepyTask;

    public ConfigLoader configLoader;

    @Override
    public void onEnable()
    {
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
    }

    ////////////////////////////////////////Load config in memory/////////////////////////////////////////////
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
            if (!sender.hasPermission("VanillaPlus.reload"))
            {
                sender.sendMessage("§cYou do not have permission to reload this plugin.");
                return true;
            }

            //reload the recipes
            this.registerRecipes();
            //let the configloader get all the vars fresh from file
            configLoader.LoadAllVars();
            //init the plugin
            PluginInit();

            sender.sendMessage("§aVanillaPlus configuration reloaded!");
            return true;
        }
        return false;
    }
    /////////////////////////////////////////////ScoreBoard Stuff/////////////////////////////////////////////////////
    public void Update_ScoreBoard(Player player)
    {
        // Get the deaths from the built-in statistics
        int deathCount = player.getStatistic(Statistic.DEATHS);

        // Get the time since last sleep (24000 ticks = 1 day)
        int ticksSinceRest = player.getStatistic(Statistic.TIME_SINCE_REST);
        int daysAwake = ticksSinceRest / 24000;

        //Start building the string: "PlayerName §7[§cDeaths: X§7]"
        String status = " §7[§cDeaths: " + deathCount + "§7]";

        //Set the days awake
        status += " §e[" + daysAwake + "d Awake]";

        // This is the magic line that changes the Tab list entry
        player.setPlayerListName(player.getName() + status);
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
    @EventHandler
    public void onPlayerLeaveBed(PlayerBedLeaveEvent event)
    {
        Player Rested_Player = event.getPlayer();

        //Wait 20 ticks so the server has time to update the TIME_SINCE_REST stat to 0.
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                Update_ScoreBoard(Rested_Player);
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
                            openMysticalStash(event.getPlayer());
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
                //check if this is the book or a chest by chacking if its labeled StashHolder
                if (!(event.getInventory().getHolder() instanceof StashHolder))
                {
                    // Stop the player from opening the physical chest
                    event.setCancelled(true);

                    //Open the "Real" virtual stash instead
                    openMysticalStash((Player) event.getPlayer());
                }
                // If it IS a StashHolder/the book, we do nothing and let it open normally!
            }
        }
    }

    private void openMysticalStash(Player player)
    {
        if(this.configLoader.Enable_Mystical_Stash)
        {
            File file = new File(this.getDataFolder(), player.getUniqueId() + ".yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            Inventory extraInv = Bukkit.createInventory(new StashHolder(), 54, "Mystical Stash");

            for (int i = 0; i < extraInv.getSize(); ++i)
            {
                extraInv.setItem(i, config.getItemStack("inventory." + i));
            }

            player.openInventory(extraInv);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event)
    {
        if(this.configLoader.Enable_Mystical_Stash)
        {
            if (event.getView().getTitle().equals("Mystical Stash"))
            {
                Player player = (Player) event.getPlayer();

                ItemStack[] Contents = event.getInventory().getContents();
                UUID PlayerUUID = player.getUniqueId();

                //do the saving on a separate thread;
                new BukkitRunnable()
                {
                    @Override
                    public void run()
                    {
                        Save_Stash_To_File(PlayerUUID, Contents, player.getName());
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
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            for (int i = 0; i < Contents.length; ++i)
            {
                config.set("inventory." + i, Contents[i]);
            }

            try
            {
                config.save(file);
            }
            catch (IOException e)
            {
                this.getLogger().severe("Could not save stash for " + Player_Name + ": " + e.getMessage());
            }
        }
    }
    /////////////////////////////////////End of Mystical Stash//////////////////////////////////////

    /////////////////////////////////////Ban on join////////////////////////////////////////////////
    @EventHandler
    public void onPlayerPreJoin(AsyncPlayerPreLoginEvent event)
    {
        //Check if ban on join is enabled
        if(this.configLoader.Ban_On_Join)
        {
            //get the uuid of the player about to be banned
            UUID playerUuid = event.getUniqueId();
            //get the name of the player about to be banned
            String Banned_Player_Name = event.getPlayerProfile().getName();
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);

            if (!Bukkit.getWhitelistedPlayers().contains(offlinePlayer))
            {
                if (!Bukkit.getBannedPlayers().contains(offlinePlayer))
                {
                    //Do the actual ban
                    Bukkit.getBanList(Type.PROFILE).addBan(playerUuid.toString(), this.configLoader.Ban_Reason, null, null);
                    //Send a msg to console and all players
                    String Fresh_Ban_MSG = "Fresh BAN Event Triggered for player by name: " + Banned_Player_Name;
                    this.getLogger().warning(Fresh_Ban_MSG);

                    for (Player player : Bukkit.getOnlinePlayers())
                    {
                        player.sendMessage(Component.text(Fresh_Ban_MSG, NamedTextColor.RED));
                    }
                }
                else
                {
                    //Send a msg to console and all players
                    String Already_Banned_MSG = "An Already Banned Player by name of: " + Banned_Player_Name + " Tried To Join";
                    this.getLogger().warning(Already_Banned_MSG);

                    for (Player player : Bukkit.getOnlinePlayers())
                    {
                        player.sendMessage(Component.text(Already_Banned_MSG, NamedTextColor.RED));
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
                if (fireball.getShooter() instanceof org.bukkit.entity.Ghast)
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
                        java.util.List<Location> explodedLocations = new java.util.ArrayList<>();
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
                                Block above = block.getRelative(org.bukkit.block.BlockFace.UP);

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
        java.util.List<Player> players = new java.util.ArrayList<>(Bukkit.getOnlinePlayers());
        if (players.isEmpty())
        {
            return null;
        }
        //Pick a random number between 0 and the number of players
        int randomIndex = new java.util.Random().nextInt(players.size());
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