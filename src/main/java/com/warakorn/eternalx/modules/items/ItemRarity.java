package com.warakorn.eternalx.modules.items;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * ระดับความหายากของ Custom Item
 */
public enum ItemRarity {
  COMMON("§7Common", NamedTextColor.GRAY),
  UNCOMMON("§aUncommon", NamedTextColor.GREEN),
  RARE("§9Rare", NamedTextColor.BLUE),
  EPIC("§5Epic", NamedTextColor.DARK_PURPLE),
  LEGENDARY("§6Legendary", NamedTextColor.GOLD),
  MYTHIC("§cMythic", NamedTextColor.DARK_RED);

  private final String displayName;
  private final NamedTextColor adventureColor;

  ItemRarity(String displayName, NamedTextColor adventureColor) {
    this.displayName = displayName;
    this.adventureColor = adventureColor;
  }

  public String getDisplayName() {
    return displayName;
  }

  public NamedTextColor getAdventureColor() {
    return adventureColor;
  }

  public static ItemRarity fromString(String name) {
    try {
      return valueOf(name.toUpperCase());
    } catch (IllegalArgumentException e) {
      return COMMON;
    }
  }
}
