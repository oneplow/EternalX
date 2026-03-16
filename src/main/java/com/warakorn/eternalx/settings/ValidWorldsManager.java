package com.warakorn.eternalx.settings;

import com.warakorn.eternalx.EternalX;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ValidWorldsManager implements Listener {
  private final EternalX plugin;
  private final File configFile;
  private YamlConfiguration config;
  private final Map<String, Boolean> validWorlds = new HashMap<>();
  private boolean whitelistNewWorlds;

  public ValidWorldsManager(EternalX plugin) {
    this.plugin = plugin;
    this.configFile = new File(plugin.getDataFolder(), "ValidWorlds.yml");
    load();
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

  public void load() {
    validWorlds.clear();

    if (!configFile.exists()) {
      createDefault();
    }

    config = YamlConfiguration.loadConfiguration(configFile);
    whitelistNewWorlds = config.getBoolean("new-worlds-spawn-structures", true);

    for (World world : Bukkit.getWorlds()) {
      String worldName = world.getName();
      if (config.contains("valid-worlds." + worldName)) {
        validWorlds.put(worldName, config.getBoolean("valid-worlds." + worldName));
      } else {
        registerNewWorld(world);
      }
    }

    org.bukkit.command.ConsoleCommandSender console = plugin.getServer().getConsoleSender();
    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer legacy = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();

    console.sendMessage(legacy.deserialize("§7[EternalX] Loaded valid worlds configuration:"));

    for (Map.Entry<String, Boolean> entry : validWorlds.entrySet()) {
      // ✅ Fix #10: ใช้ §a/§c แทน ANSI codes ที่ไม่ทำงานบน MC console
      String status = entry.getValue() ? "§aEnabled" : "§cDisabled";
      console.sendMessage(legacy.deserialize("§7[EternalX]   - §e" + entry.getKey() + "§r: " + status));
    }
  }

  private void createDefault() {
    try {
      configFile.getParentFile().mkdirs();
      configFile.createNewFile();
      config = YamlConfiguration.loadConfiguration(configFile);

      config.set("new-worlds-spawn-structures", true);
      config.set("info", "Set worlds to 'true' to enable structure generation, 'false' to disable");

      for (World world : Bukkit.getWorlds()) {
        config.set("valid-worlds." + world.getName(), true);
      }

      config.save(configFile);
      plugin.getLogger().info("Created default ValidWorlds.yml");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @EventHandler
  public void onWorldLoad(WorldLoadEvent event) {
    registerNewWorld(event.getWorld());
  }

  private void registerNewWorld(World world) {
    String worldName = world.getName();

    if (validWorlds.containsKey(worldName)) {
      return;
    }

    validWorlds.put(worldName, whitelistNewWorlds);
    config.set("valid-worlds." + worldName, whitelistNewWorlds);

    try {
      config.save(configFile);

      // ✅ Fix #10: ใช้ Minecraft color codes แทน ANSI
      String status = whitelistNewWorlds ? "§aenabled" : "§cdisabled";
      plugin.getLogger().info("§eRegistered new world: " + worldName + " " + status);

    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public boolean isValidWorld(World world) {
    return validWorlds.getOrDefault(world.getName(), whitelistNewWorlds);
  }

  public boolean isValidWorld(String worldName) {
    return validWorlds.getOrDefault(worldName, whitelistNewWorlds);
  }

  public void setWorldValid(String worldName, boolean valid) {
    validWorlds.put(worldName, valid);
    config.set("valid-worlds." + worldName, valid);

    try {
      config.save(configFile);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Map<String, Boolean> getValidWorlds() {
    return new HashMap<>(validWorlds);
  }

  public boolean isWhitelistNewWorlds() {
    return whitelistNewWorlds;
  }
}
