package com.warakorn.eternalx.modules.items.abilities.combat;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 8. Chain Lightning: ฟ้าผ่าใส่ศัตรูที่ตี และชิ่งไปโดนตัวอื่นใกล้ๆ
 */
public class ChainLightningAbility extends Ability {
  private final int targets; // จำนวนคนที่ชิ่งไปโดน
  private final double damage; 
  private final double radius;

  public ChainLightningAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_HIT, config);
    this.targets = getIntConfig(config, "targets", 3);
    this.damage = getDoubleConfig(config, "damage", 5.0);
    this.radius = getDoubleConfig(config, "radius", 6.0);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent e)) return false;
    if (!(e.getEntity() instanceof LivingEntity primaryVictim)) return false;

    // ผ่าตัวแรก (ให้ effect เฉยๆ ดาเมจเราทำเอง)
    primaryVictim.getWorld().strikeLightningEffect(primaryVictim.getLocation());
    primaryVictim.damage(damage, player);

    // หารอบๆ
    List<LivingEntity> hitTargets = new ArrayList<>();
    hitTargets.add(primaryVictim);

    int count = 1;

    for (Entity ent : primaryVictim.getNearbyEntities(radius, radius, radius)) {
      if (count >= targets) break;
      if (ent == player || !(ent instanceof LivingEntity target)) continue;
      if (hitTargets.contains(target)) continue;

      target.getWorld().strikeLightningEffect(target.getLocation());
      target.damage(damage, player);

      hitTargets.add(target);
      count++;
    }

    return true;
  }
}
