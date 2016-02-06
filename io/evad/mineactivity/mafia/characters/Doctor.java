package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.MafiaCharacter;
import io.evad.mineactivity.mafia.enums.CharacterAction;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Doctor extends MafiaCharacter
{
	public Doctor()
	{
		super("Doctor", new CharacterAction[]{CharacterAction.HEAL}, MafiaTeam.TOWN, "Your role is to save others from death. You can type /z heal <player> as your action.");

	}
}