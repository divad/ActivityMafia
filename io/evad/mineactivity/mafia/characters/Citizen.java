package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.actions.CharacterAction;
import io.evad.mineactivity.mafia.actions.Kill;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Citizen extends MafiaCharacter
{
	public Citizen()
	{
		this.name    = "Citizen";
		this.actions = new CharacterAction[] {new Kill()};
		this.team    = MafiaTeam.TOWN;
		this.desc    = "You may pray for the wellbeing of a townsperson with /z pray <target>";
	}
}
