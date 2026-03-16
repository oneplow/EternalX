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
 * 15. Grapple: ดึงผู้เล่นไปหาจุดที่มอง (คล้ายๆ grapple hook/spider-man)
 */
public class GrappleAbility extends Ability {
  private final int maxDistance;
  private final double forceMultiplier;

  public GrappleAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.RIGHT_CLICK, config);
    this.maxDistance = getIntConfig(config, "max_distance", 30);
    this.forceMultiplier = getDoubleConfig(config, "force", 0.5);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof PlayerInteractEvent)) return false;

    Block targetB = player.getTargetBlockExact(maxDistance);
    if (targetB == null || targetB.isPassable()) {
      // ไม่เจอบล็อคแข็ง
      return false; 
    }

    Location targetLoc = targetB.getLocation().add(0.5, 0.5, 0.5);
    Location playerLoc = player.getLocation();

    double distance = playerLoc.distance(targetLoc);
    
    // ตั้ง Vector ทิศทางไปยังจุดหมาย
    Vector trajectory = targetLoc.toVector().subtract(playerLoc.toVector()).normalize();
    
    // ปรับความแรงตามระยะทาง (ยิ่งไกล ยิ่งต้องดึงแรง)
    // เพิ่ม Y ขึ้นนิดเผื่อติดขอบ
    trajectory.multiply(distance * forceMultiplier).setY(trajectory.getY() + 0.5);
    
    // จำกัดความเร็วสูงสุดเพือป้องกัน server กระตุกหรือทะลุบล็อค
    if (trajectory.lengthSquared() > 16.0) { // Max length roughly 4.0
        trajectory.normalize().multiply(4.0);
    }

    player.setVelocity(trajectory);

    // Particle effect โยงเส้นประเสมือนสลิง
    Vector dir = targetLoc.toVector().subtract(playerLoc.toVector()).normalize();
    Location particleLoc = playerLoc.clone().add(0, 1, 0); 
    while (particleLoc.distance(targetLoc) > 1.0) {
      particleLoc.add(dir);
      player.getWorld().spawnParticle(Particle.CRIT, particleLoc, 1, 0, 0, 0, 0);
    }

    player.getWorld().playSound(player.getLocation(), Sound.ITEM_CROSSBOW_SHOOT, 1.0f, 1.5f);
    player.getWorld().playSound(targetLoc, Sound.BLOCK_TRIPWIRE_ATTACH, 1.0f, 1.0f);

    return true;
  }
}
