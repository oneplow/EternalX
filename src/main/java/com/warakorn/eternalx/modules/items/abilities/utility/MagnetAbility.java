package com.warakorn.eternalx.modules.items.abilities.utility;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;

/**
 * 19. Magnet: ดูดไอเท็มที่ตกพื้นรอบๆ ตัว (Passive)
 */
public class MagnetAbility extends Ability {
  private final double radius;
  private final double speed;

  public MagnetAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.PASSIVE, config);
    this.radius = getDoubleConfig(config, "radius", 8.0);
    this.speed = getDoubleConfig(config, "speed", 0.5);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    boolean active = false;

    // หา Item ที่ตกบนพื้น
    for (Entity en : player.getNearbyEntities(radius, radius, radius)) {
      if (en instanceof Item floorItem && floorItem.isValid()) {
        if (!floorItem.hasMetadata("no_pickup")) { // กันบางปลั๊กอิน
          Vector dir = player.getLocation().toVector().subtract(floorItem.getLocation().toVector()).normalize();
          floorItem.setVelocity(dir.multiply(speed));
          active = true;
        }
      }
    }

    return active; // ไม่ต้องติด cooldown passive เพื่อให้ดูดเรื่อยๆ (แต่ Magnet ทำงานใน task ทุก 1s)
  }
}
