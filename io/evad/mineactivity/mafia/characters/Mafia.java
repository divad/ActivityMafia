package io.evad.mineactivity.mafia.characters;
import java.util.ArrayList;

import io.evad.mineactivity.mafia.ActivityMafia;
import io.evad.mineactivity.mafia.Gamer;
import io.evad.mineactivity.mafia.actions.*;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Mafia extends MafiaCharacter
{
	public Mafia()
	{
		this.name    = "Mafia";
		this.actions = new CharacterAction[] {new Kill()};
		this.team    = MafiaTeam.MAFIA;
		this.desc    = "Your role is to kill the townsfolk by typing /z kill <target>";		
	}
	
	public void informOtherPlayersOfRole(Gamer gamer, ArrayList<Gamer> gamers)
	{
		for (Gamer other_gamer : gamers)
		{
			if (gamer != other_gamer)
			{
				if (other_gamer.character.team == MafiaTeam.MAFIA)
				{
					other_gamer.player.sendMessage(ActivityMafia.chatPrefix + gamer.player.getName() + " is Mafia");
				}
			}
		}
	}
	
}