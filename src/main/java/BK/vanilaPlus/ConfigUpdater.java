package BK.vanilaPlus;

import org.bukkit.configuration.file.FileConfiguration;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigUpdater
{
    private final VanillaPlus plugin;

    public ConfigUpdater(VanillaPlus plugin)
    {
        this.plugin = plugin;
    }

    public void update()
    {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        // If the file doesn't exist, let Bukkit generate the fresh template
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
            return;
        }

        //Get the version string directly from your plugin.yml
        String targetVersion = plugin.getDescription().getVersion();

        FileConfiguration currentConfig = plugin.getConfig();

        //Get the config version
        String currentVersion = currentConfig.getString("Config-Version", "1.0.0");

        //If the config file's version doesn't match the running plugin version
        if (!currentVersion.equals(targetVersion))
        {
            plugin.getLogger().warning("Updating outdated config.yml to version " + targetVersion + "...");

            // 1. Store all existing user settings in memory
            Map<String, Object> oldValues = new HashMap<>();
            for (String key : currentConfig.getKeys(true)) {
                // Ignore the version key so we don't copy the old version over the new one
                if (!key.equalsIgnoreCase("Config-Version")) {
                    oldValues.put(key, currentConfig.get(key));
                }
            }

            // 2. Wipe the old file out
            configFile.delete();

            // 3. Save the brand-new template from the resources jar
            plugin.saveDefaultConfig();
            plugin.reloadConfig();

            // 4. Repopulate the fresh layout with the user's old values
            FileConfiguration newConfig = plugin.getConfig();
            for (Map.Entry<String, Object> entry : oldValues.entrySet()) {
                String key = entry.getKey();

                // Keep the setting if it exists in the new template, OR if it's a user-created custom section
                if (newConfig.contains(key) ||
                        key.startsWith("Custom-Player-Messages.") ||
                        key.startsWith("Custom-Recipes."))
                {
                    newConfig.set(key, entry.getValue());
                }
            }

            // 5. Save the updated file back to disk
            plugin.saveConfig();
            plugin.getLogger().info("Configuration successfully migrated to version " + targetVersion + "!");
        }
    }
}