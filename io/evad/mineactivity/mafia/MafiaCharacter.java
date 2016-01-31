package io.evad.mineactivity.mafia;

import java.util.Arrays;

import io.evad.mineactivity.mafia.enums.CharacterAction;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public class MafiaCharacter
{
	public String name = "";
	public String desc = "";
	public CharacterAction[] actions = null;
	public MafiaTeam team = MafiaTeam.TOWN;
	
	public MafiaCharacter(String name, CharacterAction[] actions, MafiaTeam team, String desc)
	{
		this.name    = name;
		this.actions = actions;
		this.team    = team;
		this.desc    = desc;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public boolean canPerformAction(CharacterAction action)
	{
		return Arrays.asList(this.actions).contains(action);
	}
	
	public boolean hasActions()
	{
		if (this.actions == null)
		{
			return false;
		}
		else
		{
			return true;
		}
	}
}
