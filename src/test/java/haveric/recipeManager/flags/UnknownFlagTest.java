package haveric.recipeManager.flags;

import haveric.recipeManager.RecipeProcessor;
import haveric.recipeManager.recipes.BaseRecipe;
import haveric.recipeManager.recipes.CraftRecipe;
import haveric.recipeManager.recipes.ItemResult;
import haveric.recipeManagerCommon.recipes.RMCRecipeInfo;
import org.bukkit.Material;
import org.junit.Test;

import java.io.File;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class UnknownFlagTest extends FlagBaseTest {
    @Test
    public void onRecipeParse() {
        File file = new File("src/test/resources/recipes/unknownFlag/unknownFlag.txt");
        RecipeProcessor.reload(null, true, file.getPath(), workDir.getPath());

        Map<BaseRecipe, RMCRecipeInfo> queued = RecipeProcessor.getRegistrator().getQueuedRecipes();

        assertEquals(3, queued.size());
        for (Map.Entry<BaseRecipe, RMCRecipeInfo> entry : queued.entrySet()) {
            CraftRecipe recipe = (CraftRecipe) entry.getKey();
            ItemResult result = recipe.getResults().get(0);
            Material resultType = result.getType();

            if (resultType == Material.IRON_SWORD) {
                assertEquals(0, result.getFlags().get().size());
            } else if (resultType == Material.GOLD_SWORD) {
                assertEquals(1, result.getFlags().get().size());
            } else if (resultType == Material.DIAMOND_SWORD) {
                assertEquals(1, result.getFlags().get().size());
            }
        }
    }
}
