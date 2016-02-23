package io.evad.mineactivity.mafia;

import java.util.ArrayList;
import java.util.Arrays;
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

import io.evad.mineactivity.mafia.actions.CharacterAction;
import io.evad.mineactivity.mafia.characters.*;
import io.evad.mineactivity.mafia.enums.*;
import io.evad.mineactivity.mafia.timeouts.*;


/*
 * TODO:
 * new engine
 * make it so that suicides in vote/accuse removes them from stuff
 * moar gamers
 * moar roles
 * double mafia
 */

public class ActivityMafia extends JavaPlugin implements Listener 
{
	private GameStage gameStage           = GameStage.NONE;
	private final int maxPlayers          = 15; 
	private final int registrationTimeout = 1600; // 1200 is 60 seconds. 
	private final int nightLength         = 1000; // 2 minutes
	private final int discussLength       = 500; // 1 minute is 1200
	private final int nominateLength      = 1000; // 1 minute is 1200
	private final int voteLength          = 600; // 1 minute
	
	// timeouts
	private BukkitTask nightTimeout    = null;
	private BukkitTask discussTimeout  = null;
	private BukkitTask nominateTimeout = null;
	private BukkitTask voteTimeout     = null;
	
	// night time log
	private ArrayList<String> nightMessages = new ArrayList<String>();
	
	/* list of gamers who registered, even if they died or went offline */
	public ArrayList<Gamer> gamers = new ArrayList<Gamer>();
	
	/* list of characters */
	public ArrayList <MafiaCharacter> characters = new ArrayList<MafiaCharacter>();
	
	// accuse/nominate/vote
	ScoreboardManager manager = null;
	Scoreboard board          = null;
	Objective objective       = null;
	
	// Accusation tracking
	HashMap<Gamer, Gamer> accuseMap           = null;
	HashMap<Gamer, Integer> accusationCounter = null;
	
	// Vote tracking
	HashMap<Gamer, Boolean> voteMap = null;
	int voteYes = 0;
	int  voteNo = 0;
	Gamer chosenGamer = null;
	
	// chat prefix 
	public static ChatColor textColour = ChatColor.AQUA;
	public static ChatColor winAnnounceColour = ChatColor.GOLD;
	public static String chatPrefix = ChatColor.GRAY + "[" + ChatColor.GOLD + "Mafia" + ChatColor.GRAY + "] " + textColour;
	
	/***************************************************************************************************************************************************/
	
	public void addNightMessage(String message)
	{
		this.nightMessages.add(message);
	}
	
	@Override
	public void onEnable()
	{
		getServer().getPluginManager().registerEvents(this, this);
		
		// Create characters
		characters.add(new Citizen());
		characters.add(new Mafia());
		characters.add(new Detective());
		characters.add(new Doctor());
		characters.add(new Streetwalker());
		characters.add(new DoubleAgent());
		characters.add(new Bodyguard());
		characters.add(new Jailor());		
		characters.add(new Maniac());
		characters.add(new Streetwalker());
		characters.add(new Citizen());
		characters.add(new Mafia());
		characters.add(new Doctor());
		characters.add(new Citizen());		
		characters.add(new Maniac());
		
		getLogger().info("Plugin initalised successfully");
	}
	
	public void wipeScoreboard()
	{
		getLogger().info(":: wipeScoreboard");
		
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
		getLogger().info(":: onQuit");
		this.onPlayerQuit(event.getPlayer());
	}
	
	/*
	 * Handler for when a player quits (goes offline), is kicked/banned (goes offline)
	 * or asks to leave the game	
	 */
    public void onPlayerQuit(Player player)
	{	
    	getLogger().info(":: onPlayerQuit");
    	
        if (!(this.gameStage == GameStage.NONE))
        {
       		Iterator<Gamer> i = this.gamers.iterator();
        		
       		while (i.hasNext())
       		{
       			Gamer gamer = i.next();
       			if (gamer.player == player)
       			{
       	        	if (this.gameStage == GameStage.REGISTER)
       	        	{
       	        		// remove them from the list of gamers
       	        		i.remove();
       	        		
       	        		// remove them from the scoreboard
       	        		this.board.resetScores(gamer.player.getName());
       	        	}
       	        	else
       	        	{
       	        		if (gamer.isAlive())
       	        		{
       	        			this.messageAllPlayers(gamer.player.getName() + " commited suicide!");
	       	        		this.killPlayer(gamer);
	       	        		
	       	        		if (this.gameStage != GameStage.NONE)
	       	        		{
	       	        			this.isGameFinished();
	       	        		}
	       	        		else
	       	        		{
	       	        			// if vote in progress, and /this/ player was nominated/accused, then skip to next night
	       	        			if (this.gameStage == GameStage.VOTE)
	       	        			{
	       	        				if (this.chosenGamer == gamer)
	       	        				{
	       	        					if (this.voteTimeout != null)
	       	        					{
	       	        						this.voteTimeout.cancel();
	       	        					}
	       	        					
	       	        					this.startNight();	       	        					
	       	        				}
	       	        			}
	       	        			
	       	        			// TODO handle removing votes for gamers, if any, during accuse stage
	       	        		}
       	        		}    	        		
       	        	}
       	        	
   	        		// if they're still online (i.e. they did /z quit) then hide the scores from them
       	        	// and tell them they left
   					if (gamer.player.isOnline())
   					{
   						gamer.player.setScoreboard(this.manager.getMainScoreboard());
   						
   						gamer.player.sendMessage(ActivityMafia.chatPrefix + " You left the game!");   					
   					}       	        	
        		}     		
        	}
        }
    }
	
