package com.hnp_arda.wireless_hoppers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;

final class RecipeManager {
    private final Main plugin;
    private final java.util.List<NamespacedKey> recipeKeys = new java.util.ArrayList<>();

    RecipeManager(Main plugin) {
        this.plugin = plugin;
    }

    void registerAll() {
        removeVanillaSlabRecipes();

        registerRecipe("wireless_hopper", WirelessItems.createHopperItem(),
                "ROR", "IHI", "ROR",
                new Ingredient('I', new RecipeChoice.MaterialChoice(Material.IRON_INGOT)),
                new Ingredient('R', new RecipeChoice.MaterialChoice(Material.REDSTONE_TORCH)),
                new Ingredient('O', new RecipeChoice.MaterialChoice(Material.OBSIDIAN)),
                new Ingredient('H', new RecipeChoice.MaterialChoice(Material.HOPPER)));

        registerRecipe("wireless_upgrade_iron", WirelessItems.createUpgradeItem(UpgradeTier.IRON),
                "RIR", "ISI", "RIR",
                new Ingredient('I', new RecipeChoice.MaterialChoice(Material.IRON_INGOT)),
                new Ingredient('R', new RecipeChoice.MaterialChoice(Material.REDSTONE_TORCH)),
                new Ingredient('S', new RecipeChoice.MaterialChoice(Material.SUGAR)));

        registerRecipe("wireless_upgrade_gold", WirelessItems.createUpgradeItem(UpgradeTier.GOLD),
                "RGR", "GUG", "RGR",
                new Ingredient('G', new RecipeChoice.MaterialChoice(Material.GOLD_INGOT)),
                new Ingredient('R', new RecipeChoice.MaterialChoice(Material.REDSTONE_TORCH)),
                new Ingredient('U', new RecipeChoice.ExactChoice(WirelessItems.createUpgradeItem(UpgradeTier.IRON))));

        registerRecipe("wireless_upgrade_diamond", WirelessItems.createUpgradeItem(UpgradeTier.DIAMOND),
                "RDR", "DUD", "RDR",
                new Ingredient('D', new RecipeChoice.MaterialChoice(Material.DIAMOND)),
                new Ingredient('R', new RecipeChoice.MaterialChoice(Material.REDSTONE_TORCH)),
                new Ingredient('U', new RecipeChoice.ExactChoice(WirelessItems.createUpgradeItem(UpgradeTier.GOLD))));

        registerRecipe("wireless_target_tool", WirelessItems.createTargetTool(),
                "RER", "ECE", "RER",
                new Ingredient('R', new RecipeChoice.MaterialChoice(Material.REDSTONE_TORCH)),
                new Ingredient('E', new RecipeChoice.MaterialChoice(Material.ENDER_PEARL)),
                new Ingredient('C', new RecipeChoice.MaterialChoice(Material.COMPASS)));

        registerNetheriteUpgrade();

    }

    private void registerNetheriteUpgrade() {
        NamespacedKey namespacedKey = new NamespacedKey(plugin, "wireless_upgrade_netherite");
        ShapelessRecipe recipe = new ShapelessRecipe(namespacedKey, WirelessItems.createUpgradeItem(UpgradeTier.NETHERITE));
        recipe.addIngredient(new RecipeChoice.ExactChoice(WirelessItems.createUpgradeItem(UpgradeTier.DIAMOND)));
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        recipe.addIngredient(new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT));
        Bukkit.addRecipe(recipe);
        recipeKeys.add(namespacedKey);
    }

    java.util.List<NamespacedKey> recipeKeys() {
        return java.util.List.copyOf(recipeKeys);
    }

    private void removeVanillaSlabRecipes() {
        java.util.Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe == null) {
                continue;
            } else {
                recipe.getResult();
            }
            if (recipe.getResult().getType() == Material.BAMBOO_MOSAIC_SLAB) {
                iterator.remove();
            }
        }
    }


    private void registerRecipe(String key, ItemStack result, String row1, String row2, String row3,
                                Ingredient... ingredients) {
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
        recipe.shape(row1, row2, row3);
        for (Ingredient ingredient : ingredients)
            recipe.setIngredient(ingredient.ID, ingredient.ingredient);
        Bukkit.addRecipe(recipe);
        recipeKeys.add(namespacedKey);
    }


    private record Ingredient(char ID, RecipeChoice ingredient) {
    }
}
