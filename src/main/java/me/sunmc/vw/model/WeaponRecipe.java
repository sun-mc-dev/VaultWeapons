package me.sunmc.vw.model;

import java.util.List;
import java.util.Map;

public record WeaponRecipe(String type, List<String> shape, Map<Character, String> ingredients) {
    public WeaponRecipe(String type, List<String> shape,
                        Map<Character, String> ingredients) {

        this.type = type;
        this.shape = List.copyOf(shape);
        this.ingredients = Map.copyOf(ingredients);
    }
}