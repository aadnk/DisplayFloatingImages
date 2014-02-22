package com.comphenix.example;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.imageio.ImageIO;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.comphenix.example.nametags.ImageChar;
import com.comphenix.example.nametags.NameTagMessage;
import com.google.common.collect.Maps;

// Screenshot:
//   http://imgur.com/gF0qyzj

public class DisplayFloatingImages extends JavaPlugin implements Listener {
	private NameTagMessage message;
	private BufferedImage image;

	private Map<Player, BukkitTask> playerTask = Maps.newHashMap();

	@Override
	public void onEnable() {
		// Must be placed in the data folder
		URI uri = URI.create("http://www.gstatic.com/webp/gallery3/2.png");

		try {
			image = ImageIO.read(uri.toURL());
			message = new NameTagMessage(image, 30, ImageChar.BLOCK.getChar());
		} catch (IOException e) {
			throw new RuntimeException("Cannot read image " + uri, e);
		}
		getServer().getPluginManager().registerEvents(this, this);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		stopTask(e.getPlayer());
	}

	/**
	 * Stop a task associated with the given player.
	 * 
	 * @param player - the player.
	 * @return TRUE if a task was stopped, FALSE otherwise.
	 */
	private boolean stopTask(Player player) {
		BukkitTask task = playerTask.remove(player);

		if (task != null) {
			task.cancel();
			return true;
		}
		return false;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			final Player player = (Player) sender;

			if (message != null) {
				if (!stopTask(player)) {
					final Location loc = player.getLocation().add(0, 5, 0);
					message.sendToPlayer(player, loc);

					playerTask.put(player,
							getServer().getScheduler().runTaskTimer(this, new Runnable() {
								@Override
								public void run() {
									loc.add(0, 0.02, 0);
									message.move(player, loc);
								}
							}, 1, 1));

				} else {
					message.clear(player);
					stopTask(player);
				}
			} else {
				sender.sendMessage(ChatColor.RED + "No image loaded.");
			}
		}
		return true;
	}

}
