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

import com.comphenix.example.nametags.GifAnimator;
import com.comphenix.example.nametags.GifImageMessage;
import com.comphenix.example.nametags.ImageChar;
import com.comphenix.example.nametags.NameTagMessage;
import com.google.common.collect.Maps;

// Screenshot:
//   http://imgur.com/gF0qyzj

public class DisplayFloatingImages extends JavaPlugin implements Listener {
	private GifImageMessage gif;
	private Map<Player, GifAnimator> playerTask = Maps.newHashMap();

	@Override
	public void onEnable() {
		// Must be placed in the data folder
		//URI uri = URI.create("http://www.gstatic.com/webp/gallery3/2.png");
		URI uri = URI.create("http://fc02.deviantart.net/fs71/f/2012/126/a/8/low_res_hl1_explosion_gif__cheesy___by_theloyalrainbowdash-d4yoj2u.gif");
		
		try {
			gif = new GifImageMessage(uri, 30, ImageChar.BLOCK.getChar());
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
		GifAnimator task = playerTask.remove(player);

		if (task != null) {
			return task.stop();
		}
		return false;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player) {
			final Player player = (Player) sender;

			if (gif != null) {
				if (!stopTask(player)) {
					GifAnimator animator = new GifAnimator(this, player, gif);
					
					// Start and save animator
					animator.start();
					playerTask.put(player, animator);
				
				} else {
					stopTask(player);
				}
			} else {
				sender.sendMessage(ChatColor.RED + "No image loaded.");
			}
		}
		return true;
	}

}
