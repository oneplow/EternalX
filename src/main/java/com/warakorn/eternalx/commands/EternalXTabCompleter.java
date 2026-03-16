package com.warakorn.eternalx.commands;

import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.structures.StructureData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EternalXTabCompleter implements TabCompleter {
  private final EternalX plugin;

  public EternalXTabCompleter(EternalX plugin) {
    this.plugin = plugin;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
    List<String> completions = new ArrayList<>();

    if (args.length == 1) {
      List<String> commands = new ArrayList<>();
      if (sender.hasPermission("eternalx.admin")) {
        commands.add("reload");
        commands.add("debug");
        commands.add("list");
      }
      if (sender.hasPermission("eternalx.spawn")) {
        commands.add("spawn");
      }
      // ✅ Feature 3: Preview commands
      if (sender.hasPermission("eternalx.preview")) {
        commands.add("preview");
        commands.add("confirm");
        commands.add("cancel");
      }
      if (sender.hasPermission("eternalx.admin")) {
        commands.add("item");
      }
      StringUtil.copyPartialMatches(args[0], commands, completions);
    } else if (args.length == 2) {
      String sub = args[0].toLowerCase();
      // Tab complete structure names for spawn and preview
      if ((sub.equals("spawn") && sender.hasPermission("eternalx.spawn")) ||
          (sub.equals("preview") && sender.hasPermission("eternalx.preview"))) {
        List<String> structures = plugin.getStructureManager().getStructures().stream()
          .map(StructureData::getId)
          .collect(Collectors.toList());
        StringUtil.copyPartialMatches(args[1], structures, completions);
      } else if (sub.equals("item") && sender.hasPermission("eternalx.admin")) {
        StringUtil.copyPartialMatches(args[1], java.util.Arrays.asList("list", "give", "resetcooldown"), completions);
      }
    } else if (args.length == 3) {
      String sub = args[0].toLowerCase();
      if (sub.equals("preview") && sender.hasPermission("eternalx.preview")) {
        // Tab complete rotation angles for preview
        List<String> rotations = List.of("0", "90", "180", "270");
        StringUtil.copyPartialMatches(args[2], rotations, completions);
      } else if (sub.equals("item") && (args[1].equalsIgnoreCase("give") || args[1].equalsIgnoreCase("resetcooldown")) && sender.hasPermission("eternalx.admin")) {
        List<String> players = new ArrayList<>();
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
          players.add(p.getName());
        }
        StringUtil.copyPartialMatches(args[2], players, completions);
      }
    } else if (args.length == 4 && args[0].equalsIgnoreCase("item") && args[1].equalsIgnoreCase("give")) {
      if (sender.hasPermission("eternalx.admin") && plugin.getCustomItemManager() != null) {
        StringUtil.copyPartialMatches(args[3], new ArrayList<>(plugin.getCustomItemManager().getItems().keySet()), completions);
      }
    }

    Collections.sort(completions);
    return completions;
  }
}
