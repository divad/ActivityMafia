package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.MafiaCharacter;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Citizen extends MafiaCharacter
{
	public Citizen()
	{
		super("Citizen", null, MafiaTeam.TOWN, "You have no actions to perform during the night - just sleep and pray for morning!");
	}
}
