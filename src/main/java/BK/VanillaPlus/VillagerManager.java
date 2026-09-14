package BK.VanillaPlus;

import com.destroystokyo.paper.entity.villager.Reputation;
import com.destroystokyo.paper.entity.villager.ReputationType;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class VillagerManager implements Listener
{
    private final VanillaPlus plugin;
    public  VillagerManager(VanillaPlus plugin) {this.plugin = plugin;}

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent event)
    {
        //Check if global villager pricing is enabled first.
        if(!plugin.configLoader.Enable_Global_Villager_Pricing)return;
        //Check that where dealing with a villager inv
        if(!(event.getInventory() instanceof MerchantInventory merchantInventory)) return;
        if(!(merchantInventory.getHolder() instanceof Villager villager)) return;

        //Bukkit.getConsoleSender().sendMessage("§a=========================================================================");

        //Get the player rep
        Reputation rep = villager.getReputation(event.getPlayer().getUniqueId());
        //Negative rep
        int majorNegative = rep.getReputation(ReputationType.MAJOR_NEGATIVE);
        //Bukkit.getConsoleSender().sendMessage("Major negative: " + majorNegative);
        int minorNegative = rep.getReputation(ReputationType.MINOR_NEGATIVE);
        //Bukkit.getConsoleSender().sendMessage("Minor negative: " + minorNegative);
        int totalNegative = minorNegative + majorNegative;
        //Positive rep
        int majorPositive = rep.getReputation(ReputationType.MAJOR_POSITIVE);
        //Bukkit.getConsoleSender().sendMessage("Major positive: " + majorPositive);
        int minorPositive = rep.getReputation(ReputationType.MINOR_POSITIVE);
        //Bukkit.getConsoleSender().sendMessage("Minor positive: " + minorPositive);
        int tradingRep = rep.getReputation(ReputationType.TRADING);
        //Bukkit.getConsoleSender().sendMessage("TradingRep: " + tradingRep);
        int totalPositive = majorPositive + minorPositive + tradingRep;
        //Bukkit.getConsoleSender().sendMessage("TotalPositive: " + totalPositive);

        //Calculate TotalNegativeRepScore and cap it to stop
        int TotalNegativeRepScore = Math.max(0,totalNegative - totalPositive);
        //Bukkit.getConsoleSender().sendMessage("TotalNegativeRepScore: " + TotalNegativeRepScore);

        //Get the pdc of the villager
        PersistentDataContainer PDC = villager.getPersistentDataContainer();
        //Get the recipes of this villager(all the trades)
        List<MerchantRecipe> recipes = merchantInventory.getMerchant().getRecipes();
        //Go through its recipes
        for(int i = 0; i < recipes.size(); i++)
        {
            //Bukkit.getConsoleSender().sendMessage("§e=========================================================================");
            //Get the current recipe and its price
            MerchantRecipe recipe = recipes.get(i);
            int currentPlayerPriceEffect = recipe.getSpecialPrice();
            //Bukkit.getConsoleSender().sendMessage("currentPlayerPriceEffect: " + currentPlayerPriceEffect);
            //Create the key and check if we already have this key in the pdc
            NamespacedKey priceKey = new NamespacedKey(plugin, "best_price_slot_" + i);
            int bestStoredPriceEffect = PDC.getOrDefault(priceKey, PersistentDataType.INTEGER, 0);//0 Default no discount

            //Compare the current price to the best price ever offer by this villager to a player
            if(currentPlayerPriceEffect < bestStoredPriceEffect)
            {
                //Bukkit.getConsoleSender().sendMessage("new best price effect:" + currentPlayerPriceEffect + " | old best price effect:" + bestStoredPriceEffect );
                //The current price is lower(better price) so we store it as the new best price
                PDC.set(priceKey, PersistentDataType.INTEGER, currentPlayerPriceEffect);
                bestStoredPriceEffect = currentPlayerPriceEffect;
            }

            //Bukkit.getConsoleSender().sendMessage("bestStoredPriceEffect: " + bestStoredPriceEffect);

            //Scale the penalty based on the price multiplier per recipe
            float priceMultiplier = recipe.getPriceMultiplier();
            //Bukkit.getConsoleSender().sendMessage("priceMultiplier: " + priceMultiplier);
            int penaltyPriceEffect = 0;
            //Check if the final rep is bad
            if(TotalNegativeRepScore > 0)
            {
                penaltyPriceEffect = (int) Math.floor(TotalNegativeRepScore * priceMultiplier);
            }
            //Bukkit.getConsoleSender().sendMessage("penaltyPriceEffect: " + penaltyPriceEffect);

            //Calc the final effect on the price
            int finalPlayerPriceEffect = bestStoredPriceEffect + penaltyPriceEffect;
            //Bukkit.getConsoleSender().sendMessage("finalPlayerPriceEffect: " + finalPlayerPriceEffect);
            //Apply the finalPlayerPrice to the villager.
            recipe.setSpecialPrice(finalPlayerPriceEffect);
            merchantInventory.getMerchant().setRecipe(i,recipe);
            //Bukkit.getConsoleSender().sendMessage("§e=========================================================================");
        }

        //Bukkit.getConsoleSender().sendMessage("§a=========================================================================");
    }
}
