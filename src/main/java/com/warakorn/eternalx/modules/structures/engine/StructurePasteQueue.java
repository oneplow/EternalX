package com.warakorn.eternalx.modules.structures.engine;

import com.warakorn.eternalx.EternalX;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class StructurePasteQueue {
  private final EternalX plugin;
  private final Queue<Runnable> tasks = new ConcurrentLinkedQueue<>();
  private BukkitTask task;

  public StructurePasteQueue(EternalX plugin) {
    this.plugin = plugin;
  }

  public void add(Runnable runnable) {
    tasks.offer(runnable);
  }

  public void start() {
    task = new BukkitRunnable() {
      @Override
      public void run() {
        if (tasks.isEmpty()) return;
        Runnable job = tasks.poll();
        if (job != null) {
          try {
            job.run();
          } catch (Exception e) {
            plugin.getLogger().severe("Error in PasteQueue: " + e.getMessage());
          }
        }
      }
    }.runTaskTimer(plugin, 20L, 2L);
  }

  public void stop() {
    if (task != null && !task.isCancelled()) task.cancel();
    tasks.clear();
  }
}
