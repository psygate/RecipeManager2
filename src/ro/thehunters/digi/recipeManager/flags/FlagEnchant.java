package ro.thehunters.digi.recipeManager.flags;

import org.bukkit.enchantments.Enchantment;

import ro.thehunters.digi.recipeManager.Files;
import ro.thehunters.digi.recipeManager.RecipeErrorReporter;

public class FlagEnchant extends Flag
{
    public FlagEnchant()
    {
        type = FlagType.ENCHANT;
    }
    
    @Override
    public void onRemove()
    {
        getResult().getEnchantments().clear();
    }
    
    @Override
    public boolean onParse(String value)
    {
        String[] split = value.split(" ");
        value = split[0].trim();
        
        Enchantment ench = Enchantment.getByName(value);
        
        if(ench == null)
        {
            RecipeErrorReporter.error("Flag " + type + " has invalid enchantment: " + value, "Read '" + Files.FILE_INFO_NAMES + "' for enchantment names.");
            return false;
        }
        
        int level = ench.getStartLevel();
        
        if(split.length > 1)
        {
            value = split[1].trim();
            
            if(!value.equalsIgnoreCase("max"))
            {
                try
                {
                    level = Integer.valueOf(value);
                }
                catch(Exception e)
                {
                    RecipeErrorReporter.error("Flag " + type + " has invalid enchantment level number!");
                    return false;
                }
            }
            else
            {
                level = ench.getMaxLevel();
            }
        }
        
        getResult().addUnsafeEnchantment(ench, level);
        return true;
    }
}