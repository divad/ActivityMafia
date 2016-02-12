package io.evad.mineactivity.mafia.actions;

import java.util.Random;

import org.bukkit.ChatColor;

import io.evad.mineactivity.mafia.ActivityMafia;
import io.evad.mineactivity.mafia.Gamer;

public class Pray extends CharacterAction
{
	public Pray()
	{
		this.name = "pray";
		this.cmds = new String[] {"pray"};
	}
	
	public void doActionRequest(Gamer gamer, Gamer targetGamer)
	{
		gamer.player.sendMessage(ActivityMafia.chatPrefix + "You have prayed  to the flying spaghetti monster asking it to protect " + targetGamer.player.getName());
	}	
	
	public void doAction(Gamer gamer, Gamer targetGamer, String actionMessage, ActivityMafia mafia)
	{
		Random rng = new Random();
		int ran = rng.nextInt(9);
		
		if (ran <= 1)
		{
			targetGamer.heal();			
			mafia.addNightMessage(gamer.character.name + ChatColor.GREEN + " prayed for " + ActivityMafia.textColour + targetGamer.player.getName() + "'s life. They were healed by divine intervention!" + actionMessage);																				
		}
		else
		{
			mafia.addNightMessage(gamer.character.name + ChatColor.GREEN + " prayed for " + ActivityMafia.textColour + targetGamer.player.getName() + "'s life. Sadly, nothing happened." + actionMessage);																				
			
		}
	}
}