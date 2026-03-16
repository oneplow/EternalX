package com.warakorn.eternalx.modules.items.abilities.movement;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 14. Dash: พุ่งไปข้างหน้าตามทิศทางที่มอง
 */
public class DashAbility extends Ability {
  private final double force; // 1.5 = พุ่งไปข้างหน้าประมาณ 8-10 blocks
  private final double damage;

  public DashAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.SNEAK, config);
    this.force = getDoubleConfig(config, "force", 1.5);
    this.damage = getDoubleConfig(config, "damage", 5.0);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    Vector dashDir = player.getLocation().getDirection().normalize().multiply(force);
    dashDir.setY(dashDir.getY() + 0.2); // ยกตัวขึ้นนิดนึงไม่ให้ขูดดพื้นจนเสียโมเมนตัม
    
    player.setVelocity(dashDir);

    player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, 0.5, 0.5, 0.5, 0.1);
    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

    // สร้าง Task เช็คการชนเพื่อทำดาเมจ 1 ครั้งต่อ 1 ตัว ขณะพุ่ง
    new BukkitRunnable() {
      int ticks = 0;
      final Set<LivingEntity> hitEntities = new HashSet<>();
      
      @Override
      public void run() {
        if (ticks++ > 10 || (player.isOnGround() && ticks > 3)) { // ทำงาน 0.5 วิ (10 ticks) หรือวิ่งจนลงพื้นแล้วหยุด
          this.cancel();
          return;
        }

        for (Entity e : player.getNearbyEntities(1.5, 1.5, 1.5)) {
          if (e instanceof LivingEntity) {
            LivingEntity target = (LivingEntity) e;
            if (target != player && !hitEntities.contains(target)) {
              hitEntities.add(target);
              target.damage(damage, player);
              target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.5, 0.5, 0.5, 0.1);
            }
          }
        }
      }
    }.runTaskTimer(plugin, 1L, 1L);

    return true;
  }
}
