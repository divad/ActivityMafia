package io.evad.mineactivity.mafia.actions;

import org.bukkit.ChatColor;

import io.evad.mineactivity.mafia.ActivityMafia;
import io.evad.mineactivity.mafia.Gamer;

public class Pray extends CharacterAction
{
	public Pray()
	{
		this.name = "kill";
		this.cmds = new String[] {"kill", "murder", "shoot", "attack"};
	}
	
	public void doActionRequest(Gamer gamer, Gamer targetGamer)
	{
		gamer.player.sendMessage(ActivityMafia.chatPrefix + "You have prayed  to the flying spaghetti monster asking it to protect " + targetGamer.player.getName());
	}	
	
	public void doAction(Gamer gamer, Gamer targetGamer, String actionMessage, ActivityMafia mafia)
	{
		targetGamer.attack();
		mafia.addNightMessage(gamer.character.name + ChatColor.YELLOW + " attacked " + ActivityMafia.textColour + targetGamer.player.getName() + actionMessage);
		
	}
}
