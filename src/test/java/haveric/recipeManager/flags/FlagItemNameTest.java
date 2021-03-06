package haveric.recipeManager.flags;

import haveric.recipeManager.RecipeManager;
import haveric.recipeManager.RecipeProcessor;
import haveric.recipeManager.Settings;
import haveric.recipeManager.messages.MessageSender;
import haveric.recipeManager.recipes.BaseRecipe;
import haveric.recipeManager.recipes.CraftRecipe;
import haveric.recipeManager.recipes.ItemResult;
import haveric.recipeManagerCommon.recipes.RMCRecipeInfo;
import org.bukkit.Material;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Settings.class, MessageSender.class, RecipeManager.class})
public class FlagItemNameTest extends FlagBaseTest {
    //private FlagItemName flag;

    @Before
    public void setup() {
        //flag = new FlagItemName();
    }

    @Test
    public void onRecipeParse() {
        File file = new File("src/test/resources/recipes/flagItemName/flagItemName.txt");
        RecipeProcessor.reload(null, true, file.getPath(), workDir.getPath());

        Map<BaseRecipe, RMCRecipeInfo> queued = RecipeProcessor.getRegistrator().getQueuedRecipes();

        assertEquals(4, queued.size());

        for (Map.Entry<BaseRecipe, RMCRecipeInfo> entry : queued.entrySet()) {
            CraftRecipe recipe = (CraftRecipe) entry.getKey();

            Args a = ArgBuilder.create().recipe(recipe).build();
            a.setPlayerUUID(testUUID);

            ItemResult result = recipe.getResult(a);

            FlagItemName flag = (FlagItemName) result.getFlag(FlagType.ITEM_NAME);
            flag.onPrepare(a);

            Material resultType = result.getType();
            if (resultType == Material.STONE_SWORD) {
                assertEquals(flag.getItemName(), "Weird Item");
                assertEquals(result.getItemMeta().getDisplayName(), "Weird Item");
            } else if (resultType == Material.IRON_SWORD) {
                assertEquals(flag.getItemName(), "{player}'s Sword");
                assertEquals(result.getItemMeta().getDisplayName(), "TestPlayer's Sword");
            } else if (resultType == Material.GOLD_SWORD) {
                assertEquals(flag.getItemName(), "<gold> Gold");
                assertEquals(result.getItemMeta().getDisplayName(), "§6 Gold");
            } else if (resultType == Material.DIAMOND_SWORD) {
                assertEquals(flag.getItemName(), "Second");
                assertEquals(result.getItemMeta().getDisplayName(), "Second");
            }
        }
    }
}
