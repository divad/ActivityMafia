package io.evad.mineactivity.mafia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

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


/*
 * TODO:
 * moar players
 * moar roles
 * double mafia
 */

public class ActivityMafia extends JavaPlugin implements Listener 
{
	private GameStage gameStage           = GameStage.NONE;
	private final int maxPlayers          = 10;  // we only support 10 in the code atm
	private final int registrationTimeout = 1200; // 1200 is 60 seconds. 
	private final int nightLength         = 1000; // 2 minutes
	private final int discussLength       = 500; // 1 minute is 1200
	private final int nominateLength      = 1000; // 1 minute is 1200
	private final int voteLength          = 600; // 1 minute
	
	// timeouts
	private BukkitTask nightTimeout    = null;
	private BukkitTask discussTimeout  = null;
	private BukkitTask nominateTimeout = null;
	private BukkitTask voteTimeout = null;
	
	// night time log
	private ArrayList<String> nightMessages = new ArrayList<String>();
	
	/* list of players who registered, even if they died or went offline */
	public ArrayList<Gamer> players = new ArrayList<Gamer>();
	
	/* list of characters */
	public ArrayList <MafiaCharacter> characters = new ArrayList<MafiaCharacter>();
	
	// accuse/nominate/vote
	ScoreboardManager manager = null;
	Scoreboard board          = null;
	Objective objective       = null;
	
	// Accusation tracking
	HashMap<Gamer, Gamer> accuseMap = null;
	HashMap<Gamer, Integer> accusationCounter = null;
	
	// Vote tracking
	HashMap<Gamer, Boolean> voteMap = null;
	int voteYes = 0;
	int  voteNo = 0;
	Gamer chosenGamer = null;
	
