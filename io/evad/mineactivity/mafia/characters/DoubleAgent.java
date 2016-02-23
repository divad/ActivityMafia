package io.evad.mineactivity.mafia.characters;
import java.util.ArrayList;

import io.evad.mineactivity.mafia.ActivityMafia;
import io.evad.mineactivity.mafia.Gamer;
import io.evad.mineactivity.mafia.actions.*;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class DoubleAgent extends MafiaCharacter
{
	public DoubleAgent()
	{
		this.name    = "DoubleAgent";
		this.actions = new CharacterAction[] {new Check()};
		this.team    = MafiaTeam.MAFIA;
		this.desc    = "Your role is to help the mafia kill the townsfolk by searching for the detective with /z check <target>";		
	}
	
	public String getName()
	{
		// Double agent pretends to be detective
		return "Detective";
	}	
	
	public void informOtherPlayersOfRole(Gamer gamer, ArrayList<Gamer> gamers)
	{
		for (Gamer other_gamer : gamers)
		{
			if (other_gamer.character.team == MafiaTeam.MAFIA)
			{
				other_gamer.player.sendMessage(ActivityMafia.chatPrefix + gamer.player.getName() + " is DoubleAgent");
			}
		}
	}	
}