package io.evad.mineactivity.mafia.enums;

/**
 * @author db2z07
 *
 */
public enum GameStage
{
	NONE, /* no game is running */														/* DONE */
	REGISTER, /* registration phase */													/* DONE */
	PRE_GAME, /* assigns roles and sets up the game */									/* DONE */
	
	NIGHT, 			/* night time is when gamers may do actions */								/* DONE */
	END_NIGHT, 		/* game processes what happened overnight ISGAMEOVER */					/* DONE */
	
	DISCUSS,         /* gamers discuss who is evil and whatnot */ /* DONE */
	END_DISCUSS,    /* game sets up nominations */ /* DONE */

	NOMINATE,        /* gamers may nominate others to be put on the the cross */ /* DONE */
	END_NOMINATE,   /* game calculates if a vote will happen */        /* DONE */

	VOTE,            /* gamers may vote yes or no (optional phase) */ /* NOT YET IMPLEMENTED */
	END_VOTE,       /* votes are tallied (optional phase) ISGAMEOVER*/ /* NOT YET IMPLEMENTED */

	END_GAME,         /* the game ending is processed */ /* NOT YET USED...as we dont work out points or whatever */
}