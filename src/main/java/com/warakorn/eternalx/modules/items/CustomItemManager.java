package com.warakorn.eternalx.modules.items;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.crafting.CustomRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;

public class CustomItemManager {
  private final EternalX plugin;
  private final Map<String, CustomItem> items;
  private final NamespacedKey itemKey;

  public CustomItemManager(EternalX plugin) {
    this.plugin = plugin;
    this.items = new HashMap<>();
    this.itemKey = new NamespacedKey(plugin, "custom_item_id");
  }

  public void loadItems() {
    items.clear();
    File itemsFolder = new File(plugin.getDataFolder(), "items");
    if (!itemsFolder.exists()) {
      itemsFolder.mkdirs();
      // สร้างไฟล์ default items แยกตามหมวดหมู่
      String[] defaults = {
        // ⚔️ Combat
        "combat/inferno_blade.yml", "combat/lifesteal_sword.yml",
        "combat/vampire_dagger.yml", "combat/executioner_axe.yml",
        "combat/mjolnir.yml", "combat/lightning_trident.yml",
        // 🛡️ Defense
        "defense/guardian_chestplate.yml", "defense/counter_helmet.yml",
        "defense/shield_wall.yml",
        // 🏃 Movement
        "movement/dash_boots.yml", "movement/double_jump_boots.yml",
        "movement/grapple_hook.yml",
        // 🔧 Utility
        "utility/miner_pickaxe.yml", "utility/magnet_ring.yml",
        "utility/healer_staff.yml",
        // 🌊 Elemental
        "elemental/frost_bow.yml"
      };
      for (String path : defaults) {
        saveDefaultItem(path);
      }
    }

    // โหลดไฟล์ .yml จากทุก subfolder (สแกนแบบ recursive)
    List<File> files = findYmlFiles(itemsFolder);
    if (files.isEmpty()) return;

    for (File file : files) {
      try {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        
        String id = config.getString("id", file.getName().replace(".yml", ""));
        String displayName = config.getString("display-name", "&c" + id);
        Material material = Material.matchMaterial(config.getString("base-material", "STONE"));
        if (material == null) material = Material.STONE;
        
        int customModelData = config.getInt("custom-model-data", 0);
        ItemRarity rarity = ItemRarity.fromString(config.getString("rarity", "COMMON"));
        List<String> lore = config.getStringList("lore");

        // Load Enchantments
        Map<Enchantment, Integer> enchants = new HashMap<>();
        ConfigurationSection enchSec = config.getConfigurationSection("enchantments");
        if (enchSec != null) {
          for (String key : enchSec.getKeys(false)) {
            Enchantment ench = Enchantment.getByName(key);
            if (ench != null) {
              enchants.put(ench, enchSec.getInt(key));
            }
          }
        }

        // Load Attributes
        Map<String, Double> attributes = new HashMap<>();
        ConfigurationSection attrSec = config.getConfigurationSection("attributes");
        if (attrSec != null) {
          for (String key : attrSec.getKeys(false)) {
            attributes.put(key, attrSec.getDouble(key));
          }
        }

        // Load Built-in Abilities
        List<Map<String, Object>> abilities = new ArrayList<>();
        List<Map<?, ?>> abilityList = config.getMapList("abilities");
        for (Map<?, ?> raw : abilityList) {
          Map<String, Object> abMap = new HashMap<>();
          for (Map.Entry<?, ?> entry : raw.entrySet()) {
            abMap.put(String.valueOf(entry.getKey()), entry.getValue());
          }
          abilities.add(abMap);
        }

        // Load Obtainable flags
        boolean craftable = config.getBoolean("craftable", false);
        boolean structureDrop = config.getDouble("drop.chance", 0.0) > 0;
        ItemRarity dropRarity = ItemRarity.fromString(config.getString("drop.rarity", "RARE"));

        // Load Crafting Recipe
        CustomRecipe craftingRecipe = null;
        if (craftable && config.contains("crafting_recipe")) {
          ConfigurationSection recipeSec = config.getConfigurationSection("crafting_recipe");
          if (recipeSec != null) {
            List<String> shape = recipeSec.getStringList("shape");
            Map<Character, Material> ingredients = new HashMap<>();
            ConfigurationSection ingSec = recipeSec.getConfigurationSection("ingredients");
            if (ingSec != null) {
              for (String key : ingSec.getKeys(false)) {
                if (!key.isEmpty()) {
                  Material ingMat = Material.matchMaterial(ingSec.getString(key, "STONE"));
                  if (ingMat != null) {
                    ingredients.put(key.charAt(0), ingMat);
                  }
                }
              }
            }
            if (!shape.isEmpty() && !ingredients.isEmpty()) {
              craftingRecipe = new CustomRecipe(id, shape, ingredients);
            }
          }
        }

        // Create CustomItem
        CustomItem item = new CustomItem(id, displayName, material, customModelData, rarity, 
            lore, enchants, attributes, abilities, craftable, structureDrop, dropRarity, craftingRecipe);
        
        items.put(id, item);

        // Register Recipe
        if (item.isCraftable() && item.getCraftingRecipe() != null) {
          if (plugin.getCraftingManager() != null) {
            plugin.getCraftingManager().registerRecipe(item);
          }
        }

      } catch (Exception e) {
        plugin.getLogger().severe("Failed to load custom item from " + file.getName());
        e.printStackTrace();
      }
    }
    org.bukkit.command.ConsoleCommandSender console = plugin.getServer().getConsoleSender();
    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();
    console.sendMessage(legacy.deserialize("§a[EternalX] Loaded §e" + items.size() + " §acustom items."));
  }

