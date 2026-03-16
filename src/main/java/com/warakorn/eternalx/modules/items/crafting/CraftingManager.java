package com.warakorn.eternalx.modules.items.crafting;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import java.util.HashMap;
import java.util.Map;

/**
 * ระบบจัดการและขึ้นทะเบียนสูตรคราฟต์ของเข้าเซิร์ฟเวอร์
 */
public class CraftingManager {
  private final EternalX plugin;
  private final Map<String, NamespacedKey> registeredRecipes = new HashMap<>();

  public CraftingManager(EternalX plugin) {
    this.plugin = plugin;
  }

  /**
   * ลงทะเบียนสูตรคราฟต์เข้าไปใน Bukkit Engine
   */
  public void registerRecipe(CustomItem customItem) {
    CustomRecipe recipeData = customItem.getCraftingRecipe();
    if (recipeData == null) return;

    NamespacedKey key = new NamespacedKey(plugin, "custom_item_" + customItem.getId().toLowerCase());
    
    // ลบอันเก่าถ้ามี
    if (registeredRecipes.containsKey(customItem.getId())) {
      Bukkit.removeRecipe(registeredRecipes.get(customItem.getId()));
    }

    ItemStack result = plugin.getCustomItemManager().createItemStack(customItem.getId());
    if (result == null) return;

    ShapedRecipe recipe = new ShapedRecipe(key, result);

    // ระบุ Shape เช่น [" R ", "RSR", " O "]
    String[] shapeArr = recipeData.getShape().toArray(new String[0]);
    recipe.shape(shapeArr);

    // ระบุส่วนผสม R -> Redstone Block, S -> Nether Star
    for (Map.Entry<Character, Material> entry : recipeData.getIngredients().entrySet()) {
      // bukkit ต้องการให้กรอกทีละตัวอักษร
      recipe.setIngredient(entry.getKey(), entry.getValue());
    }

    Bukkit.addRecipe(recipe);
    registeredRecipes.put(customItem.getId(), key);
    
    plugin.getLogger().info("Registered custom recipe for: " + customItem.getId());
  }

  /**
   * ลบสูตรคราฟต์ทั้งหมด (ใช้ตอน /eternalx reload)
   */
  public void clearRecipes() {
    for (NamespacedKey key : registeredRecipes.values()) {
      Bukkit.removeRecipe(key);
    }
    registeredRecipes.clear();
  }
}
