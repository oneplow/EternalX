package com.warakorn.eternalx.modules.items.abilities.elemental;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Location;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.Map;

/**
 * 27. Poison Cloud: สร้างควันพิษด้านหน้า
 */
public class PoisonCloudAbility extends Ability {
  private final int durationTicks; // ระยะเวลาควันอยู่ (100 = 5s)
  private final float radius;

  public PoisonCloudAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.RIGHT_CLICK, config);
    this.durationTicks = getIntConfig(config, "duration", 100);
    this.radius = (float) getDoubleConfig(config, "radius", 4.0);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof PlayerInteractEvent)) return false;

    Vector forward = player.getLocation().getDirection().setY(0).normalize().multiply(3); // หน้า 3 blocks
    Location spawnLoc = player.getLocation().add(forward);

    AreaEffectCloud cloud = (AreaEffectCloud) spawnLoc.getWorld().spawnEntity(spawnLoc, EntityType.AREA_EFFECT_CLOUD);
    cloud.setRadius(radius);
    cloud.setDuration(durationTicks);
    cloud.setColor(org.bukkit.Color.GREEN);
    cloud.setSource(player); // ระบุผู้สร้าง — AbilityListener จะกรองไม่ให้โดนตัวเอง
    cloud.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 60, 1), true);

    return true;
  }
}
