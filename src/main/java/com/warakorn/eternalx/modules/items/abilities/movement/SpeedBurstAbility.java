package com.warakorn.eternalx.modules.items.abilities.movement;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;

/**
 * 18. Speed Burst: ได้ Speed III เมื่อฆ่าศัตรู
 */
public class SpeedBurstAbility extends Ability {
  private final int durationTicks; // 100 = 5 วินาที
  private final int amplifier;     // 2 = Level 3

  public SpeedBurstAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_KILL, config);
    this.durationTicks = getIntConfig(config, "duration", 100);
    this.amplifier = getIntConfig(config, "amplifier", 2);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDeathEvent)) return false;

    // ให้ Speed
    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, durationTicks, amplifier, false, false, true));
    
    return true; // ทำงานสำเร็จ
  }
}
