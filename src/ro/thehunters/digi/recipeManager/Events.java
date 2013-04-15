package ro.thehunters.digi.recipeManager;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceExtractEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import ro.thehunters.digi.recipeManager.apievents.RMCraftEvent;
import ro.thehunters.digi.recipeManager.apievents.RMCraftEventPost;
import ro.thehunters.digi.recipeManager.apievents.RecipeManagerPrepareCraftEvent;
import ro.thehunters.digi.recipeManager.data.BlockID;
import ro.thehunters.digi.recipeManager.flags.Args;
import ro.thehunters.digi.recipeManager.flags.FlagType;
import ro.thehunters.digi.recipeManager.recipes.FuelRecipe;
import ro.thehunters.digi.recipeManager.recipes.ItemResult;
import ro.thehunters.digi.recipeManager.recipes.SmeltRecipe;
import ro.thehunters.digi.recipeManager.recipes.WorkbenchRecipe;

/**
 * RecipeManager handled events
 */
public class Events implements Listener
{
    protected Events()
    {
        // events are registered in the reload() method
        
        for(World world : Bukkit.getWorlds())
        {
            worldLoad(world);
        }
    }
    
    protected static void reload(CommandSender sender)
    {
        HandlerList.unregisterAll(RecipeManager.events);
        Bukkit.getPluginManager().registerEvents(RecipeManager.events, RecipeManager.getPlugin());
    }
    
    /*
     *  Workbench craft events
     */
    
    @EventHandler(priority = EventPriority.LOW)
    public void prepareCraft(PrepareItemCraftEvent event)
    {
        try
        {
            CraftingInventory inv = event.getInventory();
            
            if(inv.getResult() == null)
            {
                return; // event was canceled by some other plugin
            }
            
            Player player = (event.getView() == null ? null : (Player)event.getView().getPlayer());
            
            if(!RecipeManager.getPlugin().canCraft(player))
            {
                inv.setResult(null);
                return; // player not allowed to craft, stop here
            }
            
            Location location = Workbenches.get(player); // get workbench location or null
            
            if(event.isRepair())
            {
                prepareRepairRecipe(player, inv, location);
                return; // if it's a repair recipe we don't need to move on
            }
            
            Recipe bukkitRecipe = event.getRecipe();
            
            if(bukkitRecipe == null)
            {
                return; // bukkit recipe is null ! skip it
            }
            
            ItemResult result = (inv.getResult() == null ? null : new ItemResult(inv.getResult()));
            ItemStack recipeResult = bukkitRecipe.getResult();
            
            if(prepareSpecialRecipe(player, inv, result, recipeResult))
            {
                return; // stop here if it's a special recipe
            }
            
            WorkbenchRecipe recipe = RecipeManager.getRecipes().getWorkbenchRecipe(bukkitRecipe);
            
            if(recipe == null)
            {
                return; // not a custom recipe or recipe not found, no need to move on
            }
            
            Args a = Args.create().player(player).inventory(inv).location(location).recipe(recipe).build();
            
            result = recipe.getDisplayResult(a);  // get the result from recipe
            
            // Call the RecipeManagerPrepareCraftEvent
            RecipeManagerPrepareCraftEvent callEvent = new RecipeManagerPrepareCraftEvent(recipe, result, player, location);
            Bukkit.getPluginManager().callEvent(callEvent);
            
            result = (callEvent.getResult() == null ? null : new ItemResult(callEvent.getResult()));
            
            if(result != null)
            {
                a = Args.create().player(player).inventory(inv).location(location).recipe(recipe).result(result).build();
                
                if(!recipe.sendPrepare(a))
                {
                    result = null;
                }
                
                // TODO remove ?
                if(result != null)
                {
                    a.sendEffects(a.player(), Messages.FLAG_PREFIX_RECIPE);
                }
                else
                {
                    a.sendReasons(a.player(), Messages.FLAG_PREFIX_RECIPE);
                }
            }
            
            inv.setResult(result);
        }
        catch(Exception e)
        {
            if(event.getInventory() != null)
                event.getInventory().setResult(null);
            
            CommandSender sender = (event.getView() != null && event.getView().getPlayer() instanceof Player ? (Player)event.getView().getPlayer() : null);
            Messages.error(sender, e, event.getEventName() + " cancelled due to error:");
        }
    }
    
