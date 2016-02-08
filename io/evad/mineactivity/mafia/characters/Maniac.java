package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.actions.*;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Maniac extends MafiaCharacter
{
	public Maniac()
	{
		this.name    = "Maniac";
		this.actions = new CharacterAction[] {new Kill()};
		this.team    = MafiaTeam.MANIAC;
		this.desc    = "Your role is to kill anybody you want by typing /z kill <target>";				
	}
}