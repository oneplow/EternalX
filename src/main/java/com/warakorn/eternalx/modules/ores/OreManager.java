package com.warakorn.eternalx.modules.ores;

import com.warakorn.eternalx.EternalX;
import org.bukkit.configuration.file.FileConfiguration;

public class OreManager {

  private final EternalX plugin;

  public OreManager(EternalX plugin) {
    this.plugin = plugin;
  }

  public double getRate(String featureName) {
    FileConfiguration config = plugin.getConfig();

    // Loop ดูว่า featureName มี keyword ที่เราตั้งไว้ใน config ไหม
    if (config.isConfigurationSection("ores")) {
      for (String key : config.getConfigurationSection("ores").getKeys(false)) {
        if (featureName.contains(key)) {
          return config.getDouble("ores." + key, 1.0);
        }
      }
    }
    return 1.0; // Default ถ้าไม่เจอใน config
  }

}
