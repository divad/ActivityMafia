package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.MafiaCharacter;
import io.evad.mineactivity.mafia.enums.CharacterAction;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Detective extends MafiaCharacter
{
	public Detective()
	{
		super("Detective", new CharacterAction[]{CharacterAction.KILL, CharacterAction.CHECK,}, MafiaTeam.TOWN, "You role is to hunt the mafia. Do /z check <target> to check a players role or /z kill <target> to kill.");
	}
}