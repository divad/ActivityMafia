package io.evad.mineactivity.mafia;

import java.util.ArrayList;

import org.bukkit.entity.Player;

import io.evad.mineactivity.mafia.actions.CharacterAction;
import io.evad.mineactivity.mafia.characters.MafiaCharacter;

public class Gamer
{
	public Player player;
	private boolean alive = true;
	public MafiaCharacter character;
	
	// data that changes on each night
	public CharacterAction action = null;
	public Gamer actionTarget       = null;
	public boolean actionBlocked    = false;
	public int health = 0; /* 0 and above is alive. below 0 is dead. */
	public boolean actionPerformed = false;
	public String actionMessage = null;
	
	private ArrayList<String> nightMessages = new ArrayList<String>();
	private ArrayList<String> nightMessagesIfAlive = new ArrayList<String>();
	
	private boolean guarded = false;
	public Gamer guardedBy = null;

	
	public Gamer(Player player)
	{
		this.player = player;
	}
	
	public void addNightMessage(String msg)
	{
		this.nightMessages.add(msg);
	}
	
	public void addNightMessageIfAlive(String msg)
	{
		this.nightMessagesIfAlive.add(msg);
	}	
	
	public void endNight()
	{
		for (int i = 0; i < this.nightMessages.size(); i++)
		{			
			this.player.sendMessage(this.nightMessages.get((i)));
		}
		
		if (this.alive)
		{
			for (int i = 0; i < this.nightMessagesIfAlive.size(); i++)
			{			
				this.player.sendMessage(this.nightMessagesIfAlive.get((i)));
			}
		}
		
		// Reset data for the next night
		this.health = 0;
		this.actionPerformed = false;
		this.nightMessages = new ArrayList<String>();
		this.nightMessagesIfAlive = new ArrayList<String>();
		this.actionTarget = null;
		this.actionBlocked = false;
		this.action = null;
		this.guarded = false;
		this.guardedBy = null;
		
	}
	
	public void requestAction(CharacterAction action, Gamer targetGamer, String message)
	{
		this.actionPerformed = true;
		this.action          = action;
		this.actionMessage   = message;
		this.actionTarget    = targetGamer;
		action.doActionRequest(this, this.actionTarget);
	}
	
	public void doAction(ActivityMafia mafia)
	{
		// If this gamer has actually requested an action
		if (this.actionPerformed)
		{
			// If they were not blocked this night
			if (!this.actionBlocked)
			{
				this.action.doAction(this, this.actionTarget, this.actionMessage, mafia);
			}
			else
			{
				this.player.sendMessage(ActivityMafia.chatPrefix + "The jailor blocked you from taking your action by putting you in jail!");
			}
		}
	}
	
	public boolean hasPerformedAction()
	{
		if (this.character.hasActions())
		{
			return this.actionPerformed;
		}
		else
		{
			// always return true so that the night can end once all characters actions are done
			return true;
		}
	}
	
	public void attack()
	{
		this.health = this.health - 1;
	}

	public void heal()
	{
		this.health = this.health + 1;
	}
	
	public void block()
	{
		this.actionBlocked = true;
	}
	
	public void guard(Gamer gamer)
	{
		this.guarded = true;
		this.guardedBy = gamer;
	}
	
	/* used to check if the player is alive at any time */
	public boolean isAlive()
	{
		return this.alive;
	}
	
	/* used to check if they were blocked from doing any actions */
	public boolean actionBlocked() 
	{
		return this.actionBlocked;
	}
	
	/* used if a player is put to death or the player is killed */
	public void kill()
	{
		this.alive = false;
	}
	
	/* used to check if a player was murdered overnight */
	public boolean wasKilled()
	{
		if (this.health < 0)
		{
			return true;
		}
		
		return false;
	}
	
	public boolean wasGuarded()
	{
		return this.guarded;
	}

}
