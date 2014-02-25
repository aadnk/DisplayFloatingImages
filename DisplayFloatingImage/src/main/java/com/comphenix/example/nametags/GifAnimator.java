package com.comphenix.example.nametags;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.comphenix.example.nametags.GifImageMessage.ImageFrame;
import com.google.common.base.Preconditions;

/**
 * Represents a GIF animator for a particular player.
 * <p>
 * This class is not thread safe.
 * @author Kristian
 */
public class GifAnimator {
	private GifImageMessage imageMessages;
	private Plugin plugin;
	private Player player;
	
	private BukkitTask task;
	private ImageFrame current;
	private int index;
	
	private Location location;

	/**
	 * Construct a new GIF animator for a particular player.
	 * @param plugin - the parent plugin.
	 * @param player - the player.
	 * @param 
	 */
	public GifAnimator(Plugin plugin, Player player, GifImageMessage imageMessages) {
		this.plugin = Preconditions.checkNotNull(plugin, "plugin cannot be NULL");
		this.player = Preconditions.checkNotNull(player, "player cannot be NULL");
		this.imageMessages = Preconditions.checkNotNull(imageMessages, "imageMessages cannot be NULL");
	}
	
	/**
	 * Reset the current frame to the beginning.
	 */
	public void reset() {
		index = 0;
	}
	
	/**
	 * Start animating the GIF.
	 * @return TRUE if the animation was started, FALSE if it is already running.
	 */
	public boolean start() {
		if (task == null) {
			location = player.getLocation();
			scheduleCurrent();
			return true;
		}
		return false;
	}
	
	/**
	 * Pause execution of the GIF animation.
	 * @return TRUE if it was paused, FALSE otherwise.
	 */
	public boolean pause() {
		if (task != null) {
			task.cancel();
			task = null;
			return true;
		}
		return false;
	}
	
	/**
	 * Stop execution of the GIF animation, and reset the current frame to the beginning.
	 * @return TRUE if it was paused, FALSE otherwise.
	 */
	public boolean stop() {
		if (task != null) {
			task.cancel();
			task = null;
			
			reset();
			current.getMessage().clear(player);
			return true;
		}
		return false;
	}
	
	/**
	 * Retrieve the location of the animated message.
	 * @return The location, or NULL for the player's own location.
	 */
	public Location getLocation() {
		return location;
	}
	
	/**
	 * Set the desired location to display the image.
	 * @param location - the location, or NULL to use the player's location.
	 */
	public void setLocation(Location location) {
		this.location = location;
	}
	
	/**
	 * Schedule the current GIF frame.
	 */
	private void scheduleCurrent() {
		final ImageFrame frame = imageMessages.getFrames().get(index);
		
		task = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
			@Override
			public void run() {
				// Disable previous frame
				if (current != null) {
					// Save some flickering
					if (current.getMessage().getSpawner() != frame.getMessage().getSpawner()) {
						current.getMessage().clear(player);
					}
				}
				current = frame;
				applyLocation(frame.getMessage());
				frame.getMessage().sendToPlayer(player);
				
				// Schedule the next frame
				scheduleNext();
			}
		}, (int) Math.ceil(frame.getDelay() / 5.0));
	}
	
	/**
	 * Schedule the next GIF frame.
	 */
	protected void scheduleNext() {
		index++;
		
		if (index >= imageMessages.getFrames().size()) {
			index = 0;
		}
		scheduleCurrent();
	}
	
	/**
	 * Apply the current location to the frame that will be displayed.
	 * @param frame - the frame.
	 */
	protected void applyLocation(NameTagMessage frame) {
		frame.setLocation(location);
	}
}
