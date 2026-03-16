package com.warakorn.eternalx.modules.items.abilities.combat;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * 2. Wither Touch: ให้ Wither effect เมื่อตีติด
 */
public class WitherTouchAbility extends Ability {
  private final int durationTicks; // ระยะเวลา (20 = 1s)
  private final int amplifier;     // ความแรง (0 = level 1)

  public WitherTouchAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_HIT, config);
    this.durationTicks = getIntConfig(config, "duration", 60); // 3 วินาที
    this.amplifier = getIntConfig(config, "amplifier", 0);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent e)) return false;
    if (!(e.getEntity() instanceof LivingEntity victim)) return false;

    victim.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, durationTicks, amplifier));
    return true;
  }
}
