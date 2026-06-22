package BK.vanilaPlus;

import io.papermc.paper.event.player.PlayerDeepSleepEvent;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;

public class SleepManager implements Listener
{
    private final VanillaPlus plugin;
    public SleepManager(VanillaPlus plugin)
    {
        this.plugin = plugin;
    }

    // Global variables to track state across events safely
    private boolean isSkipScheduled = false;
    private int originalRainDuration = 0;
    private int originalThunderDuration = 0;
    private boolean wasStorming = false;
    private boolean wasThundering = false;

    @EventHandler
    public void onPlayerBedEnter(PlayerBedEnterEvent event)
    {
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
        if (isSkipScheduled) return;

        int OnlinePlayers = world.getPlayers().size();
        if (OnlinePlayers == 0) return;

        long SleepingPlayers = world.getPlayers().stream().filter(Player::isSleeping).count();
        if (SleepingPlayers == 0) return;

        boolean SkipNight = false;
        if (plugin.configLoader.Sleep_Mode.equalsIgnoreCase("PERCENTAGE"))
        {
            double PercentageNeeded = (double) plugin.configLoader.Requirements / 100.0;
            double CurrentPercentage = (double) SleepingPlayers / OnlinePlayers;
            if (CurrentPercentage >= PercentageNeeded)
            {
                SkipNight = true;
            }
        }
        else if (SleepingPlayers >= plugin.configLoader.Requirements)
        {
            SkipNight = true;
        }

        if (SkipNight)
        {
            //Lock the system from double-triggering
            isSkipScheduled = true;

            //Capture states immediately
            wasStorming = world.hasStorm();
            wasThundering = world.isThundering();
            originalRainDuration = world.getWeatherDuration();
            originalThunderDuration = world.getThunderDuration();
        }
    }

    @EventHandler
    public void onPlayerDeepSleep(PlayerDeepSleepEvent event)
    {
        //If the plugin hasn't scheduled a night skip, let vanilla handle deep sleep normally
        if (!isSkipScheduled) return;

        World world = event.getPlayer().getWorld();

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

        //Clear Phantom stats for EVERYONE in this world (Matches true Vanilla night skips)
        for (Player player : world.getPlayers())
        {
            player.setStatistic(Statistic.TIME_SINCE_REST, 0);
        }

        if (!plugin.configLoader.ClearWeatherOnSleep)
        {
            //Wait 10 tick after the wake-up triggers to force our captured weather states back
            Bukkit.getScheduler().runTaskLater(plugin, () ->
            {
                world.setStorm(wasStorming);
                world.setWeatherDuration(originalRainDuration);

                world.setThundering(wasThundering);
                world.setThunderDuration(originalThunderDuration);
            }, 10L);
        }

        //Send the alert broadcast message
        if (plugin.configLoader.Night_Skipped_MSG != null)
        {
            for (Player player : world.getPlayers())
            {
                player.sendMessage(plugin.configLoader.Night_Skipped_MSG);
            }
        }

        // Reset all variables cleanly for the next sleep cycle
        isSkipScheduled = false;
    }
}