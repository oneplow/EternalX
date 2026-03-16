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
 * 5. Sweeping Wave: ปล่อยคลื่นโจมตีไปข้างหน้าเมื่อคลิกขวา
 */
public class SweepingWaveAbility extends Ability {
  private final double damage;
  private final int distance;

  public SweepingWaveAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.RIGHT_CLICK, config);
    this.damage = getDoubleConfig(config, "damage", 8.0);
    this.distance = getIntConfig(config, "distance", 10);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof PlayerInteractEvent)) return false;

    Location loc = player.getEyeLocation();
    Vector dir = loc.getDirection().normalize();
    player.getWorld().playSound(loc, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.5f);

    new BukkitRunnable() {
      int traveled = 0;
      Location current = loc.clone();

      @Override
      public void run() {
        if (traveled >= distance || !current.getBlock().isPassable()) {
          this.cancel();
          return;
        }

        current.add(dir.clone().multiply(1.0)); // ขยับทีละ 1 block
        current.getWorld().spawnParticle(Particle.SWEEP_ATTACK, current, 1);

        // ดาเมจ entity ในระยะนี้
        for (Entity e : current.getWorld().getNearbyEntities(current, 1.5, 1.5, 1.5)) {
          if (e == player || !(e instanceof LivingEntity target)) continue;

          target.damage(damage, player);
          target.setVelocity(dir.clone().multiply(0.5).setY(0.2));
        }

        traveled++;
      }
    }.runTaskTimer(plugin, 0L, 1L); // เคลื่อนที่เร็ว 20 blocks per second

    return true;
  }
}
