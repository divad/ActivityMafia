package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.actions.*;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Jailor extends MafiaCharacter
{
	public Jailor()
	{
		this.name    = "Jailor";
		this.actions = new CharacterAction[] {new Jail()};
		this.team    = MafiaTeam.TOWN;
		this.desc    = "You can jail people to block their actions at night with /z jail <target>. You are a townsperson, so don't block the detective!";				
	}
}