package io.evad.mineactivity.mafia.characters;
import io.evad.mineactivity.mafia.actions.*;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class Mafia extends MafiaCharacter
{
	public Mafia()
	{
		this.name    = "Mafia";
		this.actions = new CharacterAction[] {new Kill()};
		this.team    = MafiaTeam.MAFIA;
		this.desc    = "Your role is to kill the townsfolk by typing /z kill <target>";		
	}
}