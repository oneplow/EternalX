package com.warakorn.eternalx.modules.structures.preview;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.world.block.BaseBlock;
import com.sk89q.worldedit.world.block.BlockTypes;
import com.warakorn.eternalx.EternalX;
import com.warakorn.eternalx.modules.structures.StructureData;
import com.warakorn.eternalx.modules.structures.engine.StructurePasteEngine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * ✅ Feature 3: Structure Preview
 * ให้ admin ดู ghost blocks ก่อน spawn จริง
 * ใช้ player.sendBlockChange() — ไม่แก้ block จริงในโลก
 */
public class StructurePreviewManager {
  private final EternalX plugin;
  private final Map<UUID, PreviewSession> activePreviews = new HashMap<>();

  public StructurePreviewManager(EternalX plugin) {
    this.plugin = plugin;
  }

  /**
   * เริ่ม preview — ส่ง fake blocks ให้ player เห็น
   */
  public boolean startPreview(Player player, StructureData data, Location location, int rotation) {
    // ยกเลิก preview เก่าถ้ามี
    cancelPreview(player);

    Clipboard clipboard = data.getClipboard();
    if (clipboard == null) {
      return false;
    }

    Location pasteLoc = location.clone().add(0, data.getOffsetY(), 0);
    AffineTransform transform = new AffineTransform().rotateY(rotation);
    BlockVector3 origin = clipboard.getOrigin();

    List<PreviewBlock> previewBlocks = new ArrayList<>();

    for (BlockVector3 vec : clipboard.getRegion()) {
      BaseBlock baseBlock = clipboard.getFullBlock(vec);

      // ข้าม air, barrier, structure_void
      if (baseBlock.getBlockType() == BlockTypes.AIR ||
          baseBlock.getBlockType() == BlockTypes.CAVE_AIR ||
          baseBlock.getBlockType() == BlockTypes.VOID_AIR ||
          baseBlock.getBlockType() == BlockTypes.BARRIER ||
          baseBlock.getBlockType() == BlockTypes.STRUCTURE_VOID) {
        continue;
      }

      // ข้าม structure block ถ้า paste-air = false
      if (!data.isPasteAir() && baseBlock.getBlockType() == BlockTypes.AIR) {
        continue;
      }

      // คำนวณตำแหน่งจริงในโลก
      BlockVector3 relative = vec.subtract(origin);
      com.sk89q.worldedit.math.Vector3 rotated = transform.apply(relative.toVector3());
      Location worldLoc = pasteLoc.clone().add(rotated.x(), rotated.y(), rotated.z());

      // แปลง WorldEdit block type → Bukkit Material
      String blockId = baseBlock.getBlockType().id();
      Material material = Material.matchMaterial(blockId);
      if (material == null) continue;

      // เก็บ original block data เพื่อ restore ทีหลัง
      BlockData originalData = worldLoc.getBlock().getBlockData();
      previewBlocks.add(new PreviewBlock(worldLoc, material, originalData));
    }

    // ส่ง fake blocks ให้ player
    for (PreviewBlock pb : previewBlocks) {
      player.sendBlockChange(pb.location, pb.previewMaterial.createBlockData());
    }

    PreviewSession session = new PreviewSession(player.getUniqueId(), data, location, rotation, previewBlocks);
    activePreviews.put(player.getUniqueId(), session);

    plugin.debugLog("Preview started for " + player.getName() + ": " + data.getId() + 
      " (" + previewBlocks.size() + " blocks)");
    return true;
  }

  /**
   * ยกเลิก preview — restore original blocks
   */
  public boolean cancelPreview(Player player) {
    PreviewSession session = activePreviews.remove(player.getUniqueId());
    if (session == null) return false;

    // Restore original blocks
    for (PreviewBlock pb : session.previewBlocks) {
      player.sendBlockChange(pb.location, pb.originalData);
    }

    plugin.debugLog("Preview cancelled for " + player.getName());
    return true;
  }

  /**
   * Confirm preview — paste structure จริง
   */
  public boolean confirmPreview(Player player) {
    PreviewSession session = activePreviews.remove(player.getUniqueId());
    if (session == null) return false;

    // Restore visual ก่อน paste จริง
    for (PreviewBlock pb : session.previewBlocks) {
      player.sendBlockChange(pb.location, pb.originalData);
    }

    // Paste จริง
    StructurePasteEngine.paste(plugin, session.structureData, session.location, session.rotation);

    plugin.debugLog("Preview confirmed for " + player.getName() + ": " + session.structureData.getId());
    return true;
  }

  /**
   * เช็คว่า player มี active preview ไหม
   */
  public boolean hasPreview(Player player) {
    return activePreviews.containsKey(player.getUniqueId());
  }

  /**
   * เคลียร์ทุก preview (เมื่อ disable plugin)
   */
  public void clearAll() {
    activePreviews.clear();
  }

  // ===== Inner Classes =====

  private static class PreviewSession {
    final UUID playerId;
    final StructureData structureData;
    final Location location;
    final int rotation;
    final List<PreviewBlock> previewBlocks;

    PreviewSession(UUID playerId, StructureData data, Location location, int rotation, List<PreviewBlock> blocks) {
      this.playerId = playerId;
      this.structureData = data;
      this.location = location;
      this.rotation = rotation;
      this.previewBlocks = blocks;
    }
  }

  private static class PreviewBlock {
    final Location location;
    final Material previewMaterial;
    final BlockData originalData;

    PreviewBlock(Location location, Material previewMaterial, BlockData originalData) {
      this.location = location;
      this.previewMaterial = previewMaterial;
      this.originalData = originalData;
    }
  }
}
