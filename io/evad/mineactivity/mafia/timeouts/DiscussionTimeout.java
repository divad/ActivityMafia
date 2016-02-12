package io.evad.mineactivity.mafia.timeouts;
import org.bukkit.scheduler.BukkitRunnable;

import io.evad.mineactivity.mafia.ActivityMafia;

public class DiscussionTimeout extends BukkitRunnable
{
	private final ActivityMafia plugin;

	 public DiscussionTimeout(ActivityMafia plugin)
	 {
		 this.plugin = plugin;
	 }
	        
	@Override
	public void run()
	{
		plugin.getLogger().info(":: DiscussionTimeout run");		
		plugin.startNominations();
	}

}