    private boolean prepareSpecialRecipe(Player player, CraftingInventory inv, ItemStack result, ItemStack recipeResult)
    {
        if(!result.equals(recipeResult)) // result was processed by the game and it doesn't match the original recipe
        {
            if(!RecipeManager.getSettings().SPECIAL_LEATHER_DYE && recipeResult.equals(Vanilla.RECIPE_LEATHERDYE))
            {
                Messages.CRAFT_SPECIAL_LEATHERDYE.print(player);
                inv.setResult(null);
                return true;
            }
            
            if(!RecipeManager.getSettings().SPECIAL_MAP_CLONING && recipeResult.equals(Vanilla.RECIPE_MAPCLONE))
            {
                Messages.CRAFT_SPECIAL_MAP_CLONING.print(player);
                inv.setResult(null);
                return true;
            }
            
            if(!RecipeManager.getSettings().SPECIAL_MAP_EXTENDING && recipeResult.equals(Vanilla.RECIPE_MAPEXTEND))
            {
                Messages.CRAFT_SPECIAL_MAP_EXTENDING.print(player);
                inv.setResult(null);
                return true;
            }
            
            if(!RecipeManager.getSettings().SPECIAL_FIREWORKS && recipeResult.equals(Vanilla.RECIPE_FIREWORKS))
            {
                Messages.CRAFT_SPECIAL_FIREWORKS.print(player);
                inv.setResult(null);
                return true;
            }
            
            Messages.debug("Results don't match, special recipe ? " + recipeResult + " vs " + result);
        }
        
        return false;
    }
    
    private void prepareRepairRecipe(Player player, CraftingInventory inv, Location location) throws Exception
    {
        if(!RecipeManager.getSettings().SPECIAL_REPAIR)
        {
            if(player != null)
                player.playSound((location == null ? player.getLocation() : location), Sound.NOTE_BASS, 1, 255);
            
            inv.setResult(Tools.createItemStackWithMeta(Material.TRIPWIRE, 0, 0, Messages.CRAFT_REPAIR_DISABLED.get()));
            return;
        }
        
        ItemStack result = inv.getRecipe().getResult();
        
        if(RecipeManager.getSettings().SPECIAL_REPAIR_METADATA)
        {
            ItemStack[] matrix = inv.getMatrix();
            ItemStack[] repaired = new ItemStack[2];
            int repair[] = new int[2];
            int repairIndex = 0;
            
            for(int i = 0; i < matrix.length; i++)
            {
                if(matrix[i] != null && matrix[i].getTypeId() != 0)
                {
                    repair[repairIndex] = i;
                    repaired[repairIndex] = matrix[i];
                    
                    if(++repairIndex > 1)
                        break;
                }
            }
            
            if(repaired[0] != null && repaired[1] != null)
            {
                ItemMeta meta = null;
                
                if(repaired[0].hasItemMeta())
                {
                    meta = repaired[0].getItemMeta();
                }
                else if(repaired[1].hasItemMeta())
                {
                    meta = repaired[1].getItemMeta();
                }
                
                if(meta != null)
                {
                    result = inv.getResult();
                    result.setItemMeta(meta);
                }
            }
        }
        
        RecipeManagerPrepareCraftEvent callEvent = new RecipeManagerPrepareCraftEvent(null, result, player, location);
        Bukkit.getPluginManager().callEvent(callEvent);
        
        result = callEvent.getResult();
        
        if(RecipeManager.getSettings().SOUNDS_REPAIR && result != null && player != null)
        {
            player.playSound(location, Sound.ANVIL_USE, 0.8f, 4.0f);
        }
        
        inv.setResult(result);
    }
    
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void craftFinish(CraftItemEvent event)
    {
        try
        {
            CraftingInventory inv = event.getInventory();
            ItemResult result = (inv.getResult() == null ? null : new ItemResult(inv.getResult()));
            final Player player = (event.getView() == null ? null : (Player)event.getView().getPlayer());
            Location location = Workbenches.get(player);
            
            if(result == null)
            {
                event.setCancelled(true);
                Messages.sendDenySound(player, location);
                return;
            }
            
            Recipe bukkitRecipe = event.getRecipe();
            WorkbenchRecipe recipe = RecipeManager.getRecipes().getWorkbenchRecipe(bukkitRecipe);
            
            if(recipe == null)
                return;
            
            Args a = Args.create().player(player).inventory(inv).recipe(recipe).location(location).build();
            
            if(!recipe.checkFlags(a))
            {
                a.sendReasons(a.player(), Messages.FLAG_PREFIX_RESULT);
                Messages.sendDenySound(player, location);
                event.setCancelled(true);
                return;
            }
            
            result = Recipes.recipeGetResult(a, recipe); // gets the same stored result if event was previously canceled
            
            // Call the PRE event
            RMCraftEvent callEvent = new RMCraftEvent(recipe, result, player, event.getCursor(), event.isShiftClick(), event.isRightClick());
            Bukkit.getPluginManager().callEvent(callEvent);
            
            if(callEvent.isCancelled()) // if event was canceled by some other plugin then cancel this event
            {
                event.setCancelled(true);
                return;
            }
            
            result = callEvent.getResult(); // get the result from the event if it was changed
            
            a = Args.create().player(player).inventory(inv).recipe(recipe).location(location).result(result).build();
            
            int times = craftResult(event, inv, player, recipe, result, a); // craft the result
            
            if(result != null)
            {
                a = Args.create().player(player).inventory(inv).recipe(recipe).location(location).result(result).build();
                
                while(--times >= 0)
                {
                    Recipes.recipeResetResult(a.playerName());
                    
                    a.clear();
                    
                    if(result.sendPrepare(a))
                    {
                        a.sendEffects(a.player(), Messages.FLAG_PREFIX_RESULT);
                    }
                    
                    a.clear();
                    
                    if(recipe.sendCrafted(a))
                    {
                        a.sendEffects(a.player(), Messages.FLAG_PREFIX_RECIPE);
                    }
                    
                    a.clear();
                    
                    if(result.sendCrafted(a))
                    {
                        a.sendEffects(a.player(), Messages.FLAG_PREFIX_RESULT);
                    }
                    
                    // Call the POST event
                    Bukkit.getPluginManager().callEvent(new RMCraftEventPost(recipe, result, player, event.getCursor(), event.isShiftClick(), event.isRightClick()));
                }
            }
            
            new UpdateInventory(player, 2); // update inventory 2 ticks later
        }
        catch(Exception e)
        {
            event.setCancelled(true);
            CommandSender sender = (event.getView() != null && event.getView().getPlayer() instanceof Player ? (Player)event.getView().getPlayer() : null);
            Messages.error(sender, e, event.getEventName() + " cancelled due to error:");
        }
    }
    
