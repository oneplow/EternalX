package com.warakorn.eternalx.modules.items.abilities.defense;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.items.abilities.Ability;
import com.warakorn.eternalx.modules.items.abilities.AbilityType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 11. Shield Wall: สร้างกำแพงล้อมหรือปิดกันดาเมจข้างหน้าชั่วคราว
 * เพื่อให้ชาว Bedrock เล่นง่าย กำแพงจะโผล่ในกระดานด้านหน้า
 */
public class ShieldWallAbility extends Ability {
  private final int durationTicks; // 100 = 5 วินาที
  private final int width; // 3 blocks

  public ShieldWallAbility(EternalX plugin, String id, Map<String, Object> config) {
    super(plugin, id, AbilityType.RIGHT_CLICK, config);
    this.durationTicks = getIntConfig(config, "duration", 100);
    this.width = getIntConfig(config, "width", 3);
  }

  @Override
  public boolean execute(Player player, ItemStack item, Event event) {
    if (!(event instanceof PlayerInteractEvent)) return false;

    Location center = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(2));
    Vector right = new Vector(-center.getDirection().getZ(), 0, center.getDirection().getX()).normalize();

    List<Block> changedBlocks = new ArrayList<>();
    List<BlockData> originalData = new ArrayList<>();

    // สร้างกำแพง 3x3
    int half = width / 2;
    for (int x = -half; x <= half; x++) {
      for (int y = 0; y < width; y++) {
        Location blockLoc = center.clone().add(right.clone().multiply(x)).add(0, y, 0);
        Block block = blockLoc.getBlock();
        
        // สนใจเฉพาะ block ที่ว่าง
        if (block.getType() == Material.AIR || block.getType() == Material.WATER || block.isReplaceable()) {
          changedBlocks.add(block);
          originalData.add(block.getBlockData());
          
          // โชว์ fake block ให้ทุกคนเห็น หรือวาง Block จริงเลย (วางจริงจะกัน mob ได้)
          block.setType(Material.GLASS); // เปลี่ยนเป็น Barrier ได้แต่จะมองไม่เห็นใช้ Glass แทนให้เห็นชัด
          block.getWorld().spawnParticle(Particle.CLOUD, blockLoc.add(0.5, 0.5, 0.5), 2, 0.2, 0.2, 0.2, 0);
        }
      }
    }

    if (changedBlocks.isEmpty()) return false;

    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1.0f, 0.5f);

    // กำจัดกำแพงตามเวลา
    new BukkitRunnable() {
      @Override
      public void run() {
        for (int i = 0; i < changedBlocks.size(); i++) {
          Block b = changedBlocks.get(i);
          if (b.getType() == Material.GLASS) { // กันเผื่อมีคนไปทุบแล้วสร้างอย่างอื่นทับ
            b.setBlockData(originalData.get(i));
            b.getWorld().spawnParticle(Particle.BLOCK_CRUMBLE, b.getLocation().add(0.5, 0.5, 0.5), 5, 
                plugin.getServer().createBlockData(Material.GLASS));
          }
        }
        player.getWorld().playSound(center, Sound.BLOCK_GLASS_BREAK, 1.0f, 0.5f);
      }
    }.runTaskLater(plugin, durationTicks);

    return true; // สำเร็จ
  }
}
