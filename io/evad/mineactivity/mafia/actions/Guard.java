package io.evad.mineactivity.mafia.actions;

import org.bukkit.ChatColor;

import io.evad.mineactivity.mafia.ActivityMafia;
import io.evad.mineactivity.mafia.Gamer;

public class Guard extends CharacterAction
{
	public Guard()
	{
		this.name = "guard";
		this.cmds = new String[] {"guard", "protect", "bodyguard"};
		this.canTargetSelf = false;		
	}
	
	public void doActionRequest(Gamer gamer, Gamer targetGamer)
	{
		gamer.player.sendMessage(ActivityMafia.chatPrefix + "You are guarding " + targetGamer.player.getName());
	}	
	
	public void doAction(Gamer gamer, Gamer targetGamer, String actionMessage, ActivityMafia mafia)
	{
		targetGamer.guard(gamer);
		mafia.addNightMessage(gamer.character.name + ChatColor.GREEN + " protected " + ActivityMafia.textColour + targetGamer.player.getName() + actionMessage);						
	}
}