package com.warakorn.eternalx.modules.items.loot;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.CustomItem;
import com.warakorn.eternalx.modules.items.ItemRarity;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * จัดการเรื่อง Drop System ของ Custom Item 
 * (ใช้สำหรับ Treasure/Chests หรือ Mobs Drop)
 */
public class CustomDropManager {
  private final EternalX plugin;
  private final Random random = new Random();

  public CustomDropManager(EternalX plugin) {
    this.plugin = plugin;
  }

  /**
   * สุ่มได้ Custom Items ตาม Rarity ของเหตุการณ์/หีบ 
   * @param tier Rarity ระดับของ Chest/Mob
   * @param amount จำนวนไอเท็มที่อยากให้สุ่มออกมา
   * @return รายชื่อของ ItemStack ที่สุ่มได้
   */
  public List<ItemStack> generateLoot(ItemRarity tier, int amount) {
    List<ItemStack> loot = new ArrayList<>();
    
    // ดึงไอเท็มทั้งหมดที่ให้ drop จากกล่อง/โครงสร้างได้
    List<CustomItem> possibleItems = new ArrayList<>();
    for (CustomItem item : plugin.getCustomItemManager().getItems().values()) {
      if (item.isStructureDrop() && isTierSatisfied(tier, item.getDropRarity())) {
        possibleItems.add(item);
      }
    }

    if (possibleItems.isEmpty()) return loot;

    for (int i = 0; i < amount; i++) {
        CustomItem chosen = possibleItems.get(random.nextInt(possibleItems.size()));
        ItemStack stack = plugin.getCustomItemManager().createItemStack(chosen.getId());
        if (stack != null && stack.getType() != Material.AIR) {
          loot.add(stack);
        }
    }

    return loot;
  }

  /**
   * เช็คว่ากล่องระดับ `chestTier` โอกาสพอที่จะดรอปไอเทมระดับ `itemTier` ไหม
   */
  private boolean isTierSatisfied(ItemRarity chestTier, ItemRarity itemTier) {
    // สมมติว่า หีบ Legend จะดรอปของ Legend เท่านั้น หรือของระดับต่ำกว่าได้
    return chestTier.ordinal() >= itemTier.ordinal();
  }
}
