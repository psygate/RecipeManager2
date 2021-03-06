package haveric.recipeManager.flags;

import haveric.recipeManager.messages.MessageSender;
import org.bukkit.ChatColor;

public class FlagLoader {
    public FlagLoader() {
        loadDefaultFlags();
    }

    public void loadDefaultFlags() {
        FlagFactory.getInstance().initializeFlag(FlagType.COMMAND, new FlagCommand(), FlagBit.NONE, "cmd", "commands");
        FlagFactory.getInstance().initializeFlag(FlagType.KEEP_ITEM, new FlagKeepItem(), FlagBit.NO_SHIFT, "returnitem", "replaceitem");
        FlagFactory.getInstance().initializeFlag(FlagType.INGREDIENT_CONDITION, new FlagIngredientCondition(), FlagBit.NO_SHIFT, "ingrcondition", "ingrcond", "ifingredient", "ifingr");
        FlagFactory.getInstance().initializeFlag(FlagType.PERMISSION, new FlagPermission(), FlagBit.NONE, "permissions", "perm");
        FlagFactory.getInstance().initializeFlag(FlagType.FOR_PERMISSION, new FlagForPermission(), FlagBit.NO_SHIFT, "forperm");
        FlagFactory.getInstance().initializeFlag(FlagType.FOR_CHANCE, new FlagForChance(), FlagBit.NO_SHIFT, "bychance", "chance");
        FlagFactory.getInstance().initializeFlag(FlagType.GROUP, new FlagGroup(), FlagBit.NONE, "groups", "permissiongroup", "permgroup");
        FlagFactory.getInstance().initializeFlag(FlagType.WORLD, new FlagWorld(), FlagBit.NONE, "needworld", "worlds");
        FlagFactory.getInstance().initializeFlag(FlagType.HEIGHT, new FlagHeight(), FlagBit.NONE, "depth");
        FlagFactory.getInstance().initializeFlag(FlagType.MOD_EXP, new FlagModExp(), FlagBit.NO_SHIFT, "expmod", "modxp", "xpmod", "exp", "xp", "giveexp", "givexp", "takeexp", "takexp");
        FlagFactory.getInstance().initializeFlag(FlagType.NEED_EXP, new FlagNeedExp(), FlagBit.NONE, "needxp", "reqexp", "expreq", "reqxp", "xpreq");
        FlagFactory.getInstance().initializeFlag(FlagType.MOD_LEVEL, new FlagModLevel(), FlagBit.NO_SHIFT, "levelmod", "setlevel", "level");
        FlagFactory.getInstance().initializeFlag(FlagType.NEED_LEVEL, new FlagNeedLevel(), FlagBit.NONE, "reqlevel", "levelreq");
        FlagFactory.getInstance().initializeFlag(FlagType.MOD_MONEY, new FlagModMoney(), FlagBit.NO_SHIFT, "moneymod", "setmoney", "money");
        FlagFactory.getInstance().initializeFlag(FlagType.NEED_MONEY, new FlagNeedMoney(), FlagBit.NONE, "reqmoney", "moneyreq");
        FlagFactory.getInstance().initializeFlag(FlagType.COOLDOWN, new FlagCooldown(), FlagBit.NO_SHIFT, "cooltime", "delay");
        FlagFactory.getInstance().initializeFlag(FlagType.HOLD_ITEM, new FlagHoldItem(), FlagBit.NONE, "hold");
        FlagFactory.getInstance().initializeFlag(FlagType.GAMEMODE, new FlagGameMode(), FlagBit.NONE, "needgm");
        FlagFactory.getInstance().initializeFlag(FlagType.LIGHT_LEVEL, new FlagLightLevel(), FlagBit.NONE, "blocklight", "sunlight", "light");
        FlagFactory.getInstance().initializeFlag(FlagType.BIOME, new FlagBiome(), FlagBit.NONE);
        FlagFactory.getInstance().initializeFlag(FlagType.WEATHER, new FlagWeather(), FlagBit.NONE);
        FlagFactory.getInstance().initializeFlag(FlagType.EXPLODE, new FlagExplode(), FlagBit.NO_SHIFT | FlagBit.NO_VALUE, "explosion", "boom", "tnt");
        FlagFactory.getInstance().initializeFlag(FlagType.SOUND, new FlagSound(), FlagBit.NO_SHIFT, "playsound");
        FlagFactory.getInstance().initializeFlag(FlagType.SUMMON, new FlagSummon(), FlagBit.NO_SHIFT, "spawn", "creature", "mob", "animal");
        FlagFactory.getInstance().initializeFlag(FlagType.BLOCK_POWERED, new FlagBlockPowered(), FlagBit.NO_VALUE, "poweredblock", "blockpower", "redstonepowered");
        FlagFactory.getInstance().initializeFlag(FlagType.POTION_EFFECT, new FlagPotionEffect(), FlagBit.NONE, "potionfx");
        FlagFactory.getInstance().initializeFlag(FlagType.LAUNCH_FIREWORK, new FlagLaunchFirework(), FlagBit.NO_SHIFT, "setfirework");
        FlagFactory.getInstance().initializeFlag(FlagType.SET_BLOCK, new FlagSetBlock(), FlagBit.NO_SHIFT, "changeblock");
        FlagFactory.getInstance().initializeFlag(FlagType.MESSAGE, new FlagMessage(), FlagBit.NONE, "craftmsg", "msg");
        FlagFactory.getInstance().initializeFlag(FlagType.BROADCAST, new FlagBroadcast(), FlagBit.NONE, "announce", "msgall");
        FlagFactory.getInstance().initializeFlag(FlagType.SECRET, new FlagSecret(), FlagBit.NO_VALUE | FlagBit.NO_FOR);
        FlagFactory.getInstance().initializeFlag(FlagType.TEMPERATURE, new FlagTemperature(), FlagBit.NONE, "temp");
        FlagFactory.getInstance().initializeFlag(FlagType.INVENTORY, new FlagInventory(), FlagBit.NONE);
        // TELEPORT(FlagTeleport(), FlagBit.NO_SHIFT, "tpto", "goto"), // TODO finish flag
        // REALTIME(FlagRealTime(), FlagBit.NONE, "time", "date"),
        // ONLINETIME(FlagOnlineTime(), FlagBit.NONE, "playtime", "onlinefor"),
        // WORLDTIME(FlagWorldTime(), FlagBit.NONE),
        // PROXIMITY(FlagProximity(), FlagBit.NONE, "distance", "nearby"),
        // DEBUG(FlagDebug(), FlagBit.NO_VALUE | FlagBit.NO_FOR | FlagBit.NO_SKIP_PERMISSION, "monitor", "log"),

        // Recipe only flags
        FlagFactory.getInstance().initializeFlag(FlagType.ADD_TO_BOOK, new FlagAddToBook(), FlagBit.RECIPE | FlagBit.NO_FOR | FlagBit.NO_SKIP_PERMISSION, "recipebook");
        FlagFactory.getInstance().initializeFlag(FlagType.FAIL_MESSAGE, new FlagFailMessage(), FlagBit.RECIPE, "failmsg");
        FlagFactory.getInstance().initializeFlag(FlagType.DISPLAY_RESULT, new FlagDisplayResult(), FlagBit.RECIPE, "resultdisplay", "showresult");
        FlagFactory.getInstance().initializeFlag(FlagType.REMOVE, new FlagRemove(), FlagBit.RECIPE | FlagBit.NO_FOR | FlagBit.NO_VALUE | FlagBit.NO_SKIP_PERMISSION, "delete");
        FlagFactory.getInstance().initializeFlag(FlagType.RESTRICT, new FlagRestrict(), FlagBit.RECIPE | FlagBit.NO_VALUE, "disable", "denied", "deny");
        FlagFactory.getInstance().initializeFlag(FlagType.OVERRIDE, new FlagOverride(), FlagBit.RECIPE | FlagBit.NO_FOR | FlagBit.NO_VALUE | FlagBit.NO_SKIP_PERMISSION, "edit", "overwrite", "supercede", "replace");
        FlagFactory.getInstance().initializeFlag(FlagType.INDIVIDUAL_RESULTS, new FlagIndividualResults(), FlagBit.RECIPE | FlagBit.NO_VALUE, "individual");

        // Result only flags
        FlagFactory.getInstance().initializeFlag(FlagType.CLONE_INGREDIENT, new FlagCloneIngredient(), FlagBit.RESULT | FlagBit.NO_SHIFT, "clone", "copy", "copyingredient"); // TODO finish
        FlagFactory.getInstance().initializeFlag(FlagType.ITEM_NAME, new FlagItemName(), FlagBit.RESULT, "name", "displayname");
        FlagFactory.getInstance().initializeFlag(FlagType.ITEM_LORE, new FlagItemLore(), FlagBit.RESULT, "lore", "itemdesc");
        FlagFactory.getInstance().initializeFlag(FlagType.LEATHER_COLOR, new FlagLeatherColor(), FlagBit.RESULT, "leathercolour", "color", "colour");
        FlagFactory.getInstance().initializeFlag(FlagType.BOOK_ITEM, new FlagBookItem(), FlagBit.RESULT, "book");
        // MAPITEM(FlagMapItem(), FlagBit.RESULT, "map"), // TODO finish this flag
        FlagFactory.getInstance().initializeFlag(FlagType.FIREWORK_ITEM, new FlagFireworkItem(), FlagBit.RESULT, "firework", "fireworkrocket");
        FlagFactory.getInstance().initializeFlag(FlagType.FIREWORK_CHARGE_ITEM, new FlagFireworkChargeItem(), FlagBit.RESULT, "fireworkcharge", "fireworkeffect");
        FlagFactory.getInstance().initializeFlag(FlagType.SKULL_OWNER, new FlagSkullOwner(), FlagBit.RESULT, "skullitem");
        FlagFactory.getInstance().initializeFlag(FlagType.POTION_ITEM, new FlagPotionItem(), FlagBit.RESULT, "potion");
        FlagFactory.getInstance().initializeFlag(FlagType.ENCHANT_ITEM, new FlagEnchantItem(), FlagBit.RESULT, "enchant", "enchantment");
        FlagFactory.getInstance().initializeFlag(FlagType.ENCHANTED_BOOK, new FlagEnchantedBook(), FlagBit.RESULT, "enchantbook", "enchantingbook");
        FlagFactory.getInstance().initializeFlag(FlagType.GET_RECIPE_BOOK, new FlagGetRecipeBook(), FlagBit.RESULT | FlagBit.NO_SHIFT, "getbook", "bookresult");
        FlagFactory.getInstance().initializeFlag(FlagType.HIDE, new FlagHide(), FlagBit.RESULT);
        FlagFactory.getInstance().initializeFlag(FlagType.BANNER_ITEM, new FlagBannerItem(), FlagBit.RESULT, "banner");
        FlagFactory.getInstance().initializeFlag(FlagType.NO_RESULT, new FlagNoResult(), FlagBit.RESULT | FlagBit.NO_FOR | FlagBit.NO_VALUE);
    }

    public void loadCustomFlag(String mainAlias, Flag newFlag, int bits, String... aliases) {
        if (FlagFactory.getInstance().isInitialized()) {
            MessageSender.getInstance().info(ChatColor.RED + "Custom flags must be added in your onEnable() method.");
        } else {
            FlagFactory.getInstance().initializeFlag(mainAlias, newFlag, bits, aliases);
        }
    }
}
