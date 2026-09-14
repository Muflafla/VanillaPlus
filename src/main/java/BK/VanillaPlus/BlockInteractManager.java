package BK.VanillaPlus;

import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;

public class BlockInteractManager implements Listener
{
    private final VanillaPlus plugin;
    private final NamespacedKey toolDataKey;
    BlockInteractManager(VanillaPlus plugin)
    {
        this.plugin = plugin;
        toolDataKey = new NamespacedKey(plugin,"Times-Warned");
    }

    public boolean isTool(ItemStack item)
    {
        // Safety check
        if (item == null)
        {
            return false;
        }

        Material mat = item.getType();

        //The Tag API handles all variants (Diamond, Iron, Gold, etc.)
        return Tag.ITEMS_PICKAXES.isTagged(mat) ||
                Tag.ITEMS_AXES.isTagged(mat) ||
                Tag.ITEMS_SHOVELS.isTagged(mat) ||
                Tag.ITEMS_HOES.isTagged(mat);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if(!plugin.configLoader.EnablePickBlockFromShulker)return;

        //Check if the block was placed by the off-hand
        if (event.getHand() != EquipmentSlot.OFF_HAND) return;

        //Grab the off-hand item from the player
        ItemStack shulkerUsed = event.getPlayer().getInventory().getItemInOffHand();
        //Get the ItemMeta for the shulker
        ItemMeta itemMeta = shulkerUsed.getItemMeta();

        //Check if the item in the off-hand was a shulker
        if (shulkerUsed == null || !Tag.SHULKER_BOXES.isTagged(shulkerUsed.getType())) return;

        //Cancel the place event
        event.setCancelled(true);

        //Check if ItemMeta is an instance of BlockStateMeta
        if(itemMeta instanceof BlockStateMeta blockStateMeta)
        {
            //Get the BlockState for the shulker
            BlockState blockState = blockStateMeta.getBlockState();
            //Check if BlockState is an instance of ShulkerBox
            if(blockState instanceof ShulkerBox shulkerBox)
            {
                //Get the shulkerUsed inventory
                Inventory shulkerInv = shulkerBox.getInventory();
                //Get the block we placed the shulker against
                Material blockAgainst = event.getBlockAgainst().getType();
                //Check if the item we placed the shulker against exists inside the shulker
                for (ItemStack shulkerItem : shulkerInv.getContents())
                {
                    if (shulkerItem != null && shulkerItem.getType() == blockAgainst)
                    {
                        //Clone the item from the shulker
                        ItemStack itemToGive = shulkerItem.clone();
                        //Remove the item from the shulker
                        shulkerInv.removeItem(shulkerItem);

                        //Check if player hand is empty
                        if (event.getPlayer().getInventory().getItemInMainHand().getType().isAir())
                        {
                            //Place the item in the players hand
                            event.getPlayer().getInventory().setItemInMainHand(itemToGive);
                        }
                        else
                        {
                            //Get the players inventory
                            Inventory PlayerInv = event.getPlayer().getInventory();
                            //Try placing the item in the inventory and see what's left over
                            HashMap<Integer, ItemStack> leftover = PlayerInv.addItem(itemToGive);
                            //Get the leftoverItem
                            ItemStack leftoverItem = leftover.values().stream().findFirst().orElse(null);
                            if (!leftover.isEmpty()) {
                                //Case A: Some or none of the items did not fit place them back in the shulker
                                shulkerInv.addItem(leftoverItem);
                            }
                            //Case A: All items fit in the shulker we dont have to do anything now
                        }

                        //Update the shulker
                        blockStateMeta.setBlockState(blockState);
                        shulkerUsed.setItemMeta(blockStateMeta);
                    }
                }
            }
        }
    }

    @EventHandler
    public void OnBlockDestroyed(BlockBreakEvent event)
    {
        //Check if tool protection is enabled
        if(!plugin.configLoader.EnableToolProtection) return;

        Player player = event.getPlayer();
        ItemStack itemInHand = event.getPlayer().getInventory().getItemInMainHand();

        if (isTool(itemInHand))
        {
            int ItemDurability = itemInHand.getDurability();
            int ItemMaxDurability = itemInHand.getType().getMaxDurability();
            double PercentageLeft;

            if(ItemDurability > 0)
            {
                PercentageLeft = (((double) (ItemMaxDurability - (ItemDurability + 10)) / ItemMaxDurability * 100.0));
            }
            else
            {
                //for if the tool doesnt have dura yet
                PercentageLeft = 100.0;
            }

            //Get the item meta for the tool in hand
            ItemMeta meta = itemInHand.getItemMeta();
            //Create a new pdc container
            PersistentDataContainer Container = meta.getPersistentDataContainer();
            //TimesWarned contains the amount we already warned for this item
            Integer TimesWarned = Container.get(toolDataKey, PersistentDataType.INTEGER);
            boolean SendMSG = false;
            boolean ShouldUpdate = false;
            if(PercentageLeft <= plugin.configLoader.ToolProtectionPercentage)
            {

                //Check if the tool has a key
                if(TimesWarned != null)
                {
                    if(TimesWarned == 1)
                    {
                        if(PercentageLeft <= 4)
                        {
                            //Set key
                            Container.set(toolDataKey, PersistentDataType.INTEGER,2);
                            ShouldUpdate = true;
                            SendMSG = true;
                        }
                    }
                    else if (TimesWarned == 2)
                    {
                        if(PercentageLeft <= 3)
                        {
                            //Set key
                            Container.set(toolDataKey, PersistentDataType.INTEGER,3);
                            ShouldUpdate = true;
                            SendMSG = true;
                        }
                    }
                    else if (TimesWarned == 3)
                    {
                        if(PercentageLeft <= 2)
                        {
                            //Set key
                            Container.set(toolDataKey, PersistentDataType.INTEGER,4);
                            ShouldUpdate = true;
                            SendMSG = true;
                        }
                    }
                    else if (TimesWarned == 4)
                    {
                        if(PercentageLeft <= 1)
                        {
                            //Set key
                            Container.set(toolDataKey, PersistentDataType.INTEGER,5);
                            ShouldUpdate = true;
                            SendMSG = true;
                        }
                    }
                }
                else
                {
                    //Set key
                    Container.set(toolDataKey, PersistentDataType.INTEGER,1);
                    ShouldUpdate = true;
                    SendMSG = true;
                }
            }
            else if(TimesWarned != null)
            {
                //The tool was repaired remove the key
                Container.remove(toolDataKey);
                ShouldUpdate = true;
            }

            if(ShouldUpdate)
            {
                itemInHand.setItemMeta(meta);
            }

            if(SendMSG)
            {
                String title = "§cWARNING!";

                String subtitle = "§eTOOL ALMOST BROKEN!";

                // FadeIn: 10 ticks (0.5s), Stay: 60 ticks (3s), FadeOut: 10 ticks (0.5s)
                player.sendTitle(title, subtitle, 10, 60, 10);

                player.getLocation().getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 10, 1);
            }
        }
    }
}
