package com.warakorn.eternalx.modules.items.crafting;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

/**
 * รูปแบบสูตรคราฟต์ที่โหลดมาจาก YAML
 */
public class CustomRecipe {
  private final String itemId;
  private final List<String> shape;
  // Character -> Material
  private final Map<Character, Material> ingredients;

  public CustomRecipe(String itemId, List<String> shape, Map<Character, Material> ingredients) {
    this.itemId = itemId;
    this.shape = shape;
    this.ingredients = ingredients;
  }

  public String getItemId() {
    return itemId;
  }

  public List<String> getShape() {
    return shape;
  }

  public Map<Character, Material> getIngredients() {
    return ingredients;
  }
}
