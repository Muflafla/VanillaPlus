package BK.VanillaPlus;

import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class WarpManager
{
    private final VanillaPlus plugin;

    public WarpManager(VanillaPlus plugin) {this.plugin = plugin;}

    private void saveWarpAsync(Player player, String warpName, boolean delete)
    {
        //Get the edited files from ram
        File warpsFile = plugin.configLoader.warpFile;
        FileConfiguration warpsConfig = plugin.configLoader.warpsConfig;

        //Convert to YAML string on main thread.
        String serializedConfig = warpsConfig.saveToString();
        //Create the temp file reference
        File TempFile = new File(plugin.getDataFolder(), warpsFile.getName() + ".tmp");

        //Create an async task and run the saving async
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () ->
        {
            try
            {
                //Save the temp file
                Files.write(TempFile.toPath(), serializedConfig.getBytes());

                //Perform atomic swap
                Files.move(
                        TempFile.toPath(),
                        warpsFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );

                //Jump back to main thread to send msg
                Bukkit.getScheduler().runTask(plugin, () ->
                {
                    if(!delete)
                    {
                        player.sendMessage("§e" + warpName + " §aHas been saved to your warps!");
                    }
                    else
                    {
                        player.sendMessage("§e" + warpName + " §aHas been removed from your warps!");
                    }
                });
            }
            catch (IOException e)
            {
                plugin.getLogger().severe("FAILED TO SAVE WARPS.YML!");
                e.printStackTrace();

                //Jump back to main thread to send msg
                Bukkit.getScheduler().runTask(plugin, () ->
                {
                    if(!delete)
                    {
                        player.sendMessage( "§aFailed to save " +"§e"+ warpName + " §ato your warps!");
                    }
                    else
                    {
                        player.sendMessage( "§aFailed to remove " +"§e"+ warpName + " §afrom your warps!");
                    }
                });
            }
        });
    }

    public void CreateWarp(Player player, String warpName, Location loc)
    {
        if (!plugin.configLoader.EnableWarps)
        {
            player.sendMessage("§eWarps are disabled!");
            return;
        }

        //Make it all lowercase to not be case-sensitive
        warpName = warpName.toLowerCase();

        ConfigurationSection warpsConfig = plugin.configLoader.warpsConfig;

        //Check if a warp with this name exists
        if(warpsConfig.get("warps." + warpName) != null)
        {
            player.sendMessage("§aA warp with this name: [" + "§e" +warpName + "§a] already exist!");
            return;
        }

        warpsConfig.set("warps." + warpName + ".created_by", player.getName());
        warpsConfig.set("warps." + warpName + ".location", loc);

        saveWarpAsync(player, warpName, false);
    }

    public void DeleteWarp(Player player, String warpName)
    {
        if (!plugin.configLoader.EnableWarps)
        {
            player.sendMessage("§eWarps are disabled!");
            return;
        }

        //Make it all lowercase to not be case-sensitive
        warpName = warpName.toLowerCase();

        ConfigurationSection warpsConfig = plugin.configLoader.warpsConfig;

        //Check if a warp with this name exists
        if(warpsConfig.get("warps." + warpName) == null)
        {
            player.sendMessage("§aFailed to delete warp, Could not find warp with this name: [" + "§e" +warpName + "§a]");
            return;
        }

        //Delete the warp from the config
        warpsConfig.set("warps." + warpName , null);

        saveWarpAsync(player, warpName, true);
    }

    public void Warp(Player player, String warpName)
    {
        if (!plugin.configLoader.EnableWarps)
        {
            player.sendMessage("§eWarps are disabled!");
            return;
        }

        //Make it all lowercase to not be case-sensitive
        warpName = warpName.toLowerCase();

        ConfigurationSection warpsConfig = plugin.configLoader.warpsConfig;
        //Check if a warp with this name exists
        if(warpsConfig.get("warps." + warpName) == null)
        {
            player.sendMessage("§aThis warp: [" + "§e" +warpName + "§a] does not exist!");
            return;
        }

        Location warpLoc = warpsConfig.getLocation("warps." + warpName + ".location");
        if(warpLoc != null)
        {
            player.playSound(warpLoc, Sound.ENTITY_PLAYER_TELEPORT, 1f, 1f);
            player.teleport(warpLoc);
        }
        else
        {
            player.sendMessage("§aFailed to teleport to warp " +"§e"+ warpName + "§a" + "!");
            plugin.getLogger().warning("Warp by name of: " + warpName + " Has corrupt location data, A player failed to warp");
        }
    }
    
    public void warpList(CommandSender sender)
    {
        if (!plugin.configLoader.EnableWarps)
        {
            sender.sendMessage("§e Warps are disabled!");
            return;
        }

        //Get the warp config and get the warp section of it
        ConfigurationSection warpsConfig = plugin.configLoader.warpsConfig;
        ConfigurationSection warpsSection = warpsConfig.getConfigurationSection("warps");
        //Check warp section exists which should always exist this just exist because I have enough IDE warnings already.......
        if(warpsSection == null)
        {
            //Check if the console send the msg
            sender.sendMessage("§cThere are currently no warps in your warps!");
            return;
        }
        //Get a list of warps
        List<String> warpNames = new ArrayList<>(warpsSection.getKeys(false));

        sender.sendMessage("§e" + warpNames);
    }
}
