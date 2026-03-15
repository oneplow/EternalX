package com.warakorn.eternalx;

import com.warakorn.eternalx.commands.EternalXCommand;
import com.warakorn.eternalx.commands.EternalXTabCompleter;
import com.warakorn.eternalx.listeners.NewChunkLoadEvent;
import com.warakorn.eternalx.modules.ores.BiomeModifier;
import com.warakorn.eternalx.modules.ores.OreManager;
import com.warakorn.eternalx.modules.structures.StructureManager;
import com.warakorn.eternalx.modules.structures.engine.StructurePasteQueue;
import com.warakorn.eternalx.modules.treasures.TreasureManager;
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

    // 5. Initialize Structure Manager
    this.structureManager = new StructureManager(this);
    this.structureManager.loadStructures();

    // 6. Initialize Paste Queue
    this.pasteQueue = new StructurePasteQueue(this);
    this.pasteQueue.start();

    // 7. Register Listeners
    getServer().getPluginManager().registerEvents(new NewChunkLoadEvent(this), this);

    // 8. Register Commands
    if (getCommand("eternalx") != null) {
      getCommand("eternalx").setExecutor(new EternalXCommand(this));
      getCommand("eternalx").setTabCompleter(new EternalXTabCompleter(this));
    }

    // 9. Inject Ore Modifications (delay 1 tick เพื่อรอให้ Server โหลดโลกเสร็จก่อน)
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
    getLogger().info("EternalX has been disabled!");
  }

  public void debugLog(String message) {
    if (settingsManager.isDebugMode()) {
      getLogger().info("[DEBUG] " + message);
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
}
