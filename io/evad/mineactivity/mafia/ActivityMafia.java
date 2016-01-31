package io.evad.mineactivity.mafia;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import io.evad.mineactivity.mafia.characters.*;
import io.evad.mineactivity.mafia.enums.*;
import io.evad.mineactivity.mafia.timeouts.*;

public class ActivityMafia extends JavaPlugin implements Listener 
{
	private GameStage gameStage           = GameStage.NONE;
	private final int maxPlayers          = 10;  // we only support 10 in the code atm
	private final int registrationTimeout = 1200; // 1200 is 60 seconds. 
	private final int nightLength         = 1200; // 2 minutes
	private final int discussLength       = 600; // 1 minute is 1200
	private final int nominateLength      = 1200; // 1 minute is 1200
	//private final int voteLength          = 1200; // 1 minute
	
	// timeouts
	private BukkitTask nightTimeout    = null;
	private BukkitTask discussTimeout  = null;
	private BukkitTask nominateTimeout = null;
	// private BukkitTask voteTimeout = null;
	
	// night time log
	private ArrayList<String> nightMessages = new ArrayList<String>();
	
	/* list of players who registered, even if they died or went offline */
	public ArrayList<Gamer> players = new ArrayList<Gamer>();
	
	/* list of characters */
	public ArrayList <MafiaCharacter> characters = new ArrayList<MafiaCharacter>();
	
	// Voting
	ScoreboardManager manager = Bukkit.getScoreboardManager();
	Scoreboard board          = null;
	Objective objective       = null;
	
	// Vote tracking
	HashMap<Gamer, Gamer> accuseMap = null;
	
	///HashMap<Gamer, Boolean> voteMap = null;
	HashMap<Gamer, Integer> voteCounter = null;
	
	// chat prefix 
	public String chatPrefix = ChatColor.GRAY + "[" + ChatColor.GOLD + "Mafia" + ChatColor.GRAY + "] " + ChatColor.AQUA;
	
