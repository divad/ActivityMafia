package io.evad.mineactivity.mafia.actions;

import org.bukkit.ChatColor;

import io.evad.mineactivity.mafia.ActivityMafia;
import io.evad.mineactivity.mafia.Gamer;

public class Check extends CharacterAction
{
	public Check()
	{
		this.name = "check";
		this.cmds = new String[] {"check", "whois", "investigate"};
	}
	
	public void doActionRequest(Gamer gamer, Gamer targetGamer)
	{
		gamer.player.sendMessage(ActivityMafia.chatPrefix + "You have investigated " + targetGamer.player.getName() + " - if you are alive in the morning their role will be revealed");
	}	
	
	public void doAction(Gamer gamer, Gamer targetGamer, String actionMessage, ActivityMafia mafia)
	{
		mafia.addNightMessage(gamer.character.name + ChatColor.GREEN + " checked out " + ActivityMafia.textColour + targetGamer.player.getName() + actionMessage);						
		targetGamer.addNightMessageIfAlive(ActivityMafia.chatPrefix + ChatColor.GOLD + targetGamer.player.getName() + " is " + targetGamer.character.name);
	}

}