  /**
   * สร้าง ItemStack ที่สามารถนำไปให้ผู้เล่นหรือใส่กล่องได้
   */
  public ItemStack createItemStack(String id) {
    CustomItem customItem = items.get(id);
    if (customItem == null) return null;

    ItemStack item = new ItemStack(customItem.getBaseMaterial());
    ItemMeta meta = item.getItemMeta();
    if (meta == null) return item;

    // Display Name & Lore
    meta.setDisplayName(customItem.getDisplayName().replace("&", "§"));
    
    List<String> coloredLore = new ArrayList<>();
    for (String l : customItem.getLore()) {
      coloredLore.add(l.replace("&", "§"));
    }
    meta.setLore(coloredLore);

    // Custom Model Data
    if (customItem.getCustomModelData() > 0) {
      meta.setCustomModelData(customItem.getCustomModelData());
    }

    // Enchantments
    for (Map.Entry<Enchantment, Integer> entry : customItem.getEnchantments().entrySet()) {
      meta.addEnchant(entry.getKey(), entry.getValue(), true);
    }

    // Attributes (1.21 generic attributes using namespaced keys)
    Map<String, Double> attrs = customItem.getAttributes();
    if (!attrs.isEmpty()) {
      if (attrs.containsKey("attack-damage")) {
        AttributeModifier modifier = new AttributeModifier(
            new NamespacedKey(plugin, "eternalx_damage"),
            attrs.get("attack-damage"),
            AttributeModifier.Operation.ADD_NUMBER,
            org.bukkit.inventory.EquipmentSlotGroup.HAND);
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, modifier);
      }
      if (attrs.containsKey("attack-speed")) {
        AttributeModifier modifier = new AttributeModifier(
            new NamespacedKey(plugin, "eternalx_speed"),
            attrs.get("attack-speed"),
            AttributeModifier.Operation.ADD_NUMBER,
            org.bukkit.inventory.EquipmentSlotGroup.HAND);
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, modifier);
      }
      if (attrs.containsKey("movement-speed")) {
        AttributeModifier modifier = new AttributeModifier(
            new NamespacedKey(plugin, "eternalx_move"),
            attrs.get("movement-speed"),
            AttributeModifier.Operation.ADD_NUMBER,
            org.bukkit.inventory.EquipmentSlotGroup.HAND);
        meta.addAttributeModifier(Attribute.MOVEMENT_SPEED, modifier);
      }
      if (attrs.containsKey("armor")) {
        org.bukkit.inventory.EquipmentSlotGroup slotGroup;
        switch (getArmorSlot(customItem.getBaseMaterial())) {
          case HEAD: slotGroup = org.bukkit.inventory.EquipmentSlotGroup.HEAD; break;
          case CHEST: slotGroup = org.bukkit.inventory.EquipmentSlotGroup.CHEST; break;
          case LEGS: slotGroup = org.bukkit.inventory.EquipmentSlotGroup.LEGS; break;
          case FEET: slotGroup = org.bukkit.inventory.EquipmentSlotGroup.FEET; break;
          default: slotGroup = org.bukkit.inventory.EquipmentSlotGroup.HAND; break;
        }
        AttributeModifier modifier = new AttributeModifier(
            new NamespacedKey(plugin, "eternalx_armor"),
            attrs.get("armor"),
            AttributeModifier.Operation.ADD_NUMBER,
            slotGroup);
        meta.addAttributeModifier(Attribute.ARMOR, modifier);
      }
    }

    // PDC - Identifier tag "eternalx:custom_item_id"
    meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, customItem.getId());
    
    // Flags to hide attributes if we have custom lore
    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

    item.setItemMeta(meta);
    return item;
  }

  /**
   * เช็คว่า ItemStack นี้เป็น custom item รึเปล่า
   */
  public boolean isCustomItem(ItemStack item) {
    if (item == null || !item.hasItemMeta()) return false;
    return item.getItemMeta().getPersistentDataContainer().has(itemKey, PersistentDataType.STRING);
  }

  /**
   * ดึง ID ของ custom item ออกมาจาก ItemStack
   */
  public String getCustomItemId(ItemStack item) {
    if (item == null || !item.hasItemMeta()) return null;
    return item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
  }

  public CustomItem getCustomItem(String id) {
    return items.get(id);
  }

  public Map<String, CustomItem> getItems() {
    return items;
  }

  private void saveDefaultItem(String path) {
    File file = new File(plugin.getDataFolder(), "items/" + path);
    if (!file.exists()) {
      file.getParentFile().mkdirs();
      try {
        plugin.saveResource("items/" + path, false);
      } catch (Exception e) {
        plugin.getLogger().warning("Could not save default item: " + path);
      }
    }
  }

  /**
   * สแกนหาไฟล์ .yml ทั้งหมดใน folder และ subfolder
   */
  private List<File> findYmlFiles(File folder) {
    List<File> result = new ArrayList<>();
    File[] children = folder.listFiles();
    if (children == null) return result;
    for (File child : children) {
      if (child.isDirectory()) {
        result.addAll(findYmlFiles(child));
      } else if (child.getName().endsWith(".yml")) {
        result.add(child);
      }
    }
    return result;
  }

  private EquipmentSlot getArmorSlot(Material mat) {
    String name = mat.name();
    if (name.endsWith("_HELMET")) return EquipmentSlot.HEAD;
    if (name.endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
    if (name.endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
    if (name.endsWith("_BOOTS")) return EquipmentSlot.FEET;
    return EquipmentSlot.HAND;
  }
}
