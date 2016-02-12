package io.evad.mineactivity.mafia.actions;

import io.evad.mineactivity.mafia.ActivityMafia;
import io.evad.mineactivity.mafia.Gamer;

public abstract class CharacterAction
{
	public String name = "";
	public String[] cmds = null;
	public boolean canTargetSelf = true;
	
	public abstract void doActionRequest(Gamer gamer, Gamer targetGamer);
	public abstract void doAction(Gamer gamer, Gamer targetGamer, String actionMessage, ActivityMafia mafia);
}
