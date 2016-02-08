package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.actions.*;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Doctor extends MafiaCharacter
{
	public Doctor()
	{
		this.name    = "Doctor";
		this.actions = new CharacterAction[] {new Heal()};
		this.team    = MafiaTeam.TOWN;
		this.desc    = "Your role is to save others from death. You can type /z heal <player> as your action.";		
	}
}