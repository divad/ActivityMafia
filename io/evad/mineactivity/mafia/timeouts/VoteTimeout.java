package io.evad.mineactivity.mafia.timeouts;
import org.bukkit.scheduler.BukkitRunnable;

import io.evad.mineactivity.mafia.ActivityMafia;

public class VoteTimeout extends BukkitRunnable
{
	private final ActivityMafia plugin;

	 public VoteTimeout(ActivityMafia plugin)
	 {
		 this.plugin = plugin;
	 }
	        
	@Override
	public void run()
	{
		plugin.getLogger().info(":: VoteTimeout run");		
		plugin.endVote();
	}

}
