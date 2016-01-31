package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.MafiaCharacter;
import io.evad.mineactivity.mafia.enums.CharacterAction;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Mafia extends MafiaCharacter
{
	public Mafia()
	{
		super("Mafia", new CharacterAction[]{CharacterAction.KILL}, MafiaTeam.MAFIA, "Your role is to kill the townsfolk, especially the detective, by typing /z kill <target>");
	}
}