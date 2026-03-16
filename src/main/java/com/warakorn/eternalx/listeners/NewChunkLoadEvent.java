package com.warakorn.eternalx.listeners;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.structures.ExclusionZoneManager;
import com.warakorn.eternalx.modules.structures.StructureData;
import com.warakorn.eternalx.modules.structures.engine.StructurePasteEngine;
import com.warakorn.eternalx.modules.structures.engine.StructurePlacementEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class NewChunkLoadEvent implements Listener {
  private final EternalX plugin;

  // ✅ Fix #5: Cache grouped structures per world
  private final Map<String, Map<Integer, List<StructureData>>> worldStructureCache = new ConcurrentHashMap<>();
  private int lastKnownStructureCount = -1;

  public NewChunkLoadEvent(EternalX plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    if (!event.isNewChunk()) return;

    Chunk chunk = event.getChunk();
    World world = chunk.getWorld();

    if (!plugin.getValidWorldsManager().isValidWorld(world)) {
      return;
    }

    long seed = world.getSeed();

    // ✅ Fix #5: ใช้ cache แทนการ groupBy ทุกครั้ง
    int currentCount = plugin.getStructureManager().getStructures().size();
    if (currentCount != lastKnownStructureCount) {
      worldStructureCache.clear();
      lastKnownStructureCount = currentCount;
    }

    Map<Integer, List<StructureData>> groupedStructures =
      worldStructureCache.computeIfAbsent(world.getName(), wn ->
        plugin.getStructureManager().getStructures().stream()
          .filter(data -> data.canSpawnInWorld(world))
          .collect(Collectors.groupingBy(StructureData::getSpacing))
      );

    for (Map.Entry<Integer, List<StructureData>> entry : groupedStructures.entrySet()) {
      int spacing = entry.getKey();
      List<StructureData> candidates = entry.getValue();

      String groupSeedId = "GROUP_" + spacing;

      if (StructurePlacementEngine.shouldSpawnInChunk(seed, chunk.getX(), chunk.getZ(), groupSeedId, spacing)) {

        // ✅ Feature 1: เช็ค Exclusion Zone
        ExclusionZoneManager exclusionManager = plugin.getExclusionZoneManager();
        StructureData tempSelected = candidates.get(0); // ใช้ตัวแรกคำนวณ radius
        int exclusionRadius = ExclusionZoneManager.calculateRadius(tempSelected);
        
        if (exclusionManager.isOccupied(world.getName(), chunk.getX(), chunk.getZ(), exclusionRadius)) {
          plugin.debugLog("Skipped chunk (" + chunk.getX() + "," + chunk.getZ() + "): exclusion zone");
          continue;
        }

        Random rand = new Random(seed + chunk.getX() * 99999L + chunk.getZ() * 11111L);

        StructureData selected = selectWeightedStructure(candidates, rand);
        if (selected == null) continue;

        // ✅ Feature 1: อัพเดท radius ตาม structure ที่ถูกเลือกจริง
        int finalRadius = ExclusionZoneManager.calculateRadius(selected);

        int rotation = selected.getRotation(rand);

        Location loc = StructurePlacementEngine.findPlacementLocation(chunk, selected, seed, rotation);
        if (loc == null) continue;

        if (!checkBiome(selected, loc, world)) {
          continue;
        }

        // ✅ Feature 1: Mark exclusion zone
        exclusionManager.markOccupied(world.getName(), chunk.getX(), chunk.getZ(), finalRadius);

        int finalRotation = rotation;

        plugin.getPasteQueue().add(() -> {
          StructurePasteEngine.paste(plugin, selected, loc, finalRotation);

          if (plugin.getSettingsManager().isDebugMode()) {
            sendDebugNotification(selected, loc, world.getName(), finalRotation);
          }
        });
      }
    }
  }

  private boolean checkBiome(StructureData data, Location loc, World world) {
    Biome centerBiome = world.getBiome(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

    if (!data.isStrictBiomeCheck()) {
      return data.canSpawnInBiome(centerBiome);
    }

    int width = data.getWidth();
    int length = data.getLength();
    int samplesNeeded = Math.min(9, width * length);
    int validSamples = 0;

    for (int i = 0; i < samplesNeeded; i++) {
      int offsetX = (int) ((i % 3) * (width / 2.0));
      int offsetZ = (int) ((i / 3) * (length / 2.0));

      Biome biome = world.getBiome(
        loc.getBlockX() + offsetX,
        loc.getBlockY(),
        loc.getBlockZ() + offsetZ
      );

      if (data.canSpawnInBiome(biome)) {
        validSamples++;
      }
    }

    return (validSamples * 1.0 / samplesNeeded) >= 0.7;
  }

  private StructureData selectWeightedStructure(List<StructureData> list, Random rand) {
    double totalWeight = 0.0;
    for (StructureData data : list) {
      totalWeight += data.getFinalWeight();
    }
    if (totalWeight <= 0) return list.get(0);

    double value = rand.nextDouble() * totalWeight;
    for (StructureData data : list) {
      value -= data.getFinalWeight();
      if (value <= 0) {
        return data;
      }
    }
    return list.get(list.size() - 1);
  }

  /**
   * ✅ Feature 5: Adventure API debug notification
   */
  private void sendDebugNotification(StructureData data, Location loc, String worldName, int rotation) {
    int x = loc.getBlockX();
    int y = loc.getBlockY();
    int z = loc.getBlockZ();

    Component message = Component.text()
      .append(Component.text("[", NamedTextColor.DARK_GRAY))
      .append(Component.text("EternalX", NamedTextColor.GOLD))
      .append(Component.text("] ", NamedTextColor.DARK_GRAY))
      .append(Component.text("Structure spawned!", NamedTextColor.GRAY))
      .append(Component.newline())
      .append(Component.text("▪ ", NamedTextColor.YELLOW))
      .append(Component.text("Rarity: ", NamedTextColor.WHITE))
      .append(Component.text(data.getRarity().name(), data.getRarity().getAdventureColor()))
      .append(Component.newline())
      .append(Component.text("▪ ", NamedTextColor.YELLOW))
      .append(Component.text("Name: ", NamedTextColor.WHITE))
      .append(Component.text(data.getId(), NamedTextColor.GOLD))
      .append(Component.newline())
      .append(Component.text("▪ ", NamedTextColor.YELLOW))
      .append(Component.text("Location: ", NamedTextColor.WHITE))
      .append(Component.text("[" + x + ", " + y + ", " + z + "]", NamedTextColor.GREEN)
        .clickEvent(ClickEvent.runCommand("/tp " + x + " " + y + " " + z))
        .hoverEvent(HoverEvent.showText(
          Component.text("Click to teleport!", NamedTextColor.GREEN)
            .append(Component.newline())
            .append(Component.text("/tp " + x + " " + y + " " + z, NamedTextColor.GRAY))
        )))
      .append(Component.newline())
      .append(Component.text("▪ ", NamedTextColor.YELLOW))
      .append(Component.text("World: ", NamedTextColor.WHITE))
      .append(Component.text(worldName, NamedTextColor.AQUA))
      .append(Component.newline())
      .append(Component.text("▪ ", NamedTextColor.YELLOW))
      .append(Component.text("Type: ", NamedTextColor.WHITE))
      .append(Component.text(data.getPlacementType().name(), NamedTextColor.LIGHT_PURPLE))
      .append(Component.newline())
      .append(Component.text("▪ ", NamedTextColor.YELLOW))
      .append(Component.text("Rotation: ", NamedTextColor.WHITE))
      .append(Component.text(rotation + "°", NamedTextColor.YELLOW))
      .append(Component.newline())
      .append(Component.text("▪ ", NamedTextColor.YELLOW))
      .append(Component.text("Weight: ", NamedTextColor.WHITE))
      .append(Component.text(String.format("%.2f", data.getFinalWeight()), NamedTextColor.GRAY))
      .build();

    // Console log
    Bukkit.getConsoleSender().sendMessage("§8[§6EternalX§8] " + data.getRarity().getDisplayName() + 
      " §7structure: §6" + data.getId() +
      " §7at §a[" + x + ", " + y + ", " + z + "] " +
      "§7in §b" + worldName + " §8(" + data.getPlacementType() + ", " + rotation + "°)");

    // ✅ Feature 5: Adventure API
    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.hasPermission("eternalx.debug")) {
        player.sendMessage(message);
      }
    }
  }
}
