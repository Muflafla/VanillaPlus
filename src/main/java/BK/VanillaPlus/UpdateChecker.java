package BK.VanillaPlus;

import org.bukkit.Bukkit;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateChecker
{
    private final VanillaPlus plugin;
    private final String projectSlug;

    public UpdateChecker(VanillaPlus plugin, String projectSlug)
    {
        this.plugin = plugin;
        this.projectSlug = projectSlug;
    }

    public void checkForUpdates()
    {
        // We run this asynchronously so it never stalls or lags the server startup sequence
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
        {
            try
            {
                // Requesting the single latest version from Paper's Hangar API
                URL url = new URL("https://hangar.papermc.io/api/v1/projects/" + projectSlug + "/versions?limit=1");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "VanillaPlus-UpdateChecker");

                if (connection.getResponseCode() == 200)
                {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonArray result = response.getAsJsonArray("result");

                    if (result != null && !result.isEmpty())
                    {
                        String latestVersion = result.get(0).getAsJsonObject().get("name").getAsString();
                        String currentVersion = plugin.getDescription().getVersion();

                        // If the version string online doesn't match what is currently running
                        if (!currentVersion.equalsIgnoreCase(latestVersion))
                        {
                            Bukkit.getConsoleSender().sendMessage("§a==================================================");
                            Bukkit.getConsoleSender().sendMessage("§aA new update for VanillaPlus is available!");
                            Bukkit.getConsoleSender().sendMessage("§aCurrent Version: §e" + currentVersion + " §a| Latest: §e" + latestVersion);
                            Bukkit.getConsoleSender().sendMessage("§aDownload it here: §ehttps://hangar.papermc.io/Muflafla/" + projectSlug);
                            Bukkit.getConsoleSender().sendMessage("§a==================================================");
                        }
                        else
                        {
                            Bukkit.getConsoleSender().sendMessage("§aVanillaPlus is up to date (v§e" + currentVersion + "§a).");
                        }
                    }
                }
                connection.disconnect();
            }
            catch (Exception e)
            {
                plugin.getLogger().warning("Unable to check for updates: " + e.getMessage());
            }
        });
    }
}