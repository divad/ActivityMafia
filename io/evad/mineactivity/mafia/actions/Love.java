package io.evad.mineactivity.mafia.actions;

import java.util.Random;

import org.bukkit.ChatColor;

import io.evad.mineactivity.mafia.ActivityMafia;
import io.evad.mineactivity.mafia.Gamer;

public class Love extends CharacterAction
{
	public Love()
	{
		this.name = "love";
		this.cmds = new String[] {"love", "sleep", "fuck", "sex"};
	}
	
	public void doActionRequest(Gamer gamer, Gamer targetGamer)
	{
		gamer.player.sendMessage(ActivityMafia.chatPrefix + "You have slipped beneath the silk sheets with " + targetGamer.player.getName());
	}	
	
	public void doAction(Gamer gamer, Gamer targetGamer, String actionMessage, ActivityMafia mafia)
	{
		Random rng = new Random();
		int ran = rng.nextInt(4);
		
		if (ran == 1)
		{
			// reveal role
			mafia.addNightMessage(gamer.character.name + ChatColor.GREEN + " spent the night with " + ActivityMafia.textColour + targetGamer.player.getName() + ", who wakes up to a note saying 'I know who you are!'" + actionMessage);																				
			gamer.addNightMessageIfAlive("In their sleep your partner mumbles details to identify them!");
			gamer.addNightMessageIfAlive(ChatColor.GOLD + targetGamer.player.getName() + " is " + targetGamer.character.name);
		}
		else if (ran == 2)
		{
			// kill target
			mafia.addNightMessage(gamer.character.name + ChatColor.GREEN + " spent the night with " + ActivityMafia.textColour + targetGamer.player.getName() + " but forgot to use protection!" + actionMessage);													
			targetGamer.attack();
		}
		else if (ran == 3)
		{
			// heal target
			mafia.addNightMessage(gamer.character.name + ChatColor.GREEN + " spent the night with " + ActivityMafia.textColour + targetGamer.player.getName() + " which causes them to feel healed!" + actionMessage);													
			targetGamer.heal();
		}				
		else
		{
			// nothing happens
			mafia.addNightMessage(gamer.character.name + ChatColor.GREEN + " tried to spend the night with " + ActivityMafia.textColour + targetGamer.player.getName() + " but they were rejected!" + actionMessage);													
		}
	}
}
