package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.MafiaCharacter;
import io.evad.mineactivity.mafia.enums.CharacterAction;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Maniac extends MafiaCharacter
{
	public Maniac()
	{
		super("Maniac", new CharacterAction[]{CharacterAction.KILL}, MafiaTeam.MANIAC, "Your role is to kill anybody you like by typing /z kill <target>");
	}
}