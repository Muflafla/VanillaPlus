package BK.vanilaPlus;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class BeaconManager implements Listener
{
    private final VanillaPlus plugin;
    private File beaconFile;
    private FileConfiguration beaconConfig;
    private static final Set<Material> BEAM_PASSABLE;

    public BeaconManager(VanillaPlus plugin)
    {
        this.plugin = plugin;
        this.setupConfig();
    }

    private void setupConfig()
    {
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
        Bukkit.getScheduler().runTaskTimer(this.plugin, () ->
        {
            this.updateBeaconStatuses();

            for (Player player : Bukkit.getOnlinePlayers())
            {
                this.checkAndApplyBeaconEffect(player, (double) plugin.Beacon_Radius);
            }

        }, 0L, 100L);
    }

    public void saveBeaconAsync()
    {
        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                try
                {
                    //do the actual save to file
                    beaconConfig.save(beaconFile);
                }
                catch (IOException e)
                {
                    plugin.getLogger().severe("FAILED TO SAVE BEACONS.YML!");
                    e.printStackTrace();
                }
            }
        }.runTaskAsynchronously(this.plugin);
    }

    @EventHandler
    public void onBeaconBreak(BlockBreakEvent event)
    {
        if (event.getBlock().getType() == Material.BEACON)
        {
            Location loc = event.getBlock().getLocation();
            String worldName = loc.getWorld().getName();
            String key = worldName + "," + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();

            if (this.beaconFile != null && this.beaconFile.exists() && this.beaconConfig != null)
            {
                if (this.beaconConfig.contains("beacons." + key))
                {
                    this.beaconConfig.set("beacons." + key, null);

                    //Save current beacon status to file
                    saveBeaconAsync();
                }
            }
        }
    }

    @EventHandler
    public void onBeaconClose(InventoryCloseEvent event)
    {
        Inventory inv = event.getInventory();
        if (inv.getType() == InventoryType.BEACON)
        {
            Location beaconLoc = inv.getLocation();
            if (beaconLoc != null)
            {
                Block block = beaconLoc.getBlock();
                if (block.getType() == Material.BEACON)
                {
                    Beacon beacon = (Beacon) block.getState();
                    PotionEffect effect = beacon.getPrimaryEffect();
                    if (effect != null)
                    {
                        String worldName = beaconLoc.getWorld().getName();
                        String key = worldName + "," + beaconLoc.getBlockX() + "," + beaconLoc.getBlockY() + "," + beaconLoc.getBlockZ();
                        this.beaconConfig.set("beacons." + key, true);
                        //Save current beacon status to file
                        saveBeaconAsync();
                    }
                }
            }
        }
    }

    private boolean isBeaconUnobstructed(Location beaconLoc)
    {
        World world = beaconLoc.getWorld();
        int maxY = world.getMaxHeight();

        for (int y = beaconLoc.getBlockY() + 1; y < maxY; ++y)
        {
            Material above = world.getBlockAt(beaconLoc.getBlockX(), y, beaconLoc.getBlockZ()).getType();
            if (!BEAM_PASSABLE.contains(above))
            {
                return false;
            }
        }

        return true;
    }

    public void updateBeaconStatuses()
    {
        ConfigurationSection section = this.beaconConfig.getConfigurationSection("beacons");
        if (section != null)
        {
            for (String key : section.getKeys(false))
            {
                String[] parts = key.split(",");
                World world = Bukkit.getWorld(parts[0]);
                if (world != null)
                {
                    int x = Integer.parseInt(parts[1]);
                    int y = Integer.parseInt(parts[2]);
                    int z = Integer.parseInt(parts[3]);
                    Location beaconLoc = new Location(world, x, y, z);

                    boolean active = beaconLoc.getBlock().getType() == Material.BEACON && this.isBeaconUnobstructed(beaconLoc);
                    this.beaconConfig.set("beacons." + key + ".active", active);
                }
            }
        }
    }

    public void checkAndApplyBeaconEffect(Player player, double range)
    {
        ConfigurationSection section = this.beaconConfig.getConfigurationSection("beacons");
        if (section != null)
        {
            for (String key : section.getKeys(false))
            {
                String[] parts = key.split(",");
                World world = Bukkit.getWorld(parts[0]);
                int x = Integer.parseInt(parts[1]);
                int y = Integer.parseInt(parts[2]);
                int z = Integer.parseInt(parts[3]);
                Location beaconLoc = new Location(world, x, y, z);

                double squaredRange = range * range;
                if(beaconLoc.distanceSquared(player.getLocation()) <= squaredRange && beaconLoc.getBlock().getType() == Material.BEACON)
                {
                    boolean isActive = this.beaconConfig.getBoolean("beacons." + key + ".active", false);
                    if (isActive)
                    {
                        Beacon beacon = (Beacon) beaconLoc.getBlock().getState();
                        PotionEffect effect = beacon.getPrimaryEffect();

                        if (effect != null)
                        {
                            PotionEffectType effectType = effect.getType();
                            PotionEffect current = player.getPotionEffect(effectType);

                            if (current == null || current.getDuration() < 100)
                            {
                                player.addPotionEffect(new PotionEffect(effectType, 120, 1, true, false));
                            }
                        }
                    }
                }
            }
        }
    }

    static
    {
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
}