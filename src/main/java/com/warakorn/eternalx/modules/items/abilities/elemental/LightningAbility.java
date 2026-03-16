package com.warakorn.eternalx.modules.items.abilities.elemental;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * 26. Lightning: ผ่าจุดที่มองอยู่
 */
public class LightningAbility extends Ability {
  private final int maxDistance;

  public LightningAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.RIGHT_CLICK, config);
    this.maxDistance = getIntConfig(config, "max_distance", 30);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof PlayerInteractEvent)) return false;

    Block target = player.getTargetBlockExact(maxDistance);
    if (target != null) {
      target.getWorld().strikeLightning(target.getLocation());
      return true;
    }
    
    return false;
  }
}
