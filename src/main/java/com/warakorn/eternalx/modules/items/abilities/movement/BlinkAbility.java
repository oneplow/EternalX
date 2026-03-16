package com.warakorn.eternalx.modules.items.abilities.movement;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;

/**
 * 16. Blink: วาร์ประยะสั้นไปยังจุดที่มอง
 */
public class BlinkAbility extends Ability {
  private final int maxDistance;

  public BlinkAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.RIGHT_CLICK, config);
    this.maxDistance = getIntConfig(config, "max_distance", 15);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof PlayerInteractEvent)) return false;

    // หาระยะที่ไกลที่สุดที่ไปได้ (ติดกำแพงก็หยุดก่อนชนกำแพง)
    Location origin = player.getEyeLocation();
    Vector direction = origin.getDirection();
    Location targetLoc = origin.clone();
    
    boolean hitWall = false;
    for (int i = 0; i < maxDistance; i++) {
      targetLoc.add(direction);
      Block b = targetLoc.getBlock();
      if (!b.isPassable()) {
        hitWall = true;
        // ถอยหลังกลับมา 1 บล็อค เพื่อไม่ให้ติดคาในกำแพง
        targetLoc.subtract(direction);
        break;
      }
    }

    // หักลบความสูงตาออก เพื่อให้เท้าอยู่ที่พื้นตรงเป้าหมาย
    targetLoc.subtract(0, player.getEyeHeight() - 0.2, 0);

    // Particle ต้นทาง
    player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

    player.teleport(targetLoc);

    // Particle ปลายทาง
    player.getWorld().spawnParticle(Particle.PORTAL, targetLoc.add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.1);
    player.getWorld().playSound(targetLoc, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

    return true;
  }
}
