package BK.vanilaPlus;

public class ConfigLoader
{
    private final VanillaPlus plugin;
    //VanillaPlus main Vars
    public boolean Enable_Login_Fireworks;
    public boolean Show_Login_Messages;
    public boolean Show_Player_Specific_Login_Messages;
    public boolean Ban_On_Join;
    public String Ban_Reason;
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
    public boolean Enable_Custom_Recipes;

    //Mystical Stash Vars
    public boolean Enable_Mystical_Stash;
    public boolean Enable_Mystical_Stash_Chests;

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

    //Horror and fun
    public boolean Enable_Creepy_Stuff;

    public ConfigLoader(VanillaPlus plugin)
    {
        this.plugin = plugin;
        LoadAllVars();
    }

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

        //================== load all the Mystical Stash Vars =============================
        //if true enable mystical stash
        this.Enable_Mystical_Stash = plugin.getConfig().getBoolean("Enable-Mystical-Stash",true);
        //if true allow chests named Mystical Stash to open the Mystical Stash
        this.Enable_Mystical_Stash_Chests = plugin.getConfig().getBoolean("Enable-Mystical-Stash-Chests",true);

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

        //======================== load all the Horror and Fun Vars =============================
        //if true enable creepy things
        this.Enable_Creepy_Stuff = plugin.getConfig().getBoolean("Enable-Creepy-Stuff",false);
    }
}
