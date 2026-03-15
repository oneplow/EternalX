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
      StringUtil.copyPartialMatches(args[0], commands, completions);
    } else if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
      if (sender.hasPermission("eternalx.spawn")) {
        List<String> structures = plugin.getStructureManager().getStructures().stream()
          .map(StructureData::getId)
          .collect(Collectors.toList());
        StringUtil.copyPartialMatches(args[1], structures, completions);
      }
    }

    Collections.sort(completions);
    return completions;
  }
}
