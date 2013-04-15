package ro.thehunters.digi.recipeManager.recipes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.mutable.MutableInt;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;

import ro.thehunters.digi.recipeManager.Messages;
import ro.thehunters.digi.recipeManager.Tools;
import ro.thehunters.digi.recipeManager.flags.FlagDescription;
import ro.thehunters.digi.recipeManager.flags.FlagType;
import ro.thehunters.digi.recipeManager.flags.Flags;

public class CombineRecipe extends WorkbenchRecipe
{
    private List<ItemStack> ingredients;
    
    private ShapelessRecipe bukkitRecipe;
    
    public CombineRecipe()
    {
    }
    
    public CombineRecipe(ShapelessRecipe recipe)
    {
        setIngredients(recipe.getIngredientList());
        setResult(recipe.getResult());
    }
    
    public CombineRecipe(BaseRecipe recipe)
    {
        super(recipe);
    }
    
    public CombineRecipe(Flags flags)
    {
        super(flags);
    }
    
    public List<ItemStack> getIngredients()
    {
        return ingredients;
    }
    
    public void addIngredient(Material type)
    {
        addIngredient(1, type, (short)-1);
    }
    
    public void addIngredient(Material type, short data)
    {
        addIngredient(1, type, data);
    }
    
    public void addIngredient(ItemStack ingredient)
    {
        addIngredient(ingredient.getAmount(), ingredient.getType(), ingredient.getDurability());
    }
    
    public void addIngredient(int amount, Material type, short data)
    {
        if(ingredients == null)
        {
            ingredients = new ArrayList<ItemStack>();
        }
        
        if((ingredients.size() + amount) > 9) // check if they're more than they should...
        {
            throw new IllegalArgumentException("Recipe can't have more than 9 ingredients!");
        }
        
        while(amount-- > 0)
        {
            ingredients.add(new ItemStack(type, 1, data));
        }
        
        sort();
    }
    
    public void setIngredients(List<ItemStack> ingredients)
    {
        // unstack ingredients
        this.ingredients = new ArrayList<ItemStack>();
        int amount;
        
        for(ItemStack ingredient : ingredients)
        {
            amount = ingredient.getAmount();
            
            while(amount-- > 0)
            {
                this.ingredients.add(new ItemStack(ingredient.getType(), 1, ingredient.getDurability()));
            }
        }
        
        if(this.ingredients.size() > 9) // check if they're more than they should...
            throw new IllegalArgumentException("Recipe can't have more than 9 ingredients!");
        
        sort();
    }
    
    private void sort()
    {
        // sort by type and data
        Tools.sortIngredientList(ingredients);
        
        // build hashcode
        StringBuilder str = new StringBuilder("combine");
        
        for(ItemStack item : ingredients)
        {
            str.append(item.getTypeId()).append(':').append(item.getDurability()).append(';');
            
            // TODO remove
            if(item.getDurability() == -1)
            {
                Messages.debug("found an item with data value -1 | result=" + getFirstResult());
            }
        }
        
        hash = str.toString().hashCode();
    }
    
    @Override
    public ShapelessRecipe getBukkitRecipe()
    {
        return bukkitRecipe == null ? toShapelessRecipe() : bukkitRecipe;
    }
    
    public ShapelessRecipe toShapelessRecipe()
    {
        ShapelessRecipe bukkitRecipe = new ShapelessRecipe(Tools.createItemRecipeId(getFirstResult(), getIndex()));
        
        for(ItemStack item : ingredients)
        {
            bukkitRecipe.addIngredient(item.getAmount(), item.getType(), item.getDurability());
        }
        
        return bukkitRecipe;
    }
    
    public boolean hasIngredients()
    {
        return ingredients != null && !ingredients.isEmpty();
    }
    
    @Override
    public boolean isValid()
    {
        return hasIngredients() && hasResults();
    }
    
    @Override
    public RecipeType getType()
    {
        return RecipeType.COMBINE;
    }
    
    @Override
    public String printBookIndex()
    {
        return Tools.getItemName(getFirstResult());
    }
    
    @Override
    public String printBook()
    {
        StringBuilder s = new StringBuilder(256);
        
        s.append(Messages.RECIPEBOOK_HEADER_SHAPELESS.get());
        
        s.append('\n').append(Tools.printItem(getFirstResult(), ChatColor.DARK_GREEN, null, true));
        
        if(isMultiResult())
        {
            s.append('\n').append(Messages.RECIPEBOOK_MORERESULTS.get("{amount}", (getResults().size() - 1)));
        }
        
        if(hasFlag(FlagType.DESCRIPTION))
        {
            s.append('\n').append(ChatColor.DARK_BLUE).append(Tools.parseColors(getFlag(FlagDescription.class).getDescription(), false));
        }
        
        s.append('\n');
        s.append('\n').append(Messages.RECIPEBOOK_HEADER_INGREDIENTS.get()).append(ChatColor.BLACK);
        
        Map<ItemStack, MutableInt> items = new HashMap<ItemStack, MutableInt>();
        
        for(ItemStack item : ingredients)
        {
            MutableInt i = items.get(item);
            
            if(i == null)
            {
                i = new MutableInt();
                items.put(item, i);
            }
            
            i.add(item.getAmount());
        }
        
        for(Entry<ItemStack, MutableInt> e : items.entrySet())
        {
            ItemStack item = e.getKey();
            item.setAmount(e.getValue().intValue());
            s.append('\n').append(Tools.printItem(item, ChatColor.RED, ChatColor.BLACK, true));
        }
        
        return s.toString();
    }
}