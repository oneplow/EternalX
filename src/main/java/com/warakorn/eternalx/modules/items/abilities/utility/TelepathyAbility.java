package com.warakorn.eternalx.modules.items.abilities.utility;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 23. Telepathy: ขุดบล็อคแล้วของเข้าตัวอัตโนมัติ (และเปลี่ยน Trigger ให้รอ BlockBreakEvent แบบพิเศษเหมือน SmeltingTouch)
 */
public class TelepathyAbility extends Ability {

  public TelepathyAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.PASSIVE, config);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof BlockBreakEvent breakEvent)) return false;

    // ถ้ามีที่ว่างในตัว ให้ของเข้าตัว ถ้าไม่มีค่อยตกพื้น
    breakEvent.setDropItems(false);

    boolean inventoryFull = false;

    // ต้องพิจารณาตอนที่ใช้คู่กับ Smelting ทีหลัง แต่ตอนนี้ถือว่าดึง Drops ปกติ
    for (ItemStack drop : breakEvent.getBlock().getDrops(item)) {
      Map<Integer, ItemStack> leftover = player.getInventory().addItem(drop);
      if (!leftover.isEmpty()) {
        inventoryFull = true;
        for (ItemStack left : leftover.values()) {
          breakEvent.getBlock().getWorld().dropItemNaturally(breakEvent.getBlock().getLocation().add(0.5, 0.5, 0.5), left);
        }
      }
    }

    if (breakEvent.getExpToDrop() > 0) {
      player.giveExp(breakEvent.getExpToDrop());
      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.2f, 1.5f);
      breakEvent.setExpToDrop(0); // กันตกพื้น
    }

    if (inventoryFull) {
      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
    }

    return false; // ไม่ต้องคิด Cooldown
  }
}
