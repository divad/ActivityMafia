package io.evad.mineactivity.mafia.actions;

import org.bukkit.ChatColor;

import io.evad.mineactivity.mafia.ActivityMafia;
import io.evad.mineactivity.mafia.Gamer;

public class Heal extends CharacterAction
{
	public Heal()
	{
		this.name = "heal";
		this.cmds = new String[] {"heal", "save"};
	}
	
	public void doActionRequest(Gamer gamer, Gamer targetGamer)
	{
		gamer.player.sendMessage(ActivityMafia.chatPrefix + "You have healed " + targetGamer.player.getName());
	}	
	
	public void doAction(Gamer gamer, Gamer targetGamer, String actionMessage, ActivityMafia mafia)
	{
		targetGamer.heal();
		mafia.addNightMessage(gamer.character.name + ChatColor.GREEN + " healed " + ActivityMafia.textColour + targetGamer.player.getName() + actionMessage);		
	}
}
