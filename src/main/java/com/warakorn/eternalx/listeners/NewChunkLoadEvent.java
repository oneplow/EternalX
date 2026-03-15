package com.warakorn.eternalx.listeners;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.structures.StructureData;
import com.warakorn.eternalx.modules.structures.engine.StructurePasteEngine;
import com.warakorn.eternalx.modules.structures.engine.StructurePlacementEngine;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
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

  // ✅ Fix #5: Cache grouped structures per world เพื่อไม่ต้อง recompute ทุก chunk load
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

    // เช็ค Valid Worlds
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

        Random rand = new Random(seed + chunk.getX() * 99999L + chunk.getZ() * 11111L);

        // ⭐ ใช้ getFinalWeight() แทน getWeight() เพื่อรวม rarity
        StructureData selected = selectWeightedStructure(candidates, rand);

        if (selected == null) continue;

        int rotation = selected.getRotation(rand);

        Location loc = StructurePlacementEngine.findPlacementLocation(chunk, selected, seed, rotation);
        if (loc == null) continue;

        if (!checkBiome(selected, loc, world)) {
          continue;
        }

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

  /**
   * ⭐ Weighted random ที่ใช้ getFinalWeight() (รวม rarity)
   */
  private StructureData selectWeightedStructure(List<StructureData> list, Random rand) {
    double totalWeight = 0.0;
    for (StructureData data : list) {
      totalWeight += data.getFinalWeight(); // ⭐ ใช้ finalWeight
    }
    if (totalWeight <= 0) return list.get(0);

    double value = rand.nextDouble() * totalWeight;
    for (StructureData data : list) {
      value -= data.getFinalWeight(); // ⭐ ใช้ finalWeight
      if (value <= 0) {
        return data;
      }
    }
    return list.get(list.size() - 1);
  }

  /**
   * ⭐ Debug notification ที่แสดง rarity
   */
  private void sendDebugNotification(StructureData data, Location loc, String worldName, int rotation) {
    int x = loc.getBlockX();
    int y = loc.getBlockY();
    int z = loc.getBlockZ();

    TextComponent prefix = new TextComponent("§8[§6EternalX§8] §7Structure spawned!");

    TextComponent rarityText = new TextComponent("\n§e▪ §fRarity: " + data.getRarity().getDisplayName());
    TextComponent structureText = new TextComponent("\n§e▪ §fName: §6" + data.getId());

    TextComponent coordsLabel = new TextComponent("\n§e▪ §fLocation: ");
    TextComponent coordsClickable = new TextComponent("§a[" + x + ", " + y + ", " + z + "]");
    coordsClickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tp " + x + " " + y + " " + z));
    coordsClickable.setHoverEvent(new HoverEvent(
      HoverEvent.Action.SHOW_TEXT,
      new ComponentBuilder("§aClick to teleport!\n§7/tp " + x + " " + y + " " + z).create()
    ));

    TextComponent worldText = new TextComponent("\n§e▪ §fWorld: §b" + worldName);
    TextComponent typeText = new TextComponent("\n§e▪ §fType: §d" + data.getPlacementType());
    TextComponent dimensionText = new TextComponent("\n§e▪ §fDimension: §c" + data.getDimensionType());
    TextComponent rotationText = new TextComponent("\n§e▪ §fRotation: §e" + rotation + "°");
    TextComponent weightText = new TextComponent(String.format("\n§e▪ §fSpawn Weight: §7%.2f", data.getFinalWeight()));

    TextComponent fullMessage = new TextComponent(prefix);
    fullMessage.addExtra(rarityText);
    fullMessage.addExtra(structureText);
    fullMessage.addExtra(coordsLabel);
    fullMessage.addExtra(coordsClickable);
    fullMessage.addExtra(worldText);
    fullMessage.addExtra(typeText);
    fullMessage.addExtra(dimensionText);
    fullMessage.addExtra(rotationText);
    fullMessage.addExtra(weightText);

    Bukkit.getConsoleSender().sendMessage("§8[§6EternalX§8] " + data.getRarity().getDisplayName() + 
      " §7structure: §6" + data.getId() +
      " §7at §a[" + x + ", " + y + ", " + z + "] " +
      "§7in §b" + worldName + " §8(" + data.getPlacementType() + ", " + data.getDimensionType() + ", " + rotation + "°)");

    for (Player player : Bukkit.getOnlinePlayers()) {
      if (player.hasPermission("eternalx.debug")) {
        player.spigot().sendMessage(fullMessage);
      }
    }
  }
}
