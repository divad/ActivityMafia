package io.evad.mineactivity.mafia.actions;

import org.bukkit.ChatColor;

import io.evad.mineactivity.mafia.ActivityMafia;
import io.evad.mineactivity.mafia.Gamer;
import io.evad.mineactivity.mafia.characters.Citizen;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Kill extends CharacterAction
{
	public Kill()
	{
		this.name = "kill";
		this.cmds = new String[] {"kill", "murder", "shoot", "attack"};
	}
	
	public void doActionRequest(Gamer gamer, Gamer targetGamer)
	{
		gamer.player.sendMessage(ActivityMafia.chatPrefix + "You have attacked " + targetGamer.player.getName());
	}
	
	public void doAction(Gamer gamer, Gamer targetGamer, String actionMessage, ActivityMafia mafia)
	{
		targetGamer.attack();
		mafia.addNightMessage(gamer.character.name + ChatColor.YELLOW + " attacked " + ActivityMafia.textColour + targetGamer.player.getName() + actionMessage);
		
		if (gamer.character.team == MafiaTeam.TOWN)
		{
			if (targetGamer.character.team == MafiaTeam.TOWN)
			{
				mafia.addNightMessage("The " + gamer.character.getName() + " attacked their own team! Due to their disgrace, they resign and become a citizen.");
				gamer.character = new Citizen();
			}
		}	
	}
}
