package BK.VanillaPlus;

import java.util.*;

import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bstats.bukkit.Metrics;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class VanillaPlus extends JavaPlugin implements Listener
{
    public BukkitTask creepyTask;
    public BukkitTask ScoreboardTask;
    //Make all the Classes available here
    public ConfigLoader configLoader;
    public ConfigUpdater configUpdater;
    public CustomBlockManager  customBlockManager;
    public StashManager stashManager;
    public BeaconManager beaconManager;
    public SleepManager sleepManager;
    public ProtectionManager protectionManager;
    public BlockInteractManager blockInteractManager;
    public VillagerManager villagerManager;
    public WarpManager  warpManager;
    //This is for the miniblocks so they inject on bootup
    public boolean bootInject;

    //SCOREBOARD
    private final NamespacedKey ShowDeathsKey = new NamespacedKey(this, "hud-deaths");
    private final NamespacedKey ShowSleepKey = new NamespacedKey(this, "hud-sleep");
    private final NamespacedKey ShowPingKey = new NamespacedKey(this, "hud-ping");

    //AFK
    private final Map <UUID, Location> oldPlayerLocations = new HashMap<>();
    private final Set<UUID> afkPlayers = new HashSet<>();

    @Override
    public void onEnable()
    {
        //enable bStats
        int pluginId = 32274;
        new Metrics(this, pluginId);

        bootInject = true;

        //register this plugin events
        Bukkit.getPluginManager().registerEvents(this, this);
        //Instantiate the ConfigUpdater and call the update function inside it
        this.configUpdater = new ConfigUpdater(this);
        configUpdater.update_Config();
        //Instantiate the ConfigLoader
        this.configLoader = new ConfigLoader(this);
        //Instantiate the customBlockManager and make it able to register event
        this.customBlockManager = new CustomBlockManager(this);
        Bukkit.getPluginManager().registerEvents(customBlockManager, this);
        //Instantiate the StashManager and make it able to register event
        stashManager = new StashManager(this);
        Bukkit.getPluginManager().registerEvents(stashManager, this);
        //Instantiate the protectionManager and make it able to register event
        protectionManager = new ProtectionManager(this);
        Bukkit.getPluginManager().registerEvents(protectionManager, this);
        //Instantiate the BeaconManager and make it able to register event
        beaconManager = new BeaconManager(this);
        Bukkit.getPluginManager().registerEvents(beaconManager, this);
        //Instantiate the SleepManager and make it able to register event
        sleepManager = new SleepManager(this);
        Bukkit.getPluginManager().registerEvents(sleepManager, this);
        //Instantiate the blockInteractManager and make it able to register event
        blockInteractManager = new BlockInteractManager(this);
        Bukkit.getPluginManager().registerEvents(blockInteractManager, this);
        //Instantiate the villagerManager and make it able to register event
        villagerManager = new VillagerManager(this);
        Bukkit.getPluginManager().registerEvents(villagerManager, this);
        //Instantiate the WarpManager
        warpManager = new WarpManager(this);

        //Get the current plugin version and print it to console
        String version = this.getDescription().getVersion();
        Bukkit.getConsoleSender().sendMessage("§aVanillaPlus Plugin Enabled! Version: §e" + version);

        //init the plugin
        PluginInit();

        //Instantiate the UpdateChecker and call the checkForUpdates function inside it
        UpdateChecker checker = new UpdateChecker(this, "VanillaPlus");
        checker.checkForUpdates();
    }
    @Override
    public void onDisable()
    {
        Bukkit.getConsoleSender().sendMessage("§aCleanly Shutting Down...");
        //Force close all stashes and save them on the main thread
        stashManager.forceSaveAllAndClose();
        //Nuke all the tasks
        Bukkit.getScheduler().cancelTasks(this);
    }
    //////////////////////////////////////// init the plugin /////////////////////////////////////////////
    public void PluginInit()
    {
        //Load the config into memory.
        configLoader.LoadAllVars();

        //the timer checks if its enabled or not, this is needed for if it's changed in while server is on so timer knows to stop
        startCreepyTimer();

        //Start the RecipeManager_Init, it wil check if custom recipes and or mini blocks needs to be loaded
        this.customBlockManager.RecipeManager_Init();
        //Start the ProtectionManager
        this.protectionManager.ProtectionManager_Init();
        //Check if beacon changes are enabled
        if (configLoader.Enable_Beacon_Changes)
        {
            beaconManager.setupConfig();
        }
        //Run this even when hardened pick is disabled because it could be players have the VIII pick now so we need to clear it
        beaconManager.startPickaxeEffect();

        if(configLoader.EnableCustomSleep)
        {
            //Load the daytracker in the SleepManager
            sleepManager.DayTracker();
        }
        if(configLoader.Enable_Mystical_Stash)
        {
            stashManager.StashManager_Init();
        }

        //Always cancel the old task
        if(ScoreboardTask != null)
        {
            ScoreboardTask.cancel();
            ScoreboardTask = null;
        }
        //always start the task fresh. we run this to check ping and afk every 1 min
        ScoreboardTask = Bukkit.getScheduler().runTaskTimer(this, () ->
        {
            for (Player player : Bukkit.getOnlinePlayers())
            {
                UUID playerUUID = player.getUniqueId();
                //get old location and current location and compare them
                Location oldLocation = oldPlayerLocations.get(player.getUniqueId());
                Location currentLocation = player.getLocation();
                if(oldLocation == null)
                {
                    oldPlayerLocations.put(playerUUID, currentLocation);
                }
                else if(oldLocation.equals(currentLocation))
                {
                    afkPlayers.add(playerUUID);
                }
                else//old and current don't match, Update old location to be current location for next check in 60sec
                {
                    oldPlayerLocations.put(playerUUID, currentLocation);
                    //Remove player from afk list
                    afkPlayers.remove(playerUUID);
                }

                Update_ScoreBoard(player);
            }
        },0,1200L);//1200L is 60 sec
    }
    @EventHandler
    public void onServerResourceReloadEvent(ServerResourcesReloadedEvent event)
    {
        Bukkit.getConsoleSender().sendMessage("§aDetected Server Reload! Reloading the Plugin");
        //Clear the stash Cache
        stashManager.ClearStashCache();
        PluginInit();
    }
    //////////////////////////////////////////////////////Commands////////////////////////////////////////////////////
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if(command.getName().equalsIgnoreCase("vp-ram-stashes"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.ram_stashes"))
            {
                sender.sendMessage("§cYou do not have permission to reload this plugin.");
                return true;
            }
            //Must be a player to grab coordinates
            if (!(sender instanceof Player player))
            {
                stashManager.displayStashesInRam(null);
                return true;
            }
            else
            {
                stashManager.displayStashesInRam(player);
                return true;
            }
        }
        if(command.getName().equalsIgnoreCase("vp-chunkload"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.chunkload"))
            {
                sender.sendMessage("§cYou do not have permission to reload this plugin.");
                return true;
            }
            //Must be a player to grab coordinates
            if (!(sender instanceof Player player))
            {
                Bukkit.getConsoleSender().sendMessage("§aOnly players can use this command.");
                return true;
            }
            else
            {
                ChunkLoader(player);
                return true;
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        //================================== WARPS ===============================================
        if(command.getName().equalsIgnoreCase("vp-create-warp"))
        {
            if(!sender.hasPermission("VanillaPlus.create_warp"))
            {
                sender.sendMessage("§cYou do not have permission to create a warps.");
                return true;
            }

            if(!(sender instanceof Player player))
            {
                Bukkit.getConsoleSender().sendMessage("§aOnly players can use this command.");
                return true;
            }

            //Check if the player provided a name
            if(args.length > 0)
            {
                String warpName = args[0];
                Location loc = player.getLocation();

                warpManager.CreateWarp(player, warpName, loc);
                return true;
            }

            //Msg the player no valid name was provided
            player.sendMessage("§eUsage: /vp-create-warp <warp name>");
            return true;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if(command.getName().equalsIgnoreCase("vp-delete-warp"))
        {
            if(!sender.hasPermission("VanillaPlus.delete_warp"))
            {
                sender.sendMessage("§cYou do not have permission to delete a warps.");
                return true;
            }

            if(!(sender instanceof Player player))
            {
                Bukkit.getConsoleSender().sendMessage("§aOnly players can use this command.");
                return true;
            }

            //Check if the player provided a name
            if(args.length > 0)
            {
                String warpName = args[0];

                warpManager.DeleteWarp(player, warpName);
                return true;
            }

            //Msg the player no valid name was provided
            player.sendMessage("§eUsage: /vp-delete-warp <warp name>");
            return true;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if(command.getName().equalsIgnoreCase("vp-warp"))
        {
            if(!sender.hasPermission("VanillaPlus.warp"))
            {
                sender.sendMessage("§cYou do not have permission to warp.");
                return true;
            }

            if(!(sender instanceof Player player))
            {
                Bukkit.getConsoleSender().sendMessage("§aOnly players can use this command.");
                return true;
            }

            //Check if the player provided a name
            if(args.length > 0)
            {
                String warpName = args[0];

                warpManager.Warp(player, warpName);
                return true;
            }

            //Msg the player no valid name was provided
            player.sendMessage("§eUsage: /vp-warp <warp name>");
            return true;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if(command.getName().equalsIgnoreCase("vp-warp-list"))
        {
            if(!sender.hasPermission("VanillaPlus.warp_list"))
            {
                sender.sendMessage("§cYou do not have permission to get the list of warps.");
                return true;
            }

            //Console send this command
            warpManager.warpList(sender);
            return true;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if(command.getName().equalsIgnoreCase("vp-overflow"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.overflow"))
            {
                sender.sendMessage("§cYou do not have permission to change overflow settings.");
                return true;
            }
            //Must be a player to grab player uuid
            if (!(sender instanceof Player player))
            {
                sender.sendMessage("§cThis command can only be executed by a player in-game.");
                return true;
            }

            //If either the stashes or overflow are not enabled then just tell them we don't change anything
            if(!configLoader.Enable_Mystical_Stash|| !configLoader.Enable_Stash_Overflow)
            {
                sender.sendMessage("§cStashes or stash overflow features are currently disabled.");
                return true;
            }

            //Check if the player provided an argument (on/off)
            if (args.length > 0)
            {
                if (args[0].equalsIgnoreCase("on"))
                {
                    stashManager.UpdateOverFlowSetting(player, true);
                    player.sendMessage("§aStash overflow has been enabled.");
                    return true;
                }
                else if (args[0].equalsIgnoreCase("off"))
                {
                    stashManager.UpdateOverFlowSetting(player, false);
                    player.sendMessage("§cStash overflow has been disabled.");
                    return true;
                }
            }

            //Msg the player no valid argument was provided
            player.sendMessage("§eUsage: /vp-overflow <on|off>");
            return true;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (command.getName().equalsIgnoreCase("vp-reload"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.reload"))
            {
                sender.sendMessage("§cYou do not have permission to reload this plugin.");
                return true;
            }

            //Clear the stash Cache
            stashManager.ClearStashCache();

            //init the plugin
            PluginInit();

            sender.sendMessage("§aVanillaPlus configuration reloaded!");
            return true;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (command.getName().equalsIgnoreCase("vp-setspawn"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.setspawn"))
            {
                sender.sendMessage("§cYou do not have permission to change the world spawn.");
                return true;
            }

            //Must be a player to grab coordinates
            if (!(sender instanceof Player player))
            {
                sender.sendMessage("§cThis command can only be executed by a player in-game.");
                return true;
            }

            Location loc = player.getLocation();
            World world = loc.getWorld();

            if (world != null)
            {
                //Set the native world spawn location
                world.setSpawnLocation(loc);
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
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (command.getName().equalsIgnoreCase("vp-spawn"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.spawn"))
            {
                sender.sendMessage("§cYou do not have permission to teleport to the world spawn.");
                return true;
            }

            //Must be a player to grab coordinates
            if (!(sender instanceof Player player))
            {
                sender.sendMessage("§cThis command can only be executed by a player in-game.");
                return true;
            }

            //Get the overworld and its spawn loc
            World world = Bukkit.getWorlds().getFirst();
            Location SpawnLocation = world.getSpawnLocation();
            //Teleport player to the spawn
            player.teleport(SpawnLocation);

            return true;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (command.getName().equalsIgnoreCase("vp-inspect-stash"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.inspect_stash"))
            {
                sender.sendMessage("§cYou do not have permission to inspect stashes.");
                return true;
            }

            //Make sure the console isn't trying to open a visual screen inventory
            if (!(sender instanceof Player admin))
            {
                sender.sendMessage("§cThis command can only be executed by a player in-game.");
                return true;
            }

            //Make sure they provided a player name argument
            if (args.length < 1)
            {
                sender.sendMessage("§cUsage: /vp-inspect-stash <player>");
                return true;
            }

            String targetName = args[0];
            Player TargetOnline = Bukkit.getPlayer(targetName);

            //if TargetOnline is not null open the online player stash fastest
            if(TargetOnline != null)
            {
                stashManager.openMysticalStash(TargetOnline,admin);
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
                stashManager.openMysticalStash(TargetOffline,admin);
                admin.sendMessage("§aOpening offline virtual stash for §e" + TargetOffline.getName() + "§a.");
            }
            else
            {
                admin.sendMessage("§cPlayer '" + targetName + "' has never joined this server.");
            }
            return true;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (command.getName().equalsIgnoreCase("vp-stash"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.stash"))
            {
                sender.sendMessage("§cYou do not have permission to inspect stashes.");
                return true;
            }

            //Make sure the console isn't trying to open a visual screen inventory
            if (!(sender instanceof Player player))
            {
                sender.sendMessage("§cThis command can only be executed by a player in-game.");
                return true;
            }

            //Get the player and open their stash
            stashManager.openMysticalStash(player,null);
            return true;
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        if (command.getName().equalsIgnoreCase("vp-hud"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.hud"))
            {
                sender.sendMessage("§cYou do not have permission to change your hud.");
                return true;
            }

            if (!(sender instanceof Player player))
            {
                sender.sendMessage("§cOnly players can change their HUD settings!");
                return true;
            }

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
        if (command.getName().equalsIgnoreCase("vp-world-age"))
        {
            //Enforce permission check
            if (!sender.hasPermission("VanillaPlus.world_age"))
            {
                sender.sendMessage("§cYou do not have permission to see world age.");
                return true;
            }

            //Create the string builder
            StringBuilder playInfo = new StringBuilder();

            //Get the ticks of the world
            long ticks =  Bukkit.getWorlds().getFirst().getGameTime();
            //Convert to hours
            double totalHours = ticks / 72000.0;
            String formattedTime;
            //Format in days if more than 24 hours
            if(totalHours >= 24)
            {
                double totalDays = totalHours / 24;
                formattedTime = String.format(Locale.US, "%.1f days", totalDays);
            }//just format it as hours
            else
            {
                formattedTime = String.format(Locale.US, "%.1f hours", totalHours);
            }
            //Set the hours the world has been active for
            playInfo.append(" §e[World age: ").append(formattedTime).append("]");


            //Check if the command is sent by a player if so build the string with the players played time included
            if ((sender instanceof Player player))
            {
                int playerTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
                double playedHours = playerTicks / 72000.0;

                String formattedPlayedTime;
                if(playedHours >= 24)
                {
                    double PlayedDays = playedHours / 24.0;
                    formattedPlayedTime = String.format(Locale.US ,"%.1f days", PlayedDays);
                }
                else
                {
                    formattedPlayedTime = String.format(Locale.US ,"%.1f hours", playedHours);
                }

                playInfo.append(" [Played time: ").append(formattedPlayedTime).append("]");
            }

            sender.sendMessage(playInfo.toString());
            return true;
        }
        return false;
    }
    /////////////////////////////////////////////ScoreBoard Stuff/////////////////////////////////////////////////////
    public void Update_ScoreBoard(Player player)
    {
        boolean AFK = afkPlayers.contains(player.getUniqueId());

        var PDC = player.getPersistentDataContainer();

        //Check the settings for the player
        byte ShowDeaths = java.util.Objects.requireNonNullElse(PDC.get(ShowDeathsKey, PersistentDataType.BYTE), (byte) 2);
        byte ShowSleep  = java.util.Objects.requireNonNullElse(PDC.get(ShowSleepKey, PersistentDataType.BYTE), (byte) 2);
        byte ShowPing  = java.util.Objects.requireNonNullElse(PDC.get(ShowPingKey, PersistentDataType.BYTE), (byte) 2);

        //Get the time since last sleep (24000 ticks = 1 day)
        int ticksSinceRest = player.getStatistic(Statistic.TIME_SINCE_REST);
        int daysAwake = ticksSinceRest / 24000;

        //Create StringBuilder
        StringBuilder Status = new StringBuilder();

        if(AFK)
        {
            Status.append(" §7").append(player.getName());
        }
        else
        {
            Status.append(" ").append(player.getName());
        }

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
                Status.append(" §7[§e").append(daysAwake).append("d Awake§7]");
            }
            else if(ShowSleep == 2)
            {
                //Set the days awake
                Status.append(" §7[§e").append("🛌 ").append(daysAwake).append("d§7]");
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

        //Apply the stats
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

        //Wait 20 ticks so the server has time to increment the death stat
        Bukkit.getScheduler().runTaskLater( this, () ->
        {
            Update_ScoreBoard(Victim_Player);
        },20L); //20L is 1sec
    }
    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event)
    {
        //Clear player from the location list and afk list
        oldPlayerLocations.remove(event.getPlayer().getUniqueId());
        afkPlayers.remove(event.getPlayer().getUniqueId());
    }
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

                        String Fresh_Ban_MSG = "§aFresh BAN Event Triggered for player by name: §e" + Banned_Player_Name;
                        Bukkit.getConsoleSender().sendMessage(Fresh_Ban_MSG);

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

                        String Already_Banned_MSG = "§aAn Already Banned Player by name of: §e" + Banned_Player_Name + " §aTried To Join";
                        Bukkit.getConsoleSender().sendMessage(Already_Banned_MSG);

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

                Firework fw = world.spawn(loc, Firework.class);
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
            this.creepyTask = Bukkit.getScheduler().runTaskTimer(this, () ->
            {
                TriggerCreepyEvents();
            },200L, 12000L);
        }
    }
    @EventHandler
    public void onPlayerBed(PlayerBedEnterEvent event)
    {
        //Check if this feature is enabled first.
        if (this.configLoader.Enable_Creepy_Stuff)
        {
            //Return the lucky (or unlucky) winner.
            Player bedPlayer = event.getPlayer();
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
                            bedPlayer.playSound(bedPlayer.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 2.0F, 0.5F);
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
                Victim.playSound(Loc_Behind_Player, Sound.BLOCK_DEEPSLATE_STEP, 2.0F, 0.8F);
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

    /////////////////////////////////Chunk Loader////////////////////////////////////////
    private final Map<UUID,List<Chunk>> playerLoadedChunks = new HashMap<>();
    public void ChunkLoader(Player player)
    {
        if(!configLoader.Enable_Offline_Chunk_Loading)
        {
            player.sendMessage("§aOffline Chunk loading has been disabled.");
        }
        //Check if player isn't already loading Offline loading Chunks.
        if(playerLoadedChunks.containsKey(player.getUniqueId()))
        {
            //Get the loaded chunks
            List<Chunk> loadedChunks = playerLoadedChunks.remove(player.getUniqueId());
            if(loadedChunks != null)
            {
                //Unload all the loaded chunks
                for(Chunk chunk : loadedChunks)
                {
                    //Clear the chunks for the ticket system so the server won't keep them active no more.
                    chunk.removePluginChunkTicket(this);
                }
            }
            player.sendMessage("§aAREA INACTIVE (Chunks released)");
            return;
        }

        //Get the player loc and world
        Location loc = player.getLocation();
        World world = loc.getWorld();

        //Get the center chunk so we can load around that one
        int centerChunkX = loc.getBlockX() >> 4;
        int centerChunkZ = loc.getBlockZ() >> 4;
        //define the radius we want to load around the center chunk
        int radius = 8;
        List<Chunk> loadedChunks = new ArrayList<>();
        //Load each chunk within the defined radius
        for(int x = -radius; x <= radius; x++)
        {
            for(int z = -radius; z <= radius; z++)
            {
                Chunk chunk = world.getChunkAt(centerChunkX + x, centerChunkZ + z);

                boolean success = chunk.addPluginChunkTicket(this);
                if(success)
                {
                    //When the chunk was loaded successfully we add it to the list so we can keep track
                    loadedChunks.add(chunk);
                }
            }
        }
        //Save the loaded chunks for this player so we can keep track of what this player has loaded
        playerLoadedChunks.put(player.getUniqueId(), loadedChunks);
        player.sendMessage("§aAREA ACTIVE");
    }
}