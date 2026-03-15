package com.warakorn.eternalx.commands;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.ores.OreManager;
import com.warakorn.eternalx.modules.structures.StructureData;
import com.warakorn.eternalx.modules.structures.engine.StructurePasteEngine;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EternalXCommand implements CommandExecutor {
  private final EternalX plugin;

  public EternalXCommand(EternalX plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) return false;

    String sub = args[0].toLowerCase();

    // --- RELOAD COMMAND ---
    if (sub.equals("reload")) {
      if (!sender.hasPermission("eternalx.admin")) {
        sender.sendMessage(ChatColor.RED + "You don't have permission.");
        return true;
      }
      sender.sendMessage(ChatColor.YELLOW + "Reloading EternalX...");
      long start = System.currentTimeMillis();
      plugin.getSettingsManager().reload();
      plugin.getValidWorldsManager().load(); // ✅ Fix #13: reload valid worlds
      plugin.getStructureManager().loadStructures();
      plugin.getTreasureManager().loadTreasures();
      long time = System.currentTimeMillis() - start;
      sender.sendMessage(ChatColor.GREEN + "EternalX Reloaded in " + time + "ms.");
      return true;
    }

    // --- SPAWN COMMAND ---
    if (sub.equals("spawn")) {
      if (!(sender instanceof Player)) {
        sender.sendMessage(ChatColor.RED + "Only players can use this command.");
        return true;
      }
      if (!sender.hasPermission("eternalx.spawn")) {
        sender.sendMessage(ChatColor.RED + "You don't have permission.");
        return true;
      }
      if (args.length < 2) {
        sender.sendMessage(ChatColor.RED + "Usage: /eternalx spawn <name>");
        return true;
      }

      String structureId = args[1];
      StructureData data = plugin.getStructureManager().getStructure(structureId);
      if (data == null) {
        sender.sendMessage(ChatColor.RED + "Structure not found: " + structureId);
        return true;
      }

      Player player = (Player) sender;
      Location loc = player.getLocation();

      StructurePasteEngine.paste(plugin, data, loc, 0);

      // ส่งข้อความพร้อมพิกัดที่คลิกได้
      TextComponent message = new TextComponent(data.getRarity().getDisplayName() + " §astructure spawned: §6" + structureId + " §aat ");
      TextComponent coords = new TextComponent("§e[" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]");
      coords.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
        "/tp " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()));
      coords.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
        new ComponentBuilder("§aClick to teleport back!").create()));

      message.addExtra(coords);
      player.spigot().sendMessage(message);

      return true;
    }

    // --- DEBUG COMMAND ---
    if (sub.equals("debug")) {
      if (!sender.hasPermission("eternalx.admin")) {
        sender.sendMessage(ChatColor.RED + "You don't have permission.");
        return true;
      }

      boolean currentMode = plugin.getSettingsManager().isDebugMode();
      boolean newMode = !currentMode;
      plugin.getSettingsManager().setDebugMode(newMode);

      if (newMode) {
        sender.sendMessage("§8[§6EternalX§8] §fDebug Mode: §aENABLED");
        sender.sendMessage("§7You will now receive structure spawn notifications.");
      } else {
        sender.sendMessage("§8[§6EternalX§8] §fDebug Mode: §cDISABLED");
        sender.sendMessage("§7Structure spawn notifications are now hidden.");
      }
      return true;
    }

    // --- LIST COMMAND ---
    if (sub.equals("list")) {
      if (!sender.hasPermission("eternalx.admin")) {
        sender.sendMessage(ChatColor.RED + "You don't have permission.");
        return true;
      }

      sender.sendMessage("§8§m------------------§r §6EternalX Structures §8§m------------------");

      if (plugin.getStructureManager().getStructures().isEmpty()) {
        sender.sendMessage("§7No structures loaded.");
      } else {
        for (StructureData data : plugin.getStructureManager().getStructures()) {
          TextComponent line = new TextComponent(data.getRarity().getDisplayName() + " §f" + data.getId() + " §8(§7" +
            data.getPlacementType() + "§8, §7" +
            data.getDimensionType() + "§8)");
          if (sender instanceof Player) {
            line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
              new ComponentBuilder("§7Rarity: " + data.getRarity().getDisplayName() + "\n" +
                "§7Base Weight: §e" + data.getWeight() + "\n" +
                "§7Final Weight: §e" + String.format("%.2f", data.getFinalWeight()) + "\n" +
                "§7Spacing: §e" + data.getSpacing() + " chunks\n" +
                "§7Size: §e" + data.getWidth() + "x" + data.getHeight() + "x" + data.getLength() + "\n" +
                "§7Treasure Multiplier: §e" + data.getRarity().getTreasureMultiplier() + "x\n" +
                "§aClick to spawn!").create()));
            line.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
              "/eternalx spawn " + data.getId()));
            ((Player) sender).spigot().sendMessage(line);
          } else {
            sender.sendMessage(line.getText());
          }
        }
      }

      sender.sendMessage("§8§m---------------------------------------------------");
      return true;
    }

    return false;
  }
}
