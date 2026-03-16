package com.warakorn.eternalx.modules.items.abilities.elemental;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 24. Ignite: จุดไฟเผาศัตรูเมื่อโจมตี ติด
 */
public class IgniteAbility extends Ability {
  private final int durationTicks; // 100 = 5s

  public IgniteAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_HIT, config);
    this.durationTicks = getIntConfig(config, "duration", 100);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent e)) return false;
    if (!(e.getEntity() instanceof LivingEntity victim)) return false;

    victim.setFireTicks(Math.max(victim.getFireTicks(), durationTicks));
    return true; // สำเร็จ กิน Cooldown
  }
}
