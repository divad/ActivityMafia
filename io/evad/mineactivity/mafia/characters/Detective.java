package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.actions.*;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Detective extends MafiaCharacter
{
	public Detective()
	{
		this.name    = "Detective";
		this.actions = new CharacterAction[] {new Kill(), new Check()};
		this.team    = MafiaTeam.TOWN;
		this.desc    = "You role is to hunt the mafia. Do /z check <target> to check a players role or /z kill <target> to kill.";
	}
}