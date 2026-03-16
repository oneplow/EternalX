package com.warakorn.eternalx.modules.items.abilities.combat;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Location;
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

import java.util.Map;

/**
 * 6. Ground Slam: กระโดดขึ้นและตกลงมากระแทกพื้นสร้าง AoE Damage
 */
public class GroundSlamAbility extends Ability {
  private final double radius;
  private final double damage;
  private final double knockback;

  public GroundSlamAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.RIGHT_CLICK, config);
    this.radius = getDoubleConfig(config, "radius", 5.0);
    this.damage = getDoubleConfig(config, "damage", 10.0);
    this.knockback = getDoubleConfig(config, "knockback", 1.5);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof PlayerInteractEvent)) return false;

    // เด้งขึ้นฟ้า
    player.setVelocity(new Vector(0, 1.2, 0));
    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 0.5f, 0.5f);

    // เช็คตอนตกถึงพื้น
    new BukkitRunnable() {
      int ticks = 0;
      @Override
      public void run() {
        if (!player.isOnline() || player.isDead()) {
          this.cancel();
          return;
        }

        ticks++;
        if (ticks > 5 && ((Entity) player).isOnGround()) {
          Location loc = player.getLocation();
          loc.getWorld().spawnParticle(Particle.EXPLOSION, loc, 3);
          loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);

          for (Entity e : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
            if (e == player || !(e instanceof LivingEntity target)) continue;
            
            target.damage(damage, player);
            
            // ยกศัตรูให้ลอยออกไป (Knockback แบบกระจายออกจากตัวเล่น)
            Vector dir = target.getLocation().toVector().subtract(loc.toVector()).normalize();
            dir.multiply(knockback).setY(0.7);
            target.setVelocity(dir);
          }
          this.cancel();
        } else if (ticks > 100) { // ป้องกัน loop ค้างถ้าบิน
          this.cancel();
        }
      }
    }.runTaskTimer(plugin, 0L, 2L);

    return true;
  }
}
