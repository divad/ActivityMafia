package io.evad.mineactivity.mafia.characters;

import java.util.ArrayList;

import io.evad.mineactivity.mafia.Gamer;
import io.evad.mineactivity.mafia.actions.CharacterAction;
import io.evad.mineactivity.mafia.enums.MafiaTeam;

public abstract class MafiaCharacter
{
	public String name = "";
	public String desc = "";
	public CharacterAction[] actions = null;
	public MafiaTeam team = MafiaTeam.TOWN;
	
	public String getName()
	{
		return this.name;
	}
	
	public CharacterAction getAction(String actionName)
	{
		for (CharacterAction action : this.actions)
		{ 		      
			for (String actionString : action.cmds)
			{
				if (actionName.equalsIgnoreCase(actionString))
				{
					return action;
				}
			}
		}
		
		return null;
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
	
	public void informOtherPlayersOfRole(Gamer gamer, ArrayList<Gamer> gamers)
	{
	}
}
