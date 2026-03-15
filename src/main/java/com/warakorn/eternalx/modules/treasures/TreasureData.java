package com.warakorn.eternalx.modules.treasures;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TreasureData {
  private final String id;
  private final List<LootItem> items;

  public TreasureData(String id, List<LootItem> items) {
    this.id = id;
    this.items = items;
  }

  public String getId() {
    return id;
  }

  public List<LootItem> getItems() {
    return items;
  }

  /**
   * ⭐ สร้าง loot โดยไม่มี multiplier (ใช้ค่า default 1.0)
   */
  public List<ItemStack> generateLoot(Random random) {
    return generateLoot(random, 1.0);
  }

  /**
   * ⭐ สร้าง loot พร้อม multiplier
   * multiplier สูง = โอกาสได้ของมากขึ้น + จำนวนมากขึ้น
   */
  public List<ItemStack> generateLoot(Random random, double multiplier) {
    List<ItemStack> loot = new ArrayList<>();

    for (LootItem item : items) {
      // ⭐ Multiplier เพิ่มโอกาสได้ของ (แต่ไม่เกิน 100%)
      double adjustedChance = Math.min(item.getChance() * multiplier, 1.0);
      
      if (random.nextDouble() <= adjustedChance) {
        // ⭐ Multiplier เพิ่มจำนวนของ
        int baseAmount = item.getMinAmount();
        if (item.getMaxAmount() > item.getMinAmount()) {
          baseAmount = item.getMinAmount() + random.nextInt(item.getMaxAmount() - item.getMinAmount() + 1);
        }
        
        // คูณจำนวนด้วย multiplier (อย่างน้อย 1)
        int finalAmount = Math.max(1, (int) Math.ceil(baseAmount * multiplier));

        ItemStack itemStack = new ItemStack(item.getMaterial(), finalAmount);

        // เพิ่ม enchantments
        if (!item.getEnchantments().isEmpty()) {
          ItemMeta meta = itemStack.getItemMeta();
          for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
            // ⭐ Multiplier เพิ่มระดับ enchantment (แต่ไม่เกินขีดจำกัด)
            int enchantLevel = entry.getValue();
            if (multiplier > 1.0) {
              enchantLevel = Math.min(
                (int) Math.ceil(enchantLevel * Math.sqrt(multiplier)), // ใช้ sqrt เพื่อไม่ให้เพิ่มเร็วเกินไป
                entry.getKey().getMaxLevel()
              );
            }
            meta.addEnchant(entry.getKey(), enchantLevel, true);
          }
          itemStack.setItemMeta(meta);
        }

        // เพิ่มชื่อและ lore
        if (item.getDisplayName() != null || !item.getLore().isEmpty()) {
          ItemMeta meta = itemStack.getItemMeta();
          if (item.getDisplayName() != null) {
            meta.setDisplayName(item.getDisplayName());
          }
          if (!item.getLore().isEmpty()) {
            meta.setLore(item.getLore());
          }
          itemStack.setItemMeta(meta);
        }

        loot.add(itemStack);
      }
    }

    return loot;
  }

  // Class สำหรับ item แต่ละชิ้น
  public static class LootItem {
    private final Material material;
    private final int minAmount;
    private final int maxAmount;
    private final double chance;
    private final Map<Enchantment, Integer> enchantments;
    private final String displayName;
    private final List<String> lore;

    public LootItem(Material material, int minAmount, int maxAmount, double chance,
                    Map<Enchantment, Integer> enchantments, String displayName, List<String> lore) {
      this.material = material;
      this.minAmount = minAmount;
      this.maxAmount = maxAmount;
      this.chance = chance;
      this.enchantments = enchantments != null ? enchantments : new HashMap<>();
      this.displayName = displayName;
      this.lore = lore != null ? lore : new ArrayList<>();
    }

    public Material getMaterial() {
      return material;
    }

    public int getMinAmount() {
      return minAmount;
    }

    public int getMaxAmount() {
      return maxAmount;
    }

    public double getChance() {
      return chance;
    }

    public Map<Enchantment, Integer> getEnchantments() {
      return enchantments;
    }

    public String getDisplayName() {
      return displayName;
    }

    public List<String> getLore() {
      return lore;
    }
  }
}
