package com.warakorn.eternalx;

import com.warakorn.eternalx.commands.EternalXCommand;
import com.warakorn.eternalx.commands.EternalXTabCompleter;
import com.warakorn.eternalx.listeners.NewChunkLoadEvent;
import com.warakorn.eternalx.modules.ores.BiomeModifier;
import com.warakorn.eternalx.modules.ores.OreManager;
import com.warakorn.eternalx.modules.structures.ExclusionZoneManager;
import com.warakorn.eternalx.modules.structures.StructureManager;
import com.warakorn.eternalx.modules.structures.engine.StructurePasteQueue;
import com.warakorn.eternalx.modules.structures.preview.StructurePreviewManager;
import com.warakorn.eternalx.modules.treasures.TreasureManager;
import com.warakorn.eternalx.modules.items.CustomItemManager;
import com.warakorn.eternalx.modules.items.AbilityListener;
import com.warakorn.eternalx.modules.items.abilities.AbilityRegistry;
import com.warakorn.eternalx.modules.items.abilities.CooldownManager;
import com.warakorn.eternalx.modules.items.abilities.ManaManager;
import com.warakorn.eternalx.modules.items.crafting.CraftingManager;
import com.warakorn.eternalx.settings.SettingsManager;
import com.warakorn.eternalx.settings.ValidWorldsManager;
import org.bukkit.plugin.java.JavaPlugin;

public class EternalX extends JavaPlugin {
  private SettingsManager settingsManager;
  private OreManager oreManager;
  private StructureManager structureManager;
  private StructurePasteQueue pasteQueue;
  private TreasureManager treasureManager;
  private ValidWorldsManager validWorldsManager;
  private ExclusionZoneManager exclusionZoneManager;     // ✅ Feature 1
  private StructurePreviewManager previewManager;         // ✅ Feature 3
  
  private CustomItemManager customItemManager;
  private AbilityRegistry abilityRegistry;
  private CooldownManager cooldownManager;
  private ManaManager manaManager;
  private CraftingManager craftingManager;
  private com.warakorn.eternalx.modules.items.loot.CustomDropManager customDropManager;

  @Override
  public void onEnable() {
    // 1. Initialize Settings
    this.settingsManager = new SettingsManager(this);

    // 2. Initialize Ore Manager
    this.oreManager = new OreManager(this);

    // 3. Initialize Valid Worlds Manager
    this.validWorldsManager = new ValidWorldsManager(this);

    // 4. Initialize Treasure Manager
    this.treasureManager = new TreasureManager(this);
    this.treasureManager.loadTreasures();

    // 5. Initialize Structure Manager (includes SchematicCache)
    this.structureManager = new StructureManager(this);
    this.structureManager.loadStructures();

    // 6. Initialize Exclusion Zone Manager
    this.exclusionZoneManager = new ExclusionZoneManager(this);

    // 7. Initialize Preview Manager
    this.previewManager = new StructurePreviewManager(this);

    // 8. Initialize Paste Queue
    this.pasteQueue = new StructurePasteQueue(this);
    this.pasteQueue.start();

    // == Custom Items System Initialization ==
    this.customItemManager = new CustomItemManager(this);
    this.abilityRegistry = new AbilityRegistry(this);
    this.cooldownManager = new CooldownManager(this);
    this.manaManager = new ManaManager(this);
    this.craftingManager = new CraftingManager(this);
    this.customDropManager = new com.warakorn.eternalx.modules.items.loot.CustomDropManager(this);
    
    this.customItemManager.loadItems();

    // 9. Register Listeners
    getServer().getPluginManager().registerEvents(new NewChunkLoadEvent(this), this);
    getServer().getPluginManager().registerEvents(new AbilityListener(this), this);

    // 10. Register Commands
    if (getCommand("eternalx") != null) {
      getCommand("eternalx").setExecutor(new EternalXCommand(this));
      getCommand("eternalx").setTabCompleter(new EternalXTabCompleter(this));
    }

    // 11. Inject Ore Modifications
    if (getConfig().getBoolean("ores-enabled", true)) {
      getServer().getScheduler().runTask(this, () -> {
        getLogger().info("Injecting CoreOres...");
        try {
          BiomeModifier modifier = new BiomeModifier(this);
          int modifiedCount = modifier.modifyAllBiomes();
          getLogger().info("Success! Modified ore generation for " + modifiedCount + " biomes.");
        } catch (Exception e) {
          getLogger().severe("Failed to modify biomes!");
          e.printStackTrace();
        }
      });
    }

    getLogger().info("EternalX has been enabled!");
  }

  @Override
  public void onDisable() {
    if (pasteQueue != null) {
      pasteQueue.stop();
    }
    if (previewManager != null) {
      previewManager.clearAll();
    }
    if (craftingManager != null) {
      craftingManager.clearRecipes();
    }
    getLogger().info("EternalX has been disabled!");
  }

  public void debugLog(String message) {
    if (settingsManager.isDebugMode()) {
      getServer().getConsoleSender().sendMessage(
          net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection()
          .deserialize("§7[EternalX] [DEBUG] " + message)
      );
    }
  }

  public SettingsManager getSettingsManager() {
    return settingsManager;
  }

  public StructureManager getStructureManager() {
    return structureManager;
  }

  public StructurePasteQueue getPasteQueue() {
    return pasteQueue;
  }

  public TreasureManager getTreasureManager() {
    return treasureManager;
  }

  public ValidWorldsManager getValidWorldsManager() {
    return validWorldsManager;
  }

  public OreManager getOreManager() {
    return oreManager;
  }

  // ✅ Feature 1
  public ExclusionZoneManager getExclusionZoneManager() {
    return exclusionZoneManager;
  }

  // ✅ Feature 3
  public StructurePreviewManager getPreviewManager() {
    return previewManager;
  }

  // == Custom Items Getters ==
  public CustomItemManager getCustomItemManager() {
    return customItemManager;
  }

  public AbilityRegistry getAbilityRegistry() {
    return abilityRegistry;
  }

  public CooldownManager getCooldownManager() {
    return cooldownManager;
  }

  public ManaManager getManaManager() {
    return manaManager;
  }

  public CraftingManager getCraftingManager() {
    return craftingManager;
  }

  public com.warakorn.eternalx.modules.items.loot.CustomDropManager getCustomDropManager() {
    return customDropManager;
  }
}
