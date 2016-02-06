package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.MafiaCharacter;
import io.evad.mineactivity.mafia.enums.CharacterAction;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Citizen extends MafiaCharacter
{
	public Citizen()
	{
		super("Citizen", new CharacterAction[]{CharacterAction.PRAY}, MafiaTeam.TOWN, "You may pray for the wellbeing of a townsperson with /z pray <target>");
	}
}
