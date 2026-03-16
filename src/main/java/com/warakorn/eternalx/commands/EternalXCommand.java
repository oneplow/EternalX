package com.warakorn.eternalx.commands;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.structures.StructureData;
import com.warakorn.eternalx.modules.structures.engine.StructurePasteEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class EternalXCommand implements CommandExecutor {
  private final EternalX plugin;

  public EternalXCommand(EternalX plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (args.length == 0) return false;

    String sub = args[0].toLowerCase();

    // --- RELOAD ---
    if (sub.equals("reload")) {
      if (!sender.hasPermission("eternalx.admin")) {
        sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
        return true;
      }
      sender.sendMessage(Component.text("Reloading EternalX...", NamedTextColor.YELLOW));
      long start = System.currentTimeMillis();
      plugin.getSettingsManager().reload();
      plugin.getValidWorldsManager().load();
      plugin.getStructureManager().loadStructures();
      if (plugin.getCustomItemManager() != null) plugin.getCustomItemManager().loadItems();
      if (plugin.getCooldownManager() != null) plugin.getCooldownManager().clearAll();
      plugin.getTreasureManager().loadTreasures();
      plugin.getExclusionZoneManager().clear();
      long time = System.currentTimeMillis() - start;
      sender.sendMessage(Component.text("EternalX Reloaded in " + time + "ms.", NamedTextColor.GREEN));
      return true;
    }

    // --- ITEM (Custom Items) ---
    if (sub.equals("item") && args.length >= 2) {
      if (!sender.hasPermission("eternalx.admin")) {
        sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
        return true;
      }
      if (plugin.getCustomItemManager() == null) {
        sender.sendMessage(Component.text("Custom Item Manager is not initialized!", NamedTextColor.RED));
        return true;
      }

      String action = args[1].toLowerCase();

      if (action.equals("list")) {
        java.util.List<String> items = new java.util.ArrayList<>(plugin.getCustomItemManager().getItems().keySet());
        sender.sendMessage(Component.text("Custom Items (" + items.size() + "):", NamedTextColor.GOLD));
        for (String id : items) {
          sender.sendMessage(Component.text("- " + id, NamedTextColor.YELLOW));
        }
        return true;
      }

      if (action.equals("give") && args.length >= 4) {
        Player target = org.bukkit.Bukkit.getPlayer(args[2]);
        if (target == null) {
          sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
          return true;
        }

        String id = args[3].toLowerCase();
        org.bukkit.inventory.ItemStack item = plugin.getCustomItemManager().createItemStack(id);
        
        if (item == null) {
          sender.sendMessage(Component.text("Custom item '" + id + "' not found!", NamedTextColor.RED));
          return true;
        }

        int amount = 1;
        if (args.length >= 5) {
          try {
            amount = Integer.parseInt(args[4]);
          } catch (NumberFormatException ignored) {}
        }
        item.setAmount(amount);

        target.getInventory().addItem(item);
        sender.sendMessage(Component.text("Gave " + amount + "x " + id + " to " + target.getName() + ".", NamedTextColor.GREEN));
        return true;
      }
      if (action.equals("resetcooldown") && args.length >= 3) {
        Player target = org.bukkit.Bukkit.getPlayer(args[2]);
        if (target == null) {
          sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
          return true;
        }

        if (plugin.getCooldownManager() != null) {
          plugin.getCooldownManager().clearCooldowns(target);
          sender.sendMessage(Component.text("Reset all cooldowns for " + target.getName() + ".", NamedTextColor.GREEN));
        } else {
          sender.sendMessage(Component.text("Cooldown Manager is not initialized!", NamedTextColor.RED));
        }
        return true;
      }
      
      sender.sendMessage(Component.text("Usage: /eternalx item <list|give|resetcooldown> [arguments...]", NamedTextColor.RED));
      return true;
    }

    // --- SPAWN ---
    if (sub.equals("spawn")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
        return true;
      }
      if (!sender.hasPermission("eternalx.spawn")) {
        sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
        return true;
      }
      if (args.length < 2) {
        sender.sendMessage(Component.text("Usage: /eternalx spawn <name>", NamedTextColor.RED));
        return true;
      }

      String structureId = args[1];
      StructureData data = plugin.getStructureManager().getStructure(structureId);
      if (data == null) {
        sender.sendMessage(Component.text("Structure not found: " + structureId, NamedTextColor.RED));
        return true;
      }

      Location loc = player.getLocation();
      StructurePasteEngine.paste(plugin, data, loc, 0);

      Component coords = Component.text("[" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]", NamedTextColor.YELLOW)
        .clickEvent(ClickEvent.runCommand("/tp " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ()))
        .hoverEvent(HoverEvent.showText(Component.text("Click to teleport back!", NamedTextColor.GREEN)));

      player.sendMessage(Component.text()
        .append(Component.text(data.getRarity().getDisplayName()))
        .append(Component.text(" structure spawned: ", NamedTextColor.GREEN))
        .append(Component.text(structureId, NamedTextColor.GOLD))
        .append(Component.text(" at ", NamedTextColor.GREEN))
        .append(coords)
        .build());
      return true;
    }

    // --- PREVIEW ---
    if (sub.equals("preview")) {
      if (!(sender instanceof Player player)) {
        sender.sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
        return true;
      }
      if (!sender.hasPermission("eternalx.preview")) {
        sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
        return true;
      }
      if (args.length < 2) {
        sender.sendMessage(Component.text("Usage: /eternalx preview <name> [rotation]", NamedTextColor.RED));
        return true;
      }

      String structureId = args[1];
      StructureData data = plugin.getStructureManager().getStructure(structureId);
      if (data == null) {
        sender.sendMessage(Component.text("Structure not found: " + structureId, NamedTextColor.RED));
        return true;
      }

      int rotation = 0;
      if (args.length >= 3) {
        try {
          rotation = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
          sender.sendMessage(Component.text("Invalid rotation: " + args[2], NamedTextColor.RED));
          return true;
        }
      }

      boolean success = plugin.getPreviewManager().startPreview(player, data, player.getLocation(), rotation);
      if (success) {
        player.sendMessage(Component.text()
          .append(Component.text("[", NamedTextColor.DARK_GRAY))
          .append(Component.text("EternalX", NamedTextColor.GOLD))
          .append(Component.text("] ", NamedTextColor.DARK_GRAY))
          .append(Component.text("Preview started: ", NamedTextColor.GREEN))
          .append(Component.text(structureId, NamedTextColor.YELLOW))
          .build());
        player.sendMessage(Component.text()
          .append(Component.text("  "))
          .append(Component.text("[Confirm]", NamedTextColor.GREEN, TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/eternalx confirm"))
            .hoverEvent(HoverEvent.showText(Component.text("Click to paste structure", NamedTextColor.GREEN))))
          .append(Component.text("  "))
          .append(Component.text("[Cancel]", NamedTextColor.RED, TextDecoration.BOLD)
            .clickEvent(ClickEvent.runCommand("/eternalx cancel"))
            .hoverEvent(HoverEvent.showText(Component.text("Click to cancel preview", NamedTextColor.RED))))
          .build());
      } else {
        player.sendMessage(Component.text("Failed to start preview.", NamedTextColor.RED));
      }
      return true;
    }

    // --- CONFIRM ---
    if (sub.equals("confirm")) {
      if (!(sender instanceof Player player)) return true;
      if (!plugin.getPreviewManager().hasPreview(player)) {
        player.sendMessage(Component.text("No active preview to confirm.", NamedTextColor.RED));
        return true;
      }
      plugin.getPreviewManager().confirmPreview(player);
      player.sendMessage(Component.text()
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text("EternalX", NamedTextColor.GOLD))
        .append(Component.text("] ", NamedTextColor.DARK_GRAY))
        .append(Component.text("Structure placed!", NamedTextColor.GREEN))
        .build());
      return true;
    }

    // --- CANCEL ---
    if (sub.equals("cancel")) {
      if (!(sender instanceof Player player)) return true;
      if (!plugin.getPreviewManager().hasPreview(player)) {
        player.sendMessage(Component.text("No active preview to cancel.", NamedTextColor.RED));
        return true;
      }
      plugin.getPreviewManager().cancelPreview(player);
      player.sendMessage(Component.text()
        .append(Component.text("[", NamedTextColor.DARK_GRAY))
        .append(Component.text("EternalX", NamedTextColor.GOLD))
        .append(Component.text("] ", NamedTextColor.DARK_GRAY))
        .append(Component.text("Preview cancelled.", NamedTextColor.YELLOW))
        .build());
      return true;
    }

    // --- DEBUG ---
    if (sub.equals("debug")) {
      if (!sender.hasPermission("eternalx.admin")) {
        sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
        return true;
      }

      boolean currentMode = plugin.getSettingsManager().isDebugMode();
      boolean newMode = !currentMode;
      plugin.getSettingsManager().setDebugMode(newMode);

      if (newMode) {
        sender.sendMessage(Component.text()
          .append(Component.text("[", NamedTextColor.DARK_GRAY))
          .append(Component.text("EternalX", NamedTextColor.GOLD))
          .append(Component.text("] ", NamedTextColor.DARK_GRAY))
          .append(Component.text("Debug Mode: ", NamedTextColor.WHITE))
          .append(Component.text("ENABLED", NamedTextColor.GREEN))
          .build());
        sender.sendMessage(Component.text("You will now receive structure spawn notifications.", NamedTextColor.GRAY));
      } else {
        sender.sendMessage(Component.text()
          .append(Component.text("[", NamedTextColor.DARK_GRAY))
          .append(Component.text("EternalX", NamedTextColor.GOLD))
          .append(Component.text("] ", NamedTextColor.DARK_GRAY))
          .append(Component.text("Debug Mode: ", NamedTextColor.WHITE))
          .append(Component.text("DISABLED", NamedTextColor.RED))
          .build());
        sender.sendMessage(Component.text("Structure spawn notifications are now hidden.", NamedTextColor.GRAY));
      }
      return true;
    }

    // --- LIST ---
    if (sub.equals("list")) {
      if (!sender.hasPermission("eternalx.admin")) {
        sender.sendMessage(Component.text("You don't have permission.", NamedTextColor.RED));
        return true;
      }

      sender.sendMessage(Component.text("——————————— ", NamedTextColor.DARK_GRAY)
        .append(Component.text("EternalX Structures", NamedTextColor.GOLD))
        .append(Component.text(" ———————————", NamedTextColor.DARK_GRAY)));

      if (plugin.getStructureManager().getStructures().isEmpty()) {
        sender.sendMessage(Component.text("No structures loaded.", NamedTextColor.GRAY));
      } else {
        for (StructureData data : plugin.getStructureManager().getStructures()) {
          Component line = Component.text()
            .append(Component.text(data.getRarity().name(), data.getRarity().getAdventureColor()))
            .append(Component.text(" " + data.getId() + " ", NamedTextColor.WHITE))
            .append(Component.text("(", NamedTextColor.DARK_GRAY))
            .append(Component.text(data.getPlacementType().name(), NamedTextColor.GRAY))
            .append(Component.text(", ", NamedTextColor.DARK_GRAY))
            .append(Component.text(data.getDimensionType().name(), NamedTextColor.GRAY))
            .append(Component.text(")", NamedTextColor.DARK_GRAY))
            .build();

          if (sender instanceof Player) {
            line = line
              .hoverEvent(HoverEvent.showText(Component.text()
                .append(Component.text("Rarity: ", NamedTextColor.GRAY))
                .append(Component.text(data.getRarity().name(), data.getRarity().getAdventureColor()))
                .append(Component.newline())
                .append(Component.text("Weight: ", NamedTextColor.GRAY))
                .append(Component.text(String.format("%.2f", data.getFinalWeight()), NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("Spacing: ", NamedTextColor.GRAY))
                .append(Component.text(data.getSpacing() + " chunks", NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("Size: ", NamedTextColor.GRAY))
                .append(Component.text(data.getWidth() + "x" + data.getHeight() + "x" + data.getLength(), NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(Component.text("Tags: ", NamedTextColor.GRAY))
                .append(Component.text(data.getTags().isEmpty() ? "none" : String.join(", ", data.getTags()), NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text("Click to spawn!", NamedTextColor.GREEN))
                .build()))
              .clickEvent(ClickEvent.suggestCommand("/eternalx spawn " + data.getId()));
          }

          sender.sendMessage(line);
        }
      }

      sender.sendMessage(Component.text("————————————————————————————————", NamedTextColor.DARK_GRAY));
      return true;
    }

    return false;
  }
}
