package com.hnp_arda.wireless_hoppers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;

final class RecipeManager {
    private final Main plugin;

    RecipeManager(Main plugin) {
        this.plugin = plugin;
    }

    void registerAll() {
        removeVanillaTuffSlabRecipes();

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

        registerNetheriteSmithing();
    }

    private void registerNetheriteSmithing() {
        NamespacedKey key = new NamespacedKey(plugin, "wireless_upgrade_netherite_smithing");
        ItemStack result = WirelessItems.createUpgradeItem(UpgradeTier.NETHERITE);
        RecipeChoice template = new RecipeChoice.MaterialChoice(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        RecipeChoice base = new RecipeChoice.ExactChoice(WirelessItems.createUpgradeItem(UpgradeTier.DIAMOND));
        RecipeChoice addition = new RecipeChoice.MaterialChoice(Material.NETHERITE_INGOT);
        SmithingTransformRecipe recipe = new SmithingTransformRecipe(key, result, template, base, addition);
        Bukkit.addRecipe(recipe);
    }

    private void removeVanillaTuffSlabRecipes() {
        java.util.Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe == null) {
                continue;
            } else {
                recipe.getResult();
            }
            if (recipe.getResult().getType() == Material.TUFF_SLAB) {
                iterator.remove();
            }
        }
    }


    private void registerRecipe(String key, ItemStack result, String row1, String row2, String row3,
                                Ingredient... ingredients) {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, key), result);
        recipe.shape(row1, row2, row3);
        for (Ingredient ingredient : ingredients)
            recipe.setIngredient(ingredient.ID, ingredient.ingredient);
        Bukkit.addRecipe(recipe);
    }


    private record Ingredient(char ID, RecipeChoice ingredient) {
    }
}
