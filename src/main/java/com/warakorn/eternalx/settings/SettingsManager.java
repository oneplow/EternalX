package com.warakorn.eternalx.settings;

import com.warakorn.eternalx.EternalX;

import java.io.File;

public class SettingsManager {
  private final EternalX plugin;
  private File importsFolder;
  private File structuresFolder;
  private boolean debugMode;

  public SettingsManager(EternalX plugin) {
    this.plugin = plugin;
    setup();
  }

  private void setup() {
    plugin.saveDefaultConfig();
    loadConfigValues();

    importsFolder = new File(plugin.getDataFolder(), "imports");
    if (!importsFolder.exists()) {
      importsFolder.mkdirs();
      plugin.getLogger().info("Created 'imports' folder.");
    }

    structuresFolder = new File(plugin.getDataFolder(), "structures");
    if (!structuresFolder.exists()) {
      structuresFolder.mkdirs();
      plugin.getLogger().info("Created 'structures' folder.");
    }
  }

  private void loadConfigValues() {
    this.debugMode = plugin.getConfig().getBoolean("debug-mode", false);
  }

  public void reload() {
    plugin.reloadConfig();
    loadConfigValues();
    if (debugMode) {
      plugin.getLogger().info("§aDebug mode is enabled.");
    }
  }

  // ✅ Fix #12: ลบ broadcast ออก — command handler จะส่งข้อความเอง
  public void setDebugMode(boolean enabled) {
    this.debugMode = enabled;
    plugin.getConfig().set("debug-mode", enabled);
    plugin.saveConfig();
  }

  public boolean isDebugMode() {
    return debugMode;
  }

  public File getImportsFolder() {
    return importsFolder;
  }

  public File getStructuresFolder() {
    return structuresFolder;
  }
}