    private int craftResult(CraftItemEvent event, CraftingInventory inv, Player player, WorkbenchRecipe recipe, ItemResult result, Args a) throws Exception
    {
        if(!recipe.isMultiResult())
        {
            if(result == null || result.getTypeId() == 0)
            {
                event.setCurrentItem(null);
                return 0;
            }
            
            if(event.isShiftClick())
            {
                if(recipe.hasNoShiftBit() || result.hasNoShiftBit())
                {
                    Messages.CRAFT_RECIPE_FLAG_NOSHIFTCLICK.printOnce(player);
                    
                    event.setCancelled(true); // cancel regardless just to be safe
                    
                    if(Tools.playerCanAddItem(player, result))
                    {
                        player.getInventory().addItem(result);
                        recipe.subtractIngredients(inv, false); // subtract from ingredients manually
                        
                        return 1;
                    }
                    else
                    {
                        return 0;
                    }
                }
                
                int craftAmount = recipe.getCraftableTimes(inv); // Calculate how many times the recipe can be crafted
                
                ItemStack item = result.clone();
                item.setAmount(result.getAmount() * craftAmount);
                
                int space = Tools.playerFreeSpaceForItem(player, item);
                int crafted = Math.min((int)Math.ceil(Float.valueOf(space) / result.getAmount()), craftAmount);
                
                if(crafted > 0)
                {
                    event.setCurrentItem(result);
                    return crafted;
                }
                else
                {
                    return 0;
                }
            }
            else
            {
                ItemStack cursor = event.getCursor();
                ItemStack merged = Tools.mergeItems(cursor, result);
                
                if(merged != null)
                {
                    event.setCurrentItem(result);
                }
                else
                {
                    return 0;
                }
                
                if(recipe.hasFlag(FlagType.INGREDIENTCONDITION))
                {
                    recipe.subtractIngredients(inv, true);
                }
            }
        }
        else
        {
            // more special treatment needed for multi-result ones...
            
            event.setCancelled(true); // need to cancel this from the start.
            
            // check if result is air / recipe failed
            if(result == null || result.getTypeId() == 0)
            {
                Messages.CRAFT_RECIPE_MULTI_FAILED.printOnce(player);
                Messages.sendFailSound(player, a.location());
            }
            else
            {
                if(event.isShiftClick())
                {
                    // TODO remove ?
                    if(!recipe.hasNoShiftBit())
                    {
                        Messages.send(player, "<red>No shift+click is not allowed !"); // TODO to Messages
                        event.setCancelled(true);
                        return 0;
                    }
                    
                    Messages.CRAFT_RECIPE_MULTI_NOSHIFTCLICK.printOnce(player);
                    
                    if(Tools.playerCanAddItem(player, result))
                    {
                        player.getInventory().addItem(result);
                    }
                    else
                    {
                        return 0;
                    }
                }
                else
                {
                    ItemStack cursor = event.getCursor();
                    ItemStack merged = Tools.mergeItems(cursor, result);
                    
                    if(merged != null)
                    {
                        event.setCursor(merged);
                    }
                    else
                    {
                        Messages.CRAFT_RECIPE_MULTI_CURSORFULL.printOnce(player);
                        return 0;
                    }
                }
            }
            
            recipe.subtractIngredients(inv, false); // subtract from ingredients manually
            
            // update displayed result
            // TODO need accurate reading if there is a recipe!
            /*
            if(inv.getResult() != null && inv.getResult().getTypeId() != 0)
            {
                event.setCurrentItem(recipe.getDisplayResult(a));
            }
            else
            {
                event.setCurrentItem(null);
            }
            */
        }
        
        return 1;
    }
    
