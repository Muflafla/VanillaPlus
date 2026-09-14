package BK.VanillaPlus;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ConfigUpdater
{
    private final VanillaPlus plugin;

    public ConfigUpdater(VanillaPlus plugin)
    {
        this.plugin = plugin;
    }

    public void update_Config()
    {
        File CurrentConfigFile = new File(plugin.getDataFolder(), "config.yml");

        //If it doesn't exist, just save the default and leave
        if (!CurrentConfigFile.exists())
        {
            plugin.saveDefaultConfig();
            return;
        }

        //Get the version string directly from your plugin.yml
        String targetVersion = plugin.getDescription().getVersion();
        //Get the currentConfig
        FileConfiguration currentConfig = YamlConfiguration.loadConfiguration(CurrentConfigFile);
        //Get the version of the current config
        String currentVersion = currentConfig.getString("Config-Version", "1.0.0");

        //Only update if the version differs
        if (!currentVersion.equals(targetVersion))
        {
            Bukkit.getConsoleSender().sendMessage("§aUpdating outdated config.yml to version §e" + targetVersion + "§a...");

            //1. Load the fresh template from the JAR into memory (don't save it yet)
            InputStream defConfigStream = plugin.getResource("config.yml");
            if (defConfigStream == null)return;

            //Create the new config but don't save it yet keep it in memory
            YamlConfiguration NewConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));

            //2. Transfer the user's existing settings from currentConfig to NewConfig
            for (String key : currentConfig.getKeys(true))
            {
                if (!key.equalsIgnoreCase("Config-Version"))
                {
                    // nly carry over if it's a valid key or a custom section
                    if (NewConfig.contains(key) ||
                            key.startsWith("Custom-Player-Messages.") ||
                            key.startsWith("Custom-Recipes."))
                    {
                        NewConfig.set(key, currentConfig.get(key));
                    }
                }
            }

            //3. Update the version number in the new config
            NewConfig.set("Config-Version", targetVersion);

            //5. try atomic save
            if(SaveAtomic(CurrentConfigFile, NewConfig))
            {
                Bukkit.getConsoleSender().sendMessage("§aConfig successfully migrated!");
            }

        }
    }

    public void update_MiniBlocks()
    {
        File CurrentBlockFile = new File(plugin.getDataFolder(), "mini-blocks.yml");
        if (!CurrentBlockFile.exists())
        {
            plugin.saveResource("mini-blocks.yml", false);
            return;
        }

        //Get the version string directly from your plugin.yml
        String targetVersion = plugin.getDescription().getVersion();
        //Get the current mini-blocks file
        FileConfiguration currentBlocksFile = YamlConfiguration.loadConfiguration(CurrentBlockFile);
        //Get the version of the current Blocks File
        String currentVersion = currentBlocksFile.getString("Mini-Blocks-Version", "1.0.0");

        //If versions don't match, simply overwrite with the new template
        if (!currentVersion.equals(targetVersion))
        {
            Bukkit.getConsoleSender().sendMessage("§aUpdating system mini-blocks.yml to version §e" + targetVersion + "§a...");
            //This overwrites the old system file with the new one from the JAR
            plugin.saveResource("mini-blocks.yml", true);

            //Check if the configLoader is already loaded
            if(plugin.configLoader != null)
            {
                //reload the MiniBlocksFile into memory fresh
                plugin.configLoader.MiniBlocksFile = YamlConfiguration.loadConfiguration(CurrentBlockFile);
            }

            Bukkit.getConsoleSender().sendMessage("§aMini-blocks successfully updated to §e" + targetVersion);
        }
    }

    private Boolean SaveAtomic(File Destination, YamlConfiguration NewConfiguration)
    {
        File TempFile = new File(plugin.getDataFolder(), Destination.getName() + ".tmp");

        try
        {
            NewConfiguration.save(TempFile);
            Files.move(
                    TempFile.toPath(),
                    Destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
            return true;

        }
        catch (IOException e)
        {
            plugin.getLogger().severe("CRITICAL: Failed to save " + Destination.getName() + " to disk!");
            e.printStackTrace();
            return false;
        }
    }
}