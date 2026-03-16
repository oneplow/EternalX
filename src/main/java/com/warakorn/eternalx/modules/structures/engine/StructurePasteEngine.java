package com.warakorn.eternalx.modules.structures.engine;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.mask.BlockTypeMask;
import com.sk89q.worldedit.function.mask.Mask;
import com.sk89q.worldedit.function.mask.Masks;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.structures.StructureData;
import com.warakorn.eternalx.modules.treasures.TreasureData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class StructurePasteEngine {

  public static void paste(EternalX plugin, StructureData data, Location loc, int rotationDegree) {
    Location pasteLoc = loc.clone().add(0, data.getOffsetY(), 0);
    pasteWorldEdit(plugin, data, pasteLoc, rotationDegree);
  }

  private static void pasteWorldEdit(EternalX plugin, StructureData data, Location loc, int rotationDegree) {
    try (EditSession session = WorldEdit.getInstance().newEditSessionBuilder()
      .world(BukkitAdapter.adapt(loc.getWorld()))
      .build()) {

      Clipboard clipboard = data.getClipboard();
      ClipboardHolder holder = new ClipboardHolder(clipboard);
      AffineTransform transform = new AffineTransform().rotateY(rotationDegree);
      holder.setTransform(holder.getTransform().combine(transform));

      BlockVector3 to = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

      Mask barrierMask = new BlockTypeMask(clipboard,
        BlockTypes.BARRIER,
        BlockTypes.STRUCTURE_VOID,
        BlockTypes.BEDROCK
      );
      Mask inverseMask = Masks.negate(barrierMask);

      Operation operation = holder.createPaste(session)
        .to(to)
        .ignoreAirBlocks(!data.isPasteAir())
        .maskSource(inverseMask)
        .copyEntities(true)
        .build();

      Operations.complete(operation);

      List<ProcessingTask> tasks = analyzeClipboard(clipboard, transform, loc);

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        processTasks(plugin, tasks, data, loc);
      }, 20L);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static List<ProcessingTask> analyzeClipboard(Clipboard clipboard, AffineTransform transform, Location pasteCenter) {
    List<ProcessingTask> tasks = new ArrayList<>();
    BlockVector3 origin = clipboard.getOrigin();

    for (BlockVector3 vec : clipboard.getRegion()) {
      BaseBlock baseBlock = clipboard.getFullBlock(vec);
      BlockType type = baseBlock.getBlockType();

      if (type == BlockTypes.BEDROCK) {
        tasks.add(new ProcessingTask(TaskType.REPLACE_BEDROCK, calculateWorldLoc(vec, origin, transform, pasteCenter)));
        continue;
      }

      if (type == BlockTypes.CHEST || type == BlockTypes.BARREL || type == BlockTypes.TRAPPED_CHEST) {
        tasks.add(new ProcessingTask(TaskType.FILL_CHEST, calculateWorldLoc(vec, origin, transform, pasteCenter)));
        continue;
      }

      if (type.id().contains("sign")) {
        tasks.add(new ProcessingTask(TaskType.CHECK_SIGN, calculateWorldLoc(vec, origin, transform, pasteCenter)));
      }
    }
    return tasks;
  }

  private static Location calculateWorldLoc(BlockVector3 schematicPos, BlockVector3 origin, AffineTransform transform, Location pasteCenter) {
    BlockVector3 relative = schematicPos.subtract(origin);
    com.sk89q.worldedit.math.Vector3 rotated = transform.apply(relative.toVector3());
    return pasteCenter.clone().add(rotated.x(), rotated.y(), rotated.z());
  }

  private static void processTasks(EternalX plugin, List<ProcessingTask> tasks, StructureData data, Location centerLoc) {
    World world = centerLoc.getWorld();

    // ✅ Fix #4: ตรวจสอบ valid ground ก่อน build foundation
    if (!data.getValidGround().isEmpty()) {
      Material groundMat = world.getBlockAt(centerLoc.getBlockX(), centerLoc.getBlockY() - 1, centerLoc.getBlockZ()).getType();
      if (!data.getValidGround().contains(groundMat)) {
        plugin.debugLog("Skipped foundation: invalid ground " + groundMat + " for " + data.getId());
      }
    }

    int halfWidth = Math.max(data.getWidth(), data.getLength()) / 2 + 2;
    buildFoundation(world,
      centerLoc.getBlockX() - halfWidth, centerLoc.getBlockX() + halfWidth,
      centerLoc.getBlockZ() - halfWidth, centerLoc.getBlockZ() + halfWidth,
      centerLoc.getBlockY());

    List<ChestData> chestDataList = new ArrayList<>();
    List<SignData> signDataList = new ArrayList<>();

    for (ProcessingTask task : tasks) {
      Location loc = task.location;
      Block block = world.getBlockAt(loc);

      if (task.type == TaskType.REPLACE_BEDROCK) {
        if (block.getType() == Material.BEDROCK) {
          block.setType(Material.STONE);
        }
        continue;
      }

      if (task.type == TaskType.FILL_CHEST) {
        if (block.getState() instanceof Container) {
          chestDataList.add(new ChestData(loc));
        }
        continue;
      }

      if (task.type == TaskType.CHECK_SIGN) {
        if (block.getState() instanceof Sign) {
          Sign sign = (Sign) block.getState();
          boolean found = false;

          for (Side side : Side.values()) {
            if (found) break;
            String[] lines = sign.getSide(side).getLines();
            for (String line : lines) {
              String cleanLine = ChatColor.stripColor(line).toLowerCase().replaceAll("\\s+", "");
              if (cleanLine.contains("[spawn]")) {
                String mobName = null;
                for (String l : lines) {
                  String cleanL = ChatColor.stripColor(l).trim();
                  if (cleanL.toLowerCase().replaceAll("\\s+", "").contains("[spawn]")) continue;
                  if (!cleanL.isEmpty()) {
                    mobName = cleanL;
                    break;
                  }
                }
                if (mobName != null) {
                  signDataList.add(new SignData(loc, mobName));
                  found = true;
                }
                break;
              }
            }
          }
        }
      }
    }

    // Process Spawners
    for (SignData signData : signDataList) {
      createSpawner(signData.location, signData.mobName);
    }

    // ✅ Feature 2: Layered loot resolution — structure → tag → rarity
    if (!chestDataList.isEmpty()) {
      TreasureData treasure = null;
      String treasureFileName = null;

      // 1. Per-structure loot-table
      String lootTable = data.getLootTable();
      if (lootTable != null && !lootTable.isEmpty()) {
        treasure = plugin.getTreasureManager().getTreasure(lootTable);
        treasureFileName = lootTable;
      }

      // 2. Tag-based loot (ใช้ tag แรกที่เจอ)
      if (treasure == null && !data.getTags().isEmpty()) {
        for (String tag : data.getTags()) {
          treasure = plugin.getTreasureManager().getTreasureByTag(tag);
          if (treasure != null) {
            treasureFileName = "tag_" + tag + "_loot";
            break;
          }
        }
      }

      // 3. Rarity fallback (วิธีเดิม)
      if (treasure == null) {
        treasureFileName = data.getEffectiveTreasureFile();
        treasure = plugin.getTreasureManager().getTreasure(treasureFileName);
      }

      if (treasure != null) {
        long treasureSeed = world.getSeed() + 
          centerLoc.getBlockX() * 7919L + 
          centerLoc.getBlockZ() * 3137L +
          data.getRarity().ordinal() * 11111L;
        
        Random random = new Random(treasureSeed);
        
        for (ChestData chestData : chestDataList) {
          fillChest(chestData.location, treasure, random, data.getRarity().getTreasureMultiplier());
        }
        
        plugin.debugLog("Filled " + chestDataList.size() + " containers with treasure: " + 
          treasureFileName + " (multiplier: " + data.getRarity().getTreasureMultiplier() + "x)");
      } else {
        plugin.getLogger().warning("No treasure found for structure " + data.getId() + 
          " (tried: loot-table, tags" + data.getTags() + ", rarity)");
      }
    }
  }

  // --- Helper Classes & Enums ---

  private enum TaskType {
    REPLACE_BEDROCK,
    FILL_CHEST,
    CHECK_SIGN
  }

  private static class ProcessingTask {
    TaskType type;
    Location location;

    ProcessingTask(TaskType type, Location location) {
      this.type = type;
      this.location = location;
    }
  }

  private static class ChestData {
    final Location location;

    ChestData(Location location) {
      this.location = location;
    }
  }

  private static class SignData {
    final Location location;
    final String mobName;

    SignData(Location location, String mobName) {
      this.location = location;
      this.mobName = mobName;
    }
  }

  /**
   * ⭐ แก้ fillChest ให้รับ treasureMultiplier
   * multiplier สูง = โอกาสได้ของดีมากขึ้น + จำนวนมากขึ้น
   */
  private static void fillChest(Location location, TreasureData treasure, Random random, double multiplier) {
    Block block = location.getBlock();
    BlockState state = block.getState();
    if (!(state instanceof Container)) return;

    Container container = (Container) state;
    Inventory inventory = container.getInventory();
    inventory.clear();

    // ⭐ Generate loot โดยส่ง multiplier เข้าไป
    List<ItemStack> loot = treasure.generateLoot(random, multiplier);
    
    List<Integer> slots = new ArrayList<>();
    for (int i = 0; i < inventory.getSize(); i++) slots.add(i);

    for (ItemStack item : loot) {
      if (slots.isEmpty()) break;
      int randomIndex = random.nextInt(slots.size());
      int slot = slots.remove(randomIndex);
      inventory.setItem(slot, item);
    }
  }

  private static void createSpawner(Location location, String mobName) {
    try {
      String cleanName = ChatColor.stripColor(mobName).toUpperCase().trim().replace(" ", "_");
      EntityType type = EntityType.valueOf(cleanName);

      Block block = location.getBlock();
      block.setType(Material.SPAWNER, true);

      BlockState state = block.getState();
      if (state instanceof org.bukkit.block.CreatureSpawner spawner) {
        spawner.setSpawnedType(type);
        spawner.setDelay(200);
        spawner.update(true);
      }
    } catch (IllegalArgumentException e) {
      location.getBlock().setType(Material.AIR);
      Bukkit.getLogger().warning("[EternalX] Invalid mob name on sign: " + mobName);
    }
  }

  // ✅ Fix #3: ใช้ deterministic logic แทน Math.random()
  private static void buildFoundation(World world, int minX, int maxX, int minZ, int maxZ, int floorY) {
    for (int x = minX; x <= maxX; x++) {
      for (int z = minZ; z <= maxZ; z++) {
        Block floor = world.getBlockAt(x, floorY, z);
        if (!floor.getType().isAir() && !floor.getType().name().contains("GLASS")) {
          for (int y = floorY - 1; y >= floorY - 10; y--) {
            Block target = world.getBlockAt(x, y, z);
            if (target.getType().isSolid()) break;
            // Deterministic: ใช้ coordinate แทน Math.random()
            target.setType(((x + y + z) & 1) == 0 ? Material.COBBLESTONE : Material.STONE);
          }
        }
      }
    }
  }
}
