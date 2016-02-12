package io.evad.mineactivity.mafia.actions;

import org.bukkit.ChatColor;

import io.evad.mineactivity.mafia.ActivityMafia;
import io.evad.mineactivity.mafia.Gamer;

public class Jail extends CharacterAction
{
	public Jail()
	{
		this.name = "jail";
		this.cmds = new String[] {"jail", "arrest"};
		this.canTargetSelf = false;
	}
	
	public void doActionRequest(Gamer gamer, Gamer targetGamer)
	{
		gamer.player.sendMessage(ActivityMafia.chatPrefix + "You have put " + targetGamer.player.getName() + " in jail for the night!");
		targetGamer.block();		
	}	
	
	public void doAction(Gamer gamer, Gamer targetGamer, String actionMessage, ActivityMafia mafia)
	{
		mafia.addNightMessage(gamer.character.name + ChatColor.GREEN + " put " + ActivityMafia.textColour + targetGamer.player.getName() + " in jail overnight!" + actionMessage);						
	}
}