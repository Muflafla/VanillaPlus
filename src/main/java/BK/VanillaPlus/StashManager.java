package BK.VanillaPlus;

import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class StashManager implements Listener
{
    private final VanillaPlus plugin;

    //This is all needed for the stashes
    private boolean disableAsyncSaves = false;
    private Boolean isReloading = false;
    private boolean isShuttingDown = false;
    private final Map<UUID, BukkitTask> activeSaveTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Inventory> StashesInMemory = new ConcurrentHashMap<>();
    private final Set<UUID> activelyInspectedStashes = Collections.synchronizedSet(new HashSet<>());
    private final Set<UUID> pendingLoads = Collections.synchronizedSet(new HashSet<>());

    //this is for the OverFlow feature
    BukkitTask OverFlowTask;

    public StashManager(VanillaPlus plugin) {this.plugin = plugin;}

    public void StashManager_Init()
    {
        //Make sure stash folder exists
        File stashFolder = new File(plugin.getDataFolder(), "stashes");
        //Ensure the folder exists
        if(!stashFolder.exists())
        {
            //Create it
            if(!stashFolder.mkdir())
            {
                plugin.getLogger().severe("Could not create the 'stashes' data folder!");
            }
        }

        startStashOverflowTask();
    }

    /////////////////////////////////////Stashes//////////////////////////////////////
    public void ClearStashCache()
    {
        isReloading = true;
        //first force close and save all the stashes open right now
        forceSaveAllAndClose();
        //Then make sure everything linked to stashes is cleared
        StashesInMemory.clear();
        activeSaveTasks.clear();
        activelyInspectedStashes.clear();
        pendingLoads.clear();

        Bukkit.getConsoleSender().sendMessage("§aCLEARED STASH CACHE.");
        Bukkit.getConsoleSender().sendMessage("§aALL THE STASHES HAVE BEEN SAVED TO DISK.");
        Bukkit.getConsoleSender().sendMessage("§a=========================================================================================================");

        isReloading = false;
        disableAsyncSaves = false;
    }

    //this is here to hold the information about the holder of the stash
    public static class StashHolder implements InventoryHolder
    {
        private final UUID OwnerUUID;
        private final String OwnerName;
        private boolean OverFlowEnabled;

        public StashHolder(UUID ownerUUID, String ownerName, boolean overFlowEnabled)
        {
            this.OwnerUUID = ownerUUID;
            this.OwnerName = ownerName;
            this.OverFlowEnabled = overFlowEnabled;
        }

        public UUID getOwnerUUID()
        {
            return OwnerUUID;
        }


        public String GetOwnerName()
        {
            return OwnerName;
        }

        public boolean GetOverFlowState()
        {
            return OverFlowEnabled;
        }

        public void setOverFlowEnabled(boolean overFlowEnabled)
        {
            this.OverFlowEnabled = overFlowEnabled;
        }

        //this is here to make bukkit happy
        @Override
        public Inventory getInventory()
        {
            return null;
        }
    }

    @EventHandler
    public void giveCustomBookIfMissing(PlayerJoinEvent event)
    {
        //Check if Stashes are enabled
        if(!plugin.configLoader.Enable_Mystical_Stash) return;
        //Check if the stash book is enabled
        if(!plugin.configLoader.Enable_Mystical_Stash_Book) return;

        Player player = event.getPlayer();
        boolean hasBook = false;
        boolean hasSpace = false;

        ItemStack[] InventoryContents = player.getInventory().getContents();
        for(int i = 0; i < InventoryContents.length; i++)
        {
            //Skip the all the slots that are not hotbar or main storage (0/35)
            if(i > 35)
            {
                //skip these slots
                continue;
            }
            ItemStack invItem = InventoryContents[i];
            if (invItem == null || invItem.getType() == Material.AIR)
            {
                hasSpace = true;
            }
            if(invItem != null && invItem.getType() == Material.WRITTEN_BOOK)
            {
                BookMeta meta = (BookMeta) invItem.getItemMeta();
                if (meta.hasAuthor() && meta.getAuthor().equals("§bServer Wizard"))
                {
                    hasBook = true;
                    break;
                }
            }
        }
        //Check if they don't have the book and also if they have space
        if (!hasBook && hasSpace)
        {
            ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
            BookMeta meta = (BookMeta) book.getItemMeta();
            meta.setDisplayName("§d" + plugin.configLoader.Stash_Name);
            meta.setTitle("§6VanillaPlus Stash");
            meta.setAuthor("§bServer Wizard");

            List<String> pages = new ArrayList<>();
            pages.add("§dNothing to see here!");
            meta.setPages(pages);

            book.setItemMeta(meta);
            player.getInventory().addItem(book);
            player.sendMessage("§aYou have received the §d" + plugin.configLoader.Stash_Name + "§a!");
        }
        else if (!hasSpace && !hasBook)
        {
            player.sendMessage("§aINV IS FULL NO §d" + plugin.configLoader.Stash_Name + "§a! FOR YOU");
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if(plugin.configLoader.Enable_Mystical_Stash)
        {
            if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
            {
                ItemStack item = event.getItem();

                if (item != null && item.getType() == Material.WRITTEN_BOOK)
                {
                    ItemMeta meta = item.getItemMeta();
                    if (meta instanceof BookMeta bookMeta)
                    {
                        if (bookMeta.hasDisplayName())
                        {
                            if (bookMeta.hasAuthor() && bookMeta.getAuthor().equals("§bServer Wizard"))
                            {
                                event.setCancelled(true);
                                openMysticalStash(event.getPlayer(),null);
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        if(plugin.configLoader.Enable_Mystical_Stash)
        {
            //check if the stash inv title is the title set in config
            if (ChatColor.stripColor(event.getView().getTitle()).equals(plugin.configLoader.Stash_Name))
            {
                //check if this is the book or a chest by checking if its labeled StashHolder
                if (!(event.getInventory().getHolder() instanceof StashHolder))
                {
                    //Check if we allow usage of named chest for the opening of the Stash inv
                    if(plugin.configLoader.Enable_Mystical_Stash_Chests)
                    {
                        // Stop the player from opening the physical chest
                        event.setCancelled(true);
                        //Open the "Real" virtual stash inv instead
                        openMysticalStash((Player) event.getPlayer(),null);
                    }
                    //Let the chest open normally
                }
                // If it IS a StashHolder/the book, we do nothing and let it open normally!
            }
        }
    }

    public void openMysticalStash(OfflinePlayer player, Player admin)
    {
        if(plugin.configLoader.Enable_Mystical_Stash)
        {
            Player OnlineTarget = player.getPlayer();
            //Check if this stash inv is currently being loaded of disc by another player
            if(pendingLoads.contains(player.getUniqueId()))return;
            //Check if we aren't in a reload state or shutdown state
            if(isReloading || isShuttingDown)
            {
                if(OnlineTarget != null)
                {
                    if(isReloading)
                    {
                        OnlineTarget.sendMessage("The server is currently reloading. Please wait until the process is complete before accessing your stash.");
                    }
                    else//Shutting down
                    {
                        OnlineTarget.sendMessage("The server is currently shutting down. Can't access your stash during a shutdown.");
                    }
                    return;
                }
            }
            //Check if player stash inv is currently being inspected by an admin and thus is locked
            if (activelyInspectedStashes.contains(player.getUniqueId()))
            {
                if(admin == null)
                {
                    if(OnlineTarget != null)
                    {
                        OnlineTarget.sendMessage("§cAn admin is currently managing your stash. Access denied.");
                    }
                }
                else
                {
                    admin.sendMessage("§cAnother admin is already inspecting this stash. Access denied.");
                }

                return;//return here admin is accessing the stash inv now
            }

            //Check if stash inv is already in cache(ram) if so load if from there
            if(StashesInMemory.containsKey(player.getUniqueId()))
            {
                Inventory CachedInv = StashesInMemory.get(player.getUniqueId());
                if(admin == null)
                {
                    player.getPlayer().openInventory(CachedInv);
                }
                else
                {
                    activelyInspectedStashes.add(player.getUniqueId());
                    if(CachedInv.getHolder() instanceof StashHolder)
                    {
                        CachedInv.close();
                        if(OnlineTarget != null)
                        {
                            OnlineTarget.sendMessage("§cAn admin has accessed your " + plugin.configLoader.Stash_Name + ".");
                        }
                    }

                    admin.openInventory(CachedInv);
                }
                return;
            }

            //This is here for some edge cases where the player stash is not in memory while there online or an admin loads an offline stash
            if(admin != null)
            {
                //Send msg to admin that where loading the stash from disk
                admin.sendMessage("§cLoading this stash from disk player is offline");
            }

            loadStashFromDisk(player, (Success) ->
            {
                if(Success)
                {
                    openMysticalStash(player,admin);
                }
            });
        }
    }

    public void loadStashFromDisk(OfflinePlayer player, Consumer<Boolean> Loaded)
    {
        //Add player to pendingLoads because where getting the file from disk
        pendingLoads.add(player.getUniqueId());

        //Jump of main thread to handle file loading
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
        {
            try
            {
                //Create the stashes sub folder
                File stashFolder = new File(plugin.getDataFolder(), "stashes");

                File playerFile = new File(stashFolder, player.getUniqueId() + ".yml");
                FileConfiguration playerStash = YamlConfiguration.loadConfiguration(playerFile);

                //Jump back to main thread to perform ui actions safely
                Bukkit.getScheduler().runTask(plugin, () ->
                {
                    boolean overFlowEnabled = playerStash.getBoolean("overFlowEnabled",true);//defaults to true
                    //Create the inventory var
                    Inventory extraInv;
                    //Create the inventory
                    extraInv = Bukkit.createInventory(new StashHolder(player.getUniqueId(), player.getName(),overFlowEnabled), plugin.configLoader.Stash_Rows, plugin.configLoader.Stash_Name);

                    for (int i = 0; i < extraInv.getSize(); ++i)
                    {
                        extraInv.setItem(i, playerStash.getItemStack("inventory." + i));
                    }

                    //Add the inventory to the cache(ram) so next time we don't load from disk
                    StashesInMemory.put(player.getUniqueId(), extraInv);
                    Loaded.accept(true);
                    Bukkit.getConsoleSender().sendMessage("§aLOADED STASH FROM DISK BECAUSE IT DID NOT EXIST IN RAM");
                });
            }
            catch (Exception e)
            {
                plugin.getLogger().warning("Error in creating stash folder!");
            }
            finally
            {
                //This is guaranteed to run, preventing the player from getting stuck
                pendingLoads.remove(player.getUniqueId());
            }
        });
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event)
    {
        if(plugin.configLoader.Enable_Mystical_Stash)
        {
            Inventory stashInv = event.getInventory();
            if (stashInv.getHolder() instanceof StashHolder Holder)
            {
                UUID PlayerUUID = Holder.getOwnerUUID();
                String PlayerName = Holder.GetOwnerName();

                //if the player closing the Stash inv is not the owner, it was an admin so we can remove the lock now
                if(!event.getPlayer().getUniqueId().equals(PlayerUUID))
                {
                    //Remove the lock so the regular player can safely open their stash inv again
                    activelyInspectedStashes.remove(Holder.getOwnerUUID());

                    //Get the player object
                    Player targetPlayer = Bukkit.getPlayer(PlayerUUID);
                    //If an admin closes a stash inv check if the stash inv owner is online if not close remove that stash inv from memory to prevent memory leak
                    //Check if targetPlayer is null or else check if the player is connected and the stash inv has no viewers
                    if((targetPlayer == null || !targetPlayer.isConnected()) && stashInv.getViewers().size() <= 1)
                    {
                        //Check that where not currently in reload/shutdown this is for the edge case an admin is viewing a stash inv while the server reloads thus closing this stash inv
                        if(!isReloading || !isShuttingDown)
                        {
                            //Save the stash of the player
                            Save_Stash_To_File(Holder.getOwnerUUID(),stashInv.getContents(), Holder.OwnerName, null);
                            Bukkit.getConsoleSender().sendMessage("§aADMIN CLOSED A INSPECTED STASH FOR PLAYER: §e" + Holder.OwnerName + "§a. NOW REMOVING IT FROM MEMORY(RAM).");
                            StashesInMemory.remove(PlayerUUID);
                        }
                    }
                }

                //Check if the server is in shutdown/reloading if so don't do the save as this is done synchronously by the forceSaveAllAndClose function
                if (!disableAsyncSaves)
                {
                    //Cancel existing task for this specific player if it exists. to not have the old and new safe work against each other
                    BukkitTask existingTask = activeSaveTasks.remove(PlayerUUID);
                    if (existingTask != null)
                    {
                        existingTask.cancel();
                    }

                    //do the saving on a separate thread;
                    BukkitTask SaveTask = Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                    {
                        Save_Stash_To_File(PlayerUUID, event.getInventory().getContents(), PlayerName,null);
                        // Remove from map when finished
                        activeSaveTasks.remove(PlayerUUID);
                    });
                    activeSaveTasks.put(PlayerUUID, SaveTask);
                }

            }
        }
    }

    private void Save_Stash_To_File(UUID PlayerUUID, ItemStack[] liveContents, String Player_Name, Boolean OverflowEnabled)
    {
        if(plugin.configLoader.Enable_Mystical_Stash)
        {
            //Create the stashes sub folder
            File stashFolder = new File(plugin.getDataFolder(), "stashes");
            //Ensure the folder exists
            if(!stashFolder.exists())
            {
                //Create it
                if(!stashFolder.mkdir())
                {
                    plugin.getLogger().severe("Could not create the 'stashes' data folder!");
                }
            }

            File file = new File(stashFolder, PlayerUUID + ".yml");
            File TMPfile = new File(stashFolder, PlayerUUID + ".yml.tmp");

            //load the existing config file if it exists
            FileConfiguration playerStash = YamlConfiguration.loadConfiguration(file);

            //Check if liveContents was provided if not don't update it in the file
            if(liveContents != null)
            {
                //Creat the clone ItemStack
                ItemStack[] ClonedContents = new ItemStack[liveContents.length];
                //make a clone of the content so we can pass this clone for saving so no corruption can occur
                for (int i = 0; i < liveContents.length; ++i)
                {
                    if (liveContents[i] != null)
                    {
                        ClonedContents[i] = liveContents[i].clone();
                    }
                }

                for (int i = 0; i < ClonedContents.length; ++i)
                {
                    playerStash.set("inventory." + i, ClonedContents[i]);
                }
            }
            //Check if the setting was provided if not don't update it in the file
            if(OverflowEnabled != null)
            {
                playerStash.set("overFlowEnabled", OverflowEnabled);
            }


            try
            {
                //Try saving to temp file
                playerStash.save(TMPfile);

                //When successfully saved to temp file swap the temp file and the normal stash inv file
                Files.move(TMPfile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                if(disableAsyncSaves)
                {
                    Bukkit.getConsoleSender().sendMessage("§aSTASH OF PLAYER: §e" + Player_Name + " §aWAS SAVED SUCCESSFULLY.");
                }
            }
            catch (IOException e)
            {
                plugin.getLogger().severe("Could not save stash for " + Player_Name + ": " + e.getMessage() + ".");


            }
            finally
            {
                //Clean up the leftover temp file if the operation failed midway
                if (TMPfile.exists())
                {
                    if(TMPfile.delete())
                    {
                        plugin.getLogger().severe("Could not delete tmp file for " + Player_Name + ".");
                    }
                }
            }
        }
    }

    //This is here for edge case of a player having the stash inv open at server close or reload
    public void forceSaveAllAndClose()
    {
        //Check if isReloading is false, if this is the case we must be shutting down so isShuttingDown must be set to true
        if(!isReloading)
        {
            isShuttingDown = true;
        }

        Bukkit.getConsoleSender().sendMessage("§a=========================================================================================================");
        Bukkit.getConsoleSender().sendMessage("§aFORCE CLOSING ALL STASHES... SAVING ALL THE STASHES IN MEMORY TO DISK...");

        //Disable async saving as this could lead to issues when server is shutting down or reloading
        disableAsyncSaves = true;

        //Force every stash inv currently open to be saved and closed
        for (Player player : Bukkit.getOnlinePlayers())
        {
            //If the player has a stash inv open check if its one of our Stashes if so close it
            if (player.getOpenInventory().getTopInventory().getHolder() instanceof StashHolder)
            {
                if(isReloading)
                {
                    player.sendMessage("§cClosed the stash. Server is Reloading.");
                }
                else if(isShuttingDown)
                {
                    player.sendMessage("§cClosed the stash. Server is shutting down.");
                }
                //Force the stash inv closed so the player doesn't have a "ghost" stash inv open
                player.closeInventory();
            }
        }

        //Loop through all the entries in the StashesInMemory
        StashesInMemory.forEach((playerUUID, stashInv) ->
        {
            if (stashInv.getHolder() instanceof StashHolder holder)
            {
                Save_Stash_To_File(playerUUID, stashInv.getContents(), holder.GetOwnerName(),null);
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event)
    {
        //Get the players stash inv
        Inventory CachedInv = StashesInMemory.get(event.getPlayer().getUniqueId());
        Player player = event.getPlayer();

        //Check if the CachedInv is null if so stop here
        if(CachedInv == null)return;

        //Double check its one of our stashes
        if (CachedInv.getHolder() instanceof StashHolder holder)
        {
            //Check if anyone is viewing this stash inv now if not save it and clear it
            if(CachedInv.getViewers().isEmpty())
            {
                //This exists ass a backup
                activelyInspectedStashes.remove(holder.getOwnerUUID());
                //Save the stash to disk
                Save_Stash_To_File(player.getUniqueId(), CachedInv.getContents(), player.getName(),null);
                //Clear the stats from memory
                StashesInMemory.remove(event.getPlayer().getUniqueId());
                Bukkit.getConsoleSender().sendMessage("§aSAVED STASH & CLEARED IT FROM RAM FOR PLAYER: §e" + event.getPlayer().getName() + "§a.");
            }
            //Someone was watching the stash inv so we don't save it that happens when that person closes it
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event)
    {
        //Check if we should drop the items from the stash inv
        if(!plugin.configLoader.Enable_Drop_Stash_On_Death)return;

        Player player = event.getPlayer();
        //Check if stash exists in ram
        if(StashesInMemory.containsKey(player.getUniqueId()))
        {
            Inventory CachedInv = StashesInMemory.get(player.getUniqueId());
            ItemStack[] Contents = CachedInv.getContents();
            Location loc = event.getPlayer().getLocation();
            for (ItemStack content : Contents)
            {
                if(content == null) continue;
                loc.getWorld().dropItemNaturally(loc, content);
            }
            //Clear the stash inv
            StashesInMemory.get(player.getUniqueId()).clear();
            //Save the stash inv
            Save_Stash_To_File(player.getUniqueId(), CachedInv.getContents(), player.getName(),null);
        }
        else
        {
            //Load it from disk
            loadStashFromDisk(player, (Success) ->
            {
                if(Success)
                {
                    Inventory CachedInv = StashesInMemory.get(player.getUniqueId());
                    ItemStack[] Contents = CachedInv.getContents();
                    Location loc = event.getPlayer().getLocation();
                    for (ItemStack content : Contents)
                    {
                        if(content == null) continue;
                        loc.getWorld().dropItemNaturally(loc, content);
                    }
                    //Clear the stash inv
                    StashesInMemory.get(player.getUniqueId()).clear();
                    //Save the stash inv
                    Save_Stash_To_File(player.getUniqueId(), CachedInv.getContents(), player.getName(),null);
                }
                else
                {
                    plugin.getLogger().severe("FAILED TO LOAD STASH FROM DISK FOR DEATH EVENT FOR PLAYER: " + player.getName() + ".");
                }
            });
        }
    }

    public void displayStashesInRam(Player player)
    {
        AtomicInteger i = new AtomicInteger();
        if(player != null)
        {
            player.sendMessage("============= STASHES IN RAM =============");
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage("§a============= STASHES IN RAM =============");
        }
        StashesInMemory.forEach((uuid, itemStacks) ->
        {
            //Get the player name from UUID
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            //Check playerName is not null
            if(playerName == null) return;
            //Command send by a player so send to player
            if(player != null)
            {
                player.sendMessage(playerName);
            }
            //Command send by a console so send msg to console
            else
            {
                Bukkit.getConsoleSender().sendMessage("§e" + playerName);
            }

            i.getAndIncrement();
        });
        if(i.get() == 0)
        {
            if(player != null)
            {
                player.sendMessage("NO STASHES IN RAM");
            }
            else
            {
                Bukkit.getConsoleSender().sendMessage("§aNO STASHES IN RAM");
            }

        }

        if(player != null)
        {
            player.sendMessage("========================================");
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage("§a==========================================");
        }
    }

    public void startStashOverflowTask()
    {
        //Check if Item overFlow to stash is enabled first
        if (!plugin.configLoader.Enable_Stash_Overflow)
        {
            //Check if the OverFlowTask is running if so can it
            if (OverFlowTask != null)
            {
                OverFlowTask.cancel();
            }
            return;
        }

        //Before starting a task check if It's not already running
        if (OverFlowTask != null)
        {
            OverFlowTask.cancel();
        }

        OverFlowTask = Bukkit.getScheduler().runTaskTimer(plugin, () ->
        {
            for (Player player : Bukkit.getOnlinePlayers())
            {
                //Skip if the player's inventory has empty slots
                if (player.getInventory().firstEmpty() != -1)
                {
                    continue;
                }

                //Make sure stash is loaded in memory
                Inventory cachedInv = StashesInMemory.get(player.getUniqueId());
                if(cachedInv != null)
                {
                    boolean overFlowState = false;
                    //Make sure it has a valid instance of StashHolder and get the overflow state
                    if(cachedInv.getHolder() instanceof StashHolder holder)
                    {
                        overFlowState = holder.GetOverFlowState();
                    }
                    else
                    {
                        //This should never happen but here we are. Better safe than sorry
                        continue;
                    }

                    //only process nearby items if overflow is enabled for this player
                    if(overFlowState)
                    {
                        processNearbyItems(player, cachedInv);
                    }

                }
                else
                {
                    //Load it from disk
                    loadStashFromDisk(player, (Success) ->
                    {
                        if(Success)
                        {
                            boolean overFlowState = false;
                            Inventory cachedInv1 = StashesInMemory.get(player.getUniqueId());
                            //Make sure it has a valid instance of StashHolder and get the overflow state. This should always pass but here we are better safe than sorry
                            if(cachedInv1.getHolder() instanceof StashHolder holder)
                            {
                                overFlowState = holder.GetOverFlowState();
                            }

                            if(overFlowState)
                            {
                                processNearbyItems(player, StashesInMemory.get(player.getUniqueId()));
                            }

                        }
                        else
                        {
                            plugin.getLogger().severe("FAILED TO LOAD STASH FROM DISK FOR STASH OVERFLOW FOR PLAYER BY NAME: " + player.getName() + ".");
                        }
                    });
                }
            }
        }, 0L, 20L); // 20L is 1 sec. run every 1 sec
    }

    private void processNearbyItems(Player player, Inventory StashInv)
    {
        //Scan for nearby dropped items
        for (Entity entity : player.getNearbyEntities(1.5, 1.5, 1.5))
        {
            if (entity instanceof Item itemEntity)
            {
                //Skip if the item is invalid or not ready to be picked up
                if (itemEntity.getPickupDelay() > 0 || itemEntity.isDead())
                {
                    continue;
                }

                //Get the ground itemEntity
                ItemStack GroundItem = itemEntity.getItemStack();
                //Clone the original itemEntity
                ItemStack GroundItemClone = GroundItem.clone();

                //Check if the item is valid
                if (!itemEntity.isValid())
                {
                    continue;
                }

                //Try adding the cloned items to the stash inv
                HashMap<Integer, ItemStack> leftover = StashInv.addItem(GroundItemClone);

                //Case A: Everything fit. Remove the item from the ground
                if (leftover.isEmpty())
                {
                    itemEntity.remove();
                    player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2f, (float) (Math.random() * 0.4 + 0.8));
                }
                //Case B: It only partially fit.
                else
                {
                    //Get the leftoverItem
                    ItemStack leftoverItem = leftover.values().stream().findFirst().orElse(null);
                    //Check if the GroundItem and the GroundItemClone don't match if so we did add some items to the stash inv
                    if (!GroundItem.equals(GroundItemClone))
                    {
                        itemEntity.setItemStack(leftoverItem);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.2f, (float) (Math.random() * 0.4 + 0.8));
                    }
                }
            }
        }
    }

    public void UpdateOverFlowSetting(Player player, boolean OverflowEnabled)
    {
        //Check if the stash inv exists in memory
        if(StashesInMemory.containsKey(player.getUniqueId()))
        {
            Inventory StashInv = StashesInMemory.get(player.getUniqueId());
            //Check if its one of our stashes and get the holder
            if(StashInv.getHolder() instanceof StashHolder holder)
            {
                //Update the OverflowEnabled setting in memory
                holder.setOverFlowEnabled(OverflowEnabled);
            }
        }
        //Save the OverflowEnabled setting to disk
        Save_Stash_To_File(player.getUniqueId(),null, player.getName(), OverflowEnabled);
    }
}

