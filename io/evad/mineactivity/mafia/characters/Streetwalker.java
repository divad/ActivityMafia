package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.actions.*;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Streetwalker extends MafiaCharacter
{
	public Streetwalker()
	{
		this.name    = "StreetWalker";
		this.actions = new CharacterAction[] {new Love()};
		this.team    = MafiaTeam.TOWN;
		this.desc    = "You wish to share yourself with others. You can type /z love <player> as your action.";				
	}
}