package me.sunmc.vw.weapon;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a weapon's crafting recipe
 */
public class WeaponRecipe {

    private final List<String> shape;
    private final Map<Character, Material> ingredients;
    private final Map<Character, ItemStack> complexIngredients;

    public WeaponRecipe(@NotNull List<String> shape, @NotNull Map<Character, Material> ingredients,
                        @NotNull Map<Character, ItemStack> complexIngredients) {
        this.shape = shape;
        this.ingredients = ingredients;
        this.complexIngredients = complexIngredients;
    }

    @NotNull
    public List<String> getShape() {
        return shape;
    }

    @NotNull
    public Map<Character, Material> getIngredients() {
        return ingredients;
    }

    @NotNull
    public Map<Character, ItemStack> getComplexIngredients() {
        return complexIngredients;
    }

    /**
     * Get the ingredient at a specific shape position
     *
     * @param row    The row (0-2)
     * @param column The column (0-2)
     * @return The material at that position, or null if empty
     */
    @Nullable
    public Material getIngredientAt(int row, int column) {
        if (row < 0 || row >= shape.size() || column < 0 || column >= 3) {
            return null;
        }

        String shapeRow = shape.get(row);
        if (column >= shapeRow.length()) {
            return null;
        }

        char key = shapeRow.charAt(column);
        if (key == ' ') {
            return null;
        }

        return ingredients.get(key);
    }

    /**
     * Get the total count of ingredients required
     *
     * @return A map of materials to their required count
     */
    @NotNull
    public Map<Material, Integer> getIngredientCounts() {
        Map<Material, Integer> counts = new HashMap<>();

        for (String row : shape) {
            for (char c : row.toCharArray()) {
                if (c != ' ') {
                    Material material = ingredients.get(c);
                    if (material != null) {
                        counts.merge(material, 1, Integer::sum);
                    }
                }
            }
        }

        return counts;
    }

    /**
     * Convert a 3x3 shape to slot indices for GUI display
     *
     * @return An array of materials indexed by slot (0-8)
     */
    @NotNull
    public Material[] toSlotArray() {
        Material[] slots = new Material[9];

        for (int row = 0; row < 3; row++) {
            String shapeRow = row < shape.size() ? shape.get(row) : "";
            for (int col = 0; col < 3; col++) {
                char c = col < shapeRow.length() ? shapeRow.charAt(col) : ' ';
                slots[row * 3 + col] = c == ' ' ? null : ingredients.get(c);
            }
        }

        return slots;
    }

    /**
     * Builder class for WeaponRecipe
     */
    public static class Builder {
        private List<String> shape = List.of("   ", "   ", "   ");
        private Map<Character, Material> ingredients = new HashMap<>();
        private Map<Character, ItemStack> complexIngredients = new HashMap<>();

        public Builder shape(@NotNull List<String> shape) {
            this.shape = shape;
            return this;
        }

        public Builder ingredients(@NotNull Map<Character, Material> ingredients) {
            this.ingredients = ingredients;
            return this;
        }

        public Builder addIngredient(char key, @NotNull Material material) {
            this.ingredients.put(key, material);
            return this;
        }

        public Builder complexIngredients(@NotNull Map<Character, ItemStack> complexIngredients) {
            this.complexIngredients = complexIngredients;
            return this;
        }

        public Builder addComplexIngredient(char key, @NotNull ItemStack itemStack) {
            this.complexIngredients.put(key, itemStack);
            return this;
        }

        @NotNull
        public WeaponRecipe build() {
            return new WeaponRecipe(shape, ingredients, complexIngredients);
        }
    }
}