	/***************************************************************************************************************************************************/
	
	
	private void killPlayer(Gamer gamer)
	{
		getLogger().info(":: killPlayer");
		
		// Mark them as dead on the player object
		gamer.kill();
		
		// message people to say they died
		this.messageAllPlayers(gamer.player.getName() + " (" + gamer.character.name + ") " + ChatColor.RED + "died");
		
		// remove them from the scoreboard objective
		this.board.resetScores(gamer.player.getName());
	}
	
	/***************************************************************************************************************************************************/
	
	
	public boolean isGameFinished()
	{
		getLogger().info(":: isGameFinished");
		
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
		for (Gamer gamer : this.gamers)
		{
			if (gamer.isAlive())
			{
				teamPlayers.put(gamer.character.team, new Integer(teamPlayers.get(gamer.character.team).intValue() + 1));
				playersAlive++;
			}
		}
		
		/* Check if TOWN won */
		int town   = teamPlayers.get(MafiaTeam.TOWN).intValue();
		int mafia  = teamPlayers.get(MafiaTeam.MAFIA).intValue();
		int maniac = teamPlayers.get(MafiaTeam.MANIAC).intValue();
		
		boolean end = false;
		
		if (town > 0 && mafia == 0 && maniac == 0)
		{
			this.messageAllPlayers(ActivityMafia.winAnnounceColour + "Town won! The citizens rejoice!");
			end = true;
		}
		else if (town == 0 && mafia > 0 && maniac == 0)
		{
			this.messageAllPlayers(ActivityMafia.winAnnounceColour + "The mafia won!");
			end = true;
		}
		else if (town == 1 && mafia == 2)
		{
			this.messageAllPlayers(ActivityMafia.winAnnounceColour + "The mafia won!");
			end = true;
		}		
		else if (town == 0 && mafia == 0 && maniac > 0)
		{
			this.messageAllPlayers(ActivityMafia.winAnnounceColour + "The maniac won!");
			end = true;
		}
		else if (town == 0 && mafia == 0 && maniac == 0)
		{
			this.messageAllPlayers(ActivityMafia.winAnnounceColour + "Everybody is dead. Herobrine wins!");
			end = true;
		}
		else if (playersAlive == 2)
		{
			end = true;

			if (mafia > 0 && maniac <= 0)
			{
				this.messageAllPlayers(ActivityMafia.winAnnounceColour + "The mafia won!");
			}
			else if (maniac > 0 && mafia <= 0)
			{
				this.messageAllPlayers(ActivityMafia.winAnnounceColour + "The maniac won!");
			}
			else
			{
				this.messageAllPlayers(ActivityMafia.winAnnounceColour + "The mania and mafia both win!");
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
			
			// Remove all scoreboards from gamers
			// and print out who was what
			for (Gamer gamer : this.gamers)
			{
				if (gamer.isAlive())
				{
					this.messageAllPlayers(gamer.player.getName() + " was: " + gamer.character.name);
				}				
				
				if (gamer.player.isOnline())
				{
					gamer.player.setScoreboard(this.manager.getMainScoreboard());
				}
				
				this.board.resetScores(gamer.player.getName());
			}
		}
		
		return end;
	}	
	
	/***************************************************************************************************************************************************/
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		getLogger().info(":: onCommand");
		
		if (cmd.getName().equalsIgnoreCase("mafia"))
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage(ActivityMafia.chatPrefix + "This command can only be run by a player.");
			}
			else
			{
				Player player = (Player) sender;
				
				if (args.length > 0)
				{
					if (args[0].equals("join") || args[0].equals("register"))
					{
						onPlayerJoin(player);
					}
					else
					{
						// Find the gamer object for this player
						Gamer gamer = null;
						for (Gamer cgamer : this.gamers)
						{
							if (cgamer.player == player)
							{
								gamer = cgamer;
								break;
							}
						}
						
						// Did we find a player?
						if (gamer == null)
						{
							player.sendMessage(ActivityMafia.chatPrefix + "Error: You are not in this game of mafia. Please wait for the next game");
							return true;
						}
						
						// Is the gamer alive?
						if (!gamer.isAlive())
						{
							player.sendMessage(ActivityMafia.chatPrefix + "Error: You're dead! You can't do any more actions. :(");
							return true;
						}
						
						if (args[0].equalsIgnoreCase("quit") || args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase("exit") || args[0].equalsIgnoreCase("suicide"))
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
							// anything else is treated as a night command
							onPlayerAction(gamer,args[0],args);
							return true;
						}
					}
					
					return true;
				}
				else
				{
					return false;
				}
			}
		}
		return false;
	}
	
	/***************************************************************************************************************************************************/
	

	/*
	 * REGISTER A PLAYER
	 */
	private void onPlayerJoin(Player player)
	{
		getLogger().info(":: onPlayerJoin");
		
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
				// Block gamers who have joined already
				for (Gamer gamer : this.gamers)
				{
					if (gamer.player == player)
					{
						player.sendMessage(ActivityMafia.chatPrefix + "You're already registered, please wait for the game to start.");
						return;
					}
				}
			}
			
			// Add the player
			this.gamers.add(new Gamer(player));
			player.sendMessage(ActivityMafia.chatPrefix + "You have joined the game, please wait for it to start.");
			
			Score score = this.objective.getScore(player.getName());
			score.setScore(0);
			player.setScoreboard(this.board);		
			
			// If we've hit max gamers then start the game immediatley 
			if (this.gamers.size() >= this.maxPlayers)
			{
				this.startPreGame();
			}
			
		}
		else
		{
			player.sendMessage(ActivityMafia.chatPrefix + "Sorry but a game is already underway. Please wait for it to finish");
		}
	}
	
	/***************************************************************************************************************************************************/
	
	
	/*
	 * START REGISTRATION PHASE
	 */
	private void startRegister()
	{
		getLogger().info(":: startRegister");
		
		this.gamers = new ArrayList<Gamer>();
		this.wipeScoreboard();
		this.gameStage = GameStage.REGISTER;
		Bukkit.broadcastMessage(ActivityMafia.chatPrefix + "Mafia is about to begin! To join type /z join and then type /rp to chat.");
		new RegistrationTimeout(this).runTaskLater(this, this.registrationTimeout);
	}
	
	/***************************************************************************************************************************************************/
	
	
	/*
	 * POST REGISTRATION PHASE (SETTING UP THE GAME)
	 */
	public void startPreGame()
	{
		getLogger().info(":: startPreGame");
		
		// Make sure we're still in register phase before we end it!
		if (this.gameStage == GameStage.REGISTER)
		{
			if (this.gamers.size() <= 3)
			{
				for (Gamer gamer : this.gamers)
				{
					gamer.player.sendMessage(ActivityMafia.chatPrefix + "Not enough gamers joined the game, sorry!");
					gamer.player.setScoreboard(this.manager.getMainScoreboard());
					this.wipeScoreboard();
				}
				
				
				this.gameStage = GameStage.NONE;
				this.gamers = new ArrayList<Gamer>();
			}
			else
			{
				this.gameStage = GameStage.PRE_GAME;
				
				// shuffle the order of the gamers who joined into roles
				ArrayList<Gamer> roles = new ArrayList<Gamer>();
				for (Gamer gamer : this.gamers)
				{
					roles.add(gamer);
				}
				
				
				Collections.shuffle(roles);
				Collections.shuffle(roles);
				Collections.shuffle(roles);
				
				//String charactersString = "Characters:";
				
				// Assign roles
				for (int i = 0; i < roles.size(); i++)
				{
					Gamer gamer     = roles.get(i);
					gamer.character = this.characters.get(i);
					gamer.player.sendMessage(ActivityMafia.chatPrefix + "You have been assigned the role of: " + ChatColor.YELLOW + gamer.character.name);
					
					//charactersString += " " + gamer.character.name;
				}
				
				// Do team notifications 
				for (Gamer gamer : this.gamers)
				{
					// some characters tell other characters who they are
					gamer.character.informOtherPlayersOfRole(gamer, this.gamers);
				}
				
				// Broadcast the characters who are in the game
				/*for (Gamer gamer : this.players)
				{
					gamer.player.sendMessage(ActivityMafia.chatPrefix + charactersString);
				}*/
				
				// switch to night nowto 
				this.startNight();
			}
		}
	}
	
	/***************************************************************************************************************************************************/
	
	
	public void messageAllPlayers(String message)
	{
		getLogger().info(":: messageAllPlayers");
		
		for (Gamer gamer : this.gamers)
		{
			gamer.player.sendMessage(ActivityMafia.chatPrefix + message);
		}		
	}
	
	/***************************************************************************************************************************************************/
	
	
	public Gamer getGamerByPlayer(Player player)
	{
		getLogger().info(":: getGamerByPlayer");
		
		for (Gamer gamer : this.gamers)
		{
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
		getLogger().info(":: startNight");
		
		this.nightMessages = new ArrayList<String>();
		this.gameStage = GameStage.NIGHT;
		this.messageAllPlayers("It is now NIGHT");
		
		for (Gamer gamer : this.gamers)
		{
			if (gamer.isAlive())
			{
				gamer.player.sendMessage(ActivityMafia.chatPrefix + "It is time to do your actions!");
				gamer.player.sendMessage(ActivityMafia.chatPrefix + gamer.character.desc);
				
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
		getLogger().info(":: checkIfNightIsOver");
		
		for (Gamer gamer : this.gamers)
		{
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
		getLogger().info(":: endNight");
		
		if (this.gameStage == GameStage.NIGHT)
		{
			this.nightTimeout = null;
			this.gameStage = GameStage.END_NIGHT;
			
			
			// Do the actions requested by gamers in the night
			for (Gamer gamer : this.gamers)
			{
				if (gamer.isAlive())
				{
					getLogger().info(":: Processing night actions for " + gamer.player.getName());
					if (!gamer.actionBlocked())
					{
						getLogger().info(":: " + gamer.player.getName() + " was not blocked!");
						gamer.doAction(this);
					}
					else
					{
						getLogger().info(":: " + gamer.player.getName() + " was blocked!");
					}
				}
			}
			
			this.messageAllPlayers("The sun rises and it is DAY!");
		
			// Say what happened overnight
			for (String message : this.nightMessages)
			{
				this.messageAllPlayers(message);
			}		
			
			// process gamers and their deaths
			for (Gamer gamer : this.gamers)
			{
				if (gamer.isAlive())
				{
					if (gamer.wasKilled())
					{
						// Check if somebody acted as a bodyguard
						if (gamer.wasGuarded())
						{
							
							this.killPlayer(gamer.guardedBy);
						}
						else
						{
							this.killPlayer(gamer);
						}
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
	
	private void onPlayerAction(Gamer gamer, String cmd, String[] args)
	{
		getLogger().info(":: onPlayerAction");
		
		if (this.gameStage == GameStage.NIGHT)
		{
			// Declare variables
			Gamer targetGamer = null;
			
			if (args.length <= 1)
			{
				gamer.player.sendMessage(ActivityMafia.chatPrefix + "Error: You must specify a target");
				return;
			}
			
			// Find the target of the action
			String target = args[1];
			Player targetPlayer = Bukkit.getPlayer(target);
			if (targetPlayer == null)
			{
				gamer.player.sendMessage(ActivityMafia.chatPrefix + "Error: No such player!");
				return;
			}
			else
			{
				// Try to get the 'gamer' object for the target
				targetGamer = this.getGamerByPlayer(targetPlayer);
				
				if (targetGamer == null)
				{
					gamer.player.sendMessage(ActivityMafia.chatPrefix + "Error: That player isn't in the game!");
					return;
				}
				
				// make sure the target is alive...
				if (!(targetGamer.isAlive()))
				{
					gamer.player.sendMessage(ActivityMafia.chatPrefix + "Error: That player is dead!");
					return;
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
				
				// Check to see if the action cmd string they have sent
				// is valid for their character. This returns the action object
				// itself.
				
				CharacterAction action = gamer.character.getAction(cmd);
				
				if (action == null)
				{
					gamer.player.sendMessage(ActivityMafia.chatPrefix + "Your character cannot perform that action");
					return;		
				}
				else
				{
					// Are they targetting themselves?
					if (gamer == targetGamer)
					{
						if (!action.canTargetSelf)
						{
							gamer.player.sendMessage(ActivityMafia.chatPrefix + "Your cannot undertaken that action against yourself! Try another target.");
							return;								
						}
					}
					
					gamer.requestAction(action, targetGamer, actionMessage);
					this.checkIfNightIsOver();
					return;
				}
				
			}
			else
			{
				gamer.player.sendMessage(ActivityMafia.chatPrefix + "Error: You've already performed your action for tonight.");
				return;
			}
		}
		else
		{
			gamer.player.sendMessage(ActivityMafia.chatPrefix + "Error: You can only perform actions at night!");
			return;
			
		}
	}	
	
	/***************************************************************************************************************************************************/
	
	
	public void startDiscussion()
	{	
		getLogger().info(":: startDiscussion");
		this.gameStage = GameStage.DISCUSS;
		this.messageAllPlayers("You now have a few moments to discuss what to do next");
		this.discussTimeout = new DiscussionTimeout(this).runTaskLater(this, this.discussLength);
		
		/* Prepare for accusations */
		this.accuseMap         = new HashMap<Gamer, Gamer>();
		this.accusationCounter = new HashMap<Gamer, Integer>();
		
		for (Gamer gamer : this.gamers)
		{
			if (gamer.isAlive())
			{
				this.accusationCounter.put(gamer, new Integer(0));
			}
		}			
	}
	
	public void startNominations()
	{
		getLogger().info(":: startNominations");
		
		if (this.gameStage == GameStage.DISCUSS)
		{
			this.discussTimeout = null;
			this.gameStage = GameStage.NOMINATE;
			this.messageAllPlayers("You may now nominate gamers to be put to death with /z accuse <name>");
			this.nominateTimeout = new NominationTimeout(this).runTaskLater(this, this.nominateLength);
		}
	}
	
	public void onPlayerAccuse(Gamer gamer, String[] args)
	{
		getLogger().info(":: onPlayerAccuse");
		
		if (this.gameStage == GameStage.NOMINATE)
		{
			// Declare variables
			Gamer targetGamer = null;
			
			// Find the target of the action
			String target = args[1];
			Player targetPlayer = Bukkit.getPlayer(target);
			if (targetPlayer == null)
			{
				gamer.player.sendMessage(ActivityMafia.chatPrefix + "Error: No such player!");
				return;
			}
			else
			{
				// Try to get the 'gamer' object for the target
				targetGamer = this.getGamerByPlayer(targetPlayer);
				
				if (targetGamer == null)
				{
					gamer.player.sendMessage(ActivityMafia.chatPrefix + "Error: That player isn't in the game!");
					return;
				}
				
				// make sure the target is alive...
				if (!(targetGamer.isAlive()))
				{
					gamer.player.sendMessage(ActivityMafia.chatPrefix + "Error: That player is dead!");
					return;
				}
			}	
			
			// Has this player already voted?
			Gamer previousTargetGamer = this.accuseMap.get(gamer);
			
			if (previousTargetGamer != null)
			{
				// They've already voted so change the vote if they have changed the target
				if (previousTargetGamer != targetGamer)
				{
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
					// their vote target didnt change so return
					return;
				}
			}
			
			
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
		getLogger().info(":: endNominations");
		
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
					if (topGamer.isAlive())
					{
						this.startVote(topGamer);
						return;
					}
					else
					{
						this.startNight();						
					}
				}
			}
			
			this.messageAllPlayers("The citizens could not make a decision!");
			this.startNight();
			
		}
	}
	
	public void startVote(Gamer gamer)
	{
		getLogger().info(":: startVote");
		
		if (this.gameStage == GameStage.END_NOMINATE)
		{
			this.objective.setDisplayName("Put to death?");
				
			this.voteMap     = new HashMap<Gamer, Boolean>();
			this.voteYes     = 0;
			this.voteNo      = 0;
			this.chosenGamer = gamer;
				
			for (Gamer cgamer : this.gamers)
			{
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
		getLogger().info(":: onPlayerVote");
		
		if (this.gameStage == GameStage.VOTE)
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
	}
	
	public void endVote()
	{	
		getLogger().info(":: endVote");
		
		// update game stage
		this.gameStage = GameStage.END_VOTE;
		
		// Wipe the object variable for the chosen gamer
		Gamer targetGamer = this.chosenGamer;
		this.chosenGamer = null;
		
		// restore previous scoreboard
		this.objective.setDisplayName("Players alive");
		this.board.resetScores("yes");
		this.board.resetScores("no");
		
		
		for (Gamer cgamer : this.gamers)
		{
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
			this.killPlayer(targetGamer);
		
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
			this.messageAllPlayers("The citizens chose not to kill " + targetGamer.player.getName());
			this.startNight();
		}
	}
}