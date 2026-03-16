package com.warakorn.eternalx.modules.items.abilities.elemental;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 25. Freeze: สโลว์ + ตีช้าลง + particle น้ำแข็ง + บล็อกน้ำแข็งรอบตัว
 */
public class FreezeAbility extends Ability {
  private final int durationTicks;
  private final int amplifier;

  public FreezeAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.ON_HIT, config);
    this.durationTicks = getIntConfig(config, "duration", 60); // 3 วินาที
    this.amplifier = getIntConfig(config, "amplifier", 1); // Slowness II
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof EntityDamageByEntityEvent e)) return false;
    if (!(e.getEntity() instanceof LivingEntity victim)) return false;

    // Potion effects
    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, durationTicks, amplifier));
    victim.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, durationTicks, amplifier));

    // Snowflake particles
    victim.getWorld().spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);

    // วาง ICE block รอบตัว victim ชั่วคราว
    spawnIceCage(victim, durationTicks);

    return true;
  }

  /**
   * สร้างกรงน้ำแข็งรอบตัว mob ชั่วคราว
   * วางบล็อก ICE รอบ 4 ด้าน + บน ให้ดูเหมือนโดน freeze จริงๆ
   */
  private void spawnIceCage(LivingEntity victim, int durationTicks) {
    Location center = victim.getLocation().getBlock().getLocation();
    List<Block> iceBlocks = new ArrayList<>();

    // ตำแหน่งรอบตัว: 4 ด้าน (ข้าง) + 1 บน
    int[][] offsets = {
        {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, // 4 ด้านข้าง
        {1, 1, 0}, {-1, 1, 0}, {0, 1, 1}, {0, 1, -1}, // 4 ด้านบนรอบ
        {0, 2, 0}  // บนหัว
    };

    for (int[] offset : offsets) {
      Block block = center.clone().add(offset[0], offset[1], offset[2]).getBlock();
      // วางเฉพาะตรงที่เป็นอากาศ ไม่ทับบล็อกจริง
      if (block.getType() == Material.AIR || block.getType() == Material.CAVE_AIR) {
        block.setType(Material.ICE);
        iceBlocks.add(block);
      }
    }

    // ลบ ICE หลังหมดเวลา
    if (!iceBlocks.isEmpty()) {
      new BukkitRunnable() {
        @Override
        public void run() {
          for (Block block : iceBlocks) {
            if (block.getType() == Material.ICE) {
              block.setType(Material.AIR);
              // Particle ตอน ICE แตก
              block.getWorld().spawnParticle(Particle.BLOCK,
                  block.getLocation().add(0.5, 0.5, 0.5), 8,
                  0.3, 0.3, 0.3, 0,
                  Material.ICE.createBlockData());
            }
          }
        }
      }.runTaskLater(plugin, durationTicks);
    }
  }
}
