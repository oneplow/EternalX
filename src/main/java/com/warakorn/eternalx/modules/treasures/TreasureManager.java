package com.warakorn.eternalx.modules.treasures;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.structures.StructureRarity;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TreasureManager {
  private final EternalX plugin;
  private final Map<String, TreasureData> treasures = new HashMap<>();

  public TreasureManager(EternalX plugin) {
    this.plugin = plugin;
  }

  public void loadTreasures() {
    treasures.clear();

    File treasuresDir = new File(plugin.getDataFolder(), "treasures");
    if (!treasuresDir.exists()) {
      treasuresDir.mkdirs();
      // ⭐ สร้าง default treasure สำหรับทุก rarity
      for (StructureRarity rarity : StructureRarity.values()) {
        createDefaultTreasureForRarity(treasuresDir, rarity);
      }
    }

    File[] treasureFiles = treasuresDir.listFiles((dir, name) -> name.endsWith(".yml"));
    if (treasureFiles == null || treasureFiles.length == 0) {
      plugin.getLogger().warning("No treasure files found!");
      return;
    }

    for (File file : treasureFiles) {
      loadTreasure(file);
    }

    plugin.getLogger().info("Loaded " + treasures.size() + " treasure configurations.");
  }

  /**
   * ⭐ สร้าง treasure file ตาม rarity
   * rarity สูง = ของดีกว่า
   */
  private void createDefaultTreasureForRarity(File treasuresDir, StructureRarity rarity) {
    String fileName = rarity.getSuggestedTreasureTier() + "_loot.yml";
    File file = new File(treasuresDir, fileName);
    
    if (file.exists()) return; // มีอยู่แล้ว ไม่ต้องสร้าง
    
    try {
      file.createNewFile();
      YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

      switch (rarity) {
        case COMMON:
          // ของธรรมดา: อาหาร, ไม้, ถ่านหิน
          addItem(config, 0, "BREAD", 1, 3, 0.8);
          addItem(config, 1, "COOKED_BEEF", 1, 2, 0.6);
          addItem(config, 2, "OAK_LOG", 4, 16, 0.7);
          addItem(config, 3, "COAL", 2, 8, 0.6);
          addItem(config, 4, "STICK", 4, 16, 0.5);
          addItem(config, 5, "TORCH", 8, 16, 0.7);
          break;

        case UNCOMMON:
          // ของดีขึ้นนิดหน่อย: เหล็ก, ทอง, อาหารดี
          addItem(config, 0, "IRON_INGOT", 2, 6, 0.5);
          addItem(config, 1, "GOLD_INGOT", 1, 4, 0.3);
          addItem(config, 2, "GOLDEN_APPLE", 1, 2, 0.2);
          addItem(config, 3, "COOKED_PORKCHOP", 2, 5, 0.6);
          addItem(config, 4, "ARROW", 8, 24, 0.5);
          addItem(config, 5, "ENDER_PEARL", 1, 3, 0.2);
          break;

        case RARE:
          // ของหายาก: เพชร, emerald, enchanted items
          addItem(config, 0, "DIAMOND", 1, 3, 0.3);
          addItem(config, 1, "EMERALD", 1, 4, 0.4);
          addItem(config, 2, "ENCHANTED_GOLDEN_APPLE", 1, 1, 0.15);
          addItem(config, 3, "IRON_SWORD", 1, 1, 0.4)
            .addEnchantment("SHARPNESS", 2)
            .addEnchantment("UNBREAKING", 2);
          addItem(config, 4, "IRON_CHESTPLATE", 1, 1, 0.3)
            .addEnchantment("PROTECTION", 2);
          addItem(config, 5, "ENDER_PEARL", 2, 5, 0.5);
          addItem(config, 6, "EXPERIENCE_BOTTLE", 3, 8, 0.4);
          break;

        case EPIC:
          // ของ Epic: เพชรเยอะ, enchanted diamond gear
          addItem(config, 0, "DIAMOND", 2, 5, 0.5);
          addItem(config, 1, "EMERALD", 3, 8, 0.5);
          addItem(config, 2, "NETHERITE_SCRAP", 1, 2, 0.2);
          addItem(config, 3, "DIAMOND_SWORD", 1, 1, 0.4)
            .addEnchantment("SHARPNESS", 3)
            .addEnchantment("UNBREAKING", 3)
            .addEnchantment("LOOTING", 2);
          addItem(config, 4, "DIAMOND_CHESTPLATE", 1, 1, 0.3)
            .addEnchantment("PROTECTION", 3)
            .addEnchantment("UNBREAKING", 3);
          addItem(config, 5, "ENCHANTED_GOLDEN_APPLE", 1, 2, 0.3);
          addItem(config, 6, "TOTEM_OF_UNDYING", 1, 1, 0.1);
          addItem(config, 7, "EXPERIENCE_BOTTLE", 5, 15, 0.6);
          break;

        case LEGENDARY:
          // ของ Legendary: เกราะเพชรชุดเต็ม, netherite
          addItem(config, 0, "DIAMOND", 4, 10, 0.8);
          addItem(config, 1, "NETHERITE_INGOT", 1, 3, 0.3);
          addItem(config, 2, "DIAMOND_SWORD", 1, 1, 0.6)
            .addEnchantment("SHARPNESS", 5)
            .addEnchantment("UNBREAKING", 3)
            .addEnchantment("LOOTING", 3)
            .addEnchantment("SWEEPING_EDGE", 3)
            .setDisplayName("§6Legendary Blade")
            .addLore("§7A weapon of legend");
          addItem(config, 3, "DIAMOND_CHESTPLATE", 1, 1, 0.5)
            .addEnchantment("PROTECTION", 4)
            .addEnchantment("UNBREAKING", 3)
            .addEnchantment("THORNS", 2);
          addItem(config, 4, "DIAMOND_HELMET", 1, 1, 0.4)
            .addEnchantment("PROTECTION", 4)
            .addEnchantment("UNBREAKING", 3);
          addItem(config, 5, "ENCHANTED_GOLDEN_APPLE", 2, 4, 0.5);
          addItem(config, 6, "TOTEM_OF_UNDYING", 1, 2, 0.2);
          addItem(config, 7, "ELYTRA", 1, 1, 0.15)
            .addEnchantment("UNBREAKING", 3);
          break;

        case MYTHIC:
          // ของ Mythic: สุดยอด, netherite gear, หายากมาก
          addItem(config, 0, "NETHERITE_INGOT", 2, 6, 0.6);
          addItem(config, 1, "DIAMOND", 8, 16, 0.9);
          addItem(config, 2, "NETHERITE_SWORD", 1, 1, 0.5)
            .addEnchantment("SHARPNESS", 5)
            .addEnchantment("UNBREAKING", 3)
            .addEnchantment("LOOTING", 3)
            .addEnchantment("SWEEPING_EDGE", 3)
            .addEnchantment("FIRE_ASPECT", 2)
            .setDisplayName("§cMythic Destroyer")
            .addLore("§7Forged in the heart of the Nether")
            .addLore("§5Legendary Power");
          addItem(config, 3, "NETHERITE_CHESTPLATE", 1, 1, 0.4)
            .addEnchantment("PROTECTION", 4)
            .addEnchantment("UNBREAKING", 3)
            .addEnchantment("THORNS", 3)
            .addEnchantment("MENDING", 1);
          addItem(config, 4, "ENCHANTED_GOLDEN_APPLE", 3, 6, 0.7);
          addItem(config, 5, "TOTEM_OF_UNDYING", 1, 3, 0.4);
          addItem(config, 6, "ELYTRA", 1, 1, 0.3)
            .addEnchantment("UNBREAKING", 3)
            .addEnchantment("MENDING", 1);
          addItem(config, 7, "NETHER_STAR", 1, 2, 0.2);
          addItem(config, 8, "DRAGON_HEAD", 1, 1, 0.15);
          break;
      }

      config.save(file);
      plugin.getLogger().info("Created default treasure: " + fileName);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Helper class สำหรับสร้าง item config
   * ✅ แก้บั๊ก: เขียนข้อมูลลง config ทันทีเมื่อเรียก method
   */
  private static class ItemBuilder {
    private final YamlConfiguration config;
    private final String prefix;

    ItemBuilder(YamlConfiguration config, int index) {
      this.config = config;
      this.prefix = "items." + index;
    }

    ItemBuilder addEnchantment(String enchant, int level) {
      config.set(prefix + ".enchantments." + enchant, level);
      return this;
    }

    ItemBuilder setDisplayName(String name) {
      config.set(prefix + ".display-name", name);
      return this;
    }

    ItemBuilder addLore(String line) {
      List<String> lore = config.getStringList(prefix + ".lore");
      lore.add(line);
      config.set(prefix + ".lore", lore);
      return this;
    }
  }

  private static ItemBuilder addItem(YamlConfiguration config, int index, String material, 
                                     int min, int max, double chance) {
    config.set("items." + index + ".material", material);
    config.set("items." + index + ".min-amount", min);
    config.set("items." + index + ".max-amount", max);
    config.set("items." + index + ".chance", chance);
    return new ItemBuilder(config, index);
  }

  private void loadTreasure(File file) {
    String treasureId = file.getName().replace(".yml", "");
    YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

    List<TreasureData.LootItem> items = new ArrayList<>();

    ConfigurationSection itemsSection = config.getConfigurationSection("items");
    if (itemsSection == null) {
      plugin.getLogger().warning("No items found in treasure file: " + file.getName());
      return;
    }

    for (String key : itemsSection.getKeys(false)) {
      ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
      if (itemSection == null) continue;

      Material material = Material.matchMaterial(itemSection.getString("material", "DIRT"));
      if (material == null) {
        plugin.getLogger().warning("Invalid material in " + file.getName() + ": " + itemSection.getString("material"));
        continue;
      }

      int minAmount = itemSection.getInt("min-amount", 1);
      int maxAmount = itemSection.getInt("max-amount", 1);
      double chance = itemSection.getDouble("chance", 1.0);

      // Load enchantments
      Map<Enchantment, Integer> enchantments = new HashMap<>();
      ConfigurationSection enchSection = itemSection.getConfigurationSection("enchantments");
      if (enchSection != null) {
        for (String enchName : enchSection.getKeys(false)) {
          try {
            Enchantment enchantment = Enchantment.getByName(enchName.toUpperCase());
            if (enchantment != null) {
              enchantments.put(enchantment, enchSection.getInt(enchName));
            }
          } catch (Exception e) {
            plugin.getLogger().warning("Invalid enchantment: " + enchName);
          }
        }
      }

      String displayName = itemSection.getString("display-name");
      List<String> lore = itemSection.getStringList("lore");

      items.add(new TreasureData.LootItem(material, minAmount, maxAmount, chance,
        enchantments, displayName, lore));
    }

    treasures.put(treasureId, new TreasureData(treasureId, items));
    plugin.debugLog("Loaded treasure: " + treasureId + " with " + items.size() + " items");
  }

  public TreasureData getTreasure(String id) {
    return treasures.get(id);
  }

  public Map<String, TreasureData> getTreasures() {
    return treasures;
  }
}
