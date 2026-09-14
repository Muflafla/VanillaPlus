package BK.VanillaPlus;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigLoader
{
    private final VanillaPlus plugin;
    //VanillaPlus main Vars
    public boolean Enable_Login_Fireworks;
    public boolean Show_Login_Messages;
    public boolean Show_Player_Specific_Login_Messages;
    public boolean Ban_On_Join;
    public String Ban_Reason;

    //Protection Vars
    public boolean Disable_Phantom_Spawn;
    public boolean Disable_Ghast_Explosions;
    public boolean Disable_Ghast_Block_Damage;
    public boolean Ghast_Gives_all_Blocks;
    public boolean Disable_Creeper_Spawn;
    public boolean Disable_Creeper_Explosions;
    public boolean Disable_Creeper_Block_Damage;
    public boolean Creeper_Gives_all_Blocks;
    public boolean Stop_Enderman_From_Griefing;
    public boolean Show_Enderman_Messages;

    //Mystical Stash Vars
    public boolean Enable_Mystical_Stash;
    public boolean Enable_Mystical_Stash_Book;
    public String Stash_Name;
    public boolean Enable_Mystical_Stash_Chests;
    public int Stash_Rows;
    public boolean Enable_Drop_Stash_On_Death;
    public boolean Enable_Stash_Overflow;

    //BeaconManager Vars
    public boolean Enable_Beacon_Changes;
    public double BeaconRangeModifier;

    //SleepManager Vars
    public boolean EnableCustomSleep;
    public boolean AllPlayersRest;
    public int PlayerWakeTime;
    public String Sleep_Mode;
    public int Requirements;
    public boolean ActionBarMSG;
    public String Night_Skipped_MSG;
    public boolean ClearWeatherOnSleep;

    //Mini Blocks & Custom Recipes & Heads
    public boolean EnableMiniBlocks;
    public boolean Enable_Players_Drop_Head;
    public ConfigurationSection MiniBlocksFile;
    public ConfigurationSection CustomMiniBlocksFile;
    public boolean Enable_Custom_Recipes;
    public ConfigurationSection Custom_Recipes;

    //Tool Protection
    public boolean EnableToolProtection;
    public int ToolProtectionPercentage;

    //HardenedPick
    public boolean EnableHardenedPickAxe;
    public boolean HardenedPickaxeEverUsed;

    //Villager
    public boolean Enable_Global_Villager_Pricing;

    //Shulker options
    public boolean EnablePickBlockFromShulker;

    //Warps
    public boolean EnableWarps;
    public File warpFile;
    public FileConfiguration warpsConfig;

    //Offline Chunk Loading
    public boolean Enable_Offline_Chunk_Loading;

    //Horror and fun
    public boolean Enable_Creepy_Stuff;

    public ConfigLoader(VanillaPlus plugin) {this.plugin = plugin;}

    public void LoadAllVars()
    {
        //Grab the config fresh from disk
        plugin.reloadConfig();

        //====================== Load all the VanillaPlus Main Vars ==============================
        //Check if Fireworks are enabled.
        this.Enable_Login_Fireworks = plugin.getConfig().getBoolean("Enable-Login-Fireworks", true);
        //Check if login messages are enabled.
        this.Show_Login_Messages = plugin.getConfig().getBoolean("Show-Login-Messages", true);
        //Check if specific login messages are enabled.
        this.Show_Player_Specific_Login_Messages = plugin.getConfig().getBoolean("Show-Player-Specific-Login-Messages", true);
        //Check if ban on join is enabled
        this.Ban_On_Join = plugin.getConfig().getBoolean("Ban-On-Join", true);
        //Get the ban reason
        this.Ban_Reason = plugin.getConfig().getString("Ban-Reason", "You where not whitelisted");
        //Check if Phantom spawn is disabled.
        this.Disable_Phantom_Spawn = plugin.getConfig().getBoolean("Disable-Phantom-Spawn",false);
        //Check if Ghast explosions are disabled.
        this.Disable_Ghast_Explosions = plugin.getConfig().getBoolean("Disable-Ghast-Explosions",false);
        //Check if Ghast block damage is disabled.
        this.Disable_Ghast_Block_Damage = plugin.getConfig().getBoolean("Disable-Ghast-Block-Damage",false);
        //Check if Ghast needs to give back all blocks it destroyed.
        this.Ghast_Gives_all_Blocks = plugin.getConfig().getBoolean("Ghast-Gives-all-Blocks",false);
        //Check if Creeper spawn is disabled.
        this.Disable_Creeper_Spawn = plugin.getConfig().getBoolean("Disable-Creeper-Spawn",false);
        //Check if Creeper explosions are disabled.
        this.Disable_Creeper_Explosions = plugin.getConfig().getBoolean("Disable-Creeper-Explosions",false);
        //Check if Creeper block damage is disabled.
        this.Disable_Creeper_Block_Damage = plugin.getConfig().getBoolean("Disable-Creeper-Block-Damage",false);
        //Check if Creeper needs to give back all blocks it destroyed.
        this.Creeper_Gives_all_Blocks = plugin.getConfig().getBoolean("Creeper-Gives-all-Blocks",false);
        //Check if enderman need to stop griefing.
        this.Stop_Enderman_From_Griefing = plugin.getConfig().getBoolean("Stop-Enderman-From-Griefing",true);
        //Check if enderman msg's are enabled
        this.Show_Enderman_Messages = plugin.getConfig().getBoolean("Show-Enderman-Messages",true);

        //================== load all Custom recipe Vars =============================
        this.Enable_Custom_Recipes = plugin.getConfig().getBoolean("Enable-Custom-Recipes",true);
        this.Custom_Recipes = plugin.getConfig().getConfigurationSection("Custom-Recipes");

        //================== load all the Mystical Stash Vars =============================
        //if true enable mystical stash
        this.Enable_Mystical_Stash = plugin.getConfig().getBoolean("Enable-Mystical-Stash",true);
        //if true enable mystical stash book
        this.Enable_Mystical_Stash_Book = plugin.getConfig().getBoolean("Enable-Mystical-Stash-Book",true);
        //this is the name given to stashes default = Mystical Stash
        this.Stash_Name = plugin.getConfig().getString("Stash-Name","§5Mystical Stash");
        //if true allow chests named Mystical Stash to open the Mystical Stash
        this.Enable_Mystical_Stash_Chests = plugin.getConfig().getBoolean("Enable-Mystical-Stash-Chests",true);
        //Set the amount of rows in stashes
        this.Stash_Rows = plugin.getConfig().getInt("Stash-Rows", 6);
        if(Stash_Rows < 1 ||  Stash_Rows > 6)
        {
            plugin.getLogger().warning("Invalid Stash-Rows in config! Defaulting to 6.");
            Stash_Rows = 6; //set default
        }
        //Make it Stash_Rows a multiple of 9
        Stash_Rows = Stash_Rows * 9;
        //If this is true players wil drop there stash upon death
        Enable_Drop_Stash_On_Death = plugin.getConfig().getBoolean("Enable-Drop-Stash-On-Death",false);
        //If this is enabled items on the ground wil go into your stash if the inv is full
        Enable_Stash_Overflow = plugin.getConfig().getBoolean("Enable-Stash-Overflow",true);




        //================== load all the BeaconManger Vars =============================
        //if true enable changed beacon radius
        this.Enable_Beacon_Changes = plugin.getConfig().getBoolean("Enable-Beacon-Changes", true);
        //set the new beacon range
        this.BeaconRangeModifier = plugin.getConfig().getDouble("Beacon-Range-Modifier", 30);

        //======================== load all the SleepManager Vars =============================
        this.EnableCustomSleep = plugin.getConfig().getBoolean("Enable-Custom-Sleep",true);
        this.AllPlayersRest = plugin.getConfig().getBoolean("All-Players-Rest",true);
        this.PlayerWakeTime = plugin.getConfig().getInt("Player-Wake-Time");
        this.Sleep_Mode = plugin.getConfig().getString("Sleep-Mode","FIXED");
        this.Requirements = plugin.getConfig().getInt("Sleep-Requirement", 1);
        this.ActionBarMSG = plugin.getConfig().getBoolean("ActionBar-Player-Message", true);
        this.Night_Skipped_MSG = plugin.getConfig().getString("Night-Skipped-Message");
        this.ClearWeatherOnSleep = plugin.getConfig().getBoolean("Clear-Weather-On-Sleep",true);

        //======================== load all Custom items/mini blocks and heads Vars =============================
        this.EnableMiniBlocks = plugin.getConfig().getBoolean("Enable-Mini-Blocks",true);
        //Check if mini-blocks.yml exists if not create it
        File mini_blocks_file = new File(plugin.getDataFolder(), "mini-blocks.yml");
        if(!mini_blocks_file.exists())
        {
            plugin.saveResource("mini-blocks.yml", false);
        }
        //Load mini-blocks.yml
        this.MiniBlocksFile = YamlConfiguration.loadConfiguration(mini_blocks_file);
        //Check if custom-mini-blocks.yml exists if not create it
        File custom_mini_blocks_file = new File(plugin.getDataFolder(), "custom-mini-blocks.yml");
        if(!custom_mini_blocks_file.exists())
        {
            plugin.saveResource("custom-mini-blocks.yml", false);
        }
        //Load custom-mini-blocks.yml
        this.CustomMiniBlocksFile = YamlConfiguration.loadConfiguration(custom_mini_blocks_file);
        this.Enable_Players_Drop_Head = plugin.getConfig().getBoolean("Enable-Players-Drop-Head",true);

        //======================== load all Custom items/mini blocks and heads Vars =============================
        this.EnablePickBlockFromShulker = plugin.getConfig().getBoolean("Enable-Pick-Block-From-Shulker",true);

        //======================== load all Tool Protection Vars =============================
        this.EnableToolProtection = plugin.getConfig().getBoolean("Enable-Tool-Protection",true);
        this.ToolProtectionPercentage = 5;

        //======================== Hardened PickAxe =============================
        this.EnableHardenedPickAxe = plugin.getConfig().getBoolean("Enable-Hardened-PickAxe",false);
        this.HardenedPickaxeEverUsed = plugin.getConfig().getBoolean("Hardened-Pickaxe-Ever-Used",false);

        //======================== Global villager trading =============================
        this.Enable_Global_Villager_Pricing = plugin.getConfig().getBoolean("Enable-Global-Villager-Pricing",false);

        //======================== load all the warp vars =============================
        //Check if warps are enabled
        this.EnableWarps = plugin.getConfig().getBoolean("Enable-Warps",false);
        //Check if the warps file exist if not create it
        warpFile = new File(plugin.getDataFolder(), "warps.yml");
        if (!warpFile.exists())
        {
            try
            {
                warpFile.createNewFile();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        this.warpsConfig = YamlConfiguration.loadConfiguration(warpFile);

        //======================== load all AFK Vars =============================
        //If this is true afk chunk loading will be enabled
        this.Enable_Offline_Chunk_Loading = plugin.getConfig().getBoolean("Enable-Offline-Chunk-Loading",false);

        //======================== load all the Horror and Fun Vars =============================
        //If true enable creepy things
        this.Enable_Creepy_Stuff = plugin.getConfig().getBoolean("Enable-Creepy-Stuff",false);
    }
}
