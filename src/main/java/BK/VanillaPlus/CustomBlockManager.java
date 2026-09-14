package BK.VanillaPlus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class CustomBlockManager implements Listener
{
    private final VanillaPlus plugin;
    private boolean stopReInjection;

    public CustomBlockManager(VanillaPlus plugin) {this.plugin = plugin;}

    public void RecipeManager_Init()
    {
        //Set the default
        stopReInjection = false;
        //Get plugin version
        String targetVersion = plugin.getDescription().getVersion();
        //Get the mini block file version
        String miniBlocksVersion = plugin.configLoader.MiniBlocksFile.getString("Mini-Blocks-Version");
        //Check it ain't null
        if(miniBlocksVersion != null)
        {
            //If the mini blocks file version matches the plugin version don't reinject
            if(targetVersion.equals(miniBlocksVersion))
            {
                //Check if the server just booted if so we need to inject
                if(plugin.bootInject)
                {
                    stopReInjection = false;
                }
                else
                {
                    Bukkit.getConsoleSender().sendMessage("§aStop removal and re-injection of the mini blocks the version matches...");
                    stopReInjection = true;
                }
            }
            else
            {
                //The version doesn't match we need to update the file. and inject the blocks
                plugin.configUpdater.update_MiniBlocks();
            }
        }

        stopReInjection = true;//DEV TESTING FOR ADDING NEW BLOCKS TO THE MINI BLOCKS FILE. IF SET TO FALSE IT WON'T STOP RE-INJECTION ON RELOAD.

        //Check if the server isn't just booting up. If the server is just booting up we don't need to remove anything
        if(!plugin.bootInject)
        {
            //First wipe all recipes created by this plugin
            Iterator<Recipe> it = Bukkit.recipeIterator();
            int RecipesRemoved = 0;
            while (it.hasNext())
            {
                Recipe VP_Recipe = it.next();

                //Check the shaped, shapeless, stonecutting and furnace recipes
                if (VP_Recipe instanceof Keyed keyedRecipe)
                {
                    if (keyedRecipe.getKey().getNamespace().equalsIgnoreCase(plugin.getName().toLowerCase()))
                    {
                        //Get the keyName
                        String recipeKeyName = keyedRecipe.getKey().getKey();
                        //If stopReInjection is true and recipeKeyName starts with default skip removing this recipe
                        if(stopReInjection && recipeKeyName.startsWith("default_"))
                        {
                            continue;
                        }
                        it.remove();
                        RecipesRemoved++;
                    }
                }
            }
            Bukkit.getConsoleSender().sendMessage("§aRemoved §e" + RecipesRemoved + " §arecipes");
        }

        //Set bootInject to false so we don't re-inject the default mini blocks upon reload, And lagg the server every reload
        plugin.bootInject = false;


        //Check if Mini Blocks are enabled
        if(plugin.configLoader.EnableMiniBlocks)
        {
            MiniBlockRecipes();
        }
        //Check if custom recipes are enabled
        if(plugin.configLoader.Enable_Custom_Recipes)
        {
            registerRecipes();
        }

        if(plugin.configLoader.EnableHardenedPickAxe)
        {
            registerHardenedPickaxeRecipe();
        }
    }

    //////////////////////////////////////// Mini Block Recipes/////////////////////////////////////////////

    //Create all the custom micro-block stonecutter recipes.
    private void MiniBlockRecipes()
    {
        ConfigurationSection[] BlockFiles = {
                plugin.configLoader.MiniBlocksFile.getConfigurationSection("mini-blocks"),
                plugin.configLoader.CustomMiniBlocksFile.getConfigurationSection("custom-mini-blocks")
        };

        int i = 0;
        for (ConfigurationSection BlockFile : BlockFiles)
        {
            //check if BlockFile is null if so skip to the next file
            if (BlockFile == null) continue;
            if(BlockFile.equals(BlockFiles[0]) && stopReInjection)
            {
                i++;
                continue;
            }
            int RecipesAdded = 0;
            //Do this for each key in mini-blocks.yml
            for (String key : BlockFile.getKeys(false))
            {
                //Get all the values for this key
                String displayName = BlockFile.getString(key + ".display-name");
                String InputMaterialName = BlockFile.getString(key + ".input-material");
                String TextureHash = BlockFile.getString(key + ".texture-hash");
                int blockAmount = BlockFile.getInt(key + ".amount", 1);

                //If InputMaterialName or TextureHash is null skip this key
                if (InputMaterialName == null || TextureHash == null) continue;

                //Create the material
                Material inputMaterial = Material.matchMaterial(InputMaterialName);
                if (inputMaterial == null)
                {
                    //Created material was null send msg to console
                    plugin.getLogger().severe("Could not match material: " + InputMaterialName);
                    continue;
                }

                if(BlockFile.equals(BlockFiles[0]))
                {
                    registerMicroBlock(key,displayName,inputMaterial,TextureHash,blockAmount,true);
                }
                else
                {
                    registerMicroBlock(key,displayName,inputMaterial,TextureHash,blockAmount,false);
                }

                RecipesAdded++;
            }
            if(i == 0)
            {
                Bukkit.getConsoleSender().sendMessage("§aInjected §e" + RecipesAdded + " §amini block recipes");
            }
            else if(i == 1)
            {
                //If stopReInjection is true send the msg
                if(stopReInjection)
                {
                    Bukkit.getConsoleSender().sendMessage("§aNo need to re-inject mini block recipes");
                }
                Bukkit.getConsoleSender().sendMessage("§aInjected §e" + RecipesAdded + " §aCustom mini block recipes");
            }
            i++;
        }
    }

    //Register all the blocks passed to it
    private void registerMicroBlock(String key, String displayName, Material inputMaterial, String textureHash, int BlockAmount, boolean isNotCustomBlockFile)
    {
        //Create the ItemStack
        ItemStack miniItem = createMicroBlockHead(textureHash, displayName);
        //Set how many you get
        miniItem.setAmount(BlockAmount);
        //Give the recipe a key
        NamespacedKey recipeKey;
        //If it's our default miniBlocks file we need to add default to it so we can later differentiate them from the custom ones
        if(isNotCustomBlockFile)
        {
            recipeKey = new NamespacedKey(plugin, "default_" + key);
        }
        else
        {
            recipeKey = new NamespacedKey(plugin, key);
        }

        //Set which inputIngredient makes this new recipe
        RecipeChoice inputIngredient = new RecipeChoice.MaterialChoice(inputMaterial);

        //Finalize and add the recipe
        StonecuttingRecipe stonecutterRecipe = new StonecuttingRecipe(recipeKey, miniItem, inputIngredient);
        Bukkit.addRecipe(stonecutterRecipe);
    }

    //Dynamically constructs a permanent player head item using a direct Mojang texture hash.
    private ItemStack createMicroBlockHead(String textureHash, String displayName)
    {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null)
        {
            //Create a mock UUID from the texture hash
            UUID profileId = UUID.nameUUIDFromBytes(textureHash.getBytes());

            //Create a mock profile with no spaces in the name to keep mojang happy
            PlayerProfile profile = Bukkit.createPlayerProfile(profileId, "VP_MicroBlock");
            PlayerTextures textures = profile.getTextures();

            try
            {
                URI uri = new URI("https://textures.minecraft.net/texture/" + textureHash);
                URL url = uri.toURL();
                textures.setSkin(url);
            }
            catch (URISyntaxException e)
            {
                plugin.getLogger().severe("Failed to parse micro-block texture URL for: " + displayName + ". Texture server might be down or the hash is invalid.");
                throw new RuntimeException(e);
            }
            catch (MalformedURLException e)
            {
                plugin.getLogger().severe("Invalid URL format for micro-block texture: " + displayName);
                throw new RuntimeException(e);
            }

            profile.setTextures(textures);
            meta.setOwnerProfile(profile);

            // This is the gorgeous name players see in-game
            meta.displayName(Component.text(displayName).decoration(TextDecoration.ITALIC, false));
            head.setItemMeta(meta);
        }

        return head;
    }

    ////////////////////////////////////////Custom recipes/////////////////////////////////////////////
    private void registerRecipes()
    {
        //get the recipes
        ConfigurationSection Custom_Recipes = plugin.configLoader.Custom_Recipes;

        if (Custom_Recipes != null)
        {
            for (String recipeKey : Custom_Recipes.getKeys(false))
            {
                // Skip the Explanation-ITEM so it doesn't try to register a "dummy" recipe
                if (recipeKey.equalsIgnoreCase("Explanation-ITEM"))
                {
                    continue;
                }

                Bukkit.getConsoleSender().sendMessage("§aFound custom recipe key: §e" + recipeKey);

                ConfigurationSection section = Custom_Recipes.getConfigurationSection(recipeKey);
                if (section == null) continue;

                //Get Result and Amount, so we know which item and how many to give
                String resultName = section.getString("Result", "DIRT");
                Bukkit.getConsoleSender().sendMessage("§aAttempting to register: §e" + recipeKey + " §a(Result: §e" + resultName + "§a)");
                int amount = section.getInt("Amount", 1);
                Material resultMat = Material.matchMaterial(resultName);

                if (resultMat == null)
                {
                    //no item matched the item name used in result so display a msg
                    Bukkit.getConsoleSender().sendMessage("§aSkipping recipe: §e" + recipeKey + " §aInvalid Material");
                    continue;
                }

                //Setup the Item and Custom Name
                ItemStack resultItem = new ItemStack(resultMat, amount);
                //setup base meta
                ItemMeta meta = resultItem.getItemMeta();

                if (meta != null)
                {
                    //Check if we need to do special Player Head logic
                    if (resultMat.equals(Material.PLAYER_HEAD))
                    {
                        String Player_Head_Name = section.getString("Player-Head-Name", "");
                        if(!Player_Head_Name.isEmpty())
                        {
                            OfflinePlayer Offline_Player_Head = Bukkit.getOfflinePlayer(Player_Head_Name);
                            // We "cast" the existing meta to SkullMeta to access head features
                            SkullMeta skullMeta = (SkullMeta) meta;
                            skullMeta.setOwningPlayer(Offline_Player_Head);
                        }
                    }
                    //Set Display name
                    String customName = section.getString("Display-Name", "");
                    if (!customName.isEmpty())
                    {
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', customName));
                        resultItem.setItemMeta(meta);
                    }
                }

                //Create the Recipe
                NamespacedKey key = new NamespacedKey(plugin, "custom_" + recipeKey.toLowerCase());
                ShapedRecipe recipe = new ShapedRecipe(key, resultItem);

                //Set the Shape
                List<String> shape = section.getStringList("Shape");
                if (shape.size() == 3)
                {
                    recipe.shape(shape.get(0), shape.get(1), shape.get(2));
                }
                else
                {
                    plugin.getLogger().severe("Recipe '" + recipeKey + "' has an invalid shape (must be 3 rows).");
                    continue;
                }

                //Map the Ingredients
                ConfigurationSection ingredients = section.getConfigurationSection("Ingredients");
                if (ingredients != null)
                {
                    for (String charKey : ingredients.getKeys(false))
                    {
                        String matName = ingredients.getString(charKey);
                        if(matName != null)
                        {
                            Material mat = Material.matchMaterial(matName);

                            if (mat != null && !mat.isAir())
                            {
                                recipe.setIngredient(charKey.charAt(0), mat);
                            }
                        }
                        else
                        {
                            plugin.getLogger().severe("Recipe by name of: " + recipeKey + " is malformed");
                        }
                    }
                }

                //Register it!
                Bukkit.getConsoleSender().sendMessage("§aSuccessfully registered custom recipe: §e" + recipeKey);
                Bukkit.addRecipe(recipe);
            }
            Bukkit.getConsoleSender().sendMessage("§a--- Finished Custom Recipe Registration ---");
        }
        else
        {
            plugin.getLogger().severe("Could not find 'Custom-Recipes' section in config.yml!");
        }
    }

    ///////////////////////////////////// PLAYER HEADS DROP //////////////////////////////////////////////
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event)
    {
        //Check if we should drop player heads upon getting killed
        if(!plugin.configLoader.Enable_Players_Drop_Head)return;

        //Check if the player is killed or died of other reasons
        if(event.getEntity().getKiller() != null)
        {
            //Create the head item
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            //Get the SkullMeta
            SkullMeta skullMeta = (SkullMeta) head.getItemMeta();
            //Set the owner of the head
            skullMeta.setOwningPlayer(event.getPlayer());
            //Apply display name and lore
            skullMeta.setDisplayName("§e" + event.getPlayer().getName() + "'s Head");
            //Apply the owning player to the head item
            head.setItemMeta(skullMeta);
            //Add the created head to the drops of the killed player
            event.getDrops().add(head);
        }
    }

    ///////////////////////////////////// CUSTOM PICKAXE //////////////////////////////////////////////
    private void registerHardenedPickaxeRecipe()
    {
        NamespacedKey pickKey = new NamespacedKey(plugin, "Custom_Hardened_Netherite_Pickaxe");

        // Build the ItemStack
        ItemStack resultItem = new ItemStack(Material.NETHERITE_PICKAXE, 1);
        ItemMeta meta = resultItem.getItemMeta();

        if (meta != null)
        {
            //Apply your custom PDC tag
            PersistentDataContainer pickPDC = meta.getPersistentDataContainer();
            pickPDC.set(pickKey, PersistentDataType.INTEGER, 1);

            //Set the display name
            meta.setDisplayName("Hardened Netherite Pickaxe");
            resultItem.setItemMeta(meta);
        }

        //Create the shaped recipe
        ShapedRecipe recipe = new ShapedRecipe(pickKey, resultItem);
        recipe.shape(
                "ABA",
                "BCB",
                "ABA"
        );

        //Map the ingredients
        recipe.setIngredient('A', Material.NETHER_STAR);
        recipe.setIngredient('B', Material.NETHERITE_INGOT);
        recipe.setIngredient('C', Material.NETHERITE_PICKAXE);

        //Register it to the server
        Bukkit.addRecipe(recipe);

        Bukkit.getConsoleSender().sendMessage("§aHardened Netherite Pickaxe has been injected");
    }
}