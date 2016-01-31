package io.evad.mineactivity.mafia.timeouts;
import org.bukkit.scheduler.BukkitRunnable;

import io.evad.mineactivity.mafia.ActivityMafia;

public class NightTimeout extends BukkitRunnable
{
	private final ActivityMafia plugin;

	 public NightTimeout(ActivityMafia plugin)
	 {
		 this.plugin = plugin;
	 }
	        
	@Override
	public void run()
	{
		plugin.endNight();
	}

}
