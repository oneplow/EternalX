package com.warakorn.eternalx.modules.items.abilities.utility;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 22. Smelting Touch: ขุดแร่แล้วหลอมเป็น Ingot อัตโนมัติ (และเปลี่ยน Trigger ให้รอ BlockBreakEvent)
 */
public class SmeltingTouchAbility extends Ability {

  public SmeltingTouchAbility(EternalX plugin, String id, Map<String, Object> config) {
    // จริงๆ ใช้ ON_HIT ก็ได้ แต่เดี๋ยวบล็อคแตกจะใช้ BlockBreakEvent แทรกลงมาแทนใน Listener แบบพิเศษ 
    super(plugin, id, AbilityType.PASSIVE, config); 
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof BlockBreakEvent breakEvent)) return false;

    // เช็คว่าไอเท็มที่ตกมีอะไรบ้าง แล้วแปลง
    breakEvent.setDropItems(false); // ปิดการดร็อปปกติ

    Material type = breakEvent.getBlock().getType();
    Material result = switch (type) {
      case IRON_ORE, DEEPSLATE_IRON_ORE -> Material.IRON_INGOT;
      case GOLD_ORE, DEEPSLATE_GOLD_ORE -> Material.GOLD_INGOT;
      case COPPER_ORE, DEEPSLATE_COPPER_ORE -> Material.COPPER_INGOT;
      case ANCIENT_DEBRIS -> Material.NETHERITE_SCRAP;
      case SAND -> Material.GLASS;
      case COBBLESTONE -> Material.STONE;
      case STONE -> Material.SMOOTH_STONE;
      case CLAY -> Material.TERRACOTTA;
      case NETHERRACK -> Material.NETHER_BRICK;
      default -> null; // ไม่ใช่แร่ที่หลอมได้ ให้อย่างอื่นดร็อปปกติ
    };

    if (result != null) {
      // ดรอปของหลอมสุก
      int count = 1; // อิง Fortune ได้ถ้าอยากให้แอดวานซ์ แต่ทำแบบ 1:1 ง่ายๆก่อน
      breakEvent.getBlock().getWorld().dropItemNaturally(breakEvent.getBlock().getLocation().add(0.5, 0.5, 0.5), new ItemStack(result, count));
      
      // ดรอป EXP ให้ด้วย ถือว่าหลอมแร่สำเร็จแบบเตาเผา
      breakEvent.setExpToDrop((int)(Math.random() * 3) + 1);
    } else {
      // แร่อื่น ดรอปปกติ
      for (ItemStack drop : breakEvent.getBlock().getDrops(item)) {
        breakEvent.getBlock().getWorld().dropItemNaturally(breakEvent.getBlock().getLocation().add(0.5, 0.5, 0.5), drop);
      }
    }

    return false; // ไม่จับ Cooldown ถือว่าเป็น Passive ติดตัวขวาน/ที่ขุดตลอด
  }
}