	/***************************************************************************************************************************************************/
	
	
	@Override
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);
		
		// Create characters
		characters.add(new Citizen());
		characters.add(new Mafia());
		characters.add(new Detective());
		characters.add(new Citizen());
		characters.add(new Doctor());
		characters.add(new Citizen());
		characters.add(new Maniac());
		characters.add(new Citizen());
		characters.add(new Citizen());
		characters.add(new Citizen());
	}
	
	public void wipeScoreboard()
	{
		this.board = manager.getNewScoreboard();
		this.objective = board.registerNewObjective("mafia", "dummy");
		this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		this.objective.setDisplayName("Players alive");		
	}
	
	/***************************************************************************************************************************************************/
	

	@EventHandler
    public void onQuit(PlayerQuitEvent event)
	{
        Player player = event.getPlayer();
        
        getLogger().info("Player " + player.getName() + " left!");
        
        if (!(this.gameStage == GameStage.NONE))
        {
       		Iterator<Gamer> i = this.players.iterator();
        		
       		while (i.hasNext())
       		{
       			Gamer gamer = i.next();
       			if (gamer.player == player)
       			{
       	        	if (this.gameStage == GameStage.REGISTER)
       	        	{
       	        		i.remove();
       	        	}
       	        	else
       	        	{
       	        		if (gamer.isAlive())
       	        		{
	       	        		gamer.kill();
	       	        		this.markPlayerAsDead(gamer);
	       	        		
	       	        		if (this.gameStage != GameStage.NONE)
	       	        		{
	       	        			this.isGameFinished();
	       	        		}
       	        		}
       	        	}
        		}     		
        	}
        }
    }
	
	/***************************************************************************************************************************************************/
	
	
	private void markPlayerAsDead(Gamer gamer)
	{
		// message people to say they died
		this.messageAllPlayers(gamer.player.getName() + " (" + gamer.character.name + ") died");
		
		// remove them from the scoreboard objective
		this.board.resetScores(gamer.player.getName());
	}
	
	/***************************************************************************************************************************************************/
	
	
	public boolean isGameFinished()
	{
		/* if only 'TOWN' is left, then TOWN wins */
		/* if only 'mafia' is left, then MAFIA wins */
		/* if only 'maniac' is left, then MANIAC wins */
		/* if everybody is dead...then its a draw */
		
		int playersAlive = 0;
		
		/* set up a counter for each team */
		HashMap<MafiaTeam,Integer> teamPlayers = new HashMap<MafiaTeam,Integer>();
		for (MafiaTeam team : MafiaTeam.values())
		{
			teamPlayers.put(team, new Integer(0));
		}
		
		/* count members of each team */
		for (int i = 0; i < players.size(); i++)
		{
			Gamer gamer = players.get(i);
			if (gamer.isAlive())
			{
				teamPlayers.put(gamer.character.team, new Integer(teamPlayers.get(gamer.character.team).intValue() + 1));
				playersAlive++;
			}
		}
		
		/* Check if TOWN won */
		int town = teamPlayers.get(MafiaTeam.TOWN).intValue();
		int mafia = teamPlayers.get(MafiaTeam.MAFIA).intValue();
		int maniac = teamPlayers.get(MafiaTeam.MANIAC).intValue();
		
		boolean end = false;
		
		if (town > 0 && mafia == 0 && maniac == 0)
		{
			this.messageAllPlayers("Town won! The citizens rejoice!");
			end = true;
		}
		else if (town == 0 && mafia > 0 && maniac == 0)
		{
			this.messageAllPlayers("The mafia won! Emperor Palpatine is pleased.");
			this.messageAllPlayers("");
			end = true;
		}
		else if (town == 0 && mafia == 0 && maniac > 0)
		{
			this.messageAllPlayers("The maniac won! The maniac soon begins to feel lonely and sad.");
			end = true;
		}
		else if (town == 0 && mafia == 0 && maniac == 0)
		{
			this.messageAllPlayers("Everybody is dead. Herobrine won!");
			end = true;
		}
		else if (playersAlive == 2)
		{
			end = true;

			if (mafia > 0 && maniac <= 0)
			{
				this.messageAllPlayers("The mafia won! Khan is pleased. The Mafia turns the last townsperson evil.");
			}
			else if (maniac > 0 && mafia <= 0)
			{
				this.messageAllPlayers("The maniac won! They copulate with the last townsperson and have maniac babies");
			}
			else
			{
				this.messageAllPlayers("Only the mafia and the maniac survive! They eye each other suspiciously. #nohomo");
			}
						
		}
		
		
		// Cancel any scheduled tasks if the game is over
		// this is because the game could end during a timeout 
		// if a player leaves the server.
		if (end)
		{
			if (this.nightTimeout != null)
			{
				this.nightTimeout.cancel();
			}
			if (this.discussTimeout != null)
			{
				this.discussTimeout.cancel();
			}
			if (this.nominateTimeout != null)
			{
				this.nominateTimeout.cancel();
			}
			
			this.gameStage = GameStage.NONE;
			
			// Remove all scoreboards from players
			// and print out who was what
			for (int i = 0; i < this.players.size(); i++)
			{
				Gamer gamer = this.players.get(i);
				
				if (gamer.isAlive())
				{
					this.messageAllPlayers(gamer.player.getName() + " was: " + gamer.character.getName());
				}				
				
				if (gamer.player.isOnline())
				{
					gamer.player.setScoreboard(this.manager.getMainScoreboard());
				}
				
				// clear their scoreboard entries too
				this.board.resetScores(gamer.player.getName());
			}
		}
		
		return end;
	}	
	
	/***************************************************************************************************************************************************/
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equalsIgnoreCase("mafia"))
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage(this.chatPrefix + "This command can only be run by a player.");
			}
			else
			{
				Player player = (Player) sender;
				getLogger().info("Player " + player.getName() + " sent command: " + String.join(" ", args));
				
				if (args.length > 0)
				{
					if (args[0].equals("join") || args[0].equals("register"))
					{
						onPlayerJoin(player);
					}
					// TODO add other non in-game commands here
					else
					{
						// Find the gamer object for this player
						boolean found = false;
						Gamer gamer = null;
						for (int i = 0; i < this.players.size(); i++)
						{
							gamer = this.players.get(i);
							if (gamer.player == player)
							{
								found = true;
								break;
							}
						}
						
						// Did we find a player?
						if (!found)
						{
							player.sendMessage(this.chatPrefix + "Error: You are not in this game of mafia. Please wait for the next game");
							return true;
						}
						
						// Is the gamer alive?
						if (!gamer.isAlive())
						{
							player.sendMessage(this.chatPrefix + "Error: You're dead! You can't do any more actions. :(");
							return true;
						}
						
						if (args[0].equals("quit") || args[0].equals("leave") || args[0].equals("exit"))
						{
							// TODO IMPLEMENT PLAYER LEAVING
						}
						else if (args[0].equalsIgnoreCase("accuse"))
						{
							this.onPlayerAccuse(gamer,args);
							return true;
						}
						else
						{
							if (args.length == 2)
							{
								return onPlayerAction(gamer,args[0],args);
							}
							else
							{
								sender.sendMessage(this.chatPrefix + "Error! Usage: /z <action> <target>");
							}
						}
					}
				}
				else
				{
					return false;
				}
				
			}

			return true;
		}
		return false;
	}
	
	/***************************************************************************************************************************************************/
	

	/*
	 * REGISTER A PLAYER
	 */
	private void onPlayerJoin(Player player)
	{
		// Only allow player joins if there is no game (start it) or we're in registration
		if (this.gameStage == GameStage.NONE || this.gameStage == GameStage.REGISTER)
		{
			if (this.gameStage == GameStage.NONE)
			{
				// Start the game registration process
				this.startRegister();
			}
			else
			{
				// Block players who have joined already
				for (int i = 0; i < players.size(); i++)
				{
					if (players.get(i).player == player)
					{
						player.sendMessage(this.chatPrefix + "You're already registered - please wait for the game to start.");
						return;
					}
				}
			}
			
			// Add the player
			this.players.add(new Gamer(player));
			player.sendMessage(this.chatPrefix + "You have joined the game. Please wait for others to join.");
			
			Score score = this.objective.getScore(player.getName());
			score.setScore(0);
			player.setScoreboard(this.board);		
			
			// If we've hit max players then start the game immediatley 
			if (this.players.size() >= this.maxPlayers)
			{
				this.startPreGame();
			}
			
		}
		else
		{
			player.sendMessage(this.chatPrefix + "Sorry but a game is already underway. Please wait for it to finish");
		}
	}
	
	/***************************************************************************************************************************************************/
	
	
	/*
	 * START REGISTRATION PHASE
	 */
	private void startRegister()
	{
		this.players = new ArrayList<Gamer>();
		this.wipeScoreboard();
		this.gameStage = GameStage.REGISTER;
		Bukkit.broadcastMessage(this.chatPrefix + "A game of mafia is about to begin! To join type /z join and then type /rp to chat in-game.");
		new RegistrationTimeout(this).runTaskLater(this, this.registrationTimeout);
	}
	
	/***************************************************************************************************************************************************/
	
	
	/*
	 * POST REGISTRATION PHASE (SETTING UP THE GAME)
	 */
	public void startPreGame()
	{
		// Make sure we're still in register phase before we end it!
		if (this.gameStage == GameStage.REGISTER)
		{
			if (this.players.size() <= 2)
			{
				for (int i = 0; i < this.players.size(); i++)
				{
					this.players.get(i).player.sendMessage(this.chatPrefix + "Not enough players joined the game, sorry!");
				}
				
				this.gameStage = GameStage.NONE;
				this.players = new ArrayList<Gamer>();
			}
			else
			{
				this.gameStage = GameStage.PRE_GAME;
				
				// shuffle the order of the players who joined into roles
				ArrayList<Gamer> roles = new ArrayList<Gamer>();
				for (int i = 0; i < players.size(); i++)
				{
					roles.add(players.get(i));
				}
				Collections.shuffle(roles);
				
				String charactersString = "Characters:";
				
				// Assign roles
				for (int i = 0; i < roles.size(); i++)
				{
					Gamer gamer = roles.get(i);
					gamer.character = this.characters.get(i);
					gamer.player.sendMessage(this.chatPrefix + "You have been assigned the role of: " + ChatColor.YELLOW + gamer.character.getName());
					gamer.player.sendMessage(this.chatPrefix + gamer.character.desc);
					charactersString += " " + gamer.character.name;
					getLogger().info("Player " + gamer.player.getName() + " was assigned role " + gamer.character.name);
				}
				
				// Build string of players who are playing
				/*String playersString = "Players:";
				for (int i = 0; i < players.size(); i++)
				{
					Gamer gamer = players.get(i);
					playersString += " " + gamer.player.getName();
					
				}*/				
				
				// Broadcast who is playing and what the characters are!
				for (int i = 0; i < players.size(); i++)
				{
					Gamer gamer = players.get(i);
					//gamer.player.sendMessage(this.chatPrefix + playersString);
					gamer.player.sendMessage(this.chatPrefix + charactersString);
				}
				
				// switch to night now
				this.startNight();
			}
		}
	}
	
	/***************************************************************************************************************************************************/
	
	
	public void messageAllPlayers(String message)
	{
		for (int i = 0; i < players.size(); i++)
		{
			Gamer gamer = players.get(i);
			gamer.player.sendMessage(this.chatPrefix + message);
		}		
	}
	
	/***************************************************************************************************************************************************/
	
	
	public Gamer getGamerByPlayer(Player player)
	{
		for (int i = 0; i < players.size(); i++)
		{
			Gamer gamer = players.get(i);
			if (gamer.player == player)
			{
				return gamer;
			}
		}
		
		return null;
	}
	
	/***************************************************************************************************************************************************/
	
	
	public void startNight()
	{
		this.nightMessages = new ArrayList<String>();
		this.gameStage = GameStage.NIGHT;
		this.messageAllPlayers("It is now NIGHT");
		
		for (int i = 0; i < this.players.size(); i++)
		{
			Gamer gamer = this.players.get(i);
			
			if (gamer.isAlive())
			{
				gamer.player.sendMessage(this.chatPrefix + "It is time to do your actions!");
				gamer.player.sendMessage(this.chatPrefix + gamer.character.desc);
				
				// Reset their score to 0 in the scoreboard
				Score score = this.objective.getScore(gamer.player.getName());
				score.setScore(0);
			}
		}
		
		this.nightTimeout = new NightTimeout(this).runTaskLater(this, this.nightLength);
	}
	
	/***************************************************************************************************************************************************/
	
	
	public void checkIfNightIsOver()
	{
		for (int i =0; i < players.size(); i++)
		{
			Gamer gamer = players.get(i);
			if (gamer.isAlive())
			{
				if (!(gamer.hasPerformedAction()))
				{
					return;
				}
			}
		}
		
		// Night must be over cos every gamer returned true!
		this.nightTimeout.cancel();
		this.endNight();
		
	}
	
	/***************************************************************************************************************************************************/
	
	
	public void endNight()
	{
		if (this.gameStage == GameStage.NIGHT)
		{
			this.nightTimeout = null;
			this.gameStage = GameStage.END_NIGHT;
			this.messageAllPlayers("The sun rises and its DAY!");
		
			// Say what happened overnight
			for (int i = 0; i < this.nightMessages.size(); i++)
			{
				this.messageAllPlayers(this.nightMessages.get(i));
			}		
			
			// process players and their deaths
			for (int i = 0; i < players.size(); i++)
			{
				Gamer gamer = players.get(i);
				if (gamer.isAlive())
				{
					if (!(gamer.isAliveAfterNight()))
					{
						this.markPlayerAsDead(gamer);
					}
					
					// send the player messages and reset
					gamer.endNight();
				}
			}

			/* If the game hasn't yet finished... */
			if (!(this.isGameFinished()))
			{
				/* then proceed to start discussing */
				this.startDiscussion();
			}
		}
	}
	
	/***************************************************************************************************************************************************/
	
	private boolean onPlayerAction(Gamer gamer, String string, String[] args)
	{
        getLogger().info(":: onPlayerAction: Called by " + gamer.player.getName() + " with args: " + String.join(" ",args));

		
		if (this.gameStage == GameStage.NIGHT)
		{
			// Declare variables
			Gamer targetGamer = null;
			
			// Find the target of the action
			String target = args[1];
			Player targetPlayer = Bukkit.getPlayer(target);
			if (targetPlayer == null)
			{
				gamer.player.sendMessage(this.chatPrefix + "Error: No such player!");
				return true;
			}
			else
			{
				// Try to get the 'gamer' object for the target
				targetGamer = this.getGamerByPlayer(targetPlayer);
				
				if (targetGamer == null)
				{
					gamer.player.sendMessage(this.chatPrefix + "Error: That player isn't in the game!");
					return true;							
				}
				
				// make sure the target is alive...
				if (!(targetGamer.isAlive()))
				{
					gamer.player.sendMessage(this.chatPrefix + "Error: That player is dead!");
					return true;
				}
				
			}
			
			// Make sure they player hasn't already done their action
			if (!(gamer.hasPerformedAction()))
			{
				// KILL
				if (string.equalsIgnoreCase("kill"))
				{
					if (gamer.character.canPerformAction(CharacterAction.KILL))
					{
						gamer.setActionPerformed();
						targetGamer.attack();
						this.nightMessages.add(gamer.character.name + " attacked " + targetPlayer.getName());
						gamer.player.sendMessage(this.chatPrefix + "You have attacked " + targetPlayer.getName());
						this.checkIfNightIsOver();							
						return true;
					}
					else
					{
						gamer.player.sendMessage(this.chatPrefix + "Your character cannot perform that action");
						return true;
					}					
				}
				else if (string.equalsIgnoreCase("heal"))
				{
					if (gamer.character.canPerformAction(CharacterAction.HEAL))
					{
						gamer.setActionPerformed();
						targetGamer.heal();
						this.nightMessages.add(gamer.character.name + " healed " + targetPlayer.getName());
						gamer.player.sendMessage(this.chatPrefix + "You have healed " + targetPlayer.getName());
						this.checkIfNightIsOver();		
						return true;
					}
					else
					{
						gamer.player.sendMessage(this.chatPrefix + "Your character cannot perform that action");
						return true;
					}
				}
				else if (string.equalsIgnoreCase("check"))
				{
					if (gamer.character.canPerformAction(CharacterAction.CHECK))
					{
						gamer.setActionPerformed();
						gamer.addNightMessageIfAlive(targetPlayer.getName() + " is " + targetGamer.character.name);
						gamer.player.sendMessage(this.chatPrefix + "You have checked " + targetPlayer.getName());
						this.checkIfNightIsOver();							
						return true;
					}
					else
					{
						gamer.player.sendMessage(this.chatPrefix + "Your character cannot perform that action");
						return true;
					}					
				}
				else
				{
					gamer.player.sendMessage(this.chatPrefix + "What? I didn't understand that command");
					return true;	
				}
			}
			else
			{
				gamer.player.sendMessage(this.chatPrefix + "Error: You've already performed your action for tonight.");
				return true;
			}
		}
		else
		{
			gamer.player.sendMessage(this.chatPrefix + "Error: You can only perform actions at night!");
			return true;
			
		}
	}	
	
	/***************************************************************************************************************************************************/
	
	
	public void startDiscussion()
	{	
		this.gameStage = GameStage.DISCUSS;
		this.messageAllPlayers("You now have a few moments to discuss what to do next");
		this.discussTimeout = new DiscussionTimeout(this).runTaskLater(this, this.discussLength);
		
		/* Prepare for accusations */
		this.accuseMap   = new HashMap<Gamer, Gamer>();
		this.voteCounter = new HashMap<Gamer, Integer>();
		
		for (int i = 0; i < this.players.size(); i++)
		{
			Gamer gamer = this.players.get(i);
			if (gamer.isAlive())
			{
				// Create a zero score for the player in our vote counter
				this.voteCounter.put(gamer, new Integer(0));
			}
		}			
	}
	
	public void startNominations()
	{
		if (this.gameStage == GameStage.DISCUSS)
		{
			this.discussTimeout = null;
			this.gameStage = GameStage.NOMINATE;
			this.messageAllPlayers("You may now nominate players to be put to death with /z accuse <name>");
			this.nominateTimeout = new NominationTimeout(this).runTaskLater(this, this.nominateLength);
		}
	}
	
	public void onPlayerAccuse(Gamer gamer, String[] args)
	{
		getLogger().info(":: onPlayerAccuse: Accusation from " + gamer.player.getName() + " accusing: " + String.join("", args));

		
		if (this.gameStage == GameStage.NOMINATE)
		{
			// Declare variables
			Gamer targetGamer = null;
			
			// Find the target of the action
			String target = args[1];
			Player targetPlayer = Bukkit.getPlayer(target);
			if (targetPlayer == null)
			{
				gamer.player.sendMessage(this.chatPrefix + "Error: No such player!");
				return;
			}
			else
			{
				// Try to get the 'gamer' object for the target
				targetGamer = this.getGamerByPlayer(targetPlayer);
				
				if (targetGamer == null)
				{
					gamer.player.sendMessage(this.chatPrefix + "Error: That player isn't in the game!");
					return;
				}
				
				// make sure the target is alive...
				if (!(targetGamer.isAlive()))
				{
					gamer.player.sendMessage(this.chatPrefix + "Error: That player is dead!");
					return;
				}
			}	
			
			// Has this player already voted?
			Gamer previousTargetGamer = this.accuseMap.get(gamer);
			
			if (previousTargetGamer == null)
			{
				getLogger().info(":: onPlayerAccuse: Player hasn't voted yet...");
			}
			else
			{
				getLogger().info(":: onPlayerAccuse: Player has already voted");

				// They've already voted. 
				// check if they're changing their vote
				// if they're changing their vote, change it!
				if (previousTargetGamer != targetGamer)
				{
					getLogger().info(":: onPlayerAccuse: Reducing score of " + previousTargetGamer.player.getName());
					
					// Change the scoreboard view
					Score score = objective.getScore(previousTargetGamer.player.getName());
					score.setScore(score.getScore() - 1);
					
					// Change our tracker of votes
					this.voteCounter.put(previousTargetGamer, new Integer(this.voteCounter.get(previousTargetGamer).intValue() - 1));
				}
				else
				{
					// No vote to change!
					return;
				}
			}
			
			getLogger().info(":: onPlayerAccuse: Increasing score of " + targetGamer.player.getName());
			
			
			// Change the scoreboard to show +1 votes on the target player
			Score score = objective.getScore(targetGamer.player.getName());
			score.setScore(score.getScore() + 1);
						
			// Record this player voted for the target player
			this.accuseMap.put(gamer, targetGamer);
			
			// tell everybody
			this.messageAllPlayers(gamer.player.getName() + " accused " + targetGamer.player.getName());	
			
			// Record the total votes for the target in our map
			this.voteCounter.put(targetGamer, new Integer(this.voteCounter.get(targetGamer).intValue() + 1));
		}
	}
	
	public void endNominations()
	{
		if (this.gameStage == GameStage.NOMINATE)
		{
			this.nominateTimeout = null;
			this.gameStage = GameStage.END_NOMINATE;
			
			// Calculate if anybody got a majority of votes
			Gamer topGamer = null;
			int topScore = 0;
			boolean sameScore = false;
			
			for (Map.Entry<Gamer, Integer> entry : this.voteCounter.entrySet())
			{
				Gamer tGamer = entry.getKey();
				int score    = entry.getValue().intValue();
				
				if (score > topScore)
				{
					topScore = score;
					topGamer = tGamer;
					sameScore = false;
				}
				else if (score == topScore)
				{
					sameScore = true;
				}
			}
			
			if (!(topGamer == null))
			{
				// Somebody might have been chosen
				if (!sameScore)
				{
					// we can kill the person!
					// TODO do a vote instead...
					this.messageAllPlayers("The citizens have chosen to put " + topGamer.player.getName() + " to death!");
					topGamer.kill();
					this.markPlayerAsDead(topGamer);
					
					if (this.isGameFinished())
					{
						return;
					}
					else
					{
						this.startNight();
						return;
					}
				}
			}
			
			this.messageAllPlayers("The citizens could not make a decision!");
			this.startNight();
			
		}
	}
}