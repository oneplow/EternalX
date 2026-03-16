package com.warakorn.eternalx.modules.items.abilities.combat;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.Map;

/**
 * 7. Backstab: ทริกเกอร์ On-Hit โจมตีแรงขึ้นหากเล็งจากหลัง
 */
public class BackstabAbility extends Ability {
  private final double multiplier;

  public BackstabAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_HIT, config);
    this.multiplier = getDoubleConfig(config, "multiplier", 2.5);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent e)) return false;
    if (!(e.getEntity() instanceof LivingEntity victim)) return false;

    Vector attackerDir = player.getLocation().getDirection().normalize();
    Vector victimDir = victim.getLocation().getDirection().normalize();

    // ดอตโปรดักต์ระหว่าง 2 เวกเตอร์
    // ถ้ามองไปทางเดียวกัน = อยู่ข้างหลังกัน = ใกล้ 1.0 (สมมติให้มากกว่า 0.5 ประมาณ 60 องศา)
    if (attackerDir.dot(victimDir) > 0.5) {
      double originalDamage = e.getDamage();
      e.setDamage(originalDamage * multiplier);

      victim.getWorld().spawnParticle(Particle.DAMAGE_INDICATOR, victim.getLocation().add(0, 1, 0), 10);
      victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.0f, 1.0f);
      return true;
    }
    return false;
  }
}
