package io.evad.mineactivity.mafia;

import java.util.ArrayList;

import org.bukkit.entity.Player;

public class Gamer
{
	public Player player;
	private boolean alive = true;
	public MafiaCharacter character;
	
	public int health = 0; /* 0 and above is alive. below 0 is dead. */
	public boolean actionPerformed = false;
	
	private ArrayList<String> nightMessages = new ArrayList<String>();
	private ArrayList<String> nightMessagesIfAlive = new ArrayList<String>();

	
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
	}
	
	public void setActionPerformed()
	{
		this.actionPerformed = true;
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
	
	/* used to check if the player is alive at any time */
	public boolean isAlive()
	{
		return this.alive;
	}
	
	/* used if a player is put to death or the player is killed */
	public void kill()
	{
		this.alive = false;
	}
	
	/* used to check if a player was murdered overnight */
	public boolean isAliveAfterNight()
	{
		if (this.health < 0)
		{
			this.alive = false;
		}
		
		return this.alive;
	}

}
