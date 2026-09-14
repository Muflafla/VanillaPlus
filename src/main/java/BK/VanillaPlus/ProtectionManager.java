package BK.VanillaPlus;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ProtectionManager implements Listener
{
    private final VanillaPlus plugin;
    public ProtectionManager(VanillaPlus plugin) {this.plugin = plugin;}

    BukkitTask EndermanTask;
    public int EndermanAmount = 0;

    public void ProtectionManager_Init()
    {
        if(EndermanTask != null)
        {
            EndermanTask.cancel();
            EndermanTask = null;
        }
        if (plugin.configLoader.Show_Enderman_Messages)
        {
            // Task to broadcast Enderman griefing prevention stats
            EndermanTask = new BukkitRunnable()
            {
                @Override
                public void run()
                {

                    if (EndermanAmount > 0)
                    {
                        //get the messages from the config
                        List<String> messages = plugin.getConfig().getStringList("Enderman-Messages");
                        if(!messages.isEmpty())
                        {
                            String rawMessage = messages.get(new Random().nextInt(messages.size()));
                            String finalMessage = String.format(rawMessage, EndermanAmount);
                            //Bukkit.broadcast(Component.text(finalMessage)); use this if this actually ever gets removed.
                            for (Player player : Bukkit.getOnlinePlayers())
                            {
                                player.sendMessage(finalMessage);
                            }
                        }
                        else
                        {
                            for (Player player : Bukkit.getOnlinePlayers())
                            {
                                player.sendMessage("§aNo enderman MSG's set");

                            }
                        }

                        //reset enderman amount
                        EndermanAmount = 0;
                    }
                }
            }.runTaskTimer(plugin, 0L, 6000L);//6000L is 5 min
        }
    }

    //////////////////////////////////////creepers/enderman fixes///////////////////////////////////////
    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event)
    {
        //if true stop creepers from spawning.
        if (plugin.configLoader.Disable_Creeper_Spawn)
        {
            if (event.getEntityType().equals(EntityType.CREEPER))
            {
                event.setCancelled(true);
            }
        }
        if(plugin.configLoader.Disable_Phantom_Spawn)
        {
            if (event.getEntityType().equals(EntityType.PHANTOM))
            {
                event.setCancelled(true);
            }
        }
    }
    @EventHandler
    public void ExplosionPrime(ExplosionPrimeEvent event)
    {
        //if true stop creepers from exploding.
        if (plugin.configLoader.Disable_Creeper_Explosions)
        {
            if (event.getEntityType() == EntityType.CREEPER)
            {
                //Cancel Explode event
                event.setCancelled(true);

                //Get event location and play a sound there and delete the creeper
                Location loc = event.getEntity().getLocation();
                World world = loc.getWorld();
                if (world !=null && event.getEntity() instanceof Creeper creeper)
                {
                    //For maximum fun do a random pitch every time
                    float randomPitch = 0.5f + (float) Math.random();
                    world.playSound(loc, Sound.ENTITY_CHICKEN_EGG,1.0f,randomPitch);
                    creeper.remove();
                }

            }
        }
    }
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event)
    {
        //if true disable creepers from doing block damage.
        if (plugin.configLoader.Disable_Creeper_Block_Damage)
        {
            if (event.getEntityType() == EntityType.CREEPER)
            {
                //clear block list so no blocks get destroyed
                event.blockList().clear();
                return;
            }
        }
        //if true make it so creepers give back all blocks they destroy
        if (plugin.configLoader.Creeper_Gives_all_Blocks)
        {
            if (event.getEntityType() == EntityType.CREEPER)
            {
                event.setYield(100.0f);
            }
        }

        //check for fireballs
        if (event.getEntityType() == EntityType.FIREBALL)
        {
            //check if it's a large fireball
            if (event.getEntity() instanceof LargeFireball fireball)
            {
                //check if a ghast shot it
                if (fireball.getShooter() instanceof Ghast)
                {
                    if(plugin.configLoader.Disable_Ghast_Explosions)
                    {
                        event.setCancelled(true);

                        //Get enitiy event location if entity is null because out of render distance then get event location so we always have a location
                        Location loc = (event.getEntity() != null) ? event.getEntity().getLocation() : event.getLocation();
                        World world = loc.getWorld();
                        if (world !=null)
                        {
                            //For maximum fun do a random pitch every time
                            float randomPitch = 0.5f + (float) Math.random();
                            world.playSound(loc,Sound.ENTITY_CHICKEN_EGG,1.0f,randomPitch);
                            fireball.remove();
                        }
                    }
                    else if(plugin.configLoader.Disable_Ghast_Block_Damage)
                    {
                        //clear block list so no blocks get destroyed
                        event.blockList().clear();
                    }
                    else if (plugin.configLoader.Ghast_Gives_all_Blocks)
                    {
                        event.setYield(100.0f);

                        // 1. Create a list to remember all block locations that are exploding
                        List<Location> explodedLocations = new ArrayList<>();
                        for (Block block : event.blockList())
                        {
                            explodedLocations.add(block.getLocation());
                        }

                        // 2. Wait 1 tick for the explosion to complete and spawn its fire blocks
                        Bukkit.getScheduler().runTaskLater(plugin, () ->
                        {
                            for (Location loc : explodedLocations)
                            {
                                Block block = loc.getBlock();
                                Block above = block.getRelative(BlockFace.UP);

                                // Snuff out fire inside the exploded crater hole
                                if (block.getType() == Material.FIRE)
                                {
                                    block.setType(Material.AIR);
                                }

                                // Snuff out fire sitting on the rim/edge of the crater
                                if (above.getType() == Material.FIRE)
                                {
                                    above.setType(Material.AIR);
                                }
                            }
                        }, 1L);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event)
    {
        //if true stops enderman from griefing.
        if (plugin.configLoader.Stop_Enderman_From_Griefing)
        {
            if (event.getEntityType() == EntityType.ENDERMAN)
            {
                event.setCancelled(true);
                EndermanAmount++;
            }
        }
    }
}
