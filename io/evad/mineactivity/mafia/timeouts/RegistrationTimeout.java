package io.evad.mineactivity.mafia.timeouts;
import org.bukkit.scheduler.BukkitRunnable;

import io.evad.mineactivity.mafia.ActivityMafia;

public class RegistrationTimeout extends BukkitRunnable
{
	private final ActivityMafia plugin;

	 public RegistrationTimeout(ActivityMafia plugin)
	 {
		 this.plugin = plugin;
	 }
	        
	@Override
	public void run()
	{
		plugin.getLogger().info(":: RegistrationTimeout run");		
		plugin.startPreGame();
	}

}
