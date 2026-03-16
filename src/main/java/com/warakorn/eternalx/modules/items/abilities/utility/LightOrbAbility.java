package com.warakorn.eternalx.modules.items.abilities.utility;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.Event;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * 21. Light Orb: ยิงลูกบอลแสงสว่างไปข้างหน้า เมื่อชนกำแพงจะวาง Light Block
 */
public class LightOrbAbility extends Ability {
  private final int lightLevel; // ระดับแสง 1-15

  public LightOrbAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.RIGHT_CLICK, config);
    this.lightLevel = getIntConfig(config, "light_level", 15);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    // 1. กระบวนการยิงออกไป
    if (event instanceof PlayerInteractEvent) {
      Snowball orb = player.launchProjectile(Snowball.class);
      orb.setItem(new ItemStack(Material.GLOWSTONE_DUST));
      orb.setShooter(player);
      orb.setMetadata("eternalx_light_orb", new FixedMetadataValue(plugin, lightLevel));
      
      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.0f, 1.5f);

      // Effect หางยาวบอลแสง
      new BukkitRunnable() {
        @Override
        public void run() {
          if (!orb.isValid() || orb.isDead()) {
            this.cancel();
            return;
          }
          orb.getWorld().spawnParticle(Particle.END_ROD, orb.getLocation(), 1, 0, 0, 0, 0.05);
        }
      }.runTaskTimer(plugin, 1L, 1L);
      
      return true; // กิน Cooldown
    }
    
    // 2. กระบวนการตอนชนกำแพง (อันนี้ส่งมาจาก Listener ผ่านการยิง Event แซมเข้ามา แต่เพื่อความสะอาด เราจะดักตรงๆใน AbilityListener หรือใช้ Metadata)
    // หมายเหตุ: การเขียนรวมใน Ability เดียวกันแบบนี้ต้องให้ Listener รองรับ Event แปลกๆ ด้วย
    // ตอนนี้เราจะ return false แล้วไปดัก ProjectileHitEvent เพิ่มเติมใน AbilityListener ให้ส่งมาที่นี่
    else if (event instanceof ProjectileHitEvent hitEvent) {
      if (hitEvent.getHitBlock() != null) {
        org.bukkit.block.Block target = hitEvent.getHitBlock().getRelative(hitEvent.getHitBlockFace());
        if (target.getType() == Material.AIR || target.getType() == Material.WATER || target.getType() == Material.CAVE_AIR) {
          target.setType(Material.LIGHT);
          
          org.bukkit.block.data.type.Light lightData = (org.bukkit.block.data.type.Light) target.getBlockData();
          lightData.setLevel(Math.min(15, Math.max(0, lightLevel)));
          target.setBlockData(lightData);
          
          target.getWorld().playSound(target.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 2.0f);
          target.getWorld().spawnParticle(Particle.FLASH, target.getLocation().add(0.5, 0.5, 0.5), 1);
        }
      }
      return false; // ไม่ต้องคิด Cooldown ทวน
    }
    
    return false;
  }
}