    /*
     *  Workbenche monitor events
     */
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void inventoryClose(InventoryCloseEvent event)
    {
        HumanEntity human = event.getPlayer();
        
        if(event.getView().getType() == InventoryType.WORKBENCH)
        {
            Workbenches.remove(human);
        }
        
        // TODO re-enable ?
        
        if(RecipeManager.getSettings().FIX_MOD_RESULTS)
        {
            for(ItemStack item : human.getInventory().getContents())
            {
                itemProcess(item);
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void playerIntereact(PlayerInteractEvent event)
    {
        switch(event.getAction())
        {
            case RIGHT_CLICK_BLOCK:
            {
                Player player = event.getPlayer();
                Block block = event.getClickedBlock();
                
                switch(block.getType())
                {
                    case WORKBENCH:
                    case FURNACE:
                    case BURNING_FURNACE:
                    case BREWING_STAND:
                    case ENCHANTMENT_TABLE:
                    case ANVIL:
                    {
                        if(!RecipeManager.getPlugin().canCraft(player))
                        {
                            event.setCancelled(true);
                            return;
                        }
                        
                        if(block.getType() == Material.WORKBENCH)
                            Workbenches.add(event.getPlayer(), event.getClickedBlock().getLocation());
                        
                        break;
                    }
                }
                
                break;
            }
            
            case PHYSICAL:
                break;
            
            default:
            {
                Workbenches.remove(event.getPlayer());
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void playerTeleport(PlayerTeleportEvent event)
    {
        Workbenches.remove(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void playerDeath(PlayerDeathEvent event)
    {
        Workbenches.remove(event.getEntity());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void playerQuit(PlayerQuitEvent event)
    {
        playerDisconnect(event.getPlayer());
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void playerKick(PlayerKickEvent event)
    {
        playerDisconnect(event.getPlayer());
    }
    
    private void playerDisconnect(Player player)
    {
        Workbenches.remove(player);
        Recipes.recipeResetResult(player.getName());
        Messages.clearPlayer(player.getName());
    }
    
    /*
     *  Furnace craft events
     */
    
    @EventHandler
    public void inventoryClick(InventoryClickEvent event)
    {
        try
        {
            Inventory inv = event.getInventory();
            
            if(inv instanceof FurnaceInventory)
            {
                InventoryHolder holder = inv.getHolder();
                
                if(holder != null && holder instanceof Furnace)
                {
                    HumanEntity ent = event.getWhoClicked();
                    
                    if(ent == null || ent instanceof Player == false)
                        return;
                    
                    furnaceClick(event, (Furnace)holder, (Player)ent);
                }
                
                return;
            }
        }
        catch(Exception e)
        {
            event.setCancelled(true);
            CommandSender sender = (event.getWhoClicked() instanceof Player ? (Player)event.getWhoClicked() : null);
            Messages.error(sender, e, event.getEventName() + " cancelled due to error:");
        }
    }
    
    private void furnaceClick(InventoryClickEvent event, Furnace furnace, Player player) throws Exception
    {
        if(!RecipeManager.getPlugin().canCraft(player))
        {
            event.setCancelled(true);
            return;
        }
        
        if(event.getRawSlot() == -1)
        {
            return;
        }
        
        FurnaceInventory inv = furnace.getInventory();
        ItemStack cursor = event.getCursor();
        ItemStack clicked = event.getCurrentItem();
        int slot = event.getRawSlot();
        
        switch(slot)
        {
            case 0: // INGREDIENT slot
            case 1: // FUEL slot
            {
                // TODO middle click detection required
                if(event.isShiftClick() /* || event.isMiddleClick() */)
                {
                    cursor = null; // if you're shift+clicking or using middle click on the slot then you're not placing anything
                }
                
                if(!furnaceModifySlot(event, furnace, inv, player, slot, cursor))
                {
                    Messages.debug("CANCELLED!");
                    event.setCancelled(true);
                    new UpdateInventory(player, 0);
                    return;
                }
                
                return;
            }
            
            case 2: // RESULT slot
            {
                return;
            }
            
            default: // player inventory - Shift+Click handling in player inventory while having furnace UI opened
            {
                if(slot == -999 || !event.isShiftClick() || clicked == null || clicked.getTypeId() == 0)
                {
                    return; // abort if clicked outside of inventory OR not shift+click OR clicked on empty slot
                }
                
                // Get the target slot for the shift+click
                // First checks if the setting is for normal shift+click mode
                // Then checks if the clicked item is a fuel recipe and sends it to fuel slot if so, otherwise to ingredient slot
                // If it's left/right click mode then see if it's right click and send to fuel slot otherwise to ingredient slot
                int targetSlot = ((RecipeManager.getSettings().FURNACE_SHIFT_CLICK == 'f' ? RecipeManager.getRecipes().getFuelRecipe(clicked) != null : event.isRightClick()) ? 1 : 0);
                ItemStack item = inv.getItem(targetSlot); // Get the item at the target slot
                boolean similarItems = clicked.isSimilar(item); // Check if the clicked item is similar to the item at the targeted slot
                
                // Check if it's normal shift+click mode setting and if targeted slot is the fuel slot and there is an item there but it's not similar to our clicked item
                if(RecipeManager.getSettings().FURNACE_SHIFT_CLICK == 'f' && targetSlot == 1 && item != null && !similarItems)
                {
                    targetSlot = 0; // change the target slot to ingredient slot
                    item = inv.getItem(targetSlot); // get the item at the new set slot
                    similarItems = clicked.isSimilar(item); // update similarity check
                }
                
                if(item == null || item.getTypeId() == 0) // If targeted item slot is empty
                {
                    // Check if item is allowed to be placed on that slot
                    if(furnaceModifySlot(event, furnace, inv, player, targetSlot, clicked))
                    {
                        inv.setItem(targetSlot, clicked); // send the item to the slot
                        event.setCurrentItem(null); // clear the clicked slot
                        event.setCancelled(true); // cancel only if we're going to mess with the items
                        new UpdateInventory(player, 0); // update inventory to see the changes client-side
                    }
                }
                else
                {
                    // Otherwise the targeted slot contains some item, need to identify if we can stack over it
                    
                    int maxStack = Math.max(inv.getMaxStackSize(), item.getType().getMaxStackSize()); // see how much we can place on that slot
                    int itemAmount = item.getAmount(); // get how many items there are in the stack
                    
                    if(similarItems && itemAmount < maxStack) // if item has room for more and they're similar
                    {
                        event.setCancelled(true); // cancel only if we're going to mess with the items
                        
                        int amount = itemAmount + clicked.getAmount(); // add the stacks together
                        int diff = amount - maxStack; // check to see if there are any leftovers
                        
                        item.setAmount(Math.min(amount, maxStack)); // set the amount of targeted slot to the added amount OR max stack if it's exceeded
                        
                        if(diff > 0)
                        {
                            clicked.setAmount(diff); // reduce stack amount from clicked stack if there are leftovers
                        }
                        else
                        {
                            event.setCurrentItem(null); // entirely remove the clicked stack if there are no leftovers
                        }
                        
                        new UpdateInventory(player, 0); // update inventory to see the changes client-side
                    }
                }
            }
        }
    }
    
    private boolean furnaceModifySlot(InventoryClickEvent event, Furnace furnace, FurnaceInventory inv, Player player, int slot, ItemStack item) throws Exception
    {
        // TODO NOTE: Don't rely on AMOUNTS until the event is updated!
        
        ItemStack ingredient = Tools.nullItemIfAir(slot == 0 ? item : inv.getSmelting());
        ItemStack fuel = Tools.nullItemIfAir(slot == 1 ? item : inv.getFuel());
        
        /*
        if(slot == 0)
            Messages.debug("<green>Placed ingredient: " + Tools.printItem(ingredient));
        
        if(slot == 1)
            Messages.debug("<green>Placed fuel: " + Tools.printItem(fuel));
        */
        
        SmeltRecipe smeltRecipe = RecipeManager.getRecipes().getSmeltRecipe(ingredient);
        Location location = furnace.getLocation();
        
        if(smeltRecipe == null && fuel != null)
        {
            smeltRecipe = RecipeManager.getRecipes().getSmeltRecipeWithFuel(fuel);
        }
        
        if(smeltRecipe != null)
        {
//            Messages.debug("INGR = " + Tools.printItem(smeltRecipe.getIngredient()) + " | " + Tools.printItem(ingredient) + " | " + Tools.itemSimilarDataWildcard(smeltRecipe.getIngredient(), ingredient));
//            Messages.debug("FUEL = " + Tools.printItem(smeltRecipe.getFuel()) + " | " + Tools.printItem(fuel) + " | " + Tools.itemSimilarDataWildcard(smeltRecipe.getFuel(), fuel));
            
            if(smeltRecipe.hasFuel() && fuel != null && ingredient != null)
            {
                if(!Tools.itemSimilarDataWildcard(smeltRecipe.getIngredient(), ingredient))
                {
                    Messages.SMELT_FUEL_NEEDINGREDIENT.print(player, null, "{ingredient}", Tools.printItem(smeltRecipe.getIngredient()), "{fuel}", Tools.printItem(smeltRecipe.getFuel()));
                    return false;
                }
                
                if(!Tools.itemSimilarDataWildcard(smeltRecipe.getFuel(), fuel))
                {
                    Messages.SMELT_FUEL_NEEDFUEL.print(player, null, "{ingredient}", Tools.printItem(smeltRecipe.getIngredient()), "{fuel}", Tools.printItem(smeltRecipe.getFuel()));
                    return false;
                }
            }
            
            Args a = Args.create().player(player).location(location).inventory(inv).recipe(smeltRecipe).build();
            
            if(smeltRecipe.checkFlags(a))
            {
                a.sendEffects(player, Messages.FLAG_PREFIX_RECIPE);
                a.clear();
                smeltRecipe.sendPrepare(a);
                return true;
            }
            else
            {
                a.sendReasons(player, Messages.FLAG_PREFIX_RECIPE);
                return false;
            }
        }
        
        FuelRecipe fuelRecpe = RecipeManager.getRecipes().getFuelRecipe(fuel);
        
        if(fuelRecpe != null)
        {
            Args a = Args.create().player(player).location(location).inventory(inv).recipe(smeltRecipe).build();
            
            if(fuelRecpe.checkFlags(a))
            {
                a.sendEffects(player, Messages.FLAG_PREFIX_RECIPE);
                a.clear();
                fuelRecpe.sendPrepare(a);
                return true;
            }
            else
            {
                a.sendReasons(player, Messages.FLAG_PREFIX_RECIPE);
                return false;
            }
        }
        
        return true;
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void furnaceBurn(FurnaceBurnEvent event)
    {
        Messages.debug("BURN EVENT");
        
        try
        {
//            BlockID id = new BlockID(event.getBlock());
            
            // TODO if reverting to FurnaceWorker storage, check if exists and add furnace!
            
//            FurnaceData data = Furnaces.get(id);
            
            final FuelRecipe fuelRecipe = RecipeManager.getRecipes().getFuelRecipe(event.getFuel());
            Furnace furnace = (Furnace)event.getBlock().getState();
            
            if(fuelRecipe != null)
            {
                // Fuel recipe
                int time = (fuelRecipe.hasFlag(FlagType.REMOVE) ? 0 : fuelRecipe.getBurnTicks());
                
                Args a = Args.create().location(event.getBlock().getLocation()).recipe(fuelRecipe).inventory(furnace.getInventory()).build();
                
                fuelRecipe.sendCrafted(a);
                
                event.setBurnTime(time);
                event.setBurning(time > 0);
            }
            else
            {
                // Smelting recipe with specific fuel
                
                ItemStack ingredient = furnace.getInventory().getSmelting();
                SmeltRecipe smeltRecipe = RecipeManager.getRecipes().getSmeltRecipe(ingredient);
                
                if(smeltRecipe != null)
                {
                    if(!smeltRecipe.hasFuel() || !smeltRecipe.getFuel().isSimilar(event.getFuel()))
                    {
                        event.setCancelled(true);
                    }
                    else
                    {
                        event.setBurning(true);
                        event.setBurnTime((int)Math.ceil(smeltRecipe.getCookTime()) * 20);
                    }
                }
            }
            
            boolean running = !event.isCancelled() && event.isBurning();
            
//            data.setBurnTime(running ? event.getBurnTime() : 0);
            
//            Messages.debug("furnace set burn time to = " + data.getBurnTime());
            
            if(running)
            {
                FurnaceWorker.start(); // make sure it's started
                
//              new FurnaceBurnOut(event.getBlock(), event.getFuel(), event.getBurnTime());
            }
        }
        catch(Exception e)
        {
            event.setCancelled(true);
            Messages.error(null, e, event.getEventName() + " cancelled due to error:");
        }
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void furnaceSmelt(FurnaceSmeltEvent event)
    {
        try
        {
            SmeltRecipe recipe = RecipeManager.getRecipes().getSmeltRecipe(event.getSource());
            
            if(recipe == null)
                return;
            
            FurnaceInventory inventory = null;
            
            if(event.getBlock() instanceof Furnace)
                inventory = ((Furnace)event.getBlock()).getInventory();
            
            Args a = Args.create().location(event.getBlock().getLocation()).recipe(recipe).inventory(inventory).result(event.getResult()).build();
            
            recipe.sendCrafted(a);
        }
        catch(Exception e)
        {
            event.setCancelled(true);
            Messages.error(null, e, event.getEventName() + " cancelled due to error:");
        }
    }
    
    @EventHandler(priority = EventPriority.LOW)
    public void furnaceTakeResult(FurnaceExtractEvent event)
    {
        if(event.getExpToDrop() == 0)
            return;
        
        BlockState state = event.getBlock().getState();
        
        if(state instanceof Furnace == false)
            return; // highly unlikely but better safe than sorry
            
        Furnace furnace = (Furnace)state;
        ItemStack ingredient = furnace.getInventory().getSmelting();
        SmeltRecipe smeltRecipe = null;
        
        if(ingredient == null || ingredient.getTypeId() == 0)
        {
            ItemStack result = furnace.getInventory().getResult();
            
            if(result == null)
                return;
            
            for(SmeltRecipe r : RecipeManager.getRecipes().indexSmelt.values())
            {
                if(result.isSimilar(r.getResult()))
                {
                    smeltRecipe = r;
                    break;
                }
            }
        }
        else
        {
            smeltRecipe = RecipeManager.getRecipes().getSmeltRecipe(ingredient);
        }
        
        if(smeltRecipe != null)
        {
            event.setExpToDrop(0);
        }
    }
    
    // TODO !!!!!!!
    @EventHandler
    public void inventoryPickup(InventoryPickupItemEvent event)
    {
        Messages.debug("item = " + event.getItem());
        event.setCancelled(true);
    }
    
    /*
     *  Furnace monitor events
     */
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockPlace(BlockPlaceEvent event)
    {
        Block block = event.getBlock();
        
        switch(block.getType())
        {
            case BURNING_FURNACE:
            case FURNACE:
            {
                // TODO set furnace title ?
                /*
                BlockState state = block.getState();
                
                if(state instanceof Furnace == false)
                    return;
                
                Furnace furnace = (Furnace)state;
                
                furnace.
                */
//              Furnaces.add(BlockID.fromBlock(block), event.getPlayer().getName());
                
                Messages.debug("added furnace at " + BlockID.fromBlock(block).getCoordsString());
                FurnaceWorker.addFurnace(BlockID.fromBlock(block));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void blockBreak(BlockBreakEvent event)
    {
        Block block = event.getBlock();
        
        switch(block.getType())
        {
            case BURNING_FURNACE:
            case FURNACE:
            {
                Messages.debug("removed furnace at " + BlockID.fromBlock(block).getCoordsString());
                FurnaceWorker.removeFurnace(BlockID.fromBlock(block));
                
//                Furnaces.remove(BlockID.fromBlock(block));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void worldLoad(WorldLoadEvent event)
    {
        worldLoad(event.getWorld());
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void chunkLoad(ChunkLoadEvent event)
    {
        if(!event.isNewChunk())
            findFurnaces(event.getChunk(), true);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void chunkUnload(ChunkUnloadEvent event)
    {
        findFurnaces(event.getChunk(), false);
    }
    
    protected void worldLoad(World world)
    {
        Chunk chunks[] = world.getLoadedChunks();
        
        for(Chunk chunk : chunks)
        {
            findFurnaces(chunk, true);
        }
    }
    
    private void findFurnaces(final Chunk chunk, final boolean add)
    {
        if(chunk == null)
            return;
        
        BlockState[] tileEntities = chunk.getTileEntities();
        
        for(BlockState state : tileEntities)
        {
            if(state != null && state instanceof Furnace)
            {
                if(add)
                {
                    /*
                    Furnaces.addIfNotExists(state.getLocation());
                    */
                    
                    if(state.getType() == Material.BURNING_FURNACE)
                    {
                        Messages.debug("added furnace at " + new BlockID(state.getLocation()).getCoordsString());
                        
                        FurnaceWorker.addFurnace(BlockID.fromLocation(state.getLocation()));
                    }
                }
                else
                {
                    /*
                    Furnaces.remove(state.getLocation());
                    */
                    
                    Messages.debug("removed furnace at " + new BlockID(state.getLocation()).getCoordsString());
                    
                    FurnaceWorker.removeFurnace(BlockID.fromLocation(state.getLocation()));
                }
            }
        }
    }
    
    /*
     *  Marked item monitor events
     */
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void playerItemHeld(PlayerItemHeldEvent event)
    {
        Player player = event.getPlayer();
        
        if(RecipeManager.getSettings().UPDATE_BOOKS)
        {
            RecipeManager.getRecipeBooks().updateBook(player, player.getInventory().getItem(event.getNewSlot()));
        }
        
        if(RecipeManager.getSettings().FIX_MOD_RESULTS)
        {
            itemProcess(event.getPlayer().getInventory().getItem(event.getNewSlot()));
        }
    }
    
    private void itemProcess(ItemStack item)
    {
        if(item == null || !item.hasItemMeta())
        {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        List<String> lore = item.getItemMeta().getLore();
        
        if(lore == null || lore.isEmpty())
        {
            return;
        }
        
        for(int i = 0; i < lore.size(); i++)
        {
            String s = lore.get(i);
            
            if(s != null && s.startsWith(Recipes.RECIPE_ID_STRING))
            {
                lore.remove(i);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
        }
    }
    
    /*
     *  Update check notifier
     */
    
    @EventHandler
    public void playerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        
        if(player.isOp())
        {
            final String newVersion = UpdateChecker.getNewVersion();
            
            if(newVersion != null)
            {
                Messages.send(player, "<green>New version available: " + newVersion + " ! You're using " + RecipeManager.getPlugin().getDescription().getVersion());
                Messages.send(player, "<green>Grab it at: " + UpdateChecker.getNewLink());
            }
        }
    }
    
    /*
     *  Update inventory inner helper class
     */
    
    private class UpdateInventory extends BukkitRunnable
    {
        private final Player player;
        
        public UpdateInventory(Player player, int ticks)
        {
            this.player = player;
            
            if(ticks <= 0)
            {
                run();
            }
            else
            {
                runTaskLater(RecipeManager.getPlugin(), ticks);
            }
        }
        
        @SuppressWarnings("deprecation")
        @Override
        public void run()
        {
            player.updateInventory();
        }
    }
}