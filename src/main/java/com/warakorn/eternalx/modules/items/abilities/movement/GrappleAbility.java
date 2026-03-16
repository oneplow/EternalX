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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;

/**
 * 15. Grapple: ดึงผู้เล่นลอยไปยังจุดที่มอง (raytrace)
 * มี particle สายลากตัวผู้เล่นไปอย่างนุ่มนวล
 */
public class GrappleAbility extends Ability {
  private final int maxDistance;
  private final double speed;

  public GrappleAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.RIGHT_CLICK, config);
    this.maxDistance = getIntConfig(config, "max_distance", 30);
    this.speed = getDoubleConfig(config, "speed", 1.5);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof PlayerInteractEvent)) return false;

    Block targetBlock = player.getTargetBlockExact(maxDistance);
    if (targetBlock == null || targetBlock.isPassable()) {
      return false; // ไม่เจอบล็อกแข็ง
    }

    Location targetLoc = targetBlock.getLocation().add(0.5, 1.0, 0.5); // ยืนบนบล็อก
    Location startLoc = player.getLocation().clone();

    // เสียงเริ่มต้น
    player.getWorld().playSound(startLoc, Sound.ENTITY_FISHING_BOBBER_THROW, 1.0f, 1.2f);

    // Smooth flight ไปยังเป้าหมาย
    new BukkitRunnable() {
      int ticks = 0;
      final int maxTicks = 60; // ไม่เกิน 3 วินาที

      @Override
      public void run() {
        if (!player.isOnline() || player.isDead()) {
          this.cancel();
          return;
        }

        Location currentLoc = player.getLocation();
        double distance = currentLoc.distance(targetLoc);

        // ถึงแล้ว หรือ timeout
        if (distance < 1.5 || ticks >= maxTicks) {
          player.setVelocity(new Vector(0, 0, 0)); // หยุด
          player.getWorld().playSound(currentLoc, Sound.BLOCK_TRIPWIRE_ATTACH, 1.0f, 1.0f);
          player.getWorld().spawnParticle(Particle.CLOUD, currentLoc.add(0, 0.5, 0), 10, 0.3, 0.3, 0.3, 0.02);
          this.cancel();
          return;
        }

        // คำนวณทิศทาง
        Vector direction = targetLoc.toVector().subtract(currentLoc.toVector()).normalize();
        double pullSpeed = Math.min(speed, distance * 0.5); // ช้าลงเมื่อใกล้ถึง
        direction.multiply(pullSpeed);

        // ป้องกันตกน้ำหนัก gravity
        if (direction.getY() < 0.1) {
          direction.setY(0.1);
        }

        player.setVelocity(direction);
        // ปิด fall damage ระหว่างลอย
        player.setFallDistance(0);

        // Particle trail ตามตัวผู้เล่น
        player.getWorld().spawnParticle(Particle.CRIT, currentLoc.add(0, 0.5, 0), 3, 0.1, 0.1, 0.1, 0.02);

        // Particle เส้นสายไปยังเป้าหมาย (ทุกๆ 3 ticks เพื่อ performance)
        if (ticks % 3 == 0) {
          Vector lineDir = targetLoc.toVector().subtract(currentLoc.toVector()).normalize();
          Location particleLoc = currentLoc.clone();
          double lineDist = currentLoc.distance(targetLoc);
          for (double i = 0; i < lineDist; i += 2.0) {
            particleLoc.add(lineDir.clone().multiply(2.0));
            player.getWorld().spawnParticle(Particle.ENCHANTED_HIT, particleLoc, 1, 0, 0, 0, 0);
          }
        }

        ticks++;
      }
    }.runTaskTimer(plugin, 0L, 1L);

    return true;
  }
}
