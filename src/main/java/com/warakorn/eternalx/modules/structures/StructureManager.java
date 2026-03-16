package com.warakorn.eternalx.modules.structures;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.warakorn.eternalx.EternalX;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class StructureManager {
  private final EternalX plugin;
  private final List<StructureData> loadedStructures = new ArrayList<>();
  private final SchematicCache schematicCache; // ✅ Feature 4

  public StructureManager(EternalX plugin) {
    this.plugin = plugin;
    this.schematicCache = new SchematicCache(plugin);
  }

  /**
   * โหลดโครงสร้างจาก folder structure
   * imports/{rarity}/ → schematic files
   * structures/{rarity}/ → config files
   */
  public void loadStructures() {
    loadedStructures.clear();
    schematicCache.clear(); // ✅ Feature 4: เคลียร์ cache เมื่อ reload

    File importsDir = plugin.getSettingsManager().getImportsFolder();
    File structuresDir = plugin.getSettingsManager().getStructuresFolder();

    // สร้าง rarity folders ถ้ายังไม่มี
    for (StructureRarity rarity : StructureRarity.values()) {
      File rarityFolder = new File(importsDir, rarity.name().toLowerCase());
      if (!rarityFolder.exists()) {
        rarityFolder.mkdirs();
        plugin.getLogger().info("Created rarity folder: " + rarityFolder.getName());
      }
    }

    // สแกนทุก rarity folder
    for (StructureRarity rarity : StructureRarity.values()) {
      File rarityFolder = new File(importsDir, rarity.name().toLowerCase());
      loadStructuresFromRarityFolder(rarityFolder, rarity, structuresDir);
    }

    org.bukkit.command.ConsoleCommandSender console = plugin.getServer().getConsoleSender();
    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();

    console.sendMessage(legacy.deserialize("§a[EternalX] Loaded §e" + loadedStructures.size() + " §astructures:"));
    
    // แสดงสถิติตาม rarity
    Map<StructureRarity, Long> countByRarity = loadedStructures.stream()
      .collect(Collectors.groupingBy(StructureData::getRarity, Collectors.counting()));
    
    for (StructureRarity rarity : StructureRarity.values()) {
      long count = countByRarity.getOrDefault(rarity, 0L);
      if (count > 0) {
        console.sendMessage(legacy.deserialize("§7[EternalX]   " + rarity.getDisplayName() + "§r: §e" + count + " §7structures"));
      }
    }

    // ✅ Feature 4: แสดง memory info
    console.sendMessage(legacy.deserialize("§7[EternalX] Schematic cache: §e" + schematicCache.getRegisteredCount() + " §7registered"));
  }

  private void loadStructuresFromRarityFolder(File rarityFolder, StructureRarity rarity, File structuresDir) {
    if (!rarityFolder.exists() || !rarityFolder.isDirectory()) {
      return;
    }

    File[] schemFiles = rarityFolder.listFiles((dir, name) -> 
      name.endsWith(".schem") || name.endsWith(".schematic"));

    if (schemFiles == null || schemFiles.length == 0) {
      return;
    }

    // สร้าง rarity folder ใน structures/ ด้วย
    File rarityConfigFolder = new File(structuresDir, rarity.name().toLowerCase());
    if (!rarityConfigFolder.exists()) {
      rarityConfigFolder.mkdirs();
      plugin.getLogger().info("Created config folder: structures/" + rarity.name().toLowerCase());
    }

    for (File schemFile : schemFiles) {
      String structureName = schemFile.getName()
        .replace(".schem", "")
        .replace(".schematic", "");
      
      // Config file อยู่ใน structures/{rarity}/
      File configFile = new File(rarityConfigFolder, structureName + ".yml");

      if (!configFile.exists()) {
        createDefaultConfig(configFile, schemFile.getName(), rarity);
        plugin.getLogger().info("Generated config: structures/" + rarity.name().toLowerCase() + "/" + structureName + ".yml");
      }
      
      loadSingleStructure(structureName, configFile, schemFile, rarity);
    }
  }

  private void createDefaultConfig(File configFile, String schemFileName, StructureRarity rarity) {
    try {
      configFile.createNewFile();
      YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

      config.set("file", schemFileName);
      config.set("rarity", rarity.name());
      config.set("weight", 1.0);
      
      int spacing = switch (rarity) {
        case COMMON -> 15;
        case UNCOMMON -> 20;
        case RARE -> 30;
        case EPIC -> 40;
        case LEGENDARY -> 60;
        case MYTHIC -> 80;
      };
      config.set("spacing", spacing);
      config.set("offset-y", 1);

      // Rotation
      config.set("rotation.mode", "RANDOM");
      config.set("rotation.enforce-horizontal", true);
      config.set("rotation.enforce-vertical", true);
      config.set("rotation.allowed-angles", Arrays.asList(0, 90, 180, 270));

      config.set("paste-air", false);
      config.set("placement-type", "SURFACE");
      config.set("dimension", "OVERWORLD");

      // Biome
      config.set("biome.mode", "WHITELIST");
      config.set("biome.strict-check", false);
      config.set("biome.allowed", Arrays.asList("PLAINS", "FOREST", "BIRCH_FOREST"));
      config.set("biome.forbidden", Arrays.asList("DESERT", "SNOWY_TAIGA"));

      config.set("ground-blocks", Arrays.asList("GRASS_BLOCK", "DIRT", "PODZOL", "STONE"));

      // Treasure — rarity-based default
      config.set("treasure-file", rarity.getSuggestedTreasureTier() + "_loot");

      // ✅ Feature 2: Tags + loot-table (ค่า default ว่าง)
      config.set("tags", Collections.emptyList());
      config.set("loot-table", "");

      config.save(configFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void loadSingleStructure(String id, File configFile, File schemFile, StructureRarity rarity) {
    try {
      YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
      
      if (!schemFile.exists()) {
        plugin.getLogger().warning("Missing schematic: " + schemFile.getAbsolutePath());
        return;
      }

      // ✅ Feature 4: Lazy loading — โหลด clipboard เพื่ออ่าน dimensions แล้วใส่ cache
      Clipboard clipboard = schematicCache.loadAndCache(id, schemFile);
      if (clipboard == null) return;

      int width = clipboard.getDimensions().x();
      int height = clipboard.getDimensions().y();
      int length = clipboard.getDimensions().z();

      // ============ ค่าพื้นฐาน ============
      double weight = config.getDouble("weight", 1.0);
      int spacing = config.getInt("spacing", 20);
      int offsetY = config.getInt("offset-y", 0);
      boolean pasteAir = config.getBoolean("paste-air", false);

      // ============ Placement Type & Dimension ============
      PlacementType placementType;
      try {
        placementType = PlacementType.valueOf(config.getString("placement-type", "SURFACE").toUpperCase());
      } catch (IllegalArgumentException e) {
        placementType = PlacementType.SURFACE;
      }

      DimensionType dimensionType;
      try {
        dimensionType = DimensionType.valueOf(config.getString("dimension", "OVERWORLD").toUpperCase());
      } catch (IllegalArgumentException e) {
        dimensionType = DimensionType.OVERWORLD;
      }

      // ============ Rotation System ============
      RotationMode rotationMode;
      try {
        rotationMode = RotationMode.valueOf(config.getString("rotation.mode", "RANDOM").toUpperCase());
      } catch (IllegalArgumentException e) {
        rotationMode = RotationMode.RANDOM;
      }

      int[] allowedRotations = null;
      if (rotationMode == RotationMode.CUSTOM) {
        List<Integer> angles = config.getIntegerList("rotation.allowed-angles");
        if (!angles.isEmpty()) {
          allowedRotations = angles.stream().mapToInt(Integer::intValue).toArray();
        } else {
          allowedRotations = new int[]{0, 90, 180, 270};
        }
      }

      boolean enforceHorizontal = config.getBoolean("rotation.enforce-horizontal", true);
      boolean enforceVertical = config.getBoolean("rotation.enforce-vertical", true);

      // ============ Biome System ============
      BiomeMatchMode biomeMatchMode;
      try {
        biomeMatchMode = BiomeMatchMode.valueOf(config.getString("biome.mode", "WHITELIST").toUpperCase());
      } catch (IllegalArgumentException e) {
        biomeMatchMode = BiomeMatchMode.WHITELIST;
      }

      boolean strictBiomeCheck = config.getBoolean("biome.strict-check", false);

      Set<Biome> allowedBiomes = config.getStringList("biome.allowed").stream()
        .map(s -> {
          try {
            String keyStr = s.toLowerCase(Locale.ROOT);
            if (!keyStr.contains(":")) keyStr = "minecraft:" + keyStr;
            return Registry.BIOME.get(NamespacedKey.fromString(keyStr));
          } catch (Exception e) {
            return null;
          }
        }).filter(Objects::nonNull).collect(Collectors.toSet());

      Set<Biome> forbiddenBiomes = config.getStringList("biome.forbidden").stream()
        .map(s -> {
          try {
            String keyStr = s.toLowerCase(Locale.ROOT);
            if (!keyStr.contains(":")) keyStr = "minecraft:" + keyStr;
            return Registry.BIOME.get(NamespacedKey.fromString(keyStr));
          } catch (Exception e) {
            return null;
          }
        }).filter(Objects::nonNull).collect(Collectors.toSet());

      Set<Material> ground = config.getStringList("ground-blocks").stream()
        .map(Material::matchMaterial).filter(Objects::nonNull).collect(Collectors.toSet());

      // ============ Treasure ============
      String treasureFile = config.getString("treasure-file", null);

      // Rarity
      StructureRarity finalRarity;
      String rarityStr = config.getString("rarity");
      if (rarityStr != null) {
        try {
          finalRarity = StructureRarity.valueOf(rarityStr.toUpperCase());
        } catch (IllegalArgumentException e) {
          finalRarity = rarity;
        }
      } else {
        finalRarity = rarity;
      }

      // ✅ Feature 2: Tags + loot-table
      Set<String> tags = new HashSet<>(config.getStringList("tags"));
      String lootTable = config.getString("loot-table", "");

      // ============ สร้าง StructureData ============
      loadedStructures.add(new StructureData(
        id, schematicCache, width, height, length,
        weight, spacing, offsetY,
        allowedBiomes, forbiddenBiomes, ground,
        rotationMode != RotationMode.NONE, pasteAir,
        placementType, dimensionType,
        treasureFile, biomeMatchMode, strictBiomeCheck,
        rotationMode, allowedRotations, enforceHorizontal, enforceVertical,
        finalRarity, tags, lootTable
      ));

      plugin.debugLog("Loaded: §e" + id + " " + finalRarity.getDisplayName() +
        " §7[" + placementType + ", " + dimensionType + ", weight=" + 
        String.format("%.2f", finalRarity.calculateFinalWeight(weight)) + 
        (tags.isEmpty() ? "" : ", tags=" + tags) + "]");

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public List<StructureData> getStructures() {
    return Collections.unmodifiableList(loadedStructures);
  }

  public StructureData getStructure(String id) {
    for (StructureData data : loadedStructures) {
      if (data.getId().equalsIgnoreCase(id)) return data;
    }
    return null;
  }

  public SchematicCache getSchematicCache() {
    return schematicCache;
  }
}