	// chat prefix 
	public ChatColor textColour = ChatColor.AQUA;
	public ChatColor winAnnounceColour = ChatColor.GOLD;
	public String chatPrefix = ChatColor.GRAY + "[" + ChatColor.GOLD + "Mafia" + ChatColor.GRAY + "] " + textColour;
	
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
		characters.add(new Streetwalker());
		characters.add(new Maniac());
		characters.add(new Citizen());
		characters.add(new Citizen());
		characters.add(new Streetwalker());
	}
	
	public void wipeScoreboard()
	{
		this.manager   = Bukkit.getScoreboardManager();
		this.board     = this.manager.getNewScoreboard();
		this.objective = this.board.registerNewObjective("mafia", "dummy");
		this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		this.objective.setDisplayName("Players alive");		
	}
	
	/***************************************************************************************************************************************************/
	

	@EventHandler
    public void onQuit(PlayerQuitEvent event)
	{
		this.onPlayerQuit(event.getPlayer());
	}
	
	/*
	 * Handler for when a player quits (goes offline), is kicked/banned (goes offline)
	 * or asks to leave the game	
	 */
    public void onPlayerQuit(Player player)
	{	
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
       	        		// remove them from the list of players
       	        		i.remove();
       	        		
       	        		// remove them from the scoreboard
       	        		this.board.resetScores(gamer.player.getName());
       	        	}
       	        	else
       	        	{
       	        		if (gamer.isAlive())
       	        		{
       	        			this.messageAllPlayers(gamer.player.getName() + " commited suicide!");
	       	        		gamer.kill();
	       	        		this.markPlayerAsDead(gamer);
	       	        		
	       	        		if (this.gameStage != GameStage.NONE)
	       	        		{
	       	        			this.isGameFinished();
	       	        		}
       	        		}    	        		
       	        	}
       	        	
   	        		// if they're still online (i.e. they did /z quit) then hide the scores from them
       	        	// and tell them they left
   					if (gamer.player.isOnline())
   					{
   						gamer.player.setScoreboard(this.manager.getMainScoreboard());
   						
   						gamer.player.sendMessage(this.chatPrefix + " You left the game!");   					
   					}       	        	
        		}     		
        	}
        }
    }
	
	/***************************************************************************************************************************************************/
	
	
	private void markPlayerAsDead(Gamer gamer)
	{
		// message people to say they died
		this.messageAllPlayers(gamer.player.getName() + " (" + gamer.character.name + ") " + ChatColor.RED + "died");
		
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
			this.messageAllPlayers(this.winAnnounceColour + "Town won! The citizens rejoice!");
			end = true;
		}
		else if (town == 0 && mafia > 0 && maniac == 0)
		{
			this.messageAllPlayers(this.winAnnounceColour + "The mafia won! Emperor Palpatine is pleased.");
			end = true;
		}
		else if (town == 0 && mafia == 0 && maniac > 0)
		{
			this.messageAllPlayers(this.winAnnounceColour + "The maniac won! The maniac soon begins to feel lonely and sad.");
			end = true;
		}
		else if (town == 0 && mafia == 0 && maniac == 0)
		{
			this.messageAllPlayers(this.winAnnounceColour + "Everybody is dead. Herobrine won!");
			end = true;
		}
		else if (playersAlive == 2)
		{
			end = true;

			if (mafia > 0 && maniac <= 0)
			{
				this.messageAllPlayers(this.winAnnounceColour + "The mafia won! Khan is pleased. The Mafia turns the last townsperson evil.");
			}
			else if (maniac > 0 && mafia <= 0)
			{
				this.messageAllPlayers(this.winAnnounceColour + "The maniac won! They copulate with the last townsperson and have maniac babies");
			}
			else
			{
				this.messageAllPlayers(this.winAnnounceColour + "Only the mafia and the maniac survive! They eye each other suspiciously. #nohomo");
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
			if (this.voteTimeout != null)
			{
				this.voteTimeout.cancel();
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
							this.onPlayerQuit(gamer.player);
						}
						else if (args[0].equalsIgnoreCase("accuse"))
						{
							this.onPlayerAccuse(gamer,args);
							return true;
						}
						else if (args[0].equalsIgnoreCase("yes"))
						{
							this.onPlayerVote(gamer,true);
							return true;
						}
						else if (args[0].equalsIgnoreCase("no"))
						{
							this.onPlayerVote(gamer,false);
							return true;
						}						
						else
						{
							return onPlayerAction(gamer,args[0],args);
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
					this.players.get(i).player.setScoreboard(this.manager.getMainScoreboard());
					this.wipeScoreboard();
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
					charactersString += " " + gamer.character.name;
					getLogger().info("Player " + gamer.player.getName() + " was assigned role " + gamer.character.name);
				}
				
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
			
			if (args.length <= 1)
			{
				gamer.player.sendMessage(this.chatPrefix + "Error: You must specify a target");
				return false;
			}
			
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
				// get message (if any)
				String actionMessage = "";
				if (args.length > 2)
				{
					String[] actionMessages = Arrays.copyOfRange(args, 2, args.length);
					actionMessage = " (" + String.join(" ", actionMessages) + ")";
				}
				
				// KILL
				if (string.equalsIgnoreCase("kill"))
				{
					if (gamer.character.canPerformAction(CharacterAction.KILL))
					{
						gamer.setActionPerformed();
						targetGamer.attack();
						this.nightMessages.add(gamer.character.name + ChatColor.YELLOW + " attacked " + this.textColour + targetPlayer.getName() + actionMessage);
						gamer.player.sendMessage(this.chatPrefix + "You have attacked " + targetPlayer.getName());
						
						if (gamer.character.team == MafiaTeam.TOWN)
						{
							if (targetGamer.character.team == MafiaTeam.TOWN)
							{
								this.nightMessages.add("The " + gamer.character.getName() + " attacked their own team! Due to their disgrace, they resign and become a citizen.");
								gamer.character = new Citizen();
							}
						}
						
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
						this.nightMessages.add(gamer.character.name + ChatColor.GREEN + " healed " + this.textColour + targetPlayer.getName() + actionMessage);
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
						this.nightMessages.add(gamer.character.name + ChatColor.GREEN + " checked out " + this.textColour + targetPlayer.getName() + actionMessage);						
						gamer.addNightMessageIfAlive(this.chatPrefix + ChatColor.GOLD + targetPlayer.getName() + " is " + targetGamer.character.name);
						gamer.player.sendMessage(this.chatPrefix + "You have checked " + targetPlayer.getName() + " - if you survive the night you will be told their role");
						this.checkIfNightIsOver();
						return true;
					}
					else
					{
						gamer.player.sendMessage(this.chatPrefix + "Your character cannot perform that action");
						return true;
					}					
				}
				else if (string.equalsIgnoreCase("love"))
				{
					if (gamer.character.canPerformAction(CharacterAction.LOVE))
					{
						gamer.setActionPerformed();
						gamer.player.sendMessage(this.chatPrefix + "You have slipped beneath the silk sheets with " + targetPlayer.getName());

						Random rng = new Random();
						int ran = rng.nextInt(3) + 1;
						
						if (ran == 1)
						{
							// reveal role
							this.nightMessages.add(gamer.character.name + ChatColor.GREEN + " spent the night with " + this.textColour + targetPlayer.getName() + ", who wakes up to a note saying 'I know who you are!'" + actionMessage);																				
							gamer.addNightMessageIfAlive("In their sleep your partner mumbles details to identify them!");
							gamer.addNightMessageIfAlive(ChatColor.GOLD + targetPlayer.getName() + " is " + targetGamer.character.name);
						}
						else if (ran == 2)
						{
							// kill target
							this.nightMessages.add(gamer.character.name + ChatColor.GREEN + " spent the night with " + this.textColour + targetPlayer.getName() + " but forgot to use protection!" + actionMessage);													
							targetGamer.attack();
						}
						else if (ran == 3)
						{
							// heal target
							this.nightMessages.add(gamer.character.name + ChatColor.GREEN + " spent the night with " + this.textColour + targetPlayer.getName() + " which causes them to feel healed!" + actionMessage);													
							targetGamer.heal();
						}				
						else if (ran == 4)
						{
							// nothing happens
							this.nightMessages.add(gamer.character.name + ChatColor.GREEN + " tried to spend the night with " + this.textColour + targetPlayer.getName() + " but they were rejected!" + actionMessage);													
						}
						
						this.checkIfNightIsOver();
						return true;
					}
					else
					{
						gamer.player.sendMessage(this.chatPrefix + "Your character cannot perform that action");
						return true;
					}					
				}	
				else if (string.equalsIgnoreCase("pray"))
				{
					if (gamer.character.canPerformAction(CharacterAction.PRAY))
					{
						gamer.setActionPerformed();
						gamer.player.sendMessage(this.chatPrefix + "You have prayed to the flying spaghetti monster asking him to protect " + targetPlayer.getName());

						Random rng = new Random();
						int ran = rng.nextInt(9) + 1;
						
						if (ran == 1)
						{
							// reveal role
							this.nightMessages.add(gamer.character.name + ChatColor.GREEN + " prayed for " + this.textColour + targetPlayer.getName() + "'s life. They were healed by divine intervention!" + actionMessage);																				
							targetGamer.heal();
						}
						else
						{
							this.nightMessages.add(gamer.character.name + ChatColor.GREEN + " prayed for " + this.textColour + targetPlayer.getName() + "'s life. Sadly, nothing happened." + actionMessage);																				
							
						}
						

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
		this.accusationCounter = new HashMap<Gamer, Integer>();
		
		for (int i = 0; i < this.players.size(); i++)
		{
			Gamer gamer = this.players.get(i);
			if (gamer.isAlive())
			{
				// Create a zero score for the player in our vote counter
				this.accusationCounter.put(gamer, new Integer(0));
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
					// but only if the player is still alive (they could quit mid vote)
					if (previousTargetGamer.isAlive())
					{
						Score score = objective.getScore(previousTargetGamer.player.getName());
						score.setScore(score.getScore() - 1);
					}
					
					// Change our tracker of votes
					this.accusationCounter.put(previousTargetGamer, new Integer(this.accusationCounter.get(previousTargetGamer).intValue() - 1));
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
			this.accusationCounter.put(targetGamer, new Integer(this.accusationCounter.get(targetGamer).intValue() + 1));
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
			
			for (Map.Entry<Gamer, Integer> entry : this.accusationCounter.entrySet())
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
					this.startVote(topGamer);
					return;
				}
			}
			
			this.messageAllPlayers("The citizens could not make a decision!");
			this.startNight();
			
		}
	}
	
	public void startVote(Gamer gamer)
	{
		if (this.gameStage == GameStage.END_NOMINATE)
		{
			this.objective.setDisplayName("Put to death?");
				
			this.voteMap     = new HashMap<Gamer, Boolean>();
			this.voteYes     = 0;
			this.voteNo      = 0;
			this.chosenGamer = gamer;
				
			for (int i = 0; i < players.size(); i++)
			{
				Gamer cgamer = players.get(i);
				if (cgamer.isAlive())
				{
					this.board.resetScores(cgamer.player.getName());
				}
			}
			
			this.gameStage = GameStage.VOTE;
			this.messageAllPlayers("The citizens have accused " + gamer.player.getName() + " of being evil!");
			this.messageAllPlayers("You can now vote /z yes or /z no to decide their fate.");
			
			Score scorey = objective.getScore("yes");
			scorey.setScore(0);
			Score scoren = objective.getScore("no");
			scoren.setScore(0);		
			
			// start timeout for when voting ends!
			this.voteTimeout = new VoteTimeout(this).runTaskLater(this, this.voteLength);
		}
	}
		
		
	public void onPlayerVote(Gamer gamer, boolean killThem)
	{
		// did they already vote?
		Boolean existingVote = this.voteMap.get(gamer);
		
		if (existingVote != null)
		{
			// they already voted, so check if the vote has changed
			if (killThem == existingVote.booleanValue())
			{
				// they didnt change their vote, so just return
				return;
			}
			else
			{
				// They did change their vote, so reduce the previous score by one
				if (existingVote.booleanValue() == true)
				{
					this.voteYes--;
					Score score = objective.getScore("yes");
					score.setScore(score.getScore() - 1);
				}
				else
				{
					this.voteNo--;
					Score score = objective.getScore("no");
					score.setScore(score.getScore() - 1);					
				}
			}
			
		}
		
		// Map their vote (either new or updated)
		this.voteMap.put(gamer,new Boolean(killThem));
		
		// add scores
		if (killThem)
		{
			Score score = objective.getScore("yes");
			score.setScore(score.getScore() + 1);			
			this.voteYes++;
			this.messageAllPlayers(gamer.player.getName() + " voted yes");
		}
		else
		{
			Score score = objective.getScore("no");
			score.setScore(score.getScore() + 1);				
			this.voteNo++;
			this.messageAllPlayers(gamer.player.getName() + " voted no");
			
		}		
	}
	
	public void endVote()
	{	
		// update game stage
		this.gameStage = GameStage.END_VOTE;
		
		// Wipe the object variable for the chosen gamer
		Gamer targetGamer = this.chosenGamer;
		this.chosenGamer = null;
		
		// restore previous scoreboard
		this.objective.setDisplayName("Players alive");
		this.board.resetScores("yes");
		this.board.resetScores("no");
		
		
		for (int i = 0; i < players.size(); i++)
		{
			Gamer cgamer = players.get(i);
			if (cgamer.isAlive())
			{
				Score score = objective.getScore(cgamer.player.getName());
				score.setScore(0);
			}
		}		
		
		if (this.voteYes == this.voteNo)
		{
			this.messageAllPlayers("The citizens could not make a decision!");
			this.startNight();
		}
		else if (this.voteYes > this.voteNo)
		{
			this.messageAllPlayers("The citizens chose to put " + targetGamer.player.getName() + " to death!");
		
			targetGamer.kill();
			this.markPlayerAsDead(targetGamer);
		
			if (this.isGameFinished())
			{
				return;
			}
			else
			{
				this.startNight();
			}
		}
		else
		{
			this.messageAllPlayers("The citizens chose to not kill " + targetGamer.player.getName());
			this.startNight();
		}
	}
}