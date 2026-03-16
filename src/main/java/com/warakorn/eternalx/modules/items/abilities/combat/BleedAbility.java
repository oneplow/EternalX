package com.warakorn.eternalx.modules.items.abilities.combat;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * 3. Bleed: สร้าง DoT (Damage over time) เลือดไหลแบบไม่สนใจเกราะ
 */
public class BleedAbility extends Ability {
  private final int ticks;       // ระยะเวลาทั้งหมด (ติ๊กละ 20 = 1s)
  private final double damage;   // ดาเมจต่อติ๊ก (1s)

  public BleedAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_HIT, config);
    this.ticks = getIntConfig(config, "duration", 100); // 5วิ
    this.damage = getDoubleConfig(config, "damage", 1.0); // 1 damage (0.5 heart)
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent e)) return false;
    if (!(e.getEntity() instanceof LivingEntity victim)) return false;

    new BukkitRunnable() {
      int ticksLived = 0;
      @Override
      public void run() {
        if (victim.isDead() || !victim.isValid() || ticksLived >= ticks) {
          this.cancel();
          return;
        }

        victim.damage(damage, player); // สร้างความเสียหาย
        victim.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, victim.getLocation().add(0, 1, 0), 5, 
            plugin.getServer().createBlockData(org.bukkit.Material.REDSTONE_BLOCK));

        ticksLived += 20; // 1 วินาที
      }
    }.runTaskTimer(plugin, 20L, 20L); // รันทุก 1 วิ

    return true;
  }
}
