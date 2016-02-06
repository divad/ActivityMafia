package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.MafiaCharacter;
import io.evad.mineactivity.mafia.enums.CharacterAction;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Streetwalker extends MafiaCharacter
{
	public Streetwalker()
	{
		super("Streetwalker", new CharacterAction[]{CharacterAction.LOVE}, MafiaTeam.TOWN, "You wish to share yourself with others. You can type /z love <player> as your action.");
	}
}