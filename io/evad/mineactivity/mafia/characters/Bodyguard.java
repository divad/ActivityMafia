package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.actions.*;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Bodyguard extends MafiaCharacter
{
	public Bodyguard()
	{
		this.name    = "Bodyguard";
		this.actions = new CharacterAction[] {new Guard()};
		this.team    = MafiaTeam.TOWN;
		this.desc    = "You can guard another player against death with /z guard <target>. If they are killed, you die instead.";				
	}
}