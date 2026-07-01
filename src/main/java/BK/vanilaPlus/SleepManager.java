package BK.vanilaPlus;

import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;

public class SleepManager implements Listener
{
    private final VanillaPlus plugin;

    public SleepManager(VanillaPlus plugin)
    {
        this.plugin = plugin;
    }

    //Dynamic set to track active night skips per world name safely
    private final java.util.Set<String> activeSkips = new java.util.HashSet<>();
    //Tracks the day counter for each world by its name
    private final java.util.Map<String, Long> lastCheckedWorldDays = new java.util.HashMap<>();

    public void DayTracker()
    {
        Bukkit.getScheduler().runTaskTimer(plugin, () ->
        {
            //Loop through every single active world on the server
            for (World world : Bukkit.getWorlds())
            {
                long currentDay = world.getFullTime() / 24000;
                long lastDay = this.lastCheckedWorldDays.getOrDefault(world.getName(), -1L);

                //Initialize this specific world's day tracker on its first pulse
                if (lastDay == -1L)
                {
                    this.lastCheckedWorldDays.put(world.getName(), currentDay);
                    continue;
                }

                // If THIS specific world's day flipped, refresh its local players
                if (currentDay != lastDay)
                {
                    this.lastCheckedWorldDays.put(world.getName(), currentDay);

                    //Only loop through players physically inside this world!
                    for (Player player : world.getPlayers())
                    {
                        plugin.Update_ScoreBoard(player);
                    }
                }
            }
        }, 0L, 100L);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event)
    {
        Player player = event.getPlayer();
        //Force an instant scoreboard update so the HUD syncs to the new world's day count immediately
        plugin.Update_ScoreBoard(player);
    }

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event)
    {
        //check if custom sleep is enabled
        if(!plugin.configLoader.EnableCustomSleep) return;

        if(event.getBedEnterResult() == PlayerBedEnterEvent.BedEnterResult.OK)
        {
            // Delay 2 ticks just to let the server register that the player is fully in the bed
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                CheckSleepRequirements(event.getPlayer().getWorld());
            }, 2L);
        }
    }

    private void CheckSleepRequirements(World world)
    {
        if (activeSkips.contains(world.getName())) return;

        int OnlinePlayers = world.getPlayers().size();
        if (OnlinePlayers == 0) return;

        long SleepingPlayers = world.getPlayers().stream().filter(Player::isSleeping).count();
        if (SleepingPlayers == 0) return;

        String ActionBarMSG = "";

        boolean SkipNight = false;
        if(OnlinePlayers == 1)
        {
            if(plugin.configLoader.ActionBarMSG)
            {
                //send out a msg to the actionbar
                ActionBarMSG = "§eSleeping: §7(" + SleepingPlayers + "/" + "1" + ")";
            }
            SkipNight = true;
        }
        else if (plugin.configLoader.Sleep_Mode.equalsIgnoreCase("PERCENTAGE"))
        {
            double PercentageNeeded = (double) plugin.configLoader.Requirements / 100.0;
            double CurrentPercentage = (double) SleepingPlayers / OnlinePlayers;

            //Calculate the raw number of players needed, rounding up (e.g., 1.2 players means 2 are needed)
            long TargetRequired = (long) Math.ceil(OnlinePlayers * PercentageNeeded);
            //edge cases make sure it doesn't go below 1
            if(TargetRequired < 1)
            {
                TargetRequired = 1;
            }
            //Check if player wants a msg
            if(plugin.configLoader.ActionBarMSG)
            {
                //send out a msg to the actionbar
                ActionBarMSG = "§eSleeping: §7(" + SleepingPlayers + "/" + TargetRequired + ")";
            }
            if (CurrentPercentage >= PercentageNeeded)
            {
                SkipNight = true;
            }
        }
        else
        {
            //Check if player wants a msg
            if(plugin.configLoader.ActionBarMSG)
            {
                //send out a msg to the actionbar
                ActionBarMSG = "§eSleeping: §7(" + SleepingPlayers + "/" + plugin.configLoader.Requirements + ")";
            }
            if (SleepingPlayers >= plugin.configLoader.Requirements)
            {
                SkipNight = true;
            }
        }

        //Check that the ActionBarMSG is not empty before sending it out
        if(!ActionBarMSG.isEmpty())
        {
            // Convert to a modern Component for the action bar
            net.kyori.adventure.text.Component actionBarMessage = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(ActionBarMSG);

            for (Player player : world.getPlayers())
            {
                player.sendActionBar(actionBarMessage);
            }
        }

        if (SkipNight)
        {
            //Add this world to active skips to lock it from double-triggering
            activeSkips.add(world.getName());
        }
    }

    @EventHandler
    public void onPlayerDeepSleep(PlayerDeepSleepEvent event)
    {
        World world = event.getPlayer().getWorld();
        //If the plugin hasn't scheduled a night skip, let vanilla handle deep sleep normally
        if (!activeSkips.contains(world.getName())) return;

        //Capture all weather states immediately
        boolean wasStorming = world.hasStorm();
        boolean wasThundering = world.isThundering();
        int originalRainDuration = world.getWeatherDuration();
        int originalThunderDuration = world.getThunderDuration();
        int originalClearDuration = world.getClearWeatherDuration();

        //Calculate how to advance the days properly without changing morning time
        long currentFullTime = world.getFullTime();
        long currentDayTicks = currentFullTime % 24000L;
        long targetWakeTime = (long) plugin.configLoader.PlayerWakeTime;

        long ticksToWait;
        if (targetWakeTime > currentDayTicks)
        {
            ticksToWait = targetWakeTime - currentDayTicks;
        }
        else//this fixes the sleep on day vanilla mc day reset
        {
            ticksToWait = (24000L - currentDayTicks) + targetWakeTime;
        }

        //Force the day forward accurately
        world.setFullTime(currentFullTime + ticksToWait);

        //Get overworld name
        String OverworldName = world.getName();
        boolean hasMsg = plugin.configLoader.Night_Skipped_MSG != null && !plugin.configLoader.Night_Skipped_MSG.isEmpty();

        for (Player player : Bukkit.getOnlinePlayers())
        {
            if(player != null && player.isOnline())
            {
                //Get the name of the world for this player
                String PlayerWorldName = player.getWorld().getName();
                //Check if the PlayerWorldName is equal to overworld name or the name for nether or the end
                if (PlayerWorldName.equals(OverworldName) || PlayerWorldName.equals(OverworldName + "_nether") || PlayerWorldName.equals(OverworldName + "_the_end"))
                {
                    //Check if all players should have their rest stat reset
                    if(plugin.configLoader.AllPlayersRest)
                    {
                        //reset rest stat
                        player.setStatistic(Statistic.TIME_SINCE_REST, 0);
                        //update everyone's scoreboard
                        plugin.Update_ScoreBoard(player);
                    }
                    //This checks if a player is in deep sleep if so only reset it for all the players currently in deep sleep
                    else if(player.isDeeplySleeping() || player == event.getPlayer())
                    {
                        //reset rest stat
                        player.setStatistic(Statistic.TIME_SINCE_REST, 0);
                        //update everyone's scoreboard
                        plugin.Update_ScoreBoard(player);
                    }

                    //Send the alert broadcast message
                    if(hasMsg)
                    {
                        player.sendMessage(plugin.configLoader.Night_Skipped_MSG);
                    }
                }

            }
        }

        //Check if we should let mc handle the weather clearing or if we should restore it to the captured weather state
        if (!plugin.configLoader.ClearWeatherOnSleep)
        {
            //Wait 10 tick after the wake-up triggers to force our captured weather states back
            Bukkit.getScheduler().runTaskLater(plugin, () ->
            {
                world.setStorm(wasStorming);
                world.setWeatherDuration(originalRainDuration);

                world.setThundering(wasThundering);
                world.setThunderDuration(originalThunderDuration);
                world.setClearWeatherDuration(originalClearDuration);
            }, 10L);
        }

        // Reset all variables cleanly for the next sleep cycle
        activeSkips.remove(world.getName());
    }

    @EventHandler
    public void onPlayerBedLeave(PlayerBedLeaveEvent event)
    {
        World world = event.getPlayer().getWorld();

        // Safety valve: If a skip was planned but everyone left their beds early, unlock this world
        Bukkit.getScheduler().runTaskLater(plugin, () ->
        {
            long sleepingPlayers = world.getPlayers().stream().filter(Player::isSleeping).count();
            if (sleepingPlayers == 0)
            {
                activeSkips.remove(world.getName());
            }
        }, 10L);
    }
}