package BK.VanillaPlus;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import org.bukkit.*;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class BeaconManager implements Listener
{
    private final VanillaPlus plugin;
    private File beaconFile;
    private FileConfiguration beaconConfig;
    private static final Set<Material> BEAM_PASSABLE;

    BukkitTask BeaconTask;
    BukkitTask PickAxeTask;

    public BeaconManager(VanillaPlus plugin)
    {
        this.plugin = plugin;
    }

    public void setupConfig()
    {
        //Check if beacon file exists if not create it
        this.beaconFile = new File(this.plugin.getDataFolder(), "beacons.yml");
        if (!this.beaconFile.exists())
        {
            try
            {
                this.beaconFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        this.beaconConfig = YamlConfiguration.loadConfiguration(this.beaconFile);
        this.startBeaconEffectTask();
    }

    public void startBeaconEffectTask()
    {
        //Check if BeaconTask is already running before running it
        if(BeaconTask != null)
        {
            BeaconTask.cancel();
        }
        //Run this every 5 sec to check if the player needs an effect
        BeaconTask = Bukkit.getScheduler().runTaskTimer(this.plugin, () ->
        {
            //Check if the player has beacon changes enabled
            if (!plugin.configLoader.Enable_Beacon_Changes)
            {
                BeaconTask.cancel();
                return;
            }
            //Call updateBeaconStatuses so the beacon status stays upto date
            this.updateBeaconStatuses();
            //For each player online call the checkAndApplyBeaconEffect function to check if we need to apply an effect
            for (Player player : Bukkit.getOnlinePlayers())
            {
                this.checkAndApplyBeaconEffect(player, (double) plugin.configLoader.BeaconRangeModifier);
            }

        }, 0L, 100L);//100L is 5 sec
    }

    public void saveBeaconAsync()
    {
        //Create an async task and run the saving async
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () ->
        {
            try
            {
                beaconConfig.save(beaconFile);
            }
            catch (IOException e)
            {
                plugin.getLogger().severe("FAILED TO SAVE BEACONS.YML!");
                e.printStackTrace();
            }
        });
    }

    @EventHandler
    public void onBeaconBreak(BlockBreakEvent event)
    {
        //Check if beacon changes are enabled
        if (!plugin.configLoader.Enable_Beacon_Changes)return;

        //Check if the broken block is a beacon
        if (event.getBlock().getType() == Material.BEACON)
        {
            //Get the beacon location and world its in and create the key with those values
            Location loc = event.getBlock().getLocation();
            String worldName = loc.getWorld().getName();
            String key = worldName + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

            //Check if the beacon file exists if so search in the file for the key and remove the beacon from the file and call save
            if (this.beaconFile != null && this.beaconFile.exists() && this.beaconConfig != null)
            {
                if (this.beaconConfig.contains("beacons." + key))
                {
                    this.beaconConfig.set("beacons." + key, null);
                    saveBeaconAsync();
                }
            }
        }
    }

    @EventHandler
    public void onBeaconClose(InventoryCloseEvent event)
    {
        //Check if beacon changes are enabled
        if (!plugin.configLoader.Enable_Beacon_Changes)return;

        //Get the inv we closed and check if it was a beacon inv
        Inventory inv = event.getInventory();
        if (inv.getType() == InventoryType.BEACON)
        {
            //Get the beacon location form the inv and check if it's not null
            Location beaconLoc = inv.getLocation();
            if (beaconLoc != null)
            {
                //get the beacon and double check it's a beacon
                Block block = beaconLoc.getBlock();
                if (block.getType() == Material.BEACON)
                {
                    //get the beacon info to make the key for saving and call save function
                    Beacon beacon = (Beacon) block.getState();
                    String worldName = beaconLoc.getWorld().getName();
                    String key = worldName + "," + beaconLoc.getBlockX() + "," + beaconLoc.getBlockY() + "," + beaconLoc.getBlockZ();

                    PotionEffect primary = beacon.getPrimaryEffect();
                    PotionEffect secondary = beacon.getSecondaryEffect();

                    beaconConfig.set("beacons." + key + ".primary", primary != null ? primary.getType().getName() : null);
                    beaconConfig.set("beacons." + key + ".secondary", secondary != null ? secondary.getType().getName() : null);
                    beaconConfig.set("beacons." + key + ".amplifierPrimary", primary != null ? primary.getAmplifier() : -1);
                    beaconConfig.set("beacons." + key + ".amplifierSecondary", secondary != null ? secondary.getAmplifier() : -1);

                    saveBeaconAsync();
                }
            }
        }
    }

    private boolean isBeaconUnobstructed(Location beaconLoc)
    {
        //Get the world the beacon is in and check it's max height
        World world = beaconLoc.getWorld();
        int maxY = world.getMaxHeight();
        //Check from the top of the block to the max world height
        for (int y = beaconLoc.getBlockY() + 1; y < maxY; ++y)
        {
            //get the block and check if the BEAM_PASSABLE enum contains the block if not the beam isn't passable
            Material above = world.getBlockAt(beaconLoc.getBlockX(), y, beaconLoc.getBlockZ()).getType();
            if (!BEAM_PASSABLE.contains(above))
            {
                return false;
            }
        }
        //Return true if the beam is passable
        return true;
    }

    public void updateBeaconStatuses()
    {
        //Get the beacons section from the beaconConfig file and check it's not null
        ConfigurationSection section = this.beaconConfig.getConfigurationSection("beacons");
        if (section != null)
        {
            //For each key(Beacon) found run this update
            for (String key : section.getKeys(false))
            {
                //Get the world and location from the key name
                String[] parts = key.split(",");
                World world = Bukkit.getWorld(parts[0]);
                //Check if world is not null
                if (world != null)
                {
                    //Recreate the world and location from the key
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    Location beaconLoc = new Location(world, x, y, z);
                    //Get that beacon block and double check it's indeed a beacon
                    Block block = beaconLoc.getBlock();
                    if (block.getType() == Material.BEACON)
                    {
                        //Check if the beacon is active and update this in the file
                        Beacon beacon = (Beacon) block.getState();
                        int beaconTier = beacon.getTier();
                        if(beaconTier != 0)
                        {
                            boolean hasEffects = beacon.getPrimaryEffect() != null || beacon.getSecondaryEffect() != null;
                            boolean isUnobstructed = isBeaconUnobstructed(beaconLoc);
                            boolean isActive = hasEffects && isUnobstructed;

                            this.beaconConfig.set("beacons." + key + ".active", isActive);
                        }
                        else
                        {
                            this.beaconConfig.set("beacons." + key + ".active", false);
                        }

                    }
                }
            }
        }
    }

    public void checkAndApplyBeaconEffect(Player player, double range)
    {
        //Get the beaconConfig file and get the beacons section and check if it's not null
        ConfigurationSection section = this.beaconConfig.getConfigurationSection("beacons");
        if (section != null)
        {
            //For each key(Beacon) found run this code
            for (String key : section.getKeys(false))
            {
                //Get the world and Location from the key
                String[] parts = key.split(",");
                World beaconWorld = Bukkit.getWorld(parts[0]);
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                Location beaconLoc = new Location(beaconWorld, x, y, z);
                //Check if the player is in the same world as this beacon if not skip this beacon
                if(player.getWorld() != beaconWorld) continue;

                //Double check we have a beacon block
                if (beaconLoc.getBlock().getType() == Material.BEACON)
                {
                    //Check if the beacon is active if not skip this beacon
                    if(!this.beaconConfig.getBoolean("beacons." + key + ".active", false)) continue;

                    //Get the beacon stats like location and tier
                    Beacon beacon = (Beacon) beaconLoc.getBlock().getState();
                    int BeaconTier = beacon.getTier();
                    double CalculatedRange = range * (BeaconTier + 1);
                    //Apply new range to beacon
                    double squaredRange = CalculatedRange * CalculatedRange;
                    //Check if player is within range if so give the effects of this beacon to the player
                    if (beaconLoc.distanceSquared(player.getLocation()) <= squaredRange)
                    {
                        //Apply primary effect
                        PotionEffect primary = beacon.getPrimaryEffect();
                        if (primary != null)
                        {
                            PotionEffectType type = primary.getType();
                            int amplifier = primary.getAmplifier();
                            PotionEffect current = player.getPotionEffect(type);

                            if (current == null || current.getDuration() < 100)
                            {
                                player.addPotionEffect(new PotionEffect(type, 120, amplifier, true, false,true));
                            }
                        }

                        //Apply secondary effect
                        PotionEffect secondary = beacon.getSecondaryEffect();
                        if (secondary != null)
                        {
                            PotionEffectType type = secondary.getType();
                            int amplifier = secondary.getAmplifier();
                            PotionEffect current = player.getPotionEffect(type);

                            if (current == null || current.getDuration() < 100)
                            {
                                player.addPotionEffect(new PotionEffect(type, 120, amplifier, true, false,true));
                            }
                        }
                    }
                }
            }
        }
    }

    static
    {
        //Enum of all passable blocks
        BEAM_PASSABLE = EnumSet.of(
                Material.AIR, Material.GLASS, Material.TINTED_GLASS,
                Material.WHITE_STAINED_GLASS, Material.ORANGE_STAINED_GLASS,
                Material.MAGENTA_STAINED_GLASS, Material.LIGHT_BLUE_STAINED_GLASS,
                Material.YELLOW_STAINED_GLASS, Material.LIME_STAINED_GLASS,
                Material.PINK_STAINED_GLASS, Material.GRAY_STAINED_GLASS,
                Material.LIGHT_GRAY_STAINED_GLASS, Material.CYAN_STAINED_GLASS,
                Material.PURPLE_STAINED_GLASS, Material.BLUE_STAINED_GLASS,
                Material.BROWN_STAINED_GLASS, Material.GREEN_STAINED_GLASS,
                Material.RED_STAINED_GLASS, Material.BLACK_STAINED_GLASS
        );
    }

    public void startPickaxeEffect()
    {
        boolean isEnabled = plugin.configLoader.EnableHardenedPickAxe;
        boolean everUsed = plugin.configLoader.HardenedPickaxeEverUsed;
        //If its disabled and was never used skip running the loop
        if(!isEnabled && !everUsed)
        {
            return;
        }
        //If its enabled but hasn't been flagged yet then save it to config now
        if(isEnabled && !everUsed)
        {
            Bukkit.getConsoleSender().sendMessage("§aHardened Pickaxe was enabled for the first time.");
            plugin.getConfig().set("Hardened-Pickaxe-Ever-Used", true);
            plugin.saveConfig();

            //Update the memory variable as well
            plugin.configLoader.HardenedPickaxeEverUsed = true;
        }


        //Check if PickAxeTask is already running before running it
        if(PickAxeTask != null)
        {
            PickAxeTask.cancel();
        }
        //Run this every 5 sec to check if the player needs to have the pick updated
        PickAxeTask =  Bukkit.getScheduler().runTaskTimer(this.plugin, () ->
        {
            //For each player online run this
            for (Player player : Bukkit.getOnlinePlayers())
            {
                NamespacedKey pickKey = new NamespacedKey(plugin, "Hardened_PickAxe");

                ItemStack[] itemInHand1 = {
                        player.getInventory().getItemInMainHand(),
                        player.getInventory().getItemInOffHand()
                };

                for(ItemStack item : itemInHand1)
                {
                    //Check if holding the special pickaxe with item meta
                    if (item != null && item.getType() != Material.AIR && item.hasItemMeta())
                    {
                        if (item.getItemMeta().getPersistentDataContainer().has(pickKey, PersistentDataType.INTEGER))
                        {
                            int currentLevel = item.getEnchantmentLevel(Enchantment.EFFICIENCY);

                            if (currentLevel >= 5)
                            {
                                PotionEffect hasteEffect = player.getPotionEffect(PotionEffectType.HASTE);
                                //Check if the feature is enabled and check the haste effect if we have haste 2
                                boolean hasHaste2 = plugin.configLoader.EnableHardenedPickAxe && (hasteEffect != null && hasteEffect.getAmplifier() >= 1);

                                // Upgrade to 8 if Haste II is active and it's currently 5
                                if (hasHaste2 && currentLevel == 5)
                                {
                                    //Check if hardened pick is enabled first
                                    item.addUnsafeEnchantment(Enchantment.EFFICIENCY, 8);
                                }
                                // Demote back to 5 if Haste II is lost and it's currently 8
                                else if (!hasHaste2 && currentLevel == 8)
                                {
                                    item.addUnsafeEnchantment(Enchantment.EFFICIENCY, 5);
                                }
                            }
                        }
                    }
                }
            }
        }, 0L, 100L);//100L is 5 sec
    }
}
