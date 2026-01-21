package com.hnp_arda.wireless_hoppers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

final class RecipeManager {
    private final Main plugin;

    RecipeManager(Main plugin) {
        this.plugin = plugin;
    }

    void registerAll() {
        removeVanillaWarpedSlabRecipes();

        registerRecipe("wireless_hopper", WirelessItems.createHopperItem(),
                "IRI", "RHR", "IRI",
                new Ingredient('I', Material.IRON_INGOT),
                new Ingredient('R', Material.REDSTONE),
                new Ingredient('H', Material.HOPPER));

        registerRecipe("wireless_upgrade_iron", WirelessItems.createUpgradeItem(UpgradeTier.IRON),
                " I ", "IRI", " I ",
                new Ingredient('I', Material.IRON_INGOT),
                new Ingredient('R', Material.REDSTONE));

        registerRecipe("wireless_upgrade_gold", WirelessItems.createUpgradeItem(UpgradeTier.GOLD),
                " G ", "GRG", " G ",
                new Ingredient('G', Material.GOLD_INGOT),
                new Ingredient('R', Material.REDSTONE));

        registerRecipe("wireless_upgrade_diamond", WirelessItems.createUpgradeItem(UpgradeTier.DIAMOND),
                " D ", "DRD", " D ",
                new Ingredient('D', Material.DIAMOND),
                new Ingredient('R', Material.REDSTONE_BLOCK));

        registerRecipe("wireless_upgrade_netherite", WirelessItems.createUpgradeItem(UpgradeTier.NETHERITE),
                " N ", "NRN", " N ",
                new Ingredient('N', Material.NETHERITE_INGOT),
                new Ingredient('R', Material.REDSTONE));

        registerRecipe("wireless_target_tool", WirelessItems.createTargetTool(),
                " R ", " C ", " R ",
                new Ingredient('R', Material.REDSTONE),
                new Ingredient('C', Material.COMPASS));
    }

    private void removeVanillaWarpedSlabRecipes() {
        java.util.Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe == null) {
                continue;
            } else {
                recipe.getResult();
            }
            if (recipe.getResult().getType() == Material.WARPED_SLAB) {
                iterator.remove();
            }
        }
    }


    private void registerRecipe(String key, ItemStack result, String row1, String row2, String row3,
                                Ingredient... ingredients) {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, key), result);
        recipe.shape(row1, row2, row3);
        for (Ingredient ingredient : ingredients)
            recipe.setIngredient(ingredient.ID, ingredient.material);
        Bukkit.addRecipe(recipe);
    }

    private record Ingredient(char ID, Material material) {
    }
}
