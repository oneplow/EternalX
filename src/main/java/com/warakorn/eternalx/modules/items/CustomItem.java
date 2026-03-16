package com.warakorn.eternalx.modules.items;

import com.warakorn.eternalx.modules.items.crafting.CustomRecipe;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * โครงสร้างข้อมูล Custom Item ที่อ่านจาก YAML
 */
public class CustomItem {
  private final String id;
  private final String displayName;
  private final Material baseMaterial;
  private final int customModelData;
  private final ItemRarity rarity;
  private final List<String> lore;
  
  private final Map<Enchantment, Integer> enchantments;
  private final Map<String, Double> attributes; // attack-damage, attack-speed, etc.
  private final List<Map<String, Object>> rawAbilities; // เก็บ raw config ชั่วคราวก่อนแปลงเป็น Ability class
  
  private final boolean craftable;
  private final boolean structureDrop;
  private final ItemRarity dropRarity;
  private final CustomRecipe craftingRecipe;

  public CustomItem(String id, String displayName, Material baseMaterial, int customModelData, 
                    ItemRarity rarity, List<String> lore, Map<Enchantment, Integer> enchantments, 
                    Map<String, Double> attributes, List<Map<String, Object>> rawAbilities, 
                    boolean craftable, boolean structureDrop, ItemRarity dropRarity, CustomRecipe craftingRecipe) {
    this.id = id;
    this.displayName = displayName;
    this.baseMaterial = baseMaterial;
    this.customModelData = customModelData;
    this.rarity = rarity;
    this.lore = lore;
    this.enchantments = enchantments != null ? enchantments : new HashMap<>();
    this.attributes = attributes != null ? attributes : new HashMap<>();
    this.rawAbilities = rawAbilities;
    this.craftable = craftable;
    this.structureDrop = structureDrop;
    this.dropRarity = dropRarity;
    this.craftingRecipe = craftingRecipe;
  }

  // Getters
  public String getId() { return id; }
  public String getDisplayName() { return displayName; }
  public Material getBaseMaterial() { return baseMaterial; }
  public int getCustomModelData() { return customModelData; }
  public ItemRarity getRarity() { return rarity; }
  public List<String> getLore() { return lore; }
  public Map<Enchantment, Integer> getEnchantments() { return enchantments; }
  public Map<String, Double> getAttributes() { return attributes; }
  public List<Map<String, Object>> getRawAbilities() { return rawAbilities; }
  public boolean isCraftable() { return craftable; }
  public boolean isStructureDrop() { return structureDrop; }
  public ItemRarity getDropRarity() { return dropRarity; }
  public CustomRecipe getCraftingRecipe() { return craftingRecipe; }
}
