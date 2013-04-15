package ro.thehunters.digi.recipeManager;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.mutable.MutableFloat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import ro.thehunters.digi.recipeManager.data.BlockID;
import ro.thehunters.digi.recipeManager.recipes.SmeltRecipe;

class FurnaceWorker implements Runnable
{
    private static final Map<BlockID, MutableFloat> furnaces = new HashMap<BlockID, MutableFloat>();
    
    private final float ticks;
    private static BukkitTask task;
    
    static void init()
    {
    }
    
    private FurnaceWorker()
    {
        stop();
        ticks = (float)(10.0 / RecipeManager.getSettings().FURNACE_TICKS);
        task = Bukkit.getScheduler().runTaskTimer(RecipeManager.getPlugin(), this, 0, RecipeManager.getSettings().FURNACE_TICKS);
    }
    
    protected static void start()
    {
        if(!isRunning())
            new FurnaceWorker();
    }
    
    protected static void restart()
    {
        stop();
        new FurnaceWorker();
    }
    
    protected static boolean isRunning()
    {
        return task != null;
    }
    
    protected static void stop()
    {
        if(isRunning())
        {
            task.cancel();
            task = null;
        }
    }
    
    protected static void clean()
    {
        stop();
        furnaces.clear();
    }
    
    protected static boolean hasFurnace(BlockID id)
    {
        return furnaces.containsKey(id);
    }
    
    protected static void addFurnace(BlockID id)
    {
        furnaces.put(id, new MutableFloat());
    }
    
    protected static void removeFurnace(BlockID id)
    {
        furnaces.remove(id);
    }
    
    @Override
    public void run()
    {
        Iterator<Entry<BlockID, MutableFloat>> iterator = furnaces.entrySet().iterator();
        Entry<BlockID, MutableFloat> entry;
        MutableFloat data;
        Furnace furnace;
        FurnaceInventory inventory;
        ItemStack smelt;
        ItemStack result;
        ItemStack recipeResult;
        SmeltRecipe recipe;
        float time;
        
        while(iterator.hasNext())
        {
            entry = iterator.next();
            
            furnace = convertBlockIdToFurnace(entry.getKey()); // convert blockID to Furnace block object
            
            if(furnace == null) // the burning furnace no longer exists for whatever reason
            {
                iterator.remove();
                continue;
            }
            
            inventory = furnace.getInventory();
            smelt = inventory.getSmelting();
            data = entry.getValue();
            
            if(smelt == null || smelt.getType() == Material.AIR) // if there's nothing to smelt, skip furnace
            {
                data.setValue(0);
                continue;
            }
            
            recipe = RecipeManager.getRecipes().getSmeltRecipe(smelt);
            
            if(recipe == null || !recipe.hasCustomTime()) // No custom recipe for item or it has default time
            {
                data.setValue(0);
                continue;
            }
            
            recipeResult = recipe.getResult();
            result = inventory.getResult();
            
            // If we have a result and it's not the same as what we're making or it's at max stack size then skip furnace
            if(result != null && (!recipeResult.isSimilar(result) || result.getAmount() >= result.getType().getMaxStackSize()))
            {
                data.setValue(0);
                continue;
            }
            
            if(recipe.getMinTime() <= 0.0) // instant smelting
            {
                furnace.setCookTime((short)200);
            }
            else
            {
                time = data.intValue();
                
                if(time >= 200 || furnace.getCookTime() == 0 || furnace.getCookTime() >= 200)
                {
                    data.setValue(0);
                }
                else
                {
                    time = time + (ticks / recipe.getCookTime()); // (recipe.hasFuel() ? data.getBurnTime() : recipe.getCookTime()));
                    
                    furnace.setCookTime((short)Math.min(Math.max(Math.round(time), 1), 199));
                    
                    data.setValue(time);
                }
            }
        }
        
        iterator = null;
    }
    
    private Furnace convertBlockIdToFurnace(BlockID blockID)
    {
        Block block = blockID.toBlock();
        
        if(block == null || block.getType() != Material.BURNING_FURNACE) // not a running furnace
            return null;
        
        BlockState blockState = block.getState();
        
        if(blockState instanceof Furnace == false) // not really a furnace
            return null;
        
        Furnace furnace = (Furnace)blockState;
        
        if(furnace.getBurnTime() <= 0) // furnace is not running
            return null;
        
        return furnace;
    }
